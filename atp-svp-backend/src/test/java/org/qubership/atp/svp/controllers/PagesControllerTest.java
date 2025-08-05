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

package org.qubership.atp.svp.controllers;

import static org.qubership.atp.svp.tests.DbMockEntity.generatedListPageConfigurationEntity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.qubership.atp.svp.core.exceptions.project.ProjectNotFoundException;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.model.ui.PageRequest;
import org.qubership.atp.svp.repo.impl.GitRepositoryImpl;
import org.qubership.atp.svp.service.direct.EurekaDiscoveryServiceImpl;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.PageConfigurationServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class PagesControllerTest extends TestWithTestData {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitRepositoryImpl gitRepository;

    @MockBean
    private ProjectConfigurationServiceJpa projectConfigurationServiceJpa;
    @MockBean
    FolderServiceJpa folderServiceJpa;
    @MockBean
    PageConfigurationServiceJpa pageConfigurationServiceJpa;
    @MockBean
    ProjectConfigService projectConfigService;
    @MockBean
    EurekaDiscoveryServiceImpl eurekaDiscoveryServiceImpl;

    private final String fileName = "DELETE_ME";
    private final String changedFileName = "CHANGED_FILE_DELETE_ME";
    private final String fileRepoFilePath = String.format("src/test/config/project/%s/pages/%s", fileProjectName,
            fileName);
    private final String fileRepoChangedFilePath = String.format("src/test/config/project/%s/pages/%s",
            fileProjectName, changedFileName);
    private final String gitRepoFilePath = String.format("src/test/config/project/%s/pages/%s", gitProjectName,
            fileName);
    private final String gitRepoChangedFilePath = String.format("src/test/config/project/%s/pages/%s",
            gitProjectName,
            changedFileName);
    private final String folderRepoFilePath = String.format("src/test/config/project/folder_project/pages/%s",
            fileName);
    private final String folderRepoInFolderFilePath = String.format("src/test/config/project/folder_project"
                    + "/folder"
                    + "/pages/%s",
            fileName);

    @After
    public void afterEach() throws IOException {
        Files.deleteIfExists(Paths.get(fileRepoFilePath));
        Files.deleteIfExists(Paths.get(fileRepoChangedFilePath));
        Files.deleteIfExists(Paths.get(gitRepoFilePath));
        Files.deleteIfExists(Paths.get(gitRepoChangedFilePath));
        Files.deleteIfExists(Paths.get(folderRepoFilePath));
        Files.deleteIfExists(Paths.get(folderRepoInFolderFilePath));
    }

    // GENERAL CASES
    @Test
    public void deletePage_projectIsNotConfigured_status404() throws Exception {
        String notConfiguredProjectId = "11111111-1111-1111-1111-11111111111";
        String urlToDeletePageForNotConfiguredProject =
                String.format("/api/svp/project/%s/pages", notConfiguredProjectId);
        PageRequest request = new PageRequest();
        request.setName("CustomerInfo");

        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any()))
                .thenReturn(generateFolderEntity());
        when(pageConfigurationServiceJpa.getAllPagesEntityInFolder(any()))
                .thenReturn(generatedListPageConfigurationEntity());
        doThrow(ProjectNotFoundException.class).when(projectConfigurationServiceJpa).findProjectConfigById(any());
        doNothing().when(pageConfigurationServiceJpa).deletePageInFolder(any(), any(), any());

        String body = objectWriter.writeValueAsString(request);
        mockMvc.perform(delete(urlToDeletePageForNotConfiguredProject)
                .contentType(APPLICATION_JSON)
                .content(body)).andExpect(status().is(404));
    }

    @Test
    public void getPages_correctRequest_fileRepo_returnsListOfPages() throws Exception {
        List<String> expectedPages = Arrays.asList("CI", "SOI", "SP", "I");
        PageRequest request = new PageRequest();
        request.setFolder("Default");
        String body = objectWriter.writeValueAsString(request);

        List<PageConfigurationEntity> pagesListe = generatedListPageConfigurationEntity();

        when(folderServiceJpa.getFolderByProjectIdAndName(any(), anyString()))
                .thenReturn(generateFolderEntity());
        when(pageConfigurationServiceJpa.getAllPagesEntityInFolder(any()))
                .thenReturn(pagesListe);

        MvcResult response = mockMvc.perform(post("/api/svp/project/{project}/pages", fileProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        List<String> pagesList = Arrays.asList(objectMapper.readValue(response.getResponse().getContentAsString(),
                String[].class));

        Assert.assertEquals(expectedPages.size(), pagesList.size());
        Assert.assertEquals(pagesListe.get(2).getName(), pagesList.get(0));
        Assert.assertTrue(expectedPages.containsAll(pagesList));
    }

    @Test
    public void getPage_correctRequest_fileRepo_returnsPageConfiguration() throws Exception {
        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
                + "/ConfigWithoutPreconfiguredParams");
        writeToFile(fileRepoFilePath, testFile);
        PageConfiguration expectedPageConfig = objectMapper.readValue(testFile, PageConfiguration.class);
        when(pageConfigurationServiceJpa.getPageConfiguration(anyString(), any(), anyString())).thenReturn(expectedPageConfig);
        PageRequest request = new PageRequest();
        request.setName(fileName);
        request.setFolder("Default");
        String body = objectWriter.writeValueAsString(request);

        MvcResult response = mockMvc.perform(post("/api/svp/project/{project}/pages/page", fileProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        PageConfiguration actualPageConfig = objectMapper.readValue(response.getResponse().getContentAsString(),
                PageConfiguration.class);

        Assert.assertEquals(expectedPageConfig, actualPageConfig);
    }

    // FILE REPO

    @Test
    public void deletePage_fileRepo_fileExists_noPreconfiguredParameters_getRemoveCommitAndPushIsCalled()
            throws Exception {
        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
                + "/ConfigWithoutPreconfiguredParams");
        writeToFile(fileRepoFilePath, testFile);
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(fileProject));

        PageRequest request = new PageRequest();
        request.setName(fileName);
        request.setFolder("Default");
        String body = objectWriter.writeValueAsString(request);

        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any()))
                .thenReturn(generateFolderEntity());
        when(projectConfigurationServiceJpa.findProjectConfigById(any()))
                .thenReturn(projectConfigs);
        doNothing().when(pageConfigurationServiceJpa).deletePageInFolder(any(), any(), any());
        when(pageConfigurationServiceJpa.getAllPagesEntityInFolder(any(UUID.class)))
                .thenReturn(generatedListPageConfigurationEntity());

        mockMvc.perform(delete("/api/svp/project/{project}/pages", fileProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Assert.assertFalse(Paths.get(fileRepoFilePath).toFile().exists());
    }

    @Test
    public void createOrUpdatePage_fileRepo_fileExists_renamePage_successfullyRenamed() throws Exception {
        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
                + "/ConfigWithoutPreconfiguredParams");
        writeToFile(fileRepoFilePath, testFile);
        PageConfiguration pageConfigBeforeChange = objectMapper
                .readValue(loadFileToString(fileRepoFilePath), PageConfiguration.class);
        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setFolderId(UUID.randomUUID());
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(fileProject));
        folderEntity.setProject(projectConfigs);
        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any())).thenReturn(folderEntity);

        String changedName = changedFileName;
        pageConfigBeforeChange.setName(changedName);
        PageRequest request = new PageRequest();
        request.setName("NOSUCHFILE");
        request.setPage(pageConfigBeforeChange);
        request.setOldPageName(fileName);
        request.setFolder("Default");
        String body = objectWriter.writeValueAsString(request);

        mockMvc.perform(post("/api/svp/project/{project}/pages/create-or-update",
                        fileProject, fileName)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        PageConfiguration pageConfigAfterChange = objectMapper
                .readValue(loadFileToString(fileRepoChangedFilePath), PageConfiguration.class);

        Assert.assertFalse(Paths.get(fileRepoFilePath).toFile().exists());
        Assert.assertEquals(pageConfigAfterChange.getName(), changedName);
    }

    @Test
    public void createOrUpdatePage_fileRepo_fileExists_successfullyChanged() throws Exception {
        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
                + "/ConfigWithoutPreconfiguredParams");
        writeToFile(fileRepoFilePath, testFile);
        PageConfiguration pageConfigBeforeChange = objectMapper
                .readValue(loadFileToString(fileRepoFilePath), PageConfiguration.class);
        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setFolderId(UUID.randomUUID());
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(fileProject));
        folderEntity.setProject(projectConfigs);
        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any())).thenReturn(folderEntity);
