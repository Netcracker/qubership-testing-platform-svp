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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.core.exceptions.KeyParametersStorageException;
import org.qubership.atp.svp.core.exceptions.PageStorageException;
import org.qubership.atp.svp.core.exceptions.ProjectConfigException;
import org.qubership.atp.svp.core.exceptions.migration.MigrationException;
import org.qubership.atp.svp.model.configuration.FolderConfiguration;
import org.qubership.atp.svp.model.configuration.KeyParameterConfiguration;
import org.qubership.atp.svp.model.db.CommonParameterEntity;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.KeyParameterEntity;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.impl.Group;
import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.model.impl.SutParameter;
import org.qubership.atp.svp.model.impl.Tab;
import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.model.project.ProjectRepositoryConfig;
import org.qubership.atp.svp.model.ui.ProjectConfigResponseModel;
import org.qubership.atp.svp.repo.impl.FilePageConfigurationRepository;
import org.qubership.atp.svp.service.direct.GitProjectServiceImpl;
import org.qubership.atp.svp.service.direct.IntegrationServiceImpl;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.qubership.atp.svp.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Data
@Slf4j
public class ProjectMigrationToDataBaseService {

    @Value("${svp.projects.config.path}/${svp.projects.config.name}")
    private String fullPathToConfig;
    private final FilePageConfigurationRepository filePageConfigRepository;
    private final ProjectConfigService projectConfigService;
    private final FolderServiceJpa folderServiceJpa;
    private final IntegrationServiceImpl integrationService;
    private final ProjectConfigurationServiceJpa projectConfigurationServiceJpa;
    private final GitProjectServiceImpl gitProjectServiceImpl;
    private final LockManager lockManager;
    private static final int DEFAULT_VALUE_LC = 86400;
    private static final String MIGRATION_USER_NAME = "MIGRATION SVP";
    private static final String DIVIDER = " -> ";

    /**
     * Constructor of ProjectMigrationToDataBaseService.
     *
     * @param filePageConfigRepository       FilePageConfigurationRepository
     * @param projectConfigService           ProjectConfigService
     * @param folderServiceJpa               FolderServiceJpa
     * @param integrationService             integrationService
     * @param projectConfigurationServiceJpa projectConfigurationServiceJpa
     * @param gitProjectServiceImpl          gitProjectServiceImpl
     * @param lockManager                    LockManager
     */
    @Autowired
    public ProjectMigrationToDataBaseService(FilePageConfigurationRepository filePageConfigRepository,
                                             ProjectConfigService projectConfigService,
                                             FolderServiceJpa folderServiceJpa,
                                             IntegrationServiceImpl integrationService,
                                             ProjectConfigurationServiceJpa projectConfigurationServiceJpa,
                                             GitProjectServiceImpl gitProjectServiceImpl, LockManager lockManager) {
        this.filePageConfigRepository = filePageConfigRepository;
        this.projectConfigService = projectConfigService;
        this.folderServiceJpa = folderServiceJpa;
        this.integrationService = integrationService;
        this.projectConfigurationServiceJpa = projectConfigurationServiceJpa;
        this.gitProjectServiceImpl = gitProjectServiceImpl;
        this.lockManager = lockManager;
    }

    /**
     * Add or update project config.
     *
     * @param projectConfigResponseModel ProjectConfigResponseModel
     * @param projectId                  UUID
     */
    @Transactional
    public void updateProjectConfigAndMigrate(ProjectConfigResponseModel projectConfigResponseModel, UUID projectId) {
        ProjectConfigsEntity projectConfigFromDb = projectConfigurationServiceJpa.findProjectConfigById(projectId);
        String pathToFolderProject = projectConfigService.getFullPathToProjectFolder(projectId);
        ProjectConfigsEntity projectConfigsEntity = new ProjectConfigsEntity(projectConfigResponseModel,
                projectConfigService.getUserName(), projectId, pathToFolderProject);
        checkLocalPath(projectConfigsEntity, projectConfigFromDb, projectConfigResponseModel);
        boolean isMigrateProject = isMigrateProject(projectConfigResponseModel, projectConfigFromDb);
        if (isMigrateProject) {
            projectConfigurationServiceJpa.removeProjectConfig(projectId);
        }

        projectConfigurationServiceJpa.saveProjectConfig(projectConfigsEntity);

        if (isMigrateProject) {
            loadAndMigrateGitAndLocalProject(projectConfigsEntity);
        }

        log.info("Add or update ProjectConfig to DB, model: {}", projectConfigsEntity);
    }

