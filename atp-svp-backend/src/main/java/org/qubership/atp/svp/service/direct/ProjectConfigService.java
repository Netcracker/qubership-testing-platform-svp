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

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.core.exceptions.ProjectConfigException;
import org.qubership.atp.svp.core.exceptions.project.ProjectNotFoundException;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.environments.Project;
import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.model.ui.ProjectConfigResponseModel;
import org.qubership.atp.svp.repo.impl.AuthTokenProvider;
import org.qubership.atp.svp.service.AbstractRepositoryConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@NoArgsConstructor
public class ProjectConfigService extends AbstractRepositoryConfigService {

    @Value("${svp.projects.config.path}")
    private String pathToProjectsFolder;
    private Provider<UserInfo> userProvider;
    private AuthTokenProvider authTokenProvider;
    private ProjectConfigurationServiceJpa projectConfigurationServiceJpa;
    private FolderServiceJpa folderServiceJpa;
    private IntegrationServiceImpl integrationService;

    private static final String INIT_PROJECT_USER_NAME = "INIT_PROJECT";
    private static final String NOT_AUTHORIZED_USER_NAME = "NOT_AUTHORIZED";
    private static final String KAFKA_USER_NAME = "CATALOG_NOTIFICATION_TOPIC";
    private static final int DEFAULT_VALUE_LC = 86400;

    /**
     * Constructor for spring service used.
     */
    @Autowired
    public ProjectConfigService(ProjectConfigurationServiceJpa projectConfigurationServiceJpa,
                                Provider<UserInfo> userProvider,
                                AuthTokenProvider authTokenProvider, FolderServiceJpa folderServiceJpa,
                                IntegrationServiceImpl integrationService) {
        this.projectConfigurationServiceJpa = projectConfigurationServiceJpa;
        this.userProvider = userProvider;
        this.authTokenProvider = authTokenProvider;
        this.folderServiceJpa = folderServiceJpa;
        this.integrationService = integrationService;
    }

    /**
     * Searches for a project configuration by projectId
     * and throws exception ProjectConfigException if not found.
     */
    public ProjectConfigsEntity getProjectConfig(UUID projectId) {
        log.info("ProjectConfigService - getProjectConfig - {}", projectId);
        try {
            ProjectConfigsEntity projectConfigsEntity = getProjectConfigEntity(projectId);
            setPathFolderForTypeGit(projectId, projectConfigsEntity);
            return projectConfigsEntity;
        } catch (DataAccessException e) {
            log.error("ProjectConfigService - getProjectConfig - projectId: {}", projectId, e);
            throw new ProjectConfigException("Couldn't load projects configuration!", projectId, e);
        }
    }

    private void setPathFolderForTypeGit(UUID projectId, ProjectConfigsEntity projectConfigsEntity) {
        if (projectConfigsEntity.getPagesSourceType() == RepositoryType.GIT) {
            String fullPathToProjectFolder = getFullPathToProjectFolder(projectId);
            projectConfigsEntity.setPathFolderLocalProject(fullPathToProjectFolder);
        }
    }

    private ProjectConfigsEntity getProjectConfigEntity(UUID projectId) {
        return projectConfigurationServiceJpa.findProjectConfigById(projectId);
    }

    /**
     * Delete project config entity by projectId.
     */
    public void deleteProjectConfigEntity(UUID projectId) {
        projectConfigurationServiceJpa.removeProjectConfig(projectId);
    }

    /**
     * Gets list all type projects' configurations from Db.
     */
    public List<ProjectConfiguration> getAllTypesProjectsConfigs() {
        log.info("ProjectConfigService - getAllTypesProjectsConfigs");
        return projectConfigurationServiceJpa.getAllProjectConfigs().stream()
                .map(projectConfigs -> {
                    UUID projectId = projectConfigs.getProjectId();
                    if (projectConfigs.getPagesSourceType() == RepositoryType.GIT) {
                        projectConfigs.setPathFolderLocalProject(getFullPathToProjectFolder(projectId));
                    }
                    return new ProjectConfiguration(projectConfigs);
                }).collect(Collectors.toList());
    }

    /**
     * Gets all configurations Id from Db.
     */
    public List<String> getProjectIds() {
        return projectConfigurationServiceJpa.getProjectIds();
    }

    @Transactional
    public void initialProject(UUID projectId) {
        ProjectConfigsEntity config = getProjectConfigEntity(projectId);
        getRepoForConfig(config).initializationProjectConfigs(config);
    }

    /**
     * Gets project config for by id for response.
     */
    public ProjectConfigResponseModel getProjectConfigByIdForResponse(UUID projectId) {
        ProjectConfigsEntity projectConfigEntity = projectConfigurationServiceJpa.findProjectConfigById(projectId);
        return new ProjectConfigResponseModel(projectConfigEntity);
    }

    /**
     * Initialize Project Config.
     *
     * @param projectId UUID
     */
    @Transactional
    public void initializeProjectConfigByEnv(UUID projectId) {
        try {
            getProjectConfig(projectId);
        } catch (ProjectNotFoundException ex) {
            log.info("Initialize project config - Start, Project id: " + projectId);
            Project projectEnv = integrationService.getProjectFromEnvironments(projectId);
            String projectName = Objects.nonNull(projectEnv) ? projectEnv.getName() : "-";
            ProjectConfigsEntity projectConfig = createProjectConfigDb(projectName, projectId, false);
            folderServiceJpa.create(projectConfig, "Default");
            log.info("Added project to DB, ProjectName: {}, ProjectId: {}", projectName, projectId);
            log.info("Initialize project config - Finish, Project id: " + projectId);
        }
    }

    /**
     * Add new project config.
     *
     * @param projectName String
     * @param projectId UUID
     * @param isCatalogNotificationTopic boolean
     * @return ProjectConfigsEntity
     */
    public ProjectConfigsEntity createProjectConfigDb(String projectName, UUID projectId,
                                                      boolean isCatalogNotificationTopic) {
        ProjectConfigsEntity projectConfigsEntity;
        ProjectConfigResponseModel projectConfigResponseModel = getProjectConfigResponseModel(projectName);
        try {
            String userName = INIT_PROJECT_USER_NAME + "_"
                    + (isCatalogNotificationTopic ? KAFKA_USER_NAME : getUserName());
            projectConfigsEntity = new ProjectConfigsEntity(projectConfigResponseModel,
                    userName, projectId, getFullPathToProjectFolder(projectId));
            projectConfigurationServiceJpa.saveProjectConfig(projectConfigsEntity);
        } catch (Exception e) {
            throw new ProjectConfigException("The project configuration was not created in the database, project Name: "
                    + projectName + " id: " + projectId + "isKafka: " + isCatalogNotificationTopic, e);
        }
        return projectConfigsEntity;
    }

    private ProjectConfigResponseModel getProjectConfigResponseModel(String projectName) {
        return new ProjectConfigResponseModel()
                .setProjectName(projectName)
                .setType(RepositoryType.LOCAL)
                .setDefaultLogCollectorSearchTimeRange(DEFAULT_VALUE_LC)
                .setIsFullInfoNeededInPot(false)
                .setGitUrl(null);
    }

    public String getUserName() {
        return authTokenProvider.isAuthentication()
                ? userProvider.get().getUsername()
                : NOT_AUTHORIZED_USER_NAME;
    }

    public boolean isProjectExist(UUID projectId) {
        return projectConfigurationServiceJpa.isExistsProjectConfigById(projectId);
    }

    public String getFullPathToProjectFolder(UUID projectId) {
        return this.pathToProjectsFolder + "/" + projectId;
    }
}
