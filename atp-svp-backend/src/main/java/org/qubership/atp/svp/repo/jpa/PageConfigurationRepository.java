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

import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PageConfigurationRepository extends JpaRepository<PageConfigurationEntity, UUID> {

    @Query(value = "SELECT name FROM page_configs WHERE folder_id = (select folder_id from folders where project_id ="
            + " ?1 and name = ?2)",
            nativeQuery = true)
    List<String> getPagesNameInFolder(UUID projectId, String folderName);

    PageConfigurationEntity getByFolderFolderIdAndName(UUID folderId, String name);

    Optional<PageConfigurationEntity> findByNameAndFolderProjectProjectIdAndFolderName(String name,
                                                                                       UUID projectId,
                                                                                       String folderName);

    List<PageConfigurationEntity> getAllByFolderFolderId(UUID folderId);

    List<PageConfigurationEntity> getPageConfigurationEntityByPageIdIn(Set<UUID> pageIds);

    @Modifying
    @Query(value = "DELETE FROM page_configs WHERE name = ?1 and folder_id = (select folder_id from folders where "
            + "project_id  = ?2  and name = ?3)", nativeQuery = true)
    void deletePage(String name, UUID projectId, String folderName);

    List<PageConfigurationEntity> findByFolderFolderIdAndNameIn(UUID folderId, List<String> names);
}
