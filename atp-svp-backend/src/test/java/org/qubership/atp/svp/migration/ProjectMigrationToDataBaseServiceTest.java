/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is provided "AS IS", without warranties
 * or conditions of any kind, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.atp.svp.migration;

import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.core.exceptions.CommonParametersStorageException;
import org.qubership.atp.svp.core.exceptions.FolderStorageException;
import org.qubership.atp.svp.core.exceptions.KeyParametersStorageException;
import org.qubership.atp.svp.core.exceptions.PageStorageException;
import org.qubership.atp.svp.core.exceptions.ProjectConfigException;
import org.qubership.atp.svp.core.exceptions.git.GitCloneException;
import org.qubership.atp.svp.core.exceptions.migration.MigrationException;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.model.ui.ProjectConfigResponseModel;
import org.qubership.atp.svp.repo.impl.AuthTokenProvider;
import org.qubership.atp.svp.repo.impl.FilePageConfigurationRepository;
import org.qubership.atp.svp.repo.impl.GitPageConfigurationRepository;
import org.qubership.atp.svp.service.direct.GitProjectServiceImpl;
import org.qubership.atp.svp.service.direct.IntegrationServiceImpl;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.qubership.atp.svp.tests.DbMockEntity;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ProjectMigrationToDataBaseService.class)
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
@Slf4j
public class ProjectMigrationToDataBaseServiceTest extends TestWithTestData {

    @SpyBean
    ProjectMigrationToDataBaseService projectMigrationToDataBaseService;
    @SpyBean
    FilePageConfigurationRepository filePageConfigRepository;
    @SpyBean
    ProjectConfigService projectConfigService;
    @MockBean
    FolderServiceJpa folderServiceJpa;
    @MockBean
    IntegrationServiceImpl integrationService;
    @MockBean
    ProjectConfigurationServiceJpa projectConfigurationServiceJpa;
    @MockBean
    GitProjectServiceImpl gitProjectServiceImpl;
    @MockBean
    LockManager lockManager;
    @MockBean
    Provider<UserInfo> provider;
    @MockBean
    AuthTokenProvider authTokenProvider;
    @SpyBean
    ObjectMapper objectMapper;
    @MockBean
    GitPageConfigurationRepository gitPageConfigurationRepository;

    @Before
    public void CreateFolder() throws IOException {
        String path = "src/test/config/project/323eda51-47b5-414a-951a-27221fa374a2/pages";
        String path2 = "src/test/config/project/nonExistentDir/pages";
        String path3 = "src/test/config/project/pageNotExist/pages";
        Files.createDirectories(Paths.get(path));
        Files.createDirectories(Paths.get(path2));
        Files.createDirectories(Paths.get(path3));
    }