//        when(folderServiceJpa.getFolder(any())).thenReturn(folderEntity);

        String newTabName = "Changed Tab Name";
        pageConfigBeforeChange.getTabs().get(0).setName(newTabName);
        PageRequest request = new PageRequest();
        request.setPage(pageConfigBeforeChange);
        request.setOldPageName(fileName);
        request.setName(fileName);
        request.setFolder("Default");
        String body = objectWriter.writeValueAsString(request);

        mockMvc.perform(post("/api/svp/project/{project}/pages/create-or-update",
                        fileProject, fileName)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        PageConfiguration pageConfigAfterChange = objectMapper
                .readValue(loadFileToString(fileRepoFilePath), PageConfiguration.class);

        Assert.assertEquals(pageConfigAfterChange.getTabs().get(0).getName(), newTabName);
    }

    @Test
    public void createOrUpdatePage_fileRepo_fileDoesntExists_successfullyCreated() throws Exception {
        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
                + "/ConfigWithoutPreconfiguredParams");
        PageConfiguration pageConfigSample = objectMapper
                .readValue(testFile, PageConfiguration.class);

        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setFolderId(UUID.randomUUID());
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(fileProject));
        folderEntity.setProject(projectConfigs);
        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any())).thenReturn(folderEntity);
//        when(folderServiceJpa.getFolder(any())).thenReturn(folderEntity);

        PageRequest request = new PageRequest();
        request.setPage(pageConfigSample);
        request.setOldPageName(fileName);
        request.setName(fileName);
        request.setFolder("Default");
        String body = objectWriter.writeValueAsString(request);

        mockMvc.perform(post("/api/svp/project/{project}/pages/create-or-update",
                        fileProject, fileRepoFilePath)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        PageConfiguration createdPageConfig = objectMapper
                .readValue(loadFileToString(fileRepoFilePath), PageConfiguration.class);

        Assert.assertEquals(pageConfigSample, createdPageConfig);
    }

    @Test
    public void copyPage_fileIsExists_successfullyCopy() throws Exception {
        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
                + "/ConfigWithoutPreconfiguredParams");
        writeToFile(folderRepoFilePath, testFile);
        PageRequest request = new PageRequest();
        request.setTargetFolder("folder");
        request.setName(fileName);
        request.setFolder("Default");
        String body = objectWriter.writeValueAsString(request);

        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(folderProject));
        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setFolderId(UUID.randomUUID());
        folderEntity.setProject(projectConfigs);
        folderEntity.setPages(generatedListPageConfigurationEntity());

        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any())).thenReturn(folderEntity);
