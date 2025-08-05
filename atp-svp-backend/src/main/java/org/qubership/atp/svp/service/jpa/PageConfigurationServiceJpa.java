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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.exceptions.page.PageNotFoundException;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.repo.jpa.PageConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PageConfigurationServiceJpa {

    private final PageConfigurationRepository pageConfigurationRepository;

    /**
     * Constructor init PageConfigurationServiceJpa.
     *
     * @param pageConfigurationRepository PageConfigurationRepository
     */
    public PageConfigurationServiceJpa(PageConfigurationRepository pageConfigurationRepository) {
        this.pageConfigurationRepository = pageConfigurationRepository;
    }

    public List<PageConfigurationEntity> getPageConfigurationEntity(UUID folderId, List<String> names) {
        return pageConfigurationRepository.findByFolderFolderIdAndNameIn(folderId, names);
    }

    /**
     * Get {@link PageConfiguration} by page name, folder name and project id.
     *
     * @param name       page name
     * @param projectId  project id
     * @param folderName folder name
     * @return {@link PageConfiguration}.
     */
    @Transactional
    public PageConfiguration getPageConfiguration(String name, UUID projectId, String folderName) {
        PageConfigurationEntity pageEntity = pageConfigurationRepository
                .findByNameAndFolderProjectProjectIdAndFolderName(name, projectId, folderName)
                .orElseThrow(PageNotFoundException::new);
        return new PageConfiguration(pageEntity);
    }

    public List<String> getPagesNameInFolder(UUID projectId, String folderName) {
        return pageConfigurationRepository.getPagesNameInFolder(projectId, folderName);
    }

    public List<PageConfiguration> getAllPagesInFolder(UUID folderId) {
        List<PageConfigurationEntity> pagesEntity = pageConfigurationRepository.getAllByFolderFolderId(folderId);
        return pagesEntity.stream().map(PageConfiguration::new).collect(Collectors.toList());
    }

    public List<PageConfigurationEntity> getAllPagesEntityInFolder(UUID folderId) {
        return pageConfigurationRepository.getAllByFolderFolderId(folderId);
    }

    public List<PageConfigurationEntity> getAllPagesByIds(Set<UUID> pageIds) {
        return pageConfigurationRepository.getPageConfigurationEntityByPageIdIn(pageIds);
    }

    /**
     * Save Page In Folder.
     *
     * @param pageConfiguration PageConfiguration
     * @param folderEntity      FolderEntity
     */
    @Transactional
    public void savePageInFolder(PageConfiguration pageConfiguration, FolderEntity folderEntity) {
        if (pageConfiguration.getName() == null || pageConfiguration.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Page name cannot be empty");
        }
        PageConfigurationEntity newOrUpdatePage = PageConfigurationEntity
                .createPageEntity(pageConfiguration, folderEntity);
        if (pageConfiguration.getPageId() == null) {
            newOrUpdatePage.setOrder(folderEntity.getPages().size());
        }
        pageConfigurationRepository.save(newOrUpdatePage);
    }

    /**
     * Save Page.
     */
    public void saveListPage(List<PageConfigurationEntity> pageConfigurationEntity) {
        pageConfigurationRepository.saveAll(pageConfigurationEntity);
    }

    /**
     * Copy page and rename if existed.
     *
     * @param projectId     UUID
     * @param pageName      String
     * @param currentFolder String
     * @param targetFolder  FolderEntity
     * @return String, New name page's name
     */
    public String copyPageAndRename(UUID projectId, String currentFolder, String pageName, FolderEntity targetFolder) {
        PageConfiguration page = getPageConfiguration(pageName, projectId, currentFolder);
        PageConfigurationEntity copiedPage = PageConfigurationEntity.createPageEntity(page, targetFolder);
        String newName = makeNewNameForCopiedPage(targetFolder, pageName);
        copiedPage.setName(newName);
        copiedPage.updateIds();
        copiedPage.setFolder(targetFolder);
        copiedPage.setOrder(targetFolder.getPages().size());
        pageConfigurationRepository.saveAndFlush(copiedPage);
        return newName;
    }

    /**
     * Move page and rename if existed.
     *
     * @param currentFolderId     UUID
     * @param pageName     String
     * @param targetFolderEntity FolderEntity
     * @return String, New name page's name
     */
    public String moveAndRename(UUID currentFolderId, String pageName, FolderEntity targetFolderEntity) {
        PageConfigurationEntity page = pageConfigurationRepository
                .getByFolderFolderIdAndName(currentFolderId, pageName);
        String newName = makeNewNameForCopiedPage(targetFolderEntity, pageName);
        page.setName(newName);
        page.setFolder(targetFolderEntity);
        page.setOrder(targetFolderEntity.getPages().size());
        pageConfigurationRepository.saveAndFlush(page);
        return newName;
    }

    @Transactional
    public void deletePageInFolder(String pageName, UUID projectId, String folderName) {
        pageConfigurationRepository.deletePage(pageName, projectId, folderName);
    }

    private String makeNewNameForCopiedPage(FolderEntity targetFolder, String pageName) {
        StringBuilder result = new StringBuilder(pageName);
        Set<String> pageNamesSet = getAllPagesInFolder(targetFolder.getFolderId()).stream()
                .map(PageConfiguration::getName)
                .collect(Collectors.toSet());
        int count = 0;
        int length = 0;
        while (pageNamesSet.contains(result.toString())) {
            if (count == 0) {
                result.append("_Copy");
                length = result.length();
                count++;
            } else {
                result.setLength(length);
                result.append('(').append(count).append(')');
                count++;
            }
        }
        return result.toString();
    }
}
