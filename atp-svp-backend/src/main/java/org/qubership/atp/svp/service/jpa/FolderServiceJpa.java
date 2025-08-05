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

package org.qubership.atp.svp.service.jpa;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.exceptions.ProjectConfigException;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.repo.jpa.FolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FolderServiceJpa {

    private final FolderRepository folderRepository;

    public FolderServiceJpa(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    /**
     * Creates a new folder entity for the given project.
     *
     * @param project The project configuration entity.
     * @param name The name of the folder, must not be null or empty.
     * @return The saved FolderEntity instance.
     * @throws IllegalArgumentException If the folder name is null or empty.
     */
    public FolderEntity create(ProjectConfigsEntity project, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be empty");
        }
        return folderRepository.save(new FolderEntity(project, name));
    }

    public List<FolderEntity> saveAll(List<FolderEntity> folderEntities) {
        return folderRepository.saveAll(folderEntities);
    }

    public void update(FolderEntity folderEntity) {
        folderRepository.save(folderEntity);
    }

    public Optional<FolderEntity> getFolder(UUID folderId) {
        return folderRepository.findByFolderId(folderId);
    }

    public boolean isExistFolderByProjectId(UUID projectId) {
        return folderRepository.existsByProjectProjectId(projectId);
    }

    public List<FolderEntity> getFoldersByProjectId(UUID projectId) {
        return folderRepository.findByProjectProjectId(projectId);
    }

    public List<FolderEntity> getFoldersByIds(Set<UUID> folderId) {
        return folderRepository.getFolderEntitiesByFolderIdIn(folderId);
    }

    public Set<UUID> getFolderIdsByPageIds(Set<UUID> pageIds) {
        return folderRepository.getFolderIdsByPageIds(pageIds).stream().map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    /**
     * Edit folder name.
     *
     * @param projectId UUID
     * @param folderName old name String
     * @param newName new name String
     */
    public FolderEntity editFolder(UUID projectId, String folderName, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be empty");
        }
        FolderEntity folder = folderRepository.getByProjectProjectIdAndName(projectId, folderName);
        folder.setName(newName);
        return folderRepository.save(folder);
    }

    public List<String> getFolders(UUID projectId) {
        return folderRepository.getFolders(projectId);
    }

    @Transactional
    public void deleteFolder(UUID projectId, String folderName) {
        folderRepository.deleteFolderByProjectIdAndName(projectId, folderName);
    }

    @Transactional
    public void deleteFolderInProject(UUID projectId) {
        folderRepository.deleteByProjectProjectId(projectId);
        folderRepository.flush();
    }

    /**
     * get {@link FolderEntity} by project id and folder name.
     * @param projectId project id
     * @param folderName folder name
     * @return {@link FolderEntity}.
     */
    public FolderEntity getFolderByProjectIdAndName(UUID projectId, String folderName) {
        FolderEntity folder = folderRepository.getByProjectProjectIdAndName(projectId, folderName);
        if (Objects.nonNull(folder)) {
            return folder;
        }
        throw new ProjectConfigException("Couldn't load projects configuration or Folder!");
    }
}
