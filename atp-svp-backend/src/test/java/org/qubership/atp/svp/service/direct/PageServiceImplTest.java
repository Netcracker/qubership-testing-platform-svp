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

import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.ui.UpdatePageOrderRequest;
import org.qubership.atp.svp.repo.impl.FilePageConfigurationRepository;
import org.qubership.atp.svp.repo.impl.GitPageConfigurationRepository;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.PageConfigurationServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.qubership.atp.svp.tests.DbMockEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.qubership.atp.svp.tests.DbMockEntity.generatedListPageConfigurationEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PageServiceImpl.class)
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
class PageServiceImplTest {

    @Autowired
    PageServiceImpl pageService;

    @MockBean
    FolderServiceJpa folderServiceJpa;
    @MockBean
    PageConfigurationServiceJpa pageConfigurationServiceJpa;
    @MockBean
    ProjectConfigService projectConfigService;
    @MockBean
    ProjectConfigurationServiceJpa projectConfigServiceJpa;

    @MockBean
    FilePageConfigurationRepository fileRepo;
    @MockBean
    GitPageConfigurationRepository gitRepo;

    @Test
    void updatePageOrder_whenMoveUpPage() throws IOException {
        UUID projectId = UUID.randomUUID();
        FolderEntity folderEntity = DbMockEntity.generateFolderEntity();
        List<PageConfigurationEntity> primaryListPages = generatedListPageConfigurationEntity();
        folderEntity.setPages(primaryListPages);
        ArgumentCaptor<List<PageConfigurationEntity>> captor = ArgumentCaptor.forClass(List.class);

        UpdatePageOrderRequest request = new UpdatePageOrderRequest();
        request.setPage("SOI");
        request.setFolder("folderName");
        request.setOrder(1);

        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any()))
                .thenReturn(folderEntity);
        Mockito.doNothing().when(pageConfigurationServiceJpa)
                .saveListPage(captor.capture());


        pageService.updatePageOrder(projectId, request);

        List<PageConfigurationEntity> expectedList = captor.getValue().stream()
                .sorted(Comparator.comparing(PageConfigurationEntity::getOrder))
                .collect(Collectors.toList());
        PageConfigurationEntity pageConfigurationEntity = expectedList.stream()
                .filter(page -> page.getName().equals("SOI")).findFirst().get();

        Assertions.assertEquals("SP", expectedList.get(0).getName());
        Assertions.assertEquals(request.getOrder(), pageConfigurationEntity.getOrder());
        Assertions.assertEquals(request.getPage(), pageConfigurationEntity.getName());
        Assertions.assertEquals("I", expectedList.get(2).getName());
        Assertions.assertEquals("CI", expectedList.get(3).getName());
        Assertions.assertEquals(4, expectedList.size());
    }

    @Test
    void updatePageOrder_whenMoveDownPage() throws IOException {
        UUID projectId = UUID.randomUUID();
        FolderEntity folderEntity = DbMockEntity.generateFolderEntity();
        List<PageConfigurationEntity> primaryListPages = generatedListPageConfigurationEntity();
        folderEntity.setPages(primaryListPages);
        ArgumentCaptor<List<PageConfigurationEntity>> captor = ArgumentCaptor.forClass(List.class);
        UpdatePageOrderRequest request = new UpdatePageOrderRequest();
        request.setPage("SP");
        request.setFolder("folderName");
        request.setOrder(2);

        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any()))
                .thenReturn(folderEntity);
        Mockito.doNothing().when(pageConfigurationServiceJpa)
                .saveListPage(captor.capture());


        pageService.updatePageOrder(projectId, request);

        List<PageConfigurationEntity> expectedList = captor.getValue().stream()
                .sorted(Comparator.comparing(PageConfigurationEntity::getOrder))
                .collect(Collectors.toList());
        PageConfigurationEntity pageConfigurationEntity = expectedList.stream()
                .filter(page -> page.getName().equals("SP")).findFirst().get();

        Assertions.assertEquals("I", expectedList.get(0).getName());
        Assertions.assertEquals("SOI", expectedList.get(1).getName());
        Assertions.assertEquals(request.getOrder(), pageConfigurationEntity.getOrder());
        Assertions.assertEquals(request.getPage(), pageConfigurationEntity.getName());
        Assertions.assertEquals("CI", expectedList.get(3).getName());
        Assertions.assertEquals(4, expectedList.size());
    }
}
