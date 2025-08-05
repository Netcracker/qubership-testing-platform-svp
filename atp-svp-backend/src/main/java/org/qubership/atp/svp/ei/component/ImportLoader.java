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

package org.qubership.atp.svp.ei.component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.svp.core.enums.EntityType;
import org.qubership.atp.svp.core.exceptions.ei.SvpImportEntityException;
import org.qubership.atp.svp.model.ei.ImportExportIdentifier;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ImportLoader<T extends ImportExportIdentifier> {

    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;
    @Getter
    private final EntityType entityType;
    private final Class<T> clazz;

    /**
     * Load configuration from files.
     *
     * @param importData import data
     * @param path path for files
     * @return ImportExport configuration instance.
     */
    public List<T> loadConfiguration(ExportImportData importData, Path path) {
        log.info("Extract {} during import", entityType);
        List<T> extractedFolderEntities = new ArrayList<>();
        Path entityPath = Paths.get(path.toString(), entityType.name());
        Map<UUID, Path> entityFiles = objectLoaderFromDiskService.getListOfObjects(entityPath, clazz);
        Map<UUID, UUID> replacementMap = importData.getReplacementMap();
        log.debug("Extracted {} list with paths: {}", entityType, entityFiles);
        boolean isReplacement = importData.isInterProjectImport() || importData.isCreateNewProject();
        entityFiles.forEach((id, filePath) -> {
            log.debug("Extract {} configuration with ID {}", entityType, id);
            T entityConfiguration = load(filePath, replacementMap, isReplacement);
            log.debug("Loaded {} configuration: {}", entityType, entityConfiguration);
            if (entityConfiguration == null) {
                throw new SvpImportEntityException(entityType, filePath);
            }
            entityConfiguration.setSourceId(id);
            extractedFolderEntities.add(entityConfiguration);
        });
        log.info("{} are extracted during import: {}", entityType, extractedFolderEntities.size());
        return extractedFolderEntities;
    }

    private T load(Path filePath, Map<UUID, UUID> replacementMap, boolean isReplacement) {
        if (isReplacement) {
            log.debug("Load {} by path [{}] with replacementMap: {}", entityType, filePath, replacementMap);
            return objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(filePath, clazz, replacementMap,
                    true, false);
        } else {
            log.debug("Load {} by path [{}] without replacementMap", entityType, filePath);
            return objectLoaderFromDiskService.loadFileAsObject(filePath, clazz);
        }
    }
}
