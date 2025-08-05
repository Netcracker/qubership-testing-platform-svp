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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import org.qubership.atp.svp.core.exceptions.project.ProjectNotFoundException;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.folder.FolderRequest;
import org.qubership.atp.svp.service.direct.EurekaDiscoveryServiceImpl;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class FolderControllerTest extends TestWithTestData {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    ProjectConfigService configService;
    @MockBean
    ProjectConfigurationServiceJpa projectConfigServiceJpa;

    @MockBean
    FolderServiceJpa folderServiceJpa;

    @MockBean
    EurekaDiscoveryServiceImpl eurekaDiscoveryServiceImpl;

    @After
    public void afterEach() throws IOException {
        FileUtils.deleteDirectory(new File("src/test/config/project/test/folder"));
        FileUtils.deleteDirectory(new File("src/test/config/project/folder_project/folderNew"));
    }

    @Test
    public void createFolder_projectIsNotConfigured_status404() throws Exception {
        String notConfiguredProjectId = "11111111-1111-1111-1111-11111111111";
        String urlToDeletePageForNotConfiguredProject =
                String.format("/api/svp/project/%s/folders/create", notConfiguredProjectId);
        FolderRequest request = new FolderRequest();
        request.setName("folder");
        String body = objectWriter.writeValueAsString(request);
        // ProjectConfiguration mock
        Mockito.when(projectConfigServiceJpa.findProjectConfigById(any())).thenThrow(new ProjectNotFoundException());

        mockMvc.perform(post(urlToDeletePageForNotConfiguredProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(404));
    }

    @Test
    public void createFolder_projectIsConfigured_status200() throws Exception {
        String urlToDeletePageForNotConfiguredProject =
                String.format("/api/svp/project/%s/folders/create", fileProject);
        FolderRequest request = new FolderRequest();
        request.setName("folder");
        String body = objectWriter.writeValueAsString(request);
        // ProjectConfiguration mock
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(fileProject));
        Mockito.when(projectConfigServiceJpa.findProjectConfigById(any())).thenReturn(projectConfigs);
        FolderEntity folder = new FolderEntity();
        folder.setFolderId(UUID.randomUUID());
        folder.setName("folder");
        folder.setProject(projectConfigs);
        Mockito.when(folderServiceJpa.create(any(), anyString())).thenReturn(folder);

        mockMvc.perform(post(urlToDeletePageForNotConfiguredProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(200));

        Assert.assertTrue(Paths.get("src/test/config/project/test/folder").toFile().exists());
        Assert.assertTrue(Paths.get("src/test/config/project/test/folder/folder_config.json").toFile().exists());

    }

    @Test
    public void createFolder_projectIsConfiguredAndFolderAlreadyExists_status500() throws Exception {
        String urlToDeletePageForNotConfiguredProject =
                String.format("/api/svp/project/%s/folders/create", folderProject);
        FolderRequest request = new FolderRequest();
        request.setName("folder");
        String body = objectWriter.writeValueAsString(request);
        // ProjectConfiguration mock
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(folderProject));
        Mockito.when(projectConfigServiceJpa.findProjectConfigById(any())).thenReturn(projectConfigs);

        mockMvc.perform(post(urlToDeletePageForNotConfiguredProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(500));
    }

    @Test
    public void deleteFolder_projectIsNotConfigured_status404() throws Exception {
        String notConfiguredProjectId = "11111111-1111-1111-1111-11111111111";
        String urlToDeletePageForNotConfiguredProject =
                String.format("/api/svp/project/%s/folders", notConfiguredProjectId);
        FolderRequest request = new FolderRequest();
        request.setName("folder");
        String body = objectWriter.writeValueAsString(request);
        // ProjectConfiguration mock
        Mockito.when(projectConfigServiceJpa.findProjectConfigById(any())).thenThrow(new ProjectNotFoundException());

        mockMvc.perform(delete(urlToDeletePageForNotConfiguredProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(404));
    }

    @Test
    public void deleteFolder_folderIsExists_status200() throws Exception {
        String urlToDeletePageForNotConfiguredProject =
                String.format("/api/svp/project/%s/folders", folderProject);
        Path deleteFolder = Paths.get("src/test/config/project/folder_project/DELETE_ME");
        Files.createDirectories(deleteFolder);
        // ProjectConfiguration mock
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(folderProject));
        Mockito.when(projectConfigServiceJpa.findProjectConfigById(any())).thenReturn(projectConfigs);
        Mockito.when(projectConfigServiceJpa.isExistsProjectConfigById(any())).thenReturn(true);

        FolderRequest request = new FolderRequest();
        request.setName("DELETE_ME");
        String body = objectWriter.writeValueAsString(request);

        mockMvc.perform(delete(urlToDeletePageForNotConfiguredProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(200));
        Assert.assertFalse(deleteFolder.toFile().exists());
    }

    @Test
    public void editFolder_projectIsNotConfigured_status404() throws Exception {
        String notConfiguredProjectId = "11111111-1111-1111-1111-11111111111";
        String urlToDeletePageForNotConfiguredProject =
                String.format("/api/svp/project/%s/folders", notConfiguredProjectId);
        FolderRequest request = new FolderRequest();
        request.setName("folder");
        request.setOldName("folderOld");
        String body = objectWriter.writeValueAsString(request);
        // ProjectConfiguration mock
        Mockito.when(projectConfigServiceJpa.findProjectConfigById(any())).thenThrow(new ProjectNotFoundException());

        mockMvc.perform(post(urlToDeletePageForNotConfiguredProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(404));
    }

    @Test
    public void editFolder_projectIsConfiguredFolderIsExist_status200() throws Exception {
        String urlToDeletePageForNotConfiguredProject =
                String.format("/api/svp/project/%s/folders", folderProject);
        Path copiedFolder = Paths.get("src/test/config/project/folder_project/folder");
        Path newFolder = Paths.get("src/test/config/project/folder_project/folderOld");
        Files.copy(copiedFolder, newFolder, REPLACE_EXISTING);
        FolderRequest request = new FolderRequest();
        request.setName("folderNew");
        request.setOldName("folderOld");
        String body = objectWriter.writeValueAsString(request);
        // ProjectConfiguration mock
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(folderProject));
        Mockito.when(projectConfigServiceJpa.findProjectConfigById(any())).thenReturn(projectConfigs);
        Mockito.when(projectConfigServiceJpa.isExistsProjectConfigById(any())).thenReturn(true);
        FolderEntity folder = new FolderEntity();
        folder.setFolderId(UUID.randomUUID());
        folder.setName("folderNew");
        folder.setProject(projectConfigs);
        Mockito.when(folderServiceJpa.editFolder(any(), anyString(), anyString())).thenReturn(folder);

        mockMvc.perform(post(urlToDeletePageForNotConfiguredProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(200));

        Assert.assertTrue(Paths.get("src/test/config/project/folder_project/folderNew").toFile().exists());
        Assert.assertTrue(Paths.get("src/test/config/project/folder_project/folderNew/folder_config.json").toFile().exists());
    }

    @Test
    public void getFolders_projectIsConfigured_status200() throws Exception {
        String urlToDeletePageForNotConfiguredProject =
                String.format("/api/svp/project/%s/folders", folderProject);
        // ProjectConfiguration mock
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(folderProject));
        Mockito.when(projectConfigServiceJpa.findProjectConfigById(any())).thenReturn(projectConfigs);
        Mockito.when(projectConfigServiceJpa.isExistsProjectConfigById(any())).thenReturn(true);

        mockMvc.perform(get(urlToDeletePageForNotConfiguredProject))
                .andExpect(status().is(200));
    }
}
