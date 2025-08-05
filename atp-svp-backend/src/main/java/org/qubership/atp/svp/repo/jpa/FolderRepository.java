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

package org.qubership.atp.svp.repo.jpa;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.svp.model.db.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, UUID> {

    @Query(value = "SELECT name FROM folders WHERE project_id = ?1", nativeQuery = true)
    List<String> getFolders(UUID projectId);

    boolean existsByProjectProjectId(UUID projectId);

    FolderEntity getByProjectProjectIdAndName(UUID projectId, String name);

    Optional<FolderEntity> findByFolderId(UUID folderId);

    @Query(value = "delete from folders f where f.project_id = ?1 and f.name = ?2", nativeQuery = true)
    @Modifying
    void deleteFolderByProjectIdAndName(UUID projectId, String folderName);

    void deleteByProjectProjectId(UUID projectId);

    @Query(value = "select * from folders f where project_id = ?1 \n"
            + "ORDER BY CASE \n"
            + "             WHEN f.\"name\"  = 'Default' THEN 0 \n"
            + "             ELSE 1 \n"
            + "         END, f.\"name\"", nativeQuery = true)
    List<FolderEntity> findByProjectProjectId(UUID projectId);

    @Query(value = "select cast(folder_id as varchar)  from page_configs  where page_id in (?1)",
            nativeQuery = true)
    Set<String> getFolderIdsByPageIds(Set<UUID> pageIds);

    List<FolderEntity> getFolderEntitiesByFolderIdIn(Set<UUID> folderId);
}