    private void loadAndMigrateGitAndLocalProject(ProjectConfigsEntity projectConfigsEntity) {
        ProjectConfiguration projectConfig = new ProjectConfiguration(projectConfigsEntity);
        if (projectConfig.getRepoConfig().getType().equals(RepositoryType.GIT)) {
            gitProjectServiceImpl.reloadGitProject(projectConfig);
        }
        folderServiceJpa.saveAll(projectJsonMigrateToBase(projectConfig));
    }

    private boolean isLocalTypeResponse(ProjectConfigResponseModel projectConfigResponseModel) {
        return projectConfigResponseModel.getType().equals(RepositoryType.LOCAL);
    }

    private boolean isMigrateProject(ProjectConfigResponseModel projectConfigResponseModel,
                                     ProjectConfigsEntity projectConfigFromDb) {
        RepositoryType typeDb = Objects.nonNull(projectConfigFromDb) ? projectConfigFromDb.getPagesSourceType() : null;
        String gitUrlDb = Objects.nonNull(projectConfigFromDb) ? projectConfigFromDb.getGitUrl() : null;
        RepositoryType typeResponse = projectConfigResponseModel.getType();
        String gitUrlResponse = projectConfigResponseModel.getGitUrl();

        return projectConfigResponseModel.getIsMigrateProject() || !Objects.equals(typeResponse, typeDb)
                || Objects.equals(typeResponse, RepositoryType.GIT) && !Objects.equals(gitUrlResponse, gitUrlDb);
    }

    private void checkLocalPath(ProjectConfigsEntity projectConfigsEntity, ProjectConfigsEntity projectConfigFromDb,
                                ProjectConfigResponseModel projectConfigResponseModel) {
        if (isLocalTypeResponse(projectConfigResponseModel)) {
            String pathToFolderProject;
            if (Objects.nonNull(projectConfigFromDb)) {
                pathToFolderProject = projectConfigFromDb.getPathFolderLocalProject();
                if (Objects.nonNull(pathToFolderProject)) {
                    projectConfigsEntity.setPathFolderLocalProject(pathToFolderProject);
                }
            }
        }
    }

    /**
     * Gets all projects' configurations from json to database.
     */
    @Transactional(rollbackFor = {ProjectConfigException.class})
    public void migrateAllProjectConfigsFromJsonFile() throws ProjectConfigException {
        log.info("ProjectMigrationToDataBaseService - migrateAllProjectConfigsFromJsonFile - "
                + "migration Project Configs to DB");
        Path path = Paths.get(this.fullPathToConfig);
        try {
            List<ProjectConfiguration> projectConfigsJson = getProjectConfigurationsFromFile(path);
            List<ProjectConfigsEntity> projectConfigsEntitiesJson = convertToConfigEntities(projectConfigsJson);
            List<ProjectConfigsEntity> allProjectConfigsDb = projectConfigurationServiceJpa.getAllProjectConfigs();
            if (!allProjectConfigsDb.isEmpty()) {
                allProjectConfigsDb.stream()
                        .map(ProjectConfigsEntity::getProjectId)
                        .forEach(projectId -> projectConfigsEntitiesJson
                                .removeIf(p -> p.getProjectId().equals(projectId)));
            }
            if (!projectConfigsEntitiesJson.isEmpty()) {
                projectConfigurationServiceJpa.saveAllProjectsConfigs(projectConfigsEntitiesJson);
            }
        } catch (Exception e) {
            log.error("ProjectMigrationToDataBaseService - migrateAllProjectConfigsFromJsonFile - "
                    + "migration Project Configs to DB - path: {}", this.fullPathToConfig, e);
            throw new ProjectConfigException("Couldn't load projects configuration!", e);
        }
    }

    @NotNull
    private List<ProjectConfigsEntity> convertToConfigEntities(List<ProjectConfiguration> projectConfigsJson) {
        return projectConfigsJson.stream().map(projectConfig -> {
                    if (projectConfig.getRepoConfig().getType().equals(RepositoryType.GIT)) {
                        projectConfig.getRepoConfig().setPath(null);
                    }
                    if (Objects.isNull(projectConfig.getDefaultLogCollectorSearchTimeRange())) {
                        projectConfig.setDefaultLogCollectorSearchTimeRange(DEFAULT_VALUE_LC);
                    }
                    return new ProjectConfigsEntity(projectConfig, MIGRATION_USER_NAME);
                }
        ).collect(Collectors.toList());
    }

    private List<ProjectConfiguration> getProjectConfigurationsFromFile(Path path) throws IOException {
        Utils.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        byte[] fileContentBytes = Files.readAllBytes(path);
        String fileString = new String(fileContentBytes, StandardCharsets.UTF_8);
        return Utils.mapper.readValue(fileString, new TypeReference<List<ProjectConfiguration>>() {
        });
    }