    @Test()
    public void updateProjectConfigAndMigrate_ChangeUrlGitWithTypeGitAndProjectExistInDb_configUpdateInDBAndReloadAndMigrateGitToDB() throws ProjectConfigException {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        RepositoryType sourceType = RepositoryType.GIT;
        String pathFolderLocalProject = "src/test/config/project/";
        String lastUserName = "NOT_AUTHORIZED";
        String gitUrl = "http://GitUrl";
        ProjectConfigResponseModel projectConfigResponseModel = getProjectConfigResponseModel(projectName, false,
                sourceType);
        ProjectConfigsEntity projectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                pathFolderLocalProject + projectName, gitUrl + "_Old", lastUserName, 86400, true, sourceType);
        Mockito.doReturn(projectConfigsEntity).when(projectConfigurationServiceJpa).findProjectConfigById(projectId);
        List<String> folders = Collections.singletonList("Default");
        Mockito.doReturn(folders).when(filePageConfigRepository).getFolders(any());
        ProjectConfigsEntity expectedProjectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                pathFolderLocalProject + projectId, gitUrl,
                lastUserName, 86400, true, sourceType);
        Mockito.doNothing().when(gitProjectServiceImpl).reloadGitProject(any());

        projectMigrationToDataBaseService.updateProjectConfigAndMigrate(projectConfigResponseModel, projectId);

        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1)).findProjectConfigById(projectId);
        ArgumentCaptor<ProjectConfigsEntity> captorProjectConfigsEntity =
                ArgumentCaptor.forClass(ProjectConfigsEntity.class);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1))
                .saveProjectConfig(captorProjectConfigsEntity.capture());
        Assert.assertEquals(convertToStr(expectedProjectConfigsEntity),
                convertToStr(captorProjectConfigsEntity.getValue()));
        Mockito.verify(gitProjectServiceImpl, Mockito.times(1)).reloadGitProject(any(ProjectConfiguration.class));
        Mockito.verify(folderServiceJpa, Mockito.times(1)).saveAll(any());
    }

    @Test()
    public void updateProjectConfigAndMigrate_ChangeFlagPotTrueWithTypeGitAndProjectExistInDb_configUpdateInDBAndNotReloadMigrate() throws ProjectConfigException{
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        RepositoryType sourceType = RepositoryType.GIT;
        String pathFolderLocalProject = "src/test/config/project/";
        String lastUserName = "NOT_AUTHORIZED";
        String gitUrl = "http://GitUrl";
        boolean isFullInfoNeededInPot = true;
        ProjectConfigResponseModel projectConfigResponseModel = getProjectConfigResponseModel(projectName, false,
                sourceType);
        ProjectConfigsEntity projectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                pathFolderLocalProject + projectName, gitUrl, lastUserName, 86400,
                false, sourceType);
        Mockito.doReturn(projectConfigsEntity).when(projectConfigurationServiceJpa).findProjectConfigById(projectId);
        Mockito.doReturn(Collections.singletonList("Default")).when(filePageConfigRepository).getFolders(any());
        ProjectConfigsEntity expectedProjectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                pathFolderLocalProject + projectId, gitUrl,
                lastUserName, 86400, isFullInfoNeededInPot, sourceType);
        Mockito.doNothing().when(gitProjectServiceImpl).reloadGitProject(any());

        projectMigrationToDataBaseService.updateProjectConfigAndMigrate(projectConfigResponseModel, projectId);

        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1)).findProjectConfigById(projectId);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.never()).removeProjectConfig(projectId);
        ArgumentCaptor<ProjectConfigsEntity> captorProjectConfigsEntity =
                ArgumentCaptor.forClass(ProjectConfigsEntity.class);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1))
                .saveProjectConfig(captorProjectConfigsEntity.capture());
        Assert.assertEquals(convertToStr(expectedProjectConfigsEntity),
                convertToStr(captorProjectConfigsEntity.getValue()));
        Mockito.verify(gitProjectServiceImpl, Mockito.never()).reloadGitProject(any(ProjectConfiguration.class));
        Mockito.verify(folderServiceJpa, Mockito.never()).saveAll(any());
    }

    @Test()
    public void updateProjectConfigAndMigrate_typeGitWithProjectExistInDbAndFlagIsMigrateTrue_configUpdateInDBAndReloadAndMigrateGitToDB() throws ProjectConfigException{
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        RepositoryType sourceType = RepositoryType.GIT;
        String pathFolderLocalProject = "src/test/config/project/";
        String lastUserName = "NOT_AUTHORIZED";
        String gitUrl = "http://GitUrl";
        ProjectConfigResponseModel projectConfigResponseModel = getProjectConfigResponseModel(projectName, true,
                sourceType);
        ProjectConfigsEntity projectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                pathFolderLocalProject + projectName, gitUrl, lastUserName, 86400, true, sourceType);
        Mockito.doReturn(projectConfigsEntity).when(projectConfigurationServiceJpa).findProjectConfigById(projectId);
        Mockito.doReturn(Collections.singletonList("Default")).when(filePageConfigRepository).getFolders(any());
        ProjectConfigsEntity expectedProjectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                pathFolderLocalProject + projectId, gitUrl,
                lastUserName, 86400, true, sourceType);
        Mockito.doNothing().when(gitProjectServiceImpl).reloadGitProject(any());

        projectMigrationToDataBaseService.updateProjectConfigAndMigrate(projectConfigResponseModel, projectId);

        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1)).findProjectConfigById(projectId);
        ArgumentCaptor<ProjectConfigsEntity> captorProjectConfigsEntity =
                ArgumentCaptor.forClass(ProjectConfigsEntity.class);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1))
                .saveProjectConfig(captorProjectConfigsEntity.capture());
        Assert.assertEquals(convertToStr(expectedProjectConfigsEntity),
                convertToStr(captorProjectConfigsEntity.getValue()));
        Mockito.verify(gitProjectServiceImpl, Mockito.times(1)).reloadGitProject(any(ProjectConfiguration.class));
        Mockito.verify(folderServiceJpa, Mockito.times(1)).saveAll(any());
    }

    @Test()
    public void updateProjectConfigAndMigrate_typeGitWithProjectNotExistInDb_configCreateInDBAndReloadAndMigrateGitToDB() throws ProjectConfigException{
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        RepositoryType sourceType = RepositoryType.GIT;
        String pathFolderLocalProject = "src/test/config/project/";
        String lastUserName = "NOT_AUTHORIZED";
        String gitUrl = "http://GitUrl";
        ProjectConfigResponseModel projectConfigResponseModel = getProjectConfigResponseModel(projectName, false,
                sourceType);
        Mockito.doReturn(null).when(projectConfigurationServiceJpa).findProjectConfigById(projectId);
        Mockito.doReturn(Collections.singletonList("Default")).when(filePageConfigRepository).getFolders(any());
        ProjectConfigsEntity expectedProjectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                pathFolderLocalProject + projectId, gitUrl, lastUserName, 86400, true, sourceType);
        Mockito.doNothing().when(gitProjectServiceImpl).reloadGitProject(any());

        projectMigrationToDataBaseService.updateProjectConfigAndMigrate(projectConfigResponseModel, projectId);

        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1)).findProjectConfigById(projectId);
        ArgumentCaptor<ProjectConfigsEntity> captorProjectConfigsEntity =
                ArgumentCaptor.forClass(ProjectConfigsEntity.class);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1))
                .saveProjectConfig(captorProjectConfigsEntity.capture());
        Assert.assertEquals(convertToStr(expectedProjectConfigsEntity),
                convertToStr(captorProjectConfigsEntity.getValue()));
        Mockito.verify(gitProjectServiceImpl, Mockito.times(1)).reloadGitProject(any(ProjectConfiguration.class));
        Mockito.verify(folderServiceJpa, Mockito.times(1)).saveAll(any());
    }

    @Test()
    public void updateProjectConfigAndMigrate_typeLocalWithProjectExistInDbAndPathFolderIsFilled_configUpdateInDBAndNotMigrate() throws ProjectConfigException{
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        RepositoryType sourceType = RepositoryType.LOCAL;
        String pathFolderLocalProject = "src/test/config/project/" + projectName;
        String lastUserName = "NOT_AUTHORIZED";
        String gitUrl = "http://GitUrl";
        ProjectConfigResponseModel projectConfigResponseModel = getProjectConfigResponseModel(projectName, false,
                sourceType);
        ProjectConfigsEntity projectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                pathFolderLocalProject, gitUrl, lastUserName, 86400, true, sourceType);
        Mockito.doReturn(projectConfigsEntity).when(projectConfigurationServiceJpa).findProjectConfigById(projectId);
        ProjectConfigsEntity expectedProjectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                pathFolderLocalProject, gitUrl, lastUserName, 86400, true, sourceType);

        projectMigrationToDataBaseService.updateProjectConfigAndMigrate(projectConfigResponseModel, projectId);

        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1)).findProjectConfigById(projectId);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.never()).removeProjectConfig(projectId);
        ArgumentCaptor<ProjectConfigsEntity> captorProjectConfigsEntity =
                ArgumentCaptor.forClass(ProjectConfigsEntity.class);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1))
                .saveProjectConfig(captorProjectConfigsEntity.capture());
        Assert.assertEquals(convertToStr(expectedProjectConfigsEntity),
                convertToStr(captorProjectConfigsEntity.getValue()));
        Mockito.verify(gitProjectServiceImpl, Mockito.never()).reloadGitProject(any(ProjectConfiguration.class));
        Mockito.verify(folderServiceJpa, Mockito.never()).saveAll(any());
    }

    @Test()
    public void updateProjectConfigAndMigrate_typeLocalWithProjectExistInDbAndPathFolderIsNull_configUpdateInDBAndCalculateNewPathNotMigrate() throws ProjectConfigException{
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        RepositoryType sourceType = RepositoryType.LOCAL;
        String pathFolderLocalProject = null;
        String lastUserName = "NOT_AUTHORIZED";
        String gitUrl = "http://GitUrl";
        ProjectConfigResponseModel projectConfigResponseModel = getProjectConfigResponseModel(projectName, false,
                sourceType);
        ProjectConfigsEntity projectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                pathFolderLocalProject, gitUrl, lastUserName, 86400, true, sourceType);
        Mockito.doReturn(projectConfigsEntity).when(projectConfigurationServiceJpa).findProjectConfigById(projectId);
        ProjectConfigsEntity expectedProjectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                "src/test/config/project/" + projectId, gitUrl, lastUserName, 86400, true, sourceType);

        projectMigrationToDataBaseService.updateProjectConfigAndMigrate(projectConfigResponseModel, projectId);

        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1)).findProjectConfigById(projectId);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.never()).removeProjectConfig(projectId);
        ArgumentCaptor<ProjectConfigsEntity> captorProjectConfigsEntity =
                ArgumentCaptor.forClass(ProjectConfigsEntity.class);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1))
                .saveProjectConfig(captorProjectConfigsEntity.capture());
        Assert.assertEquals(convertToStr(expectedProjectConfigsEntity),
                convertToStr(captorProjectConfigsEntity.getValue()));
        Mockito.verify(gitProjectServiceImpl, Mockito.never()).reloadGitProject(any(ProjectConfiguration.class));
        Mockito.verify(folderServiceJpa, Mockito.never()).saveAll(any());
    }

    @Test()
    public void updateProjectConfigAndMigrate_typeLocalWithProjectNotExistInDbAndPathFolderIsNull_configUpdateInDBAndCalculateNewPathAndMigrateToDb() throws ProjectConfigException{
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        RepositoryType sourceType = RepositoryType.LOCAL;
        String lastUserName = "NOT_AUTHORIZED";
        String gitUrl = "http://GitUrl";
        ProjectConfigResponseModel projectConfigResponseModel = getProjectConfigResponseModel(projectName, false,
                sourceType);
        Mockito.doReturn(null).when(projectConfigurationServiceJpa).findProjectConfigById(projectId);
        Mockito.doReturn(Collections.singletonList("Default")).when(filePageConfigRepository).getFolders(any());
        ProjectConfigsEntity expectedProjectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                "src/test/config/project/" + projectId, gitUrl, lastUserName, 86400, true, sourceType);

        projectMigrationToDataBaseService.updateProjectConfigAndMigrate(projectConfigResponseModel, projectId);

        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1)).findProjectConfigById(projectId);
        ArgumentCaptor<ProjectConfigsEntity> captorProjectConfigsEntity =
                ArgumentCaptor.forClass(ProjectConfigsEntity.class);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1))
                .saveProjectConfig(captorProjectConfigsEntity.capture());
        Assert.assertEquals(convertToStr(expectedProjectConfigsEntity),
                convertToStr(captorProjectConfigsEntity.getValue()));
        Mockito.verify(gitProjectServiceImpl, Mockito.never()).reloadGitProject(any(ProjectConfiguration.class));
        Mockito.verify(folderServiceJpa, Mockito.times(1)).saveAll(any());
    }

    @Test()
    public void updateProjectConfigAndMigrate_typeLocalWithProjectNotExistInDbAndFlagIsMigrateTrue_configUpdateInDBAndCalculateNewPathAndMigrateToDb
            () throws ProjectConfigException {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        RepositoryType sourceType = RepositoryType.LOCAL;
        String lastUserName = "NOT_AUTHORIZED";
        String gitUrl = "http://GitUrl";
        ProjectConfigResponseModel projectConfigResponseModel = getProjectConfigResponseModel(projectName, true,
                sourceType);
        Mockito.doReturn(null).when(projectConfigurationServiceJpa).findProjectConfigById(projectId);
        Mockito.doReturn(Collections.singletonList("Default")).when(filePageConfigRepository).getFolders(any());
        ProjectConfigsEntity expectedProjectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                "src/test/config/project/" + projectId, gitUrl, lastUserName, 86400, true, sourceType);

        projectMigrationToDataBaseService.updateProjectConfigAndMigrate(projectConfigResponseModel, projectId);

        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1)).findProjectConfigById(projectId);
        ArgumentCaptor<ProjectConfigsEntity> captorProjectConfigsEntity =
                ArgumentCaptor.forClass(ProjectConfigsEntity.class);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1))
                .saveProjectConfig(captorProjectConfigsEntity.capture());
        Assert.assertEquals(convertToStr(expectedProjectConfigsEntity),
                convertToStr(captorProjectConfigsEntity.getValue()));
        Mockito.verify(gitProjectServiceImpl, Mockito.never()).reloadGitProject(any(ProjectConfiguration.class));
        Mockito.verify(folderServiceJpa, Mockito.times(1)).saveAll(any());
    }

    @Test(expected = GitCloneException.class)
    public void updateProjectConfigAndMigrate_errorWhileReloadGitProject_throwMigrationProjectException() throws ProjectConfigException {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        RepositoryType sourceType = RepositoryType.GIT;
        ProjectConfigResponseModel projectConfigResponseModel = getProjectConfigResponseModel(projectName, true,
                sourceType);
        Mockito.doReturn(null).when(projectConfigurationServiceJpa).findProjectConfigById(projectId);
        Mockito.doReturn(Collections.singletonList("Default")).when(filePageConfigRepository).getFolders(any());
        Mockito.doThrow(GitCloneException.class).when(gitProjectServiceImpl).reloadGitProject(any());

        projectMigrationToDataBaseService.updateProjectConfigAndMigrate(projectConfigResponseModel, projectId);
    }

