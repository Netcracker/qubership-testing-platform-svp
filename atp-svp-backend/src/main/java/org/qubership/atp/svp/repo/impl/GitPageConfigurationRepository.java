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

package org.qubership.atp.svp.repo.impl;

import static org.qubership.atp.svp.repo.impl.FilePageConfigurationRepository.PATH_TO_PAGES;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.svp.core.exceptions.PageStorageException;
import org.qubership.atp.svp.model.configuration.KeyParameterConfiguration;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.model.impl.SutParameter;
import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.repo.PageConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class GitPageConfigurationRepository implements PageConfigurationRepository {

    private final GitRepositoryImpl gitRepository;
    private final FilePageConfigurationRepository filePageConfigurationRepository;
    private final LockManager lockManager;
    public static final String DEFAULT_FOLDER_NAME = "Default";

    /**
     * GitPageConfigurationRepository constructor.
     *
     * @param gitRepository GitRepositoryImpl
     * @param filePageConfigurationRepository FilePageConfigurationRepository
     * @param lockManager LockManager
     */
    @Autowired
    public GitPageConfigurationRepository(GitRepositoryImpl gitRepository,
                                          FilePageConfigurationRepository filePageConfigurationRepository,
                                          LockManager lockManager) {
        this.gitRepository = gitRepository;
        this.filePageConfigurationRepository = filePageConfigurationRepository;
        this.lockManager = lockManager;
    }

    @Override
    public List<PageConfiguration> getPageConfigurations(ProjectConfiguration config, String folder)
            throws PageStorageException {
        return filePageConfigurationRepository.getPageConfigurations(config, folder);
    }

    @Override
    public void createOrUpdatePageConfiguration(ProjectConfigsEntity config,
                                                PageConfiguration pageConfiguration, String folder) {
        String createOrUpdateMessageCommit = "Create or update page " + pageConfiguration.getName();
        String gitPath = config.getPathFolderLocalProject();
        lockManager.executeWithLock(getLockKey(config), () -> {
            gitRepository.gitPull(gitPath);
            filePageConfigurationRepository.createOrUpdatePageConfiguration(config, pageConfiguration, folder);
            gitRepository.gitCommitAndPush(gitPath, createOrUpdateMessageCommit);
        });
    }

    @Override
    public void bulkCreateOrUpdatePageConfiguration(ProjectConfigsEntity config,
                                                    List<PageConfiguration> pageConfiguration, String folder) {
        String createOrUpdateMessageCommit = "Create or update pages in folder: " + folder;
        String gitPath = config.getPathFolderLocalProject();
        lockManager.executeWithLock(getLockKey(config), () -> {
            gitRepository.gitPull(gitPath);
            filePageConfigurationRepository.bulkCreateOrUpdatePageConfiguration(config, pageConfiguration, folder);
            gitRepository.gitCommitAndPush(gitPath, createOrUpdateMessageCommit);
        });
    }

    @Override
    public void deletePageConfigurations(ProjectConfigsEntity config, String name, String folder) {
        String deleteMessageCommit = "Delete page " + name;
        String pagePath = getPathToPage(folder, name);
        String gitPath = config.getPathFolderLocalProject();
        lockManager.executeWithLock(getLockKey(config), () -> {
            gitRepository.gitPull(gitPath);
            filePageConfigurationRepository.deletePageConfigurations(config, name, folder);
            gitRepository.gitRemoveCommitAndPush(gitPath, deleteMessageCommit, pagePath);
        });
    }

    @Override
    public List<KeyParameterConfiguration> getKeyParameters(ProjectConfiguration config, String folder) {
        return filePageConfigurationRepository.getKeyParameters(config, folder);
    }

    @Override
    public void updateKeyParameters(ProjectConfigsEntity config, List<KeyParameterConfiguration> keyParameters,
                                    String folder) {
        String updateKeyParametersCommitMessage = "Update key parameters for project " + config.getProjectName();
        String gitPath = config.getPathFolderLocalProject();
        lockManager.executeWithLock(getLockKey(config), () -> {
            gitRepository.gitPull(gitPath);
            filePageConfigurationRepository.updateKeyParameters(config, keyParameters, folder);
            gitRepository.gitCommitAndPush(gitPath, updateKeyParametersCommitMessage);
        });
    }

    @Override
    public void updateCommonParameters(ProjectConfigsEntity config, List<SutParameter> commonParameters,
                                       String folder) {
        String updateCommonParametersCommitMessage = "Update common parameters for project " + config.getProjectName();
        String gitPath = config.getPathFolderLocalProject();
        lockManager.executeWithLock(getLockKey(config), () -> {
            gitRepository.gitPull(gitPath);
            filePageConfigurationRepository.updateCommonParameters(config, commonParameters, folder);
            gitRepository.gitCommitAndPush(gitPath, updateCommonParametersCommitMessage);
        });
    }

    @Override
    public List<SutParameter> getCommonParameters(ProjectConfiguration config, String folder) {
        return filePageConfigurationRepository.getCommonParameters(config, folder);
    }

    @Override
    public void movePage(ProjectConfigsEntity config, String name, String newName,
                         String folder, String targetFolder, int newOrder) {
        String commitMessage = "The page '" + name + "' was moved from '"
                + Optional.ofNullable(folder).orElse("Default") + "' to '"
                + Optional.ofNullable(targetFolder).orElse("Default") + "', and new page name: " + newName;
        String pagePath = getPathToPage(folder, name);
        String gitPath = config.getPathFolderLocalProject();
        lockManager.executeWithLock(getLockKey(config), () -> {
            gitRepository.gitPull(gitPath);
            filePageConfigurationRepository.movePage(config, name, newName, folder, targetFolder, newOrder);
            gitRepository.gitRemoveCommitAndPush(gitPath, commitMessage, pagePath);
            gitRepository.gitCommitAndPush(config.getPathFolderLocalProject(), commitMessage);
        });
    }

    @Override
    public void copyPage(ProjectConfigsEntity config, String name, String newName,
                         String folder, String targetFolder, int newOrder) {
        String commitMessage = "The page '" + name + "' was copied from '"
                + Optional.ofNullable(folder).orElse("Default") + "' to '"
                + Optional.ofNullable(targetFolder).orElse("Default") + "', and new page name: " + newName;
        String gitPath = config.getPathFolderLocalProject();
        lockManager.executeWithLock(getLockKey(config), () -> {
            gitRepository.gitPull(gitPath);
            filePageConfigurationRepository.copyPage(config, name, newName, folder, targetFolder, newOrder);
            gitRepository.gitCommitAndPush(gitPath, commitMessage);
        });
    }

    @Override
    public void createFolder(ProjectConfigsEntity config, FolderEntity folder) {
        String commitMessage = "Added a new folder '" + folder + "'";
        String gitPath = config.getPathFolderLocalProject();
        lockManager.executeWithLock(getLockKey(config), () -> {
            gitRepository.gitPull(gitPath);
            filePageConfigurationRepository.createFolder(config, folder);
            gitRepository.gitCommitAndPush(gitPath, commitMessage);
        });
    }

    @Override
    public void editFolder(ProjectConfigsEntity config, String oldName, FolderEntity folder) {
        String commitMessage = "The folder '" + folder + "' was renamed to " + folder.getName();
        String gitPath = config.getPathFolderLocalProject();
        lockManager.executeWithLock(getLockKey(config), () -> {
            gitRepository.gitPull(gitPath);
            filePageConfigurationRepository.editFolder(config, oldName, folder);
            gitRepository.gitRemoveCommitAndPush(config.getPathFolderLocalProject(), commitMessage, folder.getName());
            gitRepository.gitCommitAndPush(gitPath, commitMessage);
        });
    }

    @Override
    public List<String> getFolders(ProjectConfiguration config) {
        return filePageConfigurationRepository.getFolders(config);
    }

    @Override
    public void deleteFolder(ProjectConfigsEntity config, String folder) {
        String commitMessage = "The folder '" + folder + "' was removed";
        String gitPath = config.getPathFolderLocalProject();
        lockManager.executeWithLock(getLockKey(config), () -> {
            gitRepository.gitPull(gitPath);
            filePageConfigurationRepository.deleteFolder(config, folder);
            gitRepository.gitRemoveCommitAndPush(config.getPathFolderLocalProject(), commitMessage, folder);
        });
    }

    @Override
    public void importProject(ProjectConfigsEntity config, List<FolderEntity> folders) {
        String commitMessage = "The project was updated by import";
        String gitPath = config.getPathFolderLocalProject();
        lockManager.executeWithLock(getLockKey(config), () -> {
            gitRepository.gitPull(gitPath);
            filePageConfigurationRepository.importProject(config, folders);
            gitRepository.gitCommitAndPush(gitPath, commitMessage);
        });
    }

    @Override
    public void initializationProjectConfigs(ProjectConfigsEntity config) {
        String pathGitProject = config.getPathFolderLocalProject();
        String gitUrl = config.getGitUrl();
        if (gitUrl != null && !gitUrl.isEmpty()
                && pathGitProject != null && !pathGitProject.isEmpty()
                && !Files.exists(Paths.get(pathGitProject))) {
            try {
                FileUtils.deleteDirectory(new File(pathGitProject));
                log.info("[{}] Cloning repository {} to path {}", pathGitProject, gitUrl, pathGitProject);
                gitRepository.gitClone(pathGitProject, gitUrl);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @NotNull
    private String getLockKey(ProjectConfigsEntity config) {
        return config.getProjectId().toString() + " - " + config.getProjectName();
    }

    private String getPathToPage(String folder, String name) {
        return DEFAULT_FOLDER_NAME.equals(folder)
                ? PATH_TO_PAGES + "/" + name
                : folder + "/" + PATH_TO_PAGES + "/" + name;
    }
}
