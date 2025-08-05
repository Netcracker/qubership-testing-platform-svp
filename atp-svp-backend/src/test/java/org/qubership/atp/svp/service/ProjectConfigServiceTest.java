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

package org.qubership.atp.svp.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.core.exceptions.ProjectConfigException;
import org.qubership.atp.svp.core.exceptions.project.ProjectNotFoundException;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.environments.Project;
import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.model.project.ProjectRepositoryConfig;
import org.qubership.atp.svp.model.ui.ProjectConfigResponseModel;
import org.qubership.atp.svp.repo.impl.AuthTokenProvider;
import org.qubership.atp.svp.repo.impl.EnvironmentRepository;
import org.qubership.atp.svp.repo.impl.FilePageConfigurationRepository;
import org.qubership.atp.svp.repo.impl.GitPageConfigurationRepository;
import org.qubership.atp.svp.service.direct.IntegrationServiceImpl;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.qubership.atp.svp.tests.DbMockEntity;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ProjectConfigService.class)
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class ProjectConfigServiceTest extends TestWithTestData {

    @SpyBean
    ProjectConfigService projectConfigService;
    @MockBean
    FolderServiceJpa folderServiceJpa;
    @MockBean
    IntegrationServiceImpl integrationService;
    @MockBean
    Provider<UserInfo> provider;
    @MockBean
    AuthTokenProvider authTokenProvider;
    @MockBean
    ProjectConfigurationServiceJpa projectConfigurationServiceJpa;
    @MockBean
    EnvironmentRepository environmentRepository;
    @MockBean
    FilePageConfigurationRepository filePageConfigurationRepository;
    @MockBean
    GitPageConfigurationRepository gitPageConfigurationRepository;

    @Test
    public void projectConfigService_getProjectConfig_defaultId_returnsDefaultConfig()
            throws ProjectConfigException {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        ProjectConfigsEntity projectConfigsEntity = getTypeGitProjectConfigsEntity(projectId,
                "src/test/config/project/test",
                "mockUserName");
        Mockito.doReturn(projectConfigsEntity).when(projectConfigurationServiceJpa)
                .findProjectConfigById(projectId);

        ProjectConfigsEntity config = projectConfigService.getProjectConfig(projectId);

        assertEquals(projectConfigsEntity, config);
    }

    @Test()
    public void getAllTypesProjectsConfigs_fieldsIsFillingTypeLOCAL_returnProjectConfigurations() throws ProjectConfigException {
        UUID projectId = UUID.randomUUID();
        String projectName = "TestProject";
        String pathFolderLocalProject = "src/test/config/project/" + projectId;
        String testUserName = "testUserName";
        int defLCTime = 86400;
        boolean isFullInfoNeededInPot = true;
        RepositoryType pagesSourceType = RepositoryType.LOCAL;

        ProjectConfigsEntity projectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                pathFolderLocalProject, null, testUserName, defLCTime, isFullInfoNeededInPot, pagesSourceType);
        List<ProjectConfigsEntity> projectConfigsEntities = Collections.singletonList(projectConfigsEntity);
        Mockito.doReturn(projectConfigsEntities).when(projectConfigurationServiceJpa).getAllProjectConfigs();

        ProjectRepositoryConfig projectRepositoryConfig = new ProjectRepositoryConfig(pathFolderLocalProject);
        ProjectConfiguration expectedProjectConfig = new ProjectConfiguration(projectId, projectName,
                isFullInfoNeededInPot, defLCTime, projectRepositoryConfig);

        List<ProjectConfiguration> actualProjectsConfigs = projectConfigService.getAllTypesProjectsConfigs();

        assertEquals(Collections.singletonList(expectedProjectConfig), actualProjectsConfigs);
    }

    @Test()
    public void getAllTypesProjectsConfigs_fieldsIsFillingTypeGITPathFolderIsNull_returnProjectConfigurations()
            throws ProjectConfigException {
        UUID projectId = UUID.randomUUID();
        String projectName = "TestProject";
        String gitUrl = "https://github.com/qubership/network-config-templates";
        String pathFolderLocalProject = "src/test/config/project/" + projectId;
        String testUserName = "testUserName";
        int defLCTime = 86400;
        boolean isFullInfoNeededInPot = true;
        RepositoryType pagesSourceType = RepositoryType.GIT;

        ProjectConfigsEntity projectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                null, gitUrl, testUserName, defLCTime, isFullInfoNeededInPot, pagesSourceType);
        List<ProjectConfigsEntity> projectConfigsEntities = Collections.singletonList(projectConfigsEntity);
        Mockito.doReturn(projectConfigsEntities).when(projectConfigurationServiceJpa).getAllProjectConfigs();

        ProjectRepositoryConfig projectRepositoryConfig = new ProjectRepositoryConfig(gitUrl, pathFolderLocalProject);
        ProjectConfiguration expectedProjectConfig = new ProjectConfiguration(projectId, projectName,
                isFullInfoNeededInPot, defLCTime, projectRepositoryConfig);

        List<ProjectConfiguration> actualProjectsConfigs = projectConfigService.getAllTypesProjectsConfigs();

        assertEquals(Collections.singletonList(expectedProjectConfig), actualProjectsConfigs);
    }

    @Test()
    public void getProjectConfigsByTypeGit_fieldsIsFillingTypeGIT_returnProjectConfigurations() throws ProjectConfigException {
        UUID projectId = UUID.randomUUID();
        String projectName = "TestProject";
        String gitUrl = "https://github.com/qubership/network-config-templates";
        String testUserName = "testUserName";
        int defLCTime = 86400;
        boolean isFullInfoNeededInPot = true;

        ProjectConfigsEntity projectConfigsEntity = DbMockEntity.getProjectConfigsEntity(projectId, projectName,
                null, gitUrl, testUserName, defLCTime, isFullInfoNeededInPot, RepositoryType.GIT);
        List<ProjectConfigsEntity> projectConfigsEntities = Collections.singletonList(projectConfigsEntity);
        Mockito.doReturn(projectConfigsEntities).when(projectConfigurationServiceJpa).getProjectConfigsByType(RepositoryType.GIT);

        String expectedPathFolderLocalProject = "src/test/config/project/" + projectId;
        ProjectRepositoryConfig projectRepositoryConfig = new ProjectRepositoryConfig(gitUrl, expectedPathFolderLocalProject);
        ProjectConfiguration expectedProjectConfig = new ProjectConfiguration(projectId, projectName,
                isFullInfoNeededInPot, defLCTime, projectRepositoryConfig);

//        List<ProjectConfiguration> actualProjectsConfigs = projectConfigService.getProjectConfigsByTypeGit();

//        assertEquals(Collections.singletonList(expectedProjectConfig), actualProjectsConfigs);
    }

    @Test()
    public void getProjectConfigByIdForResponse_pathIsCorrect_returnProjectConfigResponseModel() {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        ProjectConfigsEntity projectConfigsEntity = getTypeGitProjectConfigsEntity(projectId,
                "src/test/config/project/test",
                "mockUserName");
        ProjectConfigResponseModel expectedConfig = getProjectConfigResponseModel("test project",  false, RepositoryType.GIT);
        Mockito.doReturn(projectConfigsEntity).when(projectConfigurationServiceJpa).findProjectConfigById(projectId);

        ProjectConfigResponseModel projectConfigResponseModel = projectConfigService.getProjectConfigByIdForResponse(projectId);

        assertEquals(expectedConfig, projectConfigResponseModel);
    }

    @Test()
    public void initializeProjectConfigByEnv_existsProjectInDatabase_doNothing() {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        ProjectConfigsEntity projectConfigsEntity = getTypeGitProjectConfigsEntity(projectId,
                "src/test/config/project/test",
                "mockUserName");
        Mockito.doReturn(projectConfigsEntity).when(projectConfigurationServiceJpa)
                .findProjectConfigById(projectId);

        projectConfigService.initializeProjectConfigByEnv(projectId);

        Mockito.verify(projectConfigService).getProjectConfig(projectId);
        Mockito.verify(integrationService, Mockito.never()).getProjectFromEnvironments(projectId);
    }

    @Test()
    public void initializeProjectConfigByEnv_notExistsProjectInDatabase_creatInDBProjectConfigAndDefaultFolder() {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "TestProject";
        Project project = new Project();
        project.setName(projectName);
        Mockito.when(projectConfigurationServiceJpa.findProjectConfigById(any())).thenThrow(ProjectNotFoundException.class);
        Mockito.doReturn(project).when(integrationService).getProjectFromEnvironments(projectId);
        Mockito.doNothing().when(projectConfigurationServiceJpa).saveProjectConfig(any());

        projectConfigService.initializeProjectConfigByEnv(projectId);

        Mockito.verify(projectConfigService).getProjectConfig(projectId);
        Mockito.verify(integrationService).getProjectFromEnvironments(projectId);
        Mockito.verify(folderServiceJpa, Mockito.times(1)).create(any(), eq("Default"));
    }

    @Test(expected = ProjectConfigException.class)
    public void initializeProjectConfigByEnv_errorFindProjectConfigById_ThrowProjectConfigException() {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "TestProject";
        Project project = new Project();
        project.setName(projectName);
        Mockito.doThrow(SQLWarningException.class).when(projectConfigurationServiceJpa).findProjectConfigById(any());

        projectConfigService.initializeProjectConfigByEnv(projectId);
    }

    @Test()
    public void createProjectConfigDb_topicFalse_returnProjectConfigsEntityAndSaveToDB() {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        String testUserName = "INIT_PROJECT_NOT_AUTHORIZED";
        ProjectConfigsEntity expectedProjectConfigsEntity = getTypeLocalProjectConfigsEntity(projectId,
                "src/test/config/project/" + projectId, testUserName);
        Mockito.doNothing().when(projectConfigurationServiceJpa).saveProjectConfig(any());

        ProjectConfigsEntity actualProjectConfigsEntity = projectConfigService.createProjectConfigDb(projectName,
                projectId,
                false);

        assertEquals(convertToStr(expectedProjectConfigsEntity), convertToStr(actualProjectConfigsEntity));
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1)).saveProjectConfig(actualProjectConfigsEntity);
    }

    @Test()
    public void createProjectConfigDb_topicTrue_returnProjectConfigsEntityAndSaveToDB() {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        String testUserName = "INIT_PROJECT_CATALOG_NOTIFICATION_TOPIC";
        ProjectConfigsEntity expectedProjectConfigsEntity = getTypeLocalProjectConfigsEntity(projectId,
                "src/test/config/project/" + projectId, testUserName);
        Mockito.doNothing().when(projectConfigurationServiceJpa).saveProjectConfig(any());

        ProjectConfigsEntity actualProjectConfigsEntity = projectConfigService.createProjectConfigDb(projectName,
                projectId,
                true);

        assertEquals(convertToStr(expectedProjectConfigsEntity), convertToStr(actualProjectConfigsEntity));
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1)).saveProjectConfig(actualProjectConfigsEntity);
    }

    @Test(expected = ProjectConfigException.class)
    public void createProjectConfigDb_errorWhile_ThrowProjectConfigException() {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        String projectName = "test project";
        Mockito.doThrow(SQLWarningException.class).when(projectConfigurationServiceJpa).saveProjectConfig(any());

        projectConfigService.createProjectConfigDb(projectName, projectId, true);
    }

    @Test()
    public void deleteProjectConfigEntity_projectId_calledRemoveProjectConfig() {
        UUID projectId = UUID.randomUUID();

        projectConfigService.deleteProjectConfigEntity(projectId);

        Mockito.doNothing().when(projectConfigurationServiceJpa).removeProjectConfig(any());
        Mockito.verify(projectConfigurationServiceJpa, Mockito.times(1)).removeProjectConfig(projectId);
    }

    private ProjectConfigsEntity getTypeGitProjectConfigsEntity(UUID projectId, String pathFolder,
                                                                String lastUserName) {
        return DbMockEntity.getProjectConfigsEntity(projectId, "test project", pathFolder,
                "http://GitUrl", lastUserName, 86400, true, RepositoryType.GIT);
    }

    private ProjectConfigsEntity getTypeLocalProjectConfigsEntity(UUID projectId, String pathFolder,
                                                                  String lastUserName) {
        return DbMockEntity.getProjectConfigsEntity(projectId, "test project", pathFolder, null,
                lastUserName, 86400, false, RepositoryType.LOCAL);
    }

    private ProjectConfiguration getProjectConfigurationWithPath(UUID projectId) {
        ProjectRepositoryConfig repoConfig = new ProjectRepositoryConfig("http://GitUrl",
                "src/test/config/project/" + projectId);
        return new ProjectConfiguration(projectId,
                "test project",
                true,
                86400,
                repoConfig);
    }
}
