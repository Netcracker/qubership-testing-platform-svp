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

package org.qubership.atp.svp.ei.service;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.exceptions.ExportException;
import org.qubership.atp.svp.core.enums.ServiceScopeEntities;

public interface ExportStrategy {

    ExportFormat getFormat();

    void export(ExportImportData exportData, Path path);

    /**
     * TODO: 3/21/2024 need to add javadoc.
     */
    default void exportFolders(ExportImportData exportData, Path workDir) throws ExportException {
        Set<UUID> exportFolderIds = exportData.getExportScope().getEntities()
                .getOrDefault(ServiceScopeEntities.ENTITY_SVP_FOLDERS.getValue(), new HashSet<>())
                .stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());
        Set<UUID> exportPageIds = exportData.getExportScope().getEntities()
                .getOrDefault(ServiceScopeEntities.ENTITY_SVP_PAGES.getValue(), new HashSet<>())
                .stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());

        exportFolders(exportFolderIds, exportPageIds, workDir);
    }

    void exportFolders(Set<UUID> exportFolderIds, Set<UUID> exportPageIds, Path workDir);

    /**
     * TODO: 3/21/2024 need to add javadoc.
     */
    default void exportPages(ExportImportData exportData, Path workDir) throws ExportException {
        Set<UUID> exportPageIds = exportData.getExportScope().getEntities()
                .getOrDefault(ServiceScopeEntities.ENTITY_SVP_PAGES.getValue(), new HashSet<>())
                .stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());
        exportPages(exportPageIds, workDir);
    }

    void exportPages(Set<UUID> exportRequestIds, Path workDir) throws ExportException;
}
