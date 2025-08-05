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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.dto.validation.UserMessage;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.svp.core.enums.EntityType;
import org.qubership.atp.svp.ei.component.ImportLoader;
import org.qubership.atp.svp.model.db.CommonParameterEntity;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.KeyParameterEntity;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.db.SutParameterEntity;
import org.qubership.atp.svp.model.ei.ExportImportCommonParameter;
import org.qubership.atp.svp.model.ei.ExportImportFolder;
import org.qubership.atp.svp.model.ei.ExportImportKeyParameter;
import org.qubership.atp.svp.model.ei.ExportImportPage;
import org.qubership.atp.svp.model.ei.ImportExportIdentifier;
import org.qubership.atp.svp.service.AbstractRepositoryConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AtpImportStrategy extends AbstractRepositoryConfigService implements ImportStrategy {

    private final ProjectConfigurationServiceJpa projectConfigurationServiceJpa;
    private final FolderServiceJpa folderServiceJpa;
    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private final List<ImportLoader> importLoaders = new ArrayList<>();
    public static final ThreadLocal<Set<String>> messages = ThreadLocal.withInitial(HashSet::new);

    public ExportFormat getFormat() {
        return ExportFormat.ATP;
    }

    @Override
    public List<FolderEntity> extractFolders(ExportImportData importData, Path path) {
        log.info("Extract folders during import");
        List<ExportImportFolder> folderConfigurations =
                getImportLoaders(EntityType.FOLDER).loadConfiguration(importData, path);
        ProjectConfigsEntity project =
                projectConfigurationServiceJpa.findProjectConfigById(importData.getProjectId());
        List<FolderEntity> extractedEntities = new ArrayList<>();
        List<FolderEntity> projectFolders = project.getFolders();
        List<String> folderNames = folderServiceJpa.getFolders(importData.getProjectId());
        boolean isReplacement = importData.isInterProjectImport() || importData.isCreateNewProject();
        folderConfigurations.forEach(fc -> {
            fc.setProject(project);
            extractedEntities.add(extractFolder(fc, folderNames, isReplacement, projectFolders));
        });
        log.info("Folder are extracted during import: {}", extractedEntities.size());
        log.debug("Folders entities: {}", extractedEntities);
        return extractedEntities;
    }

    private FolderEntity extractFolder(ExportImportFolder exportFolder,
                                       List<String> folderNames, boolean isReplacement,
                                       List<FolderEntity> projectFolders) {
        if (!isReplacement) {
            Optional<FolderEntity> optionalFolder = projectFolders.stream()
                    .filter(folder -> folder.getFolderId().equals(exportFolder.getId())).findFirst();

            if (optionalFolder.isPresent()) {
                FolderEntity folder = optionalFolder.get();
                folder.setName(exportFolder.getName());
                return folder;
            }

            optionalFolder = projectFolders.stream()
                    .filter(folder -> {
                                if (folder.getSourceId() != null) {
                                    return folder.getSourceId().equals(exportFolder.getId());
                                }
                                return false;
                            }
                    ).findFirst();

            if (optionalFolder.isPresent()) {
                FolderEntity folder = optionalFolder.get();
                folder.setName(exportFolder.getName());
                return folder;
            }

            optionalFolder = projectFolders.stream()
                    .filter(folder -> folder.getName().equals(exportFolder.getName())).findFirst();

            if (optionalFolder.isPresent()) {
                FolderEntity folder = optionalFolder.get();
                folder.setSourceId(exportFolder.getId());
                return folder;
            }
        }

        validateFolder(exportFolder, projectFolders);

        Optional<FolderEntity> optionalFolder = projectFolders.stream()
                .filter(folder -> {
                            if (folder.getSourceId() != null) {
                                return folder.getSourceId().equals(exportFolder.getSourceId());
                            }
                            return false;
                        }
                ).findFirst();

        if (optionalFolder.isPresent()) {
            FolderEntity folder = optionalFolder.get();
            folder.setName(exportFolder.getName());
            return folder;
        }

        exportFolder.makeNewName(folderNames);
        FolderEntity folder = exportFolder.toEntity();
        return folder;
    }

    private void validateFolder(ExportImportFolder exportFolder, List<FolderEntity> projectFolders) {
        if (projectFolders.stream().anyMatch(folderProject -> {
            if (folderProject.getSourceId() != null) {
                return !folderProject.getFolderId().equals(exportFolder.getId())
                        && !folderProject.getSourceId().equals(exportFolder.getId())
                        && !folderProject.getSourceId().equals(exportFolder.getSourceId())
                        && folderProject.getName().equals(exportFolder.getName());
            } else {
                return !folderProject.getFolderId().equals(exportFolder.getId())
                        && folderProject.getName().equals(exportFolder.getName());
            }
        })) {
            String message =
                    String.format("the Folder: '%s' already exists name, wilt be with '_copy'", exportFolder.getName());
            messages.get().add(message);
        }
    }

    @Override
    public void extractPages(List<FolderEntity> folders, ExportImportData importData, Path path) {
        log.info("Extract pages during import");
        List<ExportImportPage> pageConfigurations =
                getImportLoaders(EntityType.PAGE).loadConfiguration(importData, path);

        boolean isReplacement = importData.isInterProjectImport() || importData.isCreateNewProject();
        pageConfigurations.forEach(pc -> {
            pc.setFolder(folders);
            extractPage(pc, isReplacement);
        });
    }

    private void extractPage(ExportImportPage pageConfiguration, boolean isReplacement) {
        List<PageConfigurationEntity> pagesInFolder = pageConfiguration.getFolder().getPages();

        validatePageImportData(pagesInFolder, pageConfiguration);

        if (!isReplacement) {
            Optional<PageConfigurationEntity> optionalPage = pagesInFolder.stream()
                    .filter(page -> page.getPageId().equals(pageConfiguration.getId())).findFirst();

            if (optionalPage.isPresent()) {
                updatePageEntity(pageConfiguration.toEntity(), optionalPage.get());
                return;
            }

            optionalPage = pagesInFolder.stream()
                    .filter(page -> {
                        if (page.getSourceId() != null) {
                            return page.getSourceId().equals(pageConfiguration.getId());
                        }
                        return false;
                    }).findFirst();

            if (optionalPage.isPresent()) {
                PageConfigurationEntity pageConfigurationEntity = optionalPage.get();
                pageConfiguration.setSourceId(pageConfiguration.getId());
                pageConfiguration.setId(pageConfigurationEntity.getPageId());

                updatePageEntity(pageConfiguration.toEntity(), pageConfigurationEntity);
                return;
            }

            optionalPage = pagesInFolder.stream()
                    .filter(page -> page.getName().equals(pageConfiguration.getName())).findFirst();

            if (optionalPage.isPresent()) {
                PageConfigurationEntity pageConfigurationEntity = optionalPage.get();
                pageConfiguration.setSourceId(pageConfiguration.getId());
                pageConfiguration.setId(pageConfigurationEntity.getPageId());
                updatePageEntity(pageConfiguration.toEntity(), optionalPage.get());
                return;
            }
        }

        Optional<PageConfigurationEntity> optionalPage = pagesInFolder.stream()
                .filter(page -> {
                    if (page.getSourceId() != null) {
                        return page.getSourceId().equals(pageConfiguration.getSourceId());
                    }
                    return false;
                }).findFirst();

        if (optionalPage.isPresent()) {
            updatePageEntity(pageConfiguration.toEntity(), optionalPage.get());
            return;
        }
        List<String> pageNames = pagesInFolder.stream()
                .map(PageConfigurationEntity::getName).collect(Collectors.toList());

        pageConfiguration.makeNewName(pageNames);
        PageConfigurationEntity importedPage = pageConfiguration.toEntity();
        importedPage.updateIds();
        importedPage.setOrder(pagesInFolder.size());
        importedPage.getFolder().addPage(importedPage);
    }

    private void validatePageImportData(List<PageConfigurationEntity> pagesInFolder,
                                        ExportImportPage pageConfigurations) {
        if (pagesInFolder.stream().anyMatch(page -> {
            if (page.getSourceId() != null) {
                return !page.getPageId().equals(pageConfigurations.getId())
                        && !page.getSourceId().equals(pageConfigurations.getId())
                        && !page.getSourceId().equals(pageConfigurations.getSourceId())
                        && page.getName().equals(pageConfigurations.getName());
            } else {
                return !page.getPageId().equals(pageConfigurations.getId())
                        && page.getName().equals(pageConfigurations.getName());
            }
        })) {

            String message = String.format("the Page: '%s' already exists, name wilt be with '_copy'",
                    pageConfigurations.getName());
            messages.get().add(message);
        }
    }

    private void updatePageEntity(PageConfigurationEntity importedPage, PageConfigurationEntity page) {
        page.getFolder().getPages().remove(page);
        importedPage.getFolder().getPages().add(importedPage);
    }

    @Override
    public void extractKeyParameters(List<FolderEntity> folders, ExportImportData importData, Path path) {
        log.info("Extract key parameters during import");
        List<ExportImportKeyParameter> keyParameterConfigurations =
                getImportLoaders(EntityType.KEY_PARAMETER).loadConfiguration(importData, path);
        boolean isReplacement = importData.isInterProjectImport() || importData.isCreateNewProject();
        keyParameterConfigurations.forEach(pc -> {
            pc.setFolder(folders);
            extractKeyParameter(pc, isReplacement);
        });
    }

    private void extractKeyParameter(ExportImportKeyParameter exportImportKeyParameter, boolean isReplacement) {

        List<KeyParameterEntity> keyParameterEntities = exportImportKeyParameter.getFolder().getKeyParameterEntities();
        List<String> keyNames = keyParameterEntities.stream().map(KeyParameterEntity::getName)
                .collect(Collectors.toList());

        if (!isReplacement) {
            Optional<KeyParameterEntity> optionalKeyParameter = keyParameterEntities.stream()
                    .filter(key -> key.getKeyParameterId().equals(exportImportKeyParameter.getId())).findFirst();

            if (optionalKeyParameter.isPresent()) {
                KeyParameterEntity keyParameterEntity = optionalKeyParameter.get();
                keyParameterEntity.setName(exportImportKeyParameter.getName());
                return;
            }

            optionalKeyParameter = keyParameterEntities.stream()
                    .filter(key -> {
                        if (key.getSourceId() != null) {
                            return key.getSourceId().equals(exportImportKeyParameter.getId());
                        }
                        return false;
                    }).findFirst();

            if (optionalKeyParameter.isPresent()) {
                KeyParameterEntity keyParameterEntity = optionalKeyParameter.get();
                keyParameterEntity.setName(exportImportKeyParameter.getName());
                return;
            }

            if (keyNames.contains(exportImportKeyParameter.getName())) {
                KeyParameterEntity keyParameterEntity = keyParameterEntities.stream()
                        .filter(key -> key.getName().equals(exportImportKeyParameter.getName())).findFirst().get();
                keyParameterEntity.setSourceId(exportImportKeyParameter.getId());
                return;
            }
        }
        validateKeyParameterImportData(keyParameterEntities, exportImportKeyParameter);

        Optional<KeyParameterEntity> optionalKeyParameter = keyParameterEntities.stream()
                .filter(key -> {
                    if (key.getSourceId() != null) {
                        return key.getSourceId().equals(exportImportKeyParameter.getSourceId());
                    }
                    return false;
                }).findFirst();

        if (optionalKeyParameter.isPresent()) {
            KeyParameterEntity keyParameterEntity = optionalKeyParameter.get();
            keyParameterEntity.setName(exportImportKeyParameter.getName());
            return;
        }

        int order = keyParameterEntities.size();
        exportImportKeyParameter.makeNewName(keyNames);
        KeyParameterEntity importKeyEntity = exportImportKeyParameter.toEntity();
        importKeyEntity.setKeyOrder(order);
        importKeyEntity.getFolder().addKeyParameter(importKeyEntity);
    }

    private void validateKeyParameterImportData(List<KeyParameterEntity> keyParameterEntities,
                                                ExportImportKeyParameter exportImportKeyParameter) {
        if (keyParameterEntities.stream().anyMatch(keyParameter -> {
            if (keyParameter.getSourceId() != null) {
                return !keyParameter.getKeyParameterId().equals(exportImportKeyParameter.getId())
                        && !keyParameter.getSourceId().equals(exportImportKeyParameter.getId())
                        && !keyParameter.getSourceId().equals(exportImportKeyParameter.getSourceId())
                        && keyParameter.getName().equals(exportImportKeyParameter.getName());
            } else {
                return !keyParameter.getKeyParameterId().equals(exportImportKeyParameter.getId())
                        && keyParameter.getName().equals(exportImportKeyParameter.getName());
            }
        })) {

            String message = String.format("the KeyParameter: '%s' already exists, name wilt be with '_copy'",
                    exportImportKeyParameter.getName());
            messages.get().add(message);
        }
    }

    @Override
    public void extractCommonParameters(List<FolderEntity> folders, ExportImportData importData, Path path) {
        log.info("Extract common parameters during import");
        List<ExportImportCommonParameter> commonParameterConfigurations =
                getImportLoaders(EntityType.COMMON_PARAMETER).loadConfiguration(importData, path);
        boolean isReplacement = importData.isInterProjectImport() || importData.isCreateNewProject();
        commonParameterConfigurations.forEach(pc -> {
            pc.setFolder(folders);
            extractCommonParameter(pc, isReplacement);
        });
    }

    private void extractCommonParameter(ExportImportCommonParameter exportImportCommonParameter,
                                        boolean isReplacement) {
        List<CommonParameterEntity> commonParameterEntities = exportImportCommonParameter.getFolder()
                .getCommonParameters();

        validateCommonParameterImportData(commonParameterEntities, exportImportCommonParameter);

        if (!isReplacement) {
            Optional<CommonParameterEntity> optionalCommonParameter = commonParameterEntities.stream()
                    .filter(commonParameter -> commonParameter.getCommonParameterId()
                            .equals(exportImportCommonParameter.getId())).findFirst();

            if (optionalCommonParameter.isPresent()) {
                CommonParameterEntity commonParameterEntity = optionalCommonParameter.get();
                SutParameterEntity importedCommonParameter = exportImportCommonParameter.toEntity()
                        .getSutParameterEntity();

                commonParameterEntity.getSutParameterEntity().updateEntity(importedCommonParameter);
                return;
            }

            optionalCommonParameter = commonParameterEntities.stream()
                    .filter(commonParameter -> {
                        if (commonParameter.getSourceId() != null) {
                            return commonParameter.getSourceId().equals(exportImportCommonParameter.getId());
                        }
                        return false;
                    }).findFirst();

            if (optionalCommonParameter.isPresent()) {
                CommonParameterEntity commonParameterEntity = optionalCommonParameter.get();
                SutParameterEntity importedCommonParameter = exportImportCommonParameter.toEntity()
                        .getSutParameterEntity();
                commonParameterEntity.getSutParameterEntity().updateEntity(importedCommonParameter);
                return;
            }

            optionalCommonParameter = commonParameterEntities.stream()
                    .filter(commonParameter -> commonParameter.getSutParameterEntity().getName()
                            .equals(exportImportCommonParameter.getName()))
                    .findFirst();

            if (optionalCommonParameter.isPresent()) {
                CommonParameterEntity commonParameterEntity = optionalCommonParameter.get();
                commonParameterEntity.setSourceId(exportImportCommonParameter.getId());
                SutParameterEntity importedCommonParameter = exportImportCommonParameter.toEntity()
                        .getSutParameterEntity();

                commonParameterEntity.getSutParameterEntity().updateEntity(importedCommonParameter);
                return;
            }
        }

        Optional<CommonParameterEntity> optionalCommonParameter = commonParameterEntities.stream()
                .filter(commonParameter -> {
                    if (commonParameter.getSourceId() != null) {
                        return commonParameter.getSourceId().equals(exportImportCommonParameter.getSourceId());
                    }
                    return false;
                }).findFirst();

        if (optionalCommonParameter.isPresent()) {
            CommonParameterEntity commonParameterEntity = optionalCommonParameter.get();
            SutParameterEntity importedCommonParameter = exportImportCommonParameter.toEntity()
                    .getSutParameterEntity();

            commonParameterEntity.getSutParameterEntity().updateEntity(importedCommonParameter);
            return;
        }

        List<String> commonNames = commonParameterEntities.stream().map(commonParameter ->
                commonParameter.getSutParameterEntity().getName()).collect(Collectors.toList());
        exportImportCommonParameter.makeNewName(commonNames);
        CommonParameterEntity importedCommonParameter = exportImportCommonParameter.toEntity();
        int order = commonParameterEntities.size();
        importedCommonParameter.getSutParameterEntity().setParameterOrder(order);
        importedCommonParameter.getFolder().getCommonParameters().add(importedCommonParameter);
    }

    private void validateCommonParameterImportData(List<CommonParameterEntity> commonParameterEntities,
                                                   ExportImportCommonParameter importCommonParameter) {
        if (commonParameterEntities.stream().anyMatch(commonParameter -> {
            if (commonParameter.getSourceId() != null) {
                return !commonParameter.getCommonParameterId().equals(importCommonParameter.getId())
                        && !commonParameter.getSourceId().equals(importCommonParameter.getId())
                        && !commonParameter.getSourceId().equals(importCommonParameter.getSourceId())
                        && commonParameter.getSutParameterEntity().getName().equals(importCommonParameter.getName());
            } else {
                return !commonParameter.getCommonParameterId().equals(importCommonParameter.getId())
                        && commonParameter.getSutParameterEntity().getName().equals(importCommonParameter.getName());
            }
        })) {

            String message = String.format("the CommonParameter: '%s' already exists, name wilt be with '_copy'",
                    importCommonParameter.getName());
            messages.get().add(message);
        }
    }

    @Override
    @Transactional
    public void save(List<FolderEntity> folders) {
        if (folders != null) {
            folderServiceJpa.saveAll(folders);
            ProjectConfigsEntity config = folders.get(0).getProject();

            getRepoForConfig(config).importProject(config, folders);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResult validateData(ExportImportData importData, Path workDir) {

        Map<UUID, UUID> repMap = new HashMap<>(importData.getReplacementMap());
        boolean isReplacement = importData.isCreateNewProject() || importData.isInterProjectImport();

        if (isReplacement) {
            List<UUID> allIds = new ArrayList<>();
            allIds.addAll(getObjectIds(workDir, ExportImportFolder.class));
            allIds.addAll(getObjectIds(workDir, ExportImportPage.class));
            allIds.addAll(getObjectIds(workDir, ExportImportKeyParameter.class));
            allIds.addAll(getObjectIds(workDir, ExportImportCommonParameter.class));
            allIds.forEach(id -> repMap.put(id, UUID.randomUUID()));
        }

        List<FolderEntity> folders = extractFolders(importData, workDir);
        extractPages(folders, importData, workDir);
        extractKeyParameters(folders, importData, workDir);
        extractCommonParameters(folders, importData, workDir);

        List<UserMessage> details = new ArrayList<>();
        if (messages.get() != null && !messages.get().isEmpty()) {
            details.addAll(messages.get().stream()
                    .map(UserMessage::new)
                    .collect(Collectors.toList()));
        }
        return new ValidationResult(details, repMap);
    }

    public <T extends ImportExportIdentifier> List<UUID> getObjectIds(Path workDir, Class<T> clazz) {
        return new ArrayList<>(objectLoaderFromDiskService.getListOfObjects(workDir, clazz).keySet());
    }

    private ImportLoader getImportLoaders(EntityType entityType) {
        Optional<ImportLoader> importLoaderOptional = importLoaders.stream()
                .filter(il -> il.getEntityType().equals(entityType)).findAny();
        if (importLoaderOptional.isPresent()) {
            return importLoaderOptional.get();
        }
        ImportLoader importLoader = null;
        switch (entityType) {
            case FOLDER:
                importLoader = new ImportLoader<ExportImportFolder>(objectLoaderFromDiskService, entityType,
                        ExportImportFolder.class);
                break;
            case PAGE:
                importLoader = new ImportLoader<ExportImportPage>(objectLoaderFromDiskService, entityType,
                        ExportImportPage.class);
                break;
            case KEY_PARAMETER:
                importLoader = new ImportLoader<ExportImportKeyParameter>(objectLoaderFromDiskService, entityType,
                        ExportImportKeyParameter.class);
                break;
            case COMMON_PARAMETER:
                importLoader = new ImportLoader<ExportImportCommonParameter>(objectLoaderFromDiskService, entityType,
                        ExportImportCommonParameter.class);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + entityType);
        }
        importLoaders.add(importLoader);
        return importLoader;
    }
}