    /**
     * Migrate projects files to Database.
     */
    @Transactional
    public void projectsJsonMigrateToBase(List<ProjectConfiguration> projectConfigurations) {
        log.info("Migration git and local projects to database started");
        List<FolderEntity> folderEntities = new ArrayList<>();
        projectConfigurations.parallelStream()
                .forEach(config -> folderEntities.addAll(projectJsonMigrateToBase(config)));
        folderServiceJpa.saveAll(folderEntities);
        log.info("Migration git and local projects to database finished");
    }

    private List<FolderEntity> projectJsonMigrateToBase(ProjectConfiguration projectConfig) {
        List<FolderEntity> folderEntities = new ArrayList<>();
        if (isFileExist(projectConfig)) {
            try {
                if (isLocalOrGitAndNonNullUrl(projectConfig)) {
                    setDefaultPathLocalProject(projectConfig);
                    List<FolderConfiguration> folders =
                            filePageConfigRepository.getFolderConfigurations(projectConfig);
                    boolean isUpdateEntities = checkProject(folders.get(0).getProjectId(), projectConfig.getId());

                    folderEntities.addAll(parallelTreeObjectsProcessing(
                            projectConfig, folders, isUpdateEntities));

                    if (isUpdateEntities && projectConfig.getRepoConfig().getType().equals(RepositoryType.GIT)) {
                        filePageConfigRepository.importProject(
                                new ProjectConfigsEntity(projectConfig, null),
                                folderEntities);
                        gitProjectServiceImpl.updateEntities(projectConfig);
                    }
                }
            } catch (Exception exp) {
                log.error("Projects migrate to base failed, projectConfiguration {}, error: {}",
                        projectConfig, exp.getMessage());
                throw new MigrationException();
            }
        }
        return folderEntities;
    }

    private boolean checkProject(UUID realProjectId, UUID migrationProjectId) {
        return !migrationProjectId.equals(realProjectId);
    }

    private boolean isFileExist(ProjectConfiguration projectConfiguration) {
        String stringPath = projectConfiguration.getRepoConfig().getPath();
        if (stringPath == null) {
            return false;
        }
        Path projectFolder = Paths.get(stringPath);
        return Files.exists(projectFolder);
    }

    private void setDefaultPathLocalProject(ProjectConfiguration projectConfiguration) {
        ProjectRepositoryConfig repoConfig = projectConfiguration.getRepoConfig();
        if (repoConfig.getType().equals(RepositoryType.LOCAL) && Strings.isBlank(repoConfig.getPath())) {
            String pathToFolderProject = projectConfigService.getFullPathToProjectFolder(projectConfiguration.getId());
            repoConfig.setPath(pathToFolderProject);
        }
    }

    private List<FolderEntity> parallelTreeObjectsProcessing(ProjectConfiguration projectConfig,
                                                             List<FolderConfiguration> folders,
                                                             boolean isUpdateEntities) {
        ProjectConfigsEntity projectConfigsEntity = new ProjectConfigsEntity(projectConfig, null);
        return folders.parallelStream().map(folder -> {
            if (isUpdateEntities) {
                folder.setId(null);
            }
            FolderEntity folderEntity = new FolderEntity(projectConfigsEntity, folder);
            buildFolderTree(projectConfig, folderEntity, isUpdateEntities);

            return folderEntity;
        }).collect(Collectors.toList());
    }

    private void saveFoldersToDataBase(List<FolderEntity> folderEntities) {
        if (!folderEntities.isEmpty()) {
            folderServiceJpa.saveAll(folderEntities);
            log.info("Migrated folder project to database complete");
        } else {
            log.info("No projects to migrate");
        }
    }

    private void buildFolderTree(ProjectConfiguration projectConfiguration, FolderEntity folderEntity,
                                 boolean isUpdateEntities) {

        folderEntity.setCommonParameters(getCommonParameterEntities(projectConfiguration, folderEntity,
                        isUpdateEntities))
                .setKeyParameterEntities(getKeyParameterEntities(projectConfiguration, folderEntity, isUpdateEntities))
                .setPages(getPageConfigurationEntities(projectConfiguration, folderEntity, isUpdateEntities));
    }

    private boolean isLocalOrGitAndNonNullUrl(ProjectConfiguration projectConfiguration) {
        return projectConfiguration.getRepoConfig().getType().equals(RepositoryType.LOCAL)
                || Objects.nonNull(projectConfiguration.getRepoConfig().getGitUrl());
    }

