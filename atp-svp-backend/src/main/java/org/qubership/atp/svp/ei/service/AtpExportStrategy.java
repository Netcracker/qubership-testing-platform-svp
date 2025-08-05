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
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.exceptions.ExportException;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.svp.core.enums.EntityType;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.ei.ExportImportCommonParameter;
import org.qubership.atp.svp.model.ei.ExportImportFolder;
import org.qubership.atp.svp.model.ei.ExportImportKeyParameter;
import org.qubership.atp.svp.model.ei.ExportImportPage;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.PageConfigurationServiceJpa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtpExportStrategy implements ExportStrategy {

    private final FolderServiceJpa folderServiceJpa;

    private final PageConfigurationServiceJpa pageConfigurationServiceJpa;

    private final ObjectSaverToDiskService objectSaverToDiskService;

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.ATP;
    }

    @Override
    public void export(ExportImportData exportData, Path path) {
        exportFolders(exportData, path);
        exportPages(exportData, path);
    }

    @Override
    @Transactional(readOnly = true)
    public void exportFolders(Set<UUID> exportFolderIds, Set<UUID> exportPageIds, Path workDir) {
        exportFolderIds.addAll(folderServiceJpa.getFolderIdsByPageIds(exportPageIds));
        folderServiceJpa.getFoldersByIds(exportFolderIds).forEach(folder -> exportFolder(folder, workDir));
    }

    private void exportFolder(FolderEntity folderToExport, Path workDir) {
        ExportImportFolder folder = new ExportImportFolder(folderToExport);
        objectSaverToDiskService.exportAtpEntity(folderToExport.getFolderId(), folder,
                workDir.resolve(EntityType.FOLDER.name()));
        exportCommonParameters(folderToExport, workDir.resolve(EntityType.COMMON_PARAMETER.name()));
        exportKeyParameters(folderToExport, workDir.resolve(EntityType.KEY_PARAMETER.name()));
    }

    private void exportCommonParameters(FolderEntity folderToExport, Path workDir) {
        folderToExport.getCommonParameters().forEach(commonParameter ->
                objectSaverToDiskService.exportAtpEntity(commonParameter.getCommonParameterId(),
                        new ExportImportCommonParameter(commonParameter),
                        workDir)
        );
    }

    private void exportKeyParameters(FolderEntity folderToExport, Path workDir) {
        folderToExport.getKeyParameterEntities().forEach(keyParameterEntity ->
                objectSaverToDiskService.exportAtpEntity(keyParameterEntity.getKeyParameterId(),
                        new ExportImportKeyParameter(keyParameterEntity),
                        workDir)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void exportPages(Set<UUID> exportPageIds, Path workDir) throws ExportException {
        List<PageConfigurationEntity> pages = pageConfigurationServiceJpa.getAllPagesByIds(exportPageIds);
        pages.forEach(page -> exportPage(page, workDir));
    }

    private void exportPage(PageConfigurationEntity page, Path workDir) {
        objectSaverToDiskService.exportAtpEntity(page.getPageId(), new ExportImportPage(page),
                workDir.resolve(EntityType.PAGE.name()));
    }
}
