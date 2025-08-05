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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.exceptions.PageStorageException;
import org.qubership.atp.svp.core.exceptions.page.PageCopyDbException;
import org.qubership.atp.svp.core.exceptions.page.PageDeleteDbException;
import org.qubership.atp.svp.core.exceptions.page.PageGetException;
import org.qubership.atp.svp.core.exceptions.page.PageMoveDbException;
import org.qubership.atp.svp.core.exceptions.page.PageOrderDbException;
import org.qubership.atp.svp.core.exceptions.page.PageSaveDbException;
import org.qubership.atp.svp.core.exceptions.page.PagesGetException;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.model.ui.UpdatePageOrderRequest;
import org.qubership.atp.svp.service.AbstractRepositoryConfigService;
import org.qubership.atp.svp.service.PageService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.PageConfigurationServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PageServiceImpl extends AbstractRepositoryConfigService implements PageService {

    private final FolderServiceJpa folderServiceJpa;
    private final PageConfigurationServiceJpa pageConfigurationServiceJpa;
    private final ProjectConfigService projectConfigService;
    private final ProjectConfigurationServiceJpa projectConfigServiceJpa;

    /**
     * Constructor PageServiceImpl.
     */
    @Autowired
    public PageServiceImpl(FolderServiceJpa folderServiceJpa,
                           PageConfigurationServiceJpa pageConfigurationServiceJpa,
                           ProjectConfigService projectConfigService,
                           ProjectConfigurationServiceJpa projectConfigServiceJpa) {
        this.folderServiceJpa = folderServiceJpa;
        this.pageConfigurationServiceJpa = pageConfigurationServiceJpa;
        this.projectConfigService = projectConfigService;
        this.projectConfigServiceJpa = projectConfigServiceJpa;
    }

    @Override
    public List<String> getPagesList(UUID projectId, String folderName) {
        log.info("Getting the names of configuration pages for projectId: {} in folder: {}", projectId, folderName);
        projectConfigService.initializeProjectConfigByEnv(projectId);
        try {
            FolderEntity folderEntity = folderServiceJpa.getFolderByProjectIdAndName(projectId, folderName);
            List<PageConfigurationEntity> pagesList = pageConfigurationServiceJpa
                    .getAllPagesEntityInFolder(folderEntity.getFolderId());

            return pagesList.stream()
                    .sorted(Comparator.comparing(PageConfigurationEntity::getOrder))
                    .map(PageConfigurationEntity::getName)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("PageServiceImpl - getPagesList(), projectId: {}, folder: {} ", projectId, folderName, e);
            throw new PagesGetException();
        }
    }

    private void resortOrderPagesAndSave(List<PageConfigurationEntity> pagesList) {
        int order = 0;
        pagesList = pagesList.stream()
                .sorted(Comparator.comparing(PageConfigurationEntity::getOrder))
                .collect(Collectors.toList());
        for (PageConfigurationEntity page : pagesList) {
            page.setOrder(order++);
        }
        pageConfigurationServiceJpa.saveListPage(pagesList);
    }

    @Override
    public PageConfiguration getPage(UUID projectId, String pageName, String folder) {
        log.info("Getting page configuration: {} for project:{} in folder: {}", projectId, folder, pageName);
        try {
            return pageConfigurationServiceJpa.getPageConfiguration(pageName, projectId, folder);
        } catch (DataAccessException e) {
            log.error("PageServiceImpl - getPage(), Page: {}, folder: {}, ProjectId: {}", pageName, folder,
                    projectId, e);
            throw new PageGetException();
        }
    }

    @Override
    @Transactional
    public void createOrUpdatePage(UUID projectId, PageConfiguration pageConfiguration, String oldPageName,
                                   String pageName, String folder) {
        log.info("Create or update configuration page - [{}] started for project: {} in folder: {}", oldPageName,
                projectId, folder);
        try {
            FolderEntity folderEntity = folderServiceJpa.getFolderByProjectIdAndName(projectId, folder);
            pageConfigurationServiceJpa.savePageInFolder(pageConfiguration, folderEntity);

            FolderEntity folderEntityNew = folderServiceJpa.getFolderByProjectIdAndName(projectId, folder);
            updateLocalAndGit(pageConfiguration, folder, folderEntityNew, oldPageName, pageName);
        } catch (DataAccessException e) {
            log.error("PageServiceImpl - createOrUpdatePage(),  Page: {}, folder: {}, ProjectId: {}", oldPageName,
                    folder, projectId);
            throw new PageSaveDbException();
        }
        String pageNameInfo = oldPageName.equals(pageName) ? oldPageName : oldPageName + " renamed to " + pageName;
        log.info("Create or update configuration page - [{}] successfully for project: {} in folder: {}",
                pageNameInfo, projectId, folder);
    }

    private void updateLocalAndGit(PageConfiguration pageConfiguration, String folder, FolderEntity folderEntity,
                                   String oldPageName, String pageName) {
        ProjectConfigsEntity projectEntity = folderEntity.getProject();
        getRepoForConfig(projectEntity).createOrUpdatePageConfiguration(projectEntity, pageConfiguration, folder);
        if (!oldPageName.equals(pageName)) {
            getRepoForConfig(projectEntity).deletePageConfigurations(projectEntity, oldPageName, folder);
        }
    }

    private void bulkUpdateLocalAndGit(List<PageConfiguration> pageConfigurationList, String folder,
                                       FolderEntity folderEntity) {
        ProjectConfigsEntity projectEntity = folderEntity.getProject();
        getRepoForConfig(projectEntity)
                .bulkCreateOrUpdatePageConfiguration(projectEntity, pageConfigurationList, folder);

    }

    @Override
    @Transactional
    public void deletePage(UUID projectId, String pageName, String folder) {
        log.info("Deleting configuration page: {} for project: {} in folder: {}, was started", pageName, projectId,
                folder);
        try {
            FolderEntity folderEntity = folderServiceJpa.getFolderByProjectIdAndName(projectId, folder);
            pageConfigurationServiceJpa.deletePageInFolder(pageName, projectId, folder);

            List<PageConfigurationEntity> pageConfigurationEntityList = pageConfigurationServiceJpa
                    .getAllPagesEntityInFolder(folderEntity.getFolderId());

            resortOrderPagesAndSave(pageConfigurationEntityList);
            deleteLocalAndGit(pageName, folder, projectId);
        } catch (DataAccessException e) {
            log.error("PageServiceImpl - deletePage(), Page: {}, folder: {}, ProjectId: {}",
                    pageName, folder, projectId, e);
            throw new PageDeleteDbException();
        }
        log.info("Configuration page [{}] for project: {} in folder: {} deleted successfully", pageName, projectId,
                folder);
    }

    private void deleteLocalAndGit(String pageName, String folder, UUID projectId) {
        ProjectConfigsEntity projectEntity = projectConfigServiceJpa.findProjectConfigById(projectId);
        getRepoForConfig(projectEntity).deletePageConfigurations(projectEntity, pageName, folder);
    }

    @Override
    @Transactional
    public void movePage(UUID projectId, String pageName, String currentFolder, String targetFolder)
            throws PageStorageException {
        log.info("Moving configuration page '{}' for project: {} from folder: {} to folder: {} started", pageName,
                projectId, currentFolder, targetFolder);
        try {
            FolderEntity currentFolderEntity = folderServiceJpa.getFolderByProjectIdAndName(projectId, currentFolder);
            FolderEntity targetFolderEntity = folderServiceJpa.getFolderByProjectIdAndName(projectId, targetFolder);
            String newPageName = pageConfigurationServiceJpa
                    .moveAndRename(currentFolderEntity.getFolderId(), pageName, targetFolderEntity);
            resortOrderPagesAndSave(targetFolderEntity.getPages());
            resortOrderPagesAndSave(currentFolderEntity.getPages());

            FolderEntity targetFolderEntityNew = folderServiceJpa.getFolderByProjectIdAndName(projectId, targetFolder);

            List<PageConfiguration> currentPageConfigurationList = pageConfigurationServiceJpa
                    .getAllPagesInFolder(currentFolderEntity.getFolderId());
            moveLocalAndGit(pageName, currentFolder, targetFolder, targetFolderEntityNew,
                    currentPageConfigurationList, newPageName);
        } catch (DataAccessException e) {
            log.error("PageServiceImpl - movePage(),  Page: {}, folder from: {}, folder to {}, ProjectId: {}", pageName,
                    currentFolder, targetFolder, projectId, e);
            throw new PageMoveDbException();
        }
        log.info("Configuration page '{}' for project: {} from folder: {} to folder: {} moved successfully", pageName,
                projectId, currentFolder, targetFolder);
    }

    @Override
    @Transactional
    public void updatePageOrder(UUID projectId, UpdatePageOrderRequest request)
            throws PageStorageException {
        log.info("Change order for page '{}' for project: {} from folder: {} by order: {}",
                request.getPage(), projectId, request.getFolder(), request.getOrder());
        FolderEntity folder = folderServiceJpa.getFolderByProjectIdAndName(projectId, request.getFolder());

        drugAndDropOrderPage(folder.getPages(), request.getOrder(), request.getPage());

        List<PageConfiguration> pageConfigurationList = pageConfigurationServiceJpa
                .getAllPagesInFolder(folder.getFolderId());
        bulkUpdateLocalAndGit(pageConfigurationList, request.getFolder(), folder);
    }

    private void drugAndDropOrderPage(List<PageConfigurationEntity> pagesList,
                                      int newOrder, String pageName) {
        Optional<PageConfigurationEntity> optionalPage = pagesList.stream()
                .filter(page -> page.getName().equals(pageName))
                .findFirst();
        pagesList = pagesList.stream()
                .sorted(Comparator.comparing(PageConfigurationEntity::getOrder))
                .collect(Collectors.toList());
        if (optionalPage.isPresent()) {
            pagesList.remove(optionalPage.get());
            pagesList.add(newOrder, optionalPage.get());
            for (int i = 0; i < pagesList.size(); i++) {
                pagesList.get(i).setOrder(i);
            }
            log.info("Order is not changed on new order {} of page: {}", newOrder, pageName);
            pageConfigurationServiceJpa.saveListPage(pagesList);

        } else {
            log.error("The warrant number can't change to the same one");
            throw new PageOrderDbException();
        }
    }

    private void moveLocalAndGit(String pageName, String currentFolder, String targetFolder,
                                 FolderEntity targetFolderEntity,
                                 List<PageConfiguration> currentFolderEntityNew, String newPageName) {
        ProjectConfigsEntity projectEntity = targetFolderEntity.getProject();
        int newOrder = targetFolderEntity.getPages().size();
        getRepoForConfig(projectEntity)
                .movePage(projectEntity, pageName, newPageName, currentFolder, targetFolder, newOrder);
        getRepoForConfig(projectEntity)
                .bulkCreateOrUpdatePageConfiguration(
                        projectEntity, currentFolderEntityNew, currentFolder);
    }

    @Override
    @Transactional
    public void copyPage(UUID projectId, String pageName, String currentFolder, String targetFolder) {
        log.info("Coping configuration page '{}' for project: {} from folder: {} to folder: {} started", pageName,
                projectId, currentFolder, targetFolder);
        try {
            FolderEntity targetFolderEntity = folderServiceJpa.getFolderByProjectIdAndName(projectId, targetFolder);
            String newName = pageConfigurationServiceJpa.copyPageAndRename(projectId, currentFolder, pageName,
                    targetFolderEntity);

            FolderEntity targetFolderEntityNew = folderServiceJpa.getFolderByProjectIdAndName(projectId, targetFolder);
            copyLocalAndGit(pageName, currentFolder, targetFolder, targetFolderEntityNew, newName);
        } catch (DataAccessException e) {
            log.error("PageServiceImpl - copyPage(),  Page: {}, folder from: {}, folder to: {}, ProjectId: {}",
                    pageName, currentFolder, targetFolder, projectId, e);
            throw new PageCopyDbException();
        }
        log.info("Configuration page '{}' for project: {} from folder: {} to folder: {} copied successfully",
                pageName, projectId, currentFolder, targetFolder);
    }

    private void copyLocalAndGit(String pageName, String currentFolder, String targetFolder,
                                 FolderEntity targetFolderEntity, String newName) {
        ProjectConfigsEntity projectEntity = targetFolderEntity.getProject();
        int newOrder = targetFolderEntity.getPages().size();
        getRepoForConfig(projectEntity)
                .copyPage(projectEntity, pageName, newName, currentFolder, targetFolder, newOrder);
    }
}
