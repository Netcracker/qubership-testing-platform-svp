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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.qubership.atp.svp.model.configuration.KeyParameterConfiguration;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.ui.KeyParameterRequest;
import org.qubership.atp.svp.repo.impl.GitRepositoryImpl;
import org.qubership.atp.svp.service.direct.EurekaDiscoveryServiceImpl;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.KeyParameterServiceJpa;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class KeyParametersControllerTest extends TestWithTestData {

    private final String fileRepoFilePath = String.format("src/test/config/project/%s/key_parameters.json",
            fileProjectName);
    private final String gitRepoFilePath = String.format("src/test/config/project/%s/key_parameters.json",
            gitProjectName);

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private GitRepositoryImpl gitRepository;
    @SpyBean
    private ProjectConfigService projectConfigService;
    @MockBean
    FolderServiceJpa folderServiceJpa;
    @MockBean
    KeyParameterServiceJpa keyParameterServiceJpa;
    @MockBean
    EurekaDiscoveryServiceImpl eurekaDiscoveryServiceImpl;


    @After
    public void afterEach() throws IOException {
        Files.deleteIfExists(Paths.get(fileRepoFilePath));
        Files.deleteIfExists(Paths.get(gitRepoFilePath));
    }

    // GENERAL CASES

    @Test
    public void getKeyParameters_correctRequest_fileRepo_returnsKeyParameters() throws Exception {
        String testFile = loadFileToString("src/test/resources/test_data/key-parameters/key_parameters.json");
        writeToFile(fileRepoFilePath, testFile);
        List<KeyParameterConfiguration> expectedKeyParameters =
        Arrays.asList(objectMapper.readValue(testFile, KeyParameterConfiguration[].class));
        KeyParameterRequest request = new KeyParameterRequest();
        String body = objectMapper.writeValueAsString(request);
        FolderEntity folderEntity = new FolderEntity();
        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any())).thenReturn(folderEntity);
        when(keyParameterServiceJpa.getKeyParameters(any())).thenReturn(expectedKeyParameters);

        MvcResult response = mockMvc.perform(post("/api/svp/project/{project}/key-parameters", fileProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        List<KeyParameterConfiguration> actualKeyParameters = Arrays.asList(objectMapper
                .readValue(response.getResponse().getContentAsString(),
                        KeyParameterConfiguration[].class));

        Assert.assertEquals(expectedKeyParameters, actualKeyParameters);
    }

    // FILE REPO

    @Test
    public void updateKeyParameters_fileRepo_successfullyUpdated() throws Exception {
        String testFile = loadFileToString("src/test/resources/test_data/key-parameters/key_parameters.json");
        writeToFile(fileRepoFilePath, testFile);
        List<KeyParameterConfiguration> keyParametersBeforeChange = Arrays.asList(objectMapper.readValue(testFile,
                KeyParameterConfiguration[].class));

        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(fileProject));
        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setProject(projectConfigs);
        folderEntity.setKeyParameterEntities(new ArrayList<>());
        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any())).thenReturn(folderEntity);

        String newParamName = "Changed Param Name";
        KeyParameterConfiguration newKeyParameter = new KeyParameterConfiguration();
        newKeyParameter.setName(newParamName);

        keyParametersBeforeChange.set(0, newKeyParameter);
        KeyParameterRequest request = new KeyParameterRequest();
        request.setKeyParameters(keyParametersBeforeChange);
        request.setFolder("Default");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/svp/project/{project}/key-parameters/update", fileProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        List<KeyParameterConfiguration> keyParametersAfterChange = Arrays.asList(objectMapper.
                readValue(loadFileToString(fileRepoFilePath),
                        KeyParameterConfiguration[].class));

        Assert.assertEquals(keyParametersAfterChange.get(0).getName(), newParamName);
    }

    // GIT REPO
// todo will implemented when be DB for testing.
//
//    @Test
//    public void updateKeyParameters_gitRepo_successfullyUpdated() throws Exception {
//        String testFile = loadFileToString("src/test/resources/test_data/key-parameters"
//                + "/key_parameters.json");
//        writeToFile(gitRepoFilePath, testFile);
//        List<String> keyParametersBeforeChange = Arrays.asList(objectMapper.readValue(testFile,
//                String[].class));
//        String newParamName = "Changed Param Name";
//        getMockProjectConfigFromBd(gitProject, projectConfigService);
//        keyParametersBeforeChange.set(0, newParamName);
//        KeyParameterRequest request = new KeyParameterRequest();
//        request.setKeyParameters(keyParametersBeforeChange);
//        String body = objectMapper.writeValueAsString(request);
//
//        mockMvc.perform(post("/api/svp/project/{project}/key-parameters/update", gitProject)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().isOk());
//        List<String> keyParametersAfterChange = Arrays.asList(objectMapper.
//                readValue(loadFileToString(gitRepoFilePath),
//                        String[].class));
//
//        Assert.assertEquals(keyParametersAfterChange.get(0), newParamName);
//        verify(gitRepository, times(1))
//                .gitCommitAndPush(Mockito.anyString(), Mockito.anyString());
//    }
}