//    @Test(expected = MigrationProjectException.class)
//    public void updateProjectConfigAndMigrate_errorWhileSaveProjectConfig_throwMigrationProjectException() throws ProjectConfigException,
//            MigrationProjectException {
//        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
//        String projectName = "test project";
//        RepositoryType sourceType = RepositoryType.GIT;
//        ProjectConfigResponseModel projectConfigResponseModel = getProjectConfigResponseModel(projectName, true,
//                sourceType);
//        Mockito.doReturn(null).when(projectConfigurationServiceJpa).findProjectConfigById(projectId);
//        Mockito.doReturn(Collections.singletonList("Default")).when(filePageConfigRepository).getFolders(any());
//        Mockito.doThrow(RuntimeException.class).when(projectConfigurationServiceJpa).saveProjectConfig(any());
//
//        projectMigrationToDataBaseService.updateProjectConfigAndMigrate(projectConfigResponseModel, projectId);
//    }

    @Test()
    public void migrateAllProjectConfigsFromJsonFile_ChangeUrlGitWithTypeGitAndProjectExistInDb_configUpdateInDBAndReloadAndMigrateGitToDB()
            throws ProjectConfigException {
        ReflectionTestUtils.setField(projectMigrationToDataBaseService, "fullPathToConfig",
                "src/test/config/project/projects_configs.json");

        UUID projectId1 = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        UUID projectId2 = UUID.fromString("323eda51-47b5-414a-951a-27221fa37333");
        String projectName1 = "test project1";
        String projectName2 = "test project2";
        RepositoryType sourceType = RepositoryType.GIT;
        String pathFolderLocalProject = "src/test/config/project/";
        String lastUserName = "NOT_AUTHORIZED";
        String gitUrl = "http://GitUrl";

        ProjectConfigsEntity projectConfigsEntity1 = DbMockEntity.getProjectConfigsEntity(projectId1, projectName1,
                pathFolderLocalProject + projectName1, gitUrl, lastUserName, 86400,
                true, sourceType);
        ProjectConfigsEntity projectConfigsEntity2 = DbMockEntity.getProjectConfigsEntity(projectId2, projectName2,
                pathFolderLocalProject + projectName2, gitUrl, lastUserName, 86400,
                true, sourceType);
        List<ProjectConfigsEntity> projectConfigsEntities = Arrays.asList(projectConfigsEntity1, projectConfigsEntity2);
        Mockito.doReturn(projectConfigsEntities).when(projectConfigurationServiceJpa).getAllProjectConfigs();
        projectMigrationToDataBaseService.migrateAllProjectConfigsFromJsonFile();
        ArgumentCaptor<List<ProjectConfigsEntity>> captorProjectConfigsEntities = ArgumentCaptor.forClass(List.class);
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1))
                .saveAllProjectsConfigs(captorProjectConfigsEntities.capture());
        Assert.assertEquals(captorProjectConfigsEntities.getValue().size(), 3);
        Assert.assertEquals(captorProjectConfigsEntities.getValue().get(0).getPagesSourceType(), RepositoryType.GIT);
        Assert.assertEquals(captorProjectConfigsEntities.getValue().get(0).getProjectId(),
                UUID.fromString("1183662d-51ae-438a-92be-0a4bb71620d9"));
        Assert.assertEquals(captorProjectConfigsEntities.getValue().get(0).getLastUpdateUserName(),
                "MIGRATION SVP");
        Assert.assertNull(captorProjectConfigsEntities.getValue().get(0).getPathFolderLocalProject());
        Assert.assertEquals(captorProjectConfigsEntities.getValue().get(2).getPagesSourceType(), RepositoryType.LOCAL);
        Assert.assertEquals(captorProjectConfigsEntities.getValue().get(2)
                .getProjectId(), UUID.fromString("e984e5d1-5f89-4113-84a0-b658800e65c4"));
        Assert.assertEquals(captorProjectConfigsEntities.getValue().get(2).getLastUpdateUserName(),
                "MIGRATION SVP");
        Assert.assertEquals(captorProjectConfigsEntities.getValue().get(2)
                .getPathFolderLocalProject(), "src/test/config/project/folder_project");
    }

    @Test(expected = ProjectConfigException.class)
    public void migrateAllProjectConfigsFromJsonFile_errorWhile_configUpdateInDBAndReloadAndMigrateGitToDB()
            throws ProjectConfigException {
        ReflectionTestUtils.setField(projectMigrationToDataBaseService, "fullPathToConfig",
                "src/test/config/project/projects_configs.json");

        UUID projectId1 = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        UUID projectId2 = UUID.fromString("323eda51-47b5-414a-951a-27221fa37333");
        String projectName1 = "test project1";
        String projectName2 = "test project2";
        RepositoryType sourceType = RepositoryType.GIT;
        String pathFolderLocalProject = "src/test/config/project/";
        String lastUserName = "NOT_AUTHORIZED";
        String gitUrl = "http://GitUrl";

        ProjectConfigsEntity projectConfigsEntity1 = DbMockEntity.getProjectConfigsEntity(projectId1, projectName1,
                pathFolderLocalProject + projectName1, gitUrl, lastUserName, 86400,
                true, sourceType);
        ProjectConfigsEntity projectConfigsEntity2 = DbMockEntity.getProjectConfigsEntity(projectId2, projectName2,
                pathFolderLocalProject + projectName2, gitUrl, lastUserName, 86400,
                true, sourceType);
        List<ProjectConfigsEntity> projectConfigsEntities = Arrays.asList(projectConfigsEntity1, projectConfigsEntity2);
        Mockito.doReturn(projectConfigsEntities).when(projectConfigurationServiceJpa).getAllProjectConfigs();
        Mockito.doThrow(RuntimeException.class).when(projectConfigurationServiceJpa).saveAllProjectsConfigs(any());
        projectMigrationToDataBaseService.migrateAllProjectConfigsFromJsonFile();
    }

    @Test()
    public void projectsMigrateToBase_toFoldersTreeObjectsDoableNameCommonParamsAndPreconfigureFlagTrue_successConvert() throws ProjectConfigException {
        UUID projectId = UUID.fromString("e984e5d1-5f89-4113-84a0-b658800e65c4");
        ProjectConfiguration projectConfiguration = getProjectConfiguration(projectId);
        List<ProjectConfiguration> projectConfigurations = Collections.singletonList(projectConfiguration);

        projectMigrationToDataBaseService.projectsJsonMigrateToBase(projectConfigurations);

        ArgumentCaptor<List<FolderEntity>> captorProjectConfigsEntity = ArgumentCaptor.forClass(List.class);
        Mockito.verify(folderServiceJpa, Mockito.times(1)).saveAll(captorProjectConfigsEntity.capture());
        List<FolderEntity> captorProjectConfigsEntityValue = captorProjectConfigsEntity.getValue();
        Assert.assertEquals(captorProjectConfigsEntityValue.size(), 2);

        FolderEntity folderEntityDefaultName;
        FolderEntity folderEntityFolderName;
        if (captorProjectConfigsEntityValue.get(0).getName().equals("Default")) {
            folderEntityDefaultName = captorProjectConfigsEntityValue.get(0);
            folderEntityFolderName = captorProjectConfigsEntityValue.get(1);
        } else {
            folderEntityDefaultName = captorProjectConfigsEntityValue.get(1);
            folderEntityFolderName = captorProjectConfigsEntityValue.get(0);
        }

        Assert.assertEquals(folderEntityDefaultName.getCommonParameters().size(), 0);
        Assert.assertEquals(folderEntityDefaultName.getKeyParameterEntities().size(), 0);
        Assert.assertEquals(folderEntityDefaultName.getPages().size(), 4);
        Assert.assertEquals(folderEntityFolderName.getCommonParameters().size(), 2);
        Assert.assertFalse(folderEntityFolderName.getCommonParameters().get(0).getSutParameterEntity()
                .isPreconfigured());
        Assert.assertEquals(folderEntityFolderName.getCommonParameters().get(0).getSutParameterEntity()
                .getName(), "Param");
        Assert.assertFalse(folderEntityFolderName.getCommonParameters().get(1).getSutParameterEntity()
                .isPreconfigured());
        Assert.assertEquals(folderEntityFolderName.getCommonParameters().get(1).getSutParameterEntity()
                .getName(), "Param_duplicateName");
        Assert.assertEquals(folderEntityFolderName.getKeyParameterEntities().size(), 3);
        Assert.assertEquals(folderEntityFolderName.getPages().size(), 1);
    }

