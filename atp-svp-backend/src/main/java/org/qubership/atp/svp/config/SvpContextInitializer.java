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

package org.qubership.atp.svp.config;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.service.GitProjectService;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@DependsOn({"liquibase"})
@Slf4j
public class SvpContextInitializer implements ServletContextInitializer {

    private final GitProjectService gitProjectService;
    private final ProjectConfigService projectConfigService;
    private final FolderServiceJpa folderServiceJpa;


    /**
     * Constructor SvpContextInitializer.
     */
    @Autowired
    public SvpContextInitializer(GitProjectService gitProjectService,
                                 ProjectConfigService projectConfigService,
                                 FolderServiceJpa folderServiceJpa) {
        this.gitProjectService = gitProjectService;
        this.projectConfigService = projectConfigService;
        this.folderServiceJpa = folderServiceJpa;
    }

    /**
     * Initialize Project Config to database and Download all GIT projects.
     */
    @Override
    public void onStartup(ServletContext servletContext) {
        List<String> projectConfigurations = projectConfigService.getProjectIds();
        projectConfigurations.parallelStream()
                .forEach(configId -> projectConfigService.initialProject(UUID.fromString(configId)));
    }


    private boolean isExistsAnyFolderProject(List<ProjectConfiguration> projectConfigurations) {
        return projectConfigurations.stream()
                .anyMatch(projectConfiguration -> {
                    String path = projectConfiguration.getRepoConfig().getPath();
                    return Files.exists(Paths.get(path));
                });
    }
}
