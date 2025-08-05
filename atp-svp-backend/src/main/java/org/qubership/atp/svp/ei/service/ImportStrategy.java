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
import java.util.List;

import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.springframework.transaction.annotation.Transactional;

public interface ImportStrategy {

    /**
     * Get Export format.
     *
     * @return ExportFormat
     */
    ExportFormat getFormat();

    /**
     * Start import.
     *
     * @param importData import data
     * @param path path for files
     */
    @Transactional
    default void svpImport(ExportImportData importData, Path path) {
        List<FolderEntity> folders = extractFolders(importData, path);
        extractPages(folders, importData, path);
        extractKeyParameters(folders, importData, path);
        extractCommonParameters(folders, importData, path);
        save(folders);
    }

    /**
     * Extract folderts from files.
     *
     * @param importData import data
     * @param path path for files
     * @return List of FolderEntity
     */
    List<FolderEntity> extractFolders(ExportImportData importData,
                                      Path path);

    /**
     * Extract pages.
     *
     * @param folders extracted folders
     * @param importData import data
     * @param path path for files
     */
    void extractPages(List<FolderEntity> folders, ExportImportData importData, Path path);

    /**
     * Extract pages.
     *
     * @param folders extracted folders
     * @param importData import data
     * @param path path for files
     */
    void extractKeyParameters(List<FolderEntity> folders, ExportImportData importData, Path path);

    /**
     * Extract pages.
     *
     * @param folders extracted folders
     * @param importData import data
     * @param path path for files
     */
    void extractCommonParameters(List<FolderEntity> folders, ExportImportData importData, Path path);

    /**
     * Save imported configuration.
     *
     * @param folders extracted folders
     */
    void save(List<FolderEntity> folders);

    /**
     * Validate imported data on name duplicates.
     *
     * @return ValidationResult
     */
    ValidationResult validateData(ExportImportData importData, Path workDir);
}
