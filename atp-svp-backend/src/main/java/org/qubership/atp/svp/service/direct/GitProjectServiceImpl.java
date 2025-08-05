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

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.model.project.ProjectRepositoryConfig;
import org.qubership.atp.svp.repo.impl.GitRepositoryImpl;
import org.qubership.atp.svp.service.GitProjectService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GitProjectServiceImpl implements GitProjectService {

    private final ProjectConfigService projectConfigService;
    private final GitRepositoryImpl gitRepo;
    public static final int TIMEOUT_SEC = 600;

    /**
     * Constructor for class GitProjectServiceImpl.
     *
     * @param projectConfigService ProjectConfigService
     * @param gitRepo GitRepositoryImpl
     */
    @Autowired
    public GitProjectServiceImpl(ProjectConfigService projectConfigService,
                                 GitRepositoryImpl gitRepo) {
        this.projectConfigService = projectConfigService;
        this.gitRepo = gitRepo;
    }

    @Override
    public void reloadAllGitProjects(List<ProjectConfiguration> projectConfigs, boolean isWaitAllGitReload) {
        log.info("Reloading of all GIT repositories is start");
        if (!projectConfigs.isEmpty()) {
            ExecutorService executorService = Executors.newFixedThreadPool(projectConfigs.size());
            Map<String, String> mdcMap = MDC.getCopyOfContextMap();
            AtomicInteger index = new AtomicInteger(1);
            projectConfigs.stream().filter(this::isTypeGitAndUrlNoneNull)
                    .forEach(config -> {
                        String timestamp = OffsetDateTime.now().toString();
                        log.info("Starting reload for project [{}]: ID={}, Name={}, Timestamp={}",
                                index.getAndIncrement(), config.getId(), config.getName(), timestamp);
                        executorService.submit(() -> {
                            MdcUtils.setContextMap(mdcMap);
                            log.info("Executing reload for project [{}]: ID={}, Name={}, Timestamp={}",
                                    index.get(), config.getId(), config.getName(), OffsetDateTime.now());
                            reloadGitProject(config);
                        });
                    });
            executorService.shutdown();
            if (isWaitAllGitReload) {
                log.info("Wait All Git projects reloading");
                try {
                    boolean isTasksCompleted = executorService.awaitTermination(TIMEOUT_SEC, TimeUnit.SECONDS);
                    if (!isTasksCompleted) {
                        String message = String.format("Interrupted loading of projects from git ofter timout: %s sec",
                                TIMEOUT_SEC);
                        log.error(message);
                        throw new RuntimeException(message);
                    }
                } catch (InterruptedException e) {
                    String message = "Interrupted executorService awaitTermination loading of projects from git";
                    log.error(message);
                    throw new RuntimeException(message, e);
                }
            }
        }
        log.info("Reloading of all GIT repositories is complete");
    }

    private boolean isTypeGitAndUrlNoneNull(ProjectConfiguration projectConfiguration) {
        return projectConfiguration.getRepoConfig().getType().equals(RepositoryType.GIT)
                && Objects.nonNull(projectConfiguration.getRepoConfig().getGitUrl());
    }

    /**
     * Reload Git Project.
     *
     * @param config ProjectConfiguration
     */
    public void reloadGitProject(ProjectConfiguration config) {
        String nameProject = config.getName();
        UUID idProject = config.getId();
        log.info("Reloading GIT repository for project id: {}, name: {}", idProject, nameProject);
        ProjectRepositoryConfig repoConfig = config.getRepoConfig();
        String gitUrl = repoConfig.getGitUrl();
        if (gitUrl != null && !gitUrl.isEmpty()) {
            try {
                String pathGitProject = projectConfigService.getFullPathToProjectFolder(idProject);
                FileUtils.deleteDirectory(new File(pathGitProject));
                log.info("[{}] Cloning repository {} to path {}", nameProject,
                        gitUrl, pathGitProject);
                gitRepo.gitClone(pathGitProject, gitUrl);
                log.info("[{}] Cloning completed.", nameProject);
            } catch (IOException e) {
                log.error("[{}] failed to delete folder project.", nameProject, e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Commit and push updated project config.
     *
     * @param config ProjectConfiguration
     */
    public void updateEntities(ProjectConfiguration config) {
        String commitMessage = "The project was updated by migration";
        String gitPath = config.getConfigPath("Default");
        gitRepo.gitCommitAndPush(gitPath, commitMessage);
    }
}
