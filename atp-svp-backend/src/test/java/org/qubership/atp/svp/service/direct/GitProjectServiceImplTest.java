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

package org.qubership.atp.svp.service.direct;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.core.exceptions.git.GitCloneException;
import org.qubership.atp.svp.migration.ProjectMigrationToDataBaseService;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.repo.impl.AuthTokenProvider;
import org.qubership.atp.svp.repo.impl.FilePageConfigurationRepository;
import org.qubership.atp.svp.repo.impl.GitPageConfigurationRepository;
import org.qubership.atp.svp.repo.impl.GitRepositoryImpl;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = GitProjectServiceImpl.class)
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class GitProjectServiceImplTest {

    @MockBean
    GitRepositoryImpl gitRepo;

    @MockBean
    ProjectConfigurationServiceJpa projectConfigurationServiceJpa;

    @Autowired
    GitProjectServiceImpl gitProjectService;

    @SpyBean
    ProjectConfigService projectConfigService;

    @MockBean
    Provider<UserInfo> provider;

    @MockBean
    AuthTokenProvider authTokenProvider;
    @MockBean
    private ProjectMigrationToDataBaseService projectMigrationService;
    @MockBean
    FolderServiceJpa folderServiceJpa;
    @MockBean
    IntegrationServiceImpl integrationService;

    @MockBean
    FilePageConfigurationRepository filePageConfigurationRepository;
    @MockBean
    GitPageConfigurationRepository gitPageConfigurationRepository;

    UUID someProjectId;

    ProjectConfigsEntity projectConfigsEntity;

    @Before
    public void initMocks() {
        someProjectId = UUID.fromString("11111111-1111-1111-1111-11111111111");
        projectConfigsEntity = new ProjectConfigsEntity();
        projectConfigsEntity.setProjectId(someProjectId);
        projectConfigsEntity.setPagesSourceType(RepositoryType.GIT);
        projectConfigsEntity.setGitUrl("mockUrl");
        projectConfigsEntity.setPathFolderLocalProject("mockPath");
    }

    @Test
    public void reloadAllGitProjects_projectsConfigIsEmpty_doesNotUpdateConfigurations() {
        ProjectConfigService projectConfigService = Mockito.mock(ProjectConfigService.class);
        Mockito.when(projectConfigService.getAllTypesProjectsConfigs()).thenReturn(Collections.emptyList());
        GitProjectServiceImpl gitProjectService = new GitProjectServiceImpl(projectConfigService, gitRepo);
        List<ProjectConfiguration> projectConfigurations =
                Collections.singletonList(new ProjectConfiguration(projectConfigsEntity));

        gitProjectService.reloadAllGitProjects(projectConfigurations, false);

        Mockito.verify(gitRepo, Mockito.never()).gitClone(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void reloadAllGitProjects_projectsConfigIsNotEmpty_updateConfigurations() {
        Mockito.doNothing().when(gitRepo).gitClone(Mockito.anyString(), Mockito.anyString());

        Mockito.when(projectConfigurationServiceJpa.findProjectConfigById(any())).thenReturn(projectConfigsEntity);
        Mockito.when(projectConfigurationServiceJpa.getProjectConfigsByType(any()))
                .thenReturn(Collections.singletonList(projectConfigsEntity));
        List<ProjectConfiguration> projectConfigurations =
                Collections.singletonList(new ProjectConfiguration(projectConfigsEntity));

        gitProjectService.reloadAllGitProjects(projectConfigurations, false);

        Mockito.verify(gitRepo, Mockito.timeout(1000)).gitClone(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void reloadAllGitProjects_projectsConfigIsNotEmptyAndIsWaitAllGitReloadFlagTrue_updateConfigurations() {
        Mockito.doNothing().when(gitRepo).gitClone(Mockito.anyString(), Mockito.anyString());
        Mockito.when(projectConfigurationServiceJpa.findProjectConfigById(any())).thenReturn(projectConfigsEntity);
        Mockito.when(projectConfigurationServiceJpa.getProjectConfigsByType(any()))
                .thenReturn(Collections.singletonList(projectConfigsEntity));
        List<ProjectConfiguration> projectConfigurations =
                Collections.singletonList(new ProjectConfiguration(projectConfigsEntity));

        gitProjectService.reloadAllGitProjects(projectConfigurations, true);

        Mockito.verify(gitRepo, Mockito.timeout(1000)).gitClone(Mockito.anyString(), Mockito.anyString());
    }

    @Test(expected = GitCloneException.class)
    public void reloadGitProject_errorGitClone_throwRuntimeException() {
        Mockito.when(projectConfigurationServiceJpa.getProjectConfigsByType(any()))
                .thenReturn(Collections.singletonList(projectConfigsEntity));
        ProjectConfiguration projectConfigurations = new ProjectConfiguration(projectConfigsEntity);
        Mockito.doThrow(GitCloneException.class).when(gitRepo).gitClone(anyString(), anyString());

        gitProjectService.reloadGitProject(projectConfigurations);

    }

}
