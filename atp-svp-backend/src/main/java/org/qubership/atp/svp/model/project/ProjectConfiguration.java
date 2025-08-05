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

package org.qubership.atp.svp.model.project;

import java.util.UUID;

import org.qubership.atp.svp.model.db.ProjectConfigsEntity;

import lombok.Data;

@Data
public class ProjectConfiguration {

    private UUID id;
    private String name;
    private Boolean isFullInfoNeededInPot;
    private Integer defaultLogCollectorSearchTimeRange;
    private ProjectRepositoryConfig repoConfig;
    public static final String DEFAULT_FOLDER_NAME = "Default";

    /**
     * Constructor for ProjectConfig if the project uses LogCollector service.
     *
     * @param id - project ID in ATP Environment Service
     * @param name - project name in ATP Environment Service
     * @param defaultLogCollectorSearchTimeRange - default relative time range in seconds for performing search requests
     *         to the LogCollector(Parameter can be also changed via execution UI)
     * @param isFullInfoNeededInPot - whether to put all csv files with uploaded data to POT report
     *         or is it enough to return POT report as a simple word document
     * @param repoConfig - configuration of project repository.
     */
    public ProjectConfiguration(UUID id, String name,
                                Boolean isFullInfoNeededInPot,
                                Integer defaultLogCollectorSearchTimeRange,
                                ProjectRepositoryConfig repoConfig) {
        this.id = id;
        this.name = name;
        this.isFullInfoNeededInPot = isFullInfoNeededInPot;
        this.defaultLogCollectorSearchTimeRange = defaultLogCollectorSearchTimeRange;
        this.repoConfig = repoConfig;
    }

    /**
     * Constructor converter from ProjectConfigsEntity to ProjectConfiguration.
     * @param projectConfigs model ProjectConfigsEntity.
     */
    public ProjectConfiguration(ProjectConfigsEntity projectConfigs) {
        this.id = projectConfigs.getProjectId();
        this.name = projectConfigs.getProjectName();
        this.isFullInfoNeededInPot = projectConfigs.isFullInfoNeededInPot();
        this.defaultLogCollectorSearchTimeRange = projectConfigs.getDefaultLogCollectorSearchTimeRange();
        this.repoConfig = new ProjectRepositoryConfig();
        this.repoConfig.setPath(projectConfigs.getPathFolderLocalProject());
        this.repoConfig.setType(projectConfigs.getPagesSourceType());
        this.repoConfig.setGitUrl(projectConfigs.getGitUrl());
    }

    /**
     * Constructor for ProjectConfig.
     * Used for test purposes.
     */
    public ProjectConfiguration() {
        this.repoConfig = new ProjectRepositoryConfig();
    }

    public boolean getIsFullInfoNeededInPot() {
        return isFullInfoNeededInPot;
    }

    public void setIsFullInfoNeededInPot(boolean fullInfoNeededInPot) {
        isFullInfoNeededInPot = fullInfoNeededInPot;
    }

    /**
     * Get path to folder if it exists.
     */
    public String getConfigPath(String folder) {
        String configPath = repoConfig.getPath();
        if (!DEFAULT_FOLDER_NAME.equals(folder)) {
            configPath += "/" + folder;
        }
        return configPath;
    }
}