//    @Test(expected = MigrationProjectException.class)
//    public void projectsMigrateToBase_errorWhileFolderSaveAll_throwMigrationProjectException() throws ProjectConfigException,
//            MigrationProjectException {
//        UUID projectId = UUID.fromString("e984e5d1-5f89-4113-84a0-b658800e65c4");
//        ProjectConfiguration projectConfiguration = getProjectConfiguration(projectId);
//        List<ProjectConfiguration> projectConfigurations = Collections.singletonList(projectConfiguration);
//        Mockito.doThrow(QueryTimeoutException.class).when(folderServiceJpa).saveAll(any());
//
//        projectMigrationToDataBaseService.projectsJsonMigrateToBase(projectConfigurations);
//    }

    @Test(expected = MigrationException.class)
    public void projectsMigrateToBase_errorWhileGetFolders_throwFolderStorageException() {
        UUID projectId = UUID.fromString("e984e5d1-5f89-4113-84a0-b658800e65c4");
        ProjectConfiguration projectConfiguration = getProjectConfiguration(projectId);
        List<ProjectConfiguration> projectConfigurations = Collections.singletonList(projectConfiguration);
        Mockito.doThrow(FolderStorageException.class).when(filePageConfigRepository).getFolders(any());

        projectMigrationToDataBaseService.projectsJsonMigrateToBase(projectConfigurations);
    }

    @Test(expected = MigrationException.class)
    public void projectsMigrateToBase_errorWhileGetCommonParameters_throwCommonParametersStorageException() {
        UUID projectId = UUID.fromString("e984e5d1-5f89-4113-84a0-b658800e65c4");
        ProjectConfiguration projectConfiguration = getProjectConfiguration(projectId);
        List<ProjectConfiguration> projectConfigurations = Collections.singletonList(projectConfiguration);
        Mockito.doThrow(CommonParametersStorageException.class).when(filePageConfigRepository)
                .getCommonParameters(any(), any());

        projectMigrationToDataBaseService.projectsJsonMigrateToBase(projectConfigurations);
    }

    @Test(expected = MigrationException.class)
    public void projectsMigrateToBase_errorWhileGetKeyParameters_throwKeyParametersStorageException() {
        UUID projectId = UUID.fromString("e984e5d1-5f89-4113-84a0-b658800e65c4");
        ProjectConfiguration projectConfiguration = getProjectConfiguration(projectId);
        List<ProjectConfiguration> projectConfigurations = Collections.singletonList(projectConfiguration);
        Mockito.doThrow(KeyParametersStorageException.class).when(filePageConfigRepository)
                .getKeyParameters(any(), any());

        projectMigrationToDataBaseService.projectsJsonMigrateToBase(projectConfigurations);
    }

    @Test(expected = MigrationException.class)
    public void projectsMigrateToBase_errorWhileGetPageConfigurations_throwPageStorageException() {
        UUID projectId = UUID.fromString("e984e5d1-5f89-4113-84a0-b658800e65c4");
        ProjectConfiguration projectConfiguration = getProjectConfiguration(projectId);
        List<ProjectConfiguration> projectConfigurations = Collections.singletonList(projectConfiguration);
        Mockito.doThrow(PageStorageException.class).when(filePageConfigRepository).getPageConfigurations(any(), any());

        projectMigrationToDataBaseService.projectsJsonMigrateToBase(projectConfigurations);
    }
}