//        when(folderServiceJpa.getFolder(any())).thenReturn(folderEntity);
        when(pageConfigurationServiceJpa.copyPageAndRename(any(), anyString(), anyString(), any())).thenReturn(fileName);

        mockMvc.perform(post("/api/svp/project/{project}/pages/copy", folderProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(200));

        Assert.assertTrue(Paths.get(folderRepoFilePath).toFile().exists());
        Assert.assertTrue(Paths.get(folderRepoInFolderFilePath).toFile().exists());
    }

    @Test
    public void movePage_fileIsExists_successfullyMove() throws Exception {
        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
                + "/ConfigWithoutPreconfiguredParams");
        writeToFile(folderRepoFilePath, testFile);
        PageRequest request = new PageRequest();
        request.setName(fileName);
        request.setFolder(null);
        request.setTargetFolder("folder");
        request.setFolder("Default");
        String body = objectWriter.writeValueAsString(request);

        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setFolderId(UUID.randomUUID());
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(folderProject));
        folderEntity.setProject(projectConfigs);
        folderEntity.setPages(generatedListPageConfigurationEntity());

        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any())).thenReturn(folderEntity);
//        when(folderServiceJpa.getFolder(any())).thenReturn(folderEntity);
        when(pageConfigurationServiceJpa.moveAndRename(any(), anyString(), any())).thenReturn(fileName);

        mockMvc.perform(post("/api/svp/project/{project}/pages/move", folderProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(200));

        Assert.assertTrue(Paths.get(folderRepoInFolderFilePath).toFile().exists());
        Assert.assertFalse(Paths.get(folderRepoFilePath).toFile().exists());
    }


    FolderEntity generateFolderEntity() throws IOException {
        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setFolderId(UUID.randomUUID());
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(folderProject));
        folderEntity.setProject(projectConfigs);
        return folderEntity;
    }


