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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.svp.core.exceptions.folder.FolderDeleteDbException;
import org.qubership.atp.svp.core.exceptions.folder.FolderSaveDbException;
import org.qubership.atp.svp.core.exceptions.folder.FoldersGetException;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.folder.FolderRequest;
import org.qubership.atp.svp.service.AbstractRepositoryConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FolderService extends AbstractRepositoryConfigService {

    private final FolderServiceJpa folderServiceJpa;
    private final ProjectConfigurationServiceJpa projectConfigServiceJpa;

    public FolderService(FolderServiceJpa folderServiceJpa, ProjectConfigurationServiceJpa projectConfigServiceJpa) {
        this.folderServiceJpa = folderServiceJpa;
        this.projectConfigServiceJpa = projectConfigServiceJpa;
    }

    /**
     * Create a new folder.
     */
    @Transactional()
    public void createFolder(UUID projectId, String nameFolder) {
        log.info("Creating folder {} for project {} was started", nameFolder, projectId);
        try {
            ProjectConfigsEntity projectEntity = projectConfigServiceJpa.findProjectConfigById(projectId);
            FolderEntity folder = folderServiceJpa.create(projectEntity, nameFolder);
            getRepoForConfig(projectEntity).createFolder(projectEntity, folder);
        } catch (DataAccessException e) {
            log.error("FolderService - createFolder() - folderServiceJpa.Create, projectId: {}, folder: {}",
                    projectId, nameFolder, e);
            throw new FolderSaveDbException();
        }
        log.info("Creating folder [{}] for project {} was finished successfully", nameFolder, projectId);
    }

    /**
     * Rename exist folder name.
     */
    @Transactional()
    public void editFolder(UUID projectId, FolderRequest requestBody) {
        String oldName = requestBody.getOldName();
        String newName = requestBody.getName();
        log.info("Rename folder {} in project {} was started", oldName, projectId);
        try {
            ProjectConfigsEntity projectEntity = projectConfigServiceJpa.findProjectConfigById(projectId);
            FolderEntity folder = folderServiceJpa.editFolder(projectId, oldName, newName);
            getRepoForConfig(projectEntity).editFolder(projectEntity, oldName, folder);
        } catch (DataAccessException e) {
            log.error("FolderService - editFolder(), projectId: {}, folder: {}. Request body -{} ", projectId,
                    newName, requestBody, e);
            throw new FolderSaveDbException();
        }
        log.info("Renaming folder {} for project {} was finished successfully, new name {}", oldName,
                projectId, newName);
    }

    /**
     * Get all folders for the project.
     */
    public List<String> getFolders(UUID projectId) {
        log.info("Getting the folders names in project {}", projectId);
        try {
            List<String> folders = folderServiceJpa.getFolders(projectId);
            return folders.isEmpty() ? Collections.singletonList("Default") : folders;
        } catch (DataAccessException e) {
            log.error("FolderService - getFolders(), projectId: {}", projectId, e);
            throw new FoldersGetException();
        }
    }

    /**
     * Delete exist folder by name.
     */
    @Transactional()
    public void deleteFolder(UUID projectId, FolderRequest requestBody) {
        String folderName = requestBody.getName();
        log.info("Deleting folder [{}] for project {} was started", folderName, projectId);
        try {
            ProjectConfigsEntity projectEntity = projectConfigServiceJpa.findProjectConfigById(projectId);
            folderServiceJpa.deleteFolder(projectId, folderName);
            getRepoForConfig(projectEntity).deleteFolder(projectEntity, folderName);
        } catch (DataAccessException e) {
            log.error("FolderService - deleteFolder(), projectId: {}, folder: {} ", projectId, folderName, e);
            throw new FolderDeleteDbException();
        }
        log.info("Deleting folder [{}] for project {} was finished successfully", folderName, projectId);
    }
}