    private List<PageConfigurationEntity> getPageConfigurationEntities(ProjectConfiguration projectConfiguration,
                                                                       FolderEntity folderEntity,
                                                                       boolean isUpdateEntities)
            throws PageStorageException {
        String folderName = folderEntity.getName();
        List<PageConfiguration> pages =
                filePageConfigRepository.getPageConfigurations(projectConfiguration, folderName);

        preprocessingTreeObjectPages(pages, isUpdateEntities);

        return pages.parallelStream()
                .map(page -> PageConfigurationEntity.createPageEntity(page, folderEntity))
                .collect(Collectors.toList());
    }

    private void preprocessingTreeObjectPages(List<PageConfiguration> pages, boolean isUpdateEntities) {
        for (PageConfiguration page : pages) {
            if (isUpdateEntities) {
                page.setPageId(null);
            }
            Set<String> uniqueSetTabs = new HashSet<>();
            preprocessingTabs(page, uniqueSetTabs, isUpdateEntities);
        }
    }

    private void preprocessingTabs(PageConfiguration page,
                                   Set<String> uniqueSetTabs,
                                   boolean isUpdateEntities) {
        for (Tab tab : page.getTabs()) {
            if (isUpdateEntities) {
                tab.setTabId(null);
            }
            String nameTab = tab.getName();
            nameTab = checkDuplicateNameEntity(uniqueSetTabs, nameTab);
            tab.setName(nameTab);
            Set<String> uniqueSetGroups = new HashSet<>();
            preprocessingGroups(tab, uniqueSetGroups, isUpdateEntities);
        }
    }

    private void preprocessingGroups(Tab tab, Set<String> uniqueSetGroups,
                                     boolean isUpdateEntities) {
        for (Group group : tab.getGroups()) {
            if (isUpdateEntities) {
                group.setGroupId(null);
            }
            String nameGroup = checkDuplicateNameEntity(uniqueSetGroups, group.getName());
            group.setName(nameGroup);
            Set<String> uniqueSetSutParameters = new HashSet<>();
            preprocessingSutParameters(group, uniqueSetSutParameters, isUpdateEntities);
        }
    }

    private void preprocessingSutParameters(Group group,
                                            Set<String> uniqueSetSutParameters,
                                            boolean isUpdateEntities) {
        for (SutParameter sutParameter : group.getSutParameters()) {
            if (isUpdateEntities) {
                sutParameter.setParameterId(null);
            }
            sutParameter.setIsPreconfigured(false);
            String nameParam = sutParameter.getName();
            nameParam = checkDuplicateNameEntity(uniqueSetSutParameters, nameParam);
            sutParameter.setName(nameParam);
        }
    }

    private String checkDuplicateNameEntity(Set<String> uniqueSet, String name) {
        StringBuilder sb = new StringBuilder();
        while (!uniqueSet.add(name)) {
            sb.append(name).append("_duplicateName");
            name = sb.toString();
        }
        return name;
    }

    private List<KeyParameterEntity> getKeyParameterEntities(ProjectConfiguration projectConfiguration,
                                                             FolderEntity folderEntity, boolean isUpdateEntities)
            throws KeyParametersStorageException {

        List<KeyParameterConfiguration> keyParameters = filePageConfigRepository.getKeyParameters(projectConfiguration,
                folderEntity.getName());
        List<KeyParameterEntity> keyParameterEntities = new ArrayList<>();
        for (int order = 0; order < keyParameters.size(); order++) {
            if (isUpdateEntities) {
                keyParameters.get(order).setId(null);
            }
            KeyParameterEntity keyParameter = new KeyParameterEntity(folderEntity, keyParameters.get(order), order);
            keyParameterEntities.add(keyParameter);
        }
        return keyParameterEntities;
    }

    private List<CommonParameterEntity> getCommonParameterEntities(ProjectConfiguration projectConfiguration,
                                                                   FolderEntity folderEntity,
                                                                   boolean isUpdateEntities) {
        List<SutParameter> commonParameters =
                filePageConfigRepository.getCommonParameters(projectConfiguration, folderEntity.getName());
        List<CommonParameterEntity> commons = new ArrayList<>();
        Set<String> uniqueSetTabs = new HashSet<>();
        for (int order = 0; order < commonParameters.size(); order++) {
            SutParameter commonParameter = commonParameters.get(order);
            if (isUpdateEntities) {
                commonParameter.setParameterId(null);
            }
            commonParameter.setIsPreconfigured(false);
            String nameCommonParameters = commonParameter.getName();
            nameCommonParameters = checkDuplicateNameEntity(uniqueSetTabs, nameCommonParameters);
            commonParameter.setName(nameCommonParameters);
            commons.add(new CommonParameterEntity(folderEntity, commonParameter, order));
        }
        return commons;
    }
}