//    @Test
//    public void movePage_projectIsNotConfigured_status404() throws Exception {
//        String notConfiguredProjectId = "11111111-1111-1111-1111-11111111111";
//        PageRequest request = new PageRequest();
//        request.setName("CustomerInfo");
//        String body = objectWriter.writeValueAsString(request);
//        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any())).thenThrow(ProjectConfigException.class);
//
//        mockMvc.perform(post("/api/svp/project/{project}/pages/move", notConfiguredProjectId)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().is(404));
//    }

//    @Test
//    public void copyPage_projectIsNotConfigured_status404() throws Exception {
//        String notConfiguredProjectId = "11111111-1111-1111-1111-11111111111";
//        PageRequest request = new PageRequest();
//        request.setName("CustomerInfo");
//        String body = objectWriter.writeValueAsString(request);
//        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any())).thenThrow(ProjectConfigException.class);
//
//        mockMvc.perform(post("/api/svp/project/{project}/pages/move", notConfiguredProjectId)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().is(404));
//    }

    // GIT REPO
    // todo will implemented when be DB for testing.

//    @Test
//    public void deletePage_gitRepo_fileExists_noPreconfiguredParameters_getRemoveCommitAndPushIsCalled() throws
//    Exception {
//        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
//                + "/ConfigWithoutPreconfiguredParams");
//        writeToFile(gitRepoFilePath, testFile);
//        getMockProjectConfigFromBd(gitProject, projectConfigService);
//        PageRequest request = new PageRequest();
//        request.setName(fileName);
//        String body = objectWriter.writeValueAsString(request);
//
//        mockMvc.perform(delete("/api/svp/project/{project}/pages", gitProject)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().isOk());
//
//        Assert.assertFalse(Paths.get(gitRepoFilePath).toFile().exists());
//        verify(gitRepository, times(1))
//                .gitRemoveCommitAndPush(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
//    }
//
//    @Test
//    public void deletePage_gitRepo_fileExists_hasPreconfiguredParameters_status500() throws Exception {
//        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
//                + "/ConfigWithPreconfiguredParams");
//        writeToFile(gitRepoFilePath, testFile);
//        getMockProjectConfigFromBd(gitProject, projectConfigService);
//        PageRequest request = new PageRequest();
//        request.setName(fileName);
//        String body = objectWriter.writeValueAsString(request);
//
//        mockMvc.perform(delete("/api/svp/project/{project}/pages/page", gitProject)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().is(500));
//
//        Assert.assertTrue(Paths.get(gitRepoFilePath).toFile().exists());
//        verify(gitRepository, never())
//                .gitRemoveCommitAndPush(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
//    }
//
//    @Test
//    public void deletePage_gitRepo_fileIsNotJson_status500_fileIsNotDeleted() throws Exception {
//        String testFile = loadFileToString("src/test/resources/test_data/page_configurations/not_json");
//        writeToFile(gitRepoFilePath, testFile);
//        getMockProjectConfigFromBd(gitProject, projectConfigService);
//        PageRequest request = new PageRequest();
//        request.setName(fileName);
//        String body = objectWriter.writeValueAsString(request);
//
//        mockMvc.perform(delete("/api/svp/project/{project}/pages", gitProject)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().is(500));
//
//        Assert.assertTrue(Paths.get(gitRepoFilePath).toFile().exists());
//    }
//
//    @Test
//    public void deletePage_gitRepo_fileDoesntExist_status404() throws Exception {
//        getMockProjectConfigFromBd(gitProject, projectConfigService);
//        PageRequest request = new PageRequest();
//        request.setName("NOSUCHFILE");
//        String body = objectWriter.writeValueAsString(request);
//
//        mockMvc.perform(delete("/api/svp/project/{project}/pages", gitProject)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().is(404));
//    }
//
//    @Test
//    public void createOrUpdatePage_gitRepo_fileExists_renamePage_successfullyRenamed() throws Exception {
//        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
//                + "/ConfigWithoutPreconfiguredParams");
//        writeToFile(gitRepoFilePath, testFile);
//        PageConfiguration pageConfigBeforeChange = objectMapper
//                .readValue(loadFileToString(gitRepoFilePath), PageConfiguration.class);
//        String changedName = changedFileName;
//        pageConfigBeforeChange.setName(changedName);
//        getMockProjectConfigFromBd(gitProject, projectConfigService);
//        PageRequest request = new PageRequest();
//        request.setPage(pageConfigBeforeChange);
//        request.setOldPageName(fileName);
//        String body = objectWriter.writeValueAsString(request);
//
//        mockMvc.perform(post("/api/svp/project/{project}/pages/create-or-update",
//                        gitProject, fileName)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().isOk());
//        PageConfiguration pageConfigAfterChange = objectMapper
//                .readValue(loadFileToString(gitRepoChangedFilePath), PageConfiguration.class);
//
//        Assert.assertFalse(Paths.get(gitRepoFilePath).toFile().exists());
//        Assert.assertEquals(pageConfigAfterChange.getName(), changedName);
//        verify(gitRepository, times(1))
//                .gitAddAllFiles(Mockito.anyString());
//        verify(gitRepository, times(1))
//                .gitRemoveCommitAndPush(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
//    }
//
//    @Test
//    public void createOrUpdatePage_gitRepo_fileExists_successfullyChanged() throws Exception {
//        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
//                + "/ConfigWithoutPreconfiguredParams");
//        writeToFile(gitRepoFilePath, testFile);
//        PageConfiguration pageConfigBeforeChange = objectMapper
//                .readValue(loadFileToString(gitRepoFilePath), PageConfiguration.class);
//        String newTabName = "Changed Tab Name";
//        getMockProjectConfigFromBd(gitProject, projectConfigService);
//
//        pageConfigBeforeChange.getTabs().get(0).setName(newTabName);
//        PageRequest request = new PageRequest();
//        request.setPage(pageConfigBeforeChange);
//        request.setOldPageName(fileName);
//        String body = objectWriter.writeValueAsString(request);
//
//        mockMvc.perform(post("/api/svp/project/{project}/pages/create-or-update",
//                        gitProject)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().isOk());
//        PageConfiguration pageConfigAfterChange = objectMapper
//                .readValue(loadFileToString(gitRepoFilePath), PageConfiguration.class);
//
//        Assert.assertEquals(pageConfigAfterChange.getTabs().get(0).getName(), newTabName);
//        verify(gitRepository, times(1))
//                .gitCommitAndPush(Mockito.anyString(), Mockito.anyString());
//    }
//
//    @Test
//    public void createOrUpdatePage_gitRepo_fileDoesntExists_successfullyCreated() throws Exception {
//        String testFile = loadFileToString("src/test/resources/test_data/page_configurations"
//                + "/ConfigWithoutPreconfiguredParams");
//        PageConfiguration pageConfigSample = objectMapper
//                .readValue(testFile, PageConfiguration.class);
//        getMockProjectConfigFromBd(gitProject, projectConfigService);
//
//        PageRequest request = new PageRequest();
//        request.setPage(pageConfigSample);
//        request.setName(gitRepoFilePath);
//        request.setOldPageName(fileName);
//        String body = objectWriter.writeValueAsString(request);
//
//        mockMvc.perform(post("/api/svp/project/{project}/pages/create-or-update",
//                        gitProject)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().isOk());
//
//        PageConfiguration createdPageConfig = objectMapper
//                .readValue(loadFileToString(gitRepoFilePath), PageConfiguration.class);
//
//        Assert.assertEquals(pageConfigSample, createdPageConfig);
//        verify(gitRepository, times(1))
//                .gitCommitAndPush(Mockito.anyString(), Mockito.anyString());
//    }
}
