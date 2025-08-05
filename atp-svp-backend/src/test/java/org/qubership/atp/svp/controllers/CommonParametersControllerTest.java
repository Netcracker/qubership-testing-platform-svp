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
import static org.mockito.ArgumentMatchers.anyString;
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

import org.qubership.atp.svp.model.db.CommonParameterEntity;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.impl.SutParameter;
import org.qubership.atp.svp.model.ui.CommonParameterRequest;
import org.qubership.atp.svp.repo.impl.GitRepositoryImpl;
import org.qubership.atp.svp.service.direct.EurekaDiscoveryServiceImpl;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.qubership.atp.svp.service.jpa.CommonParametersServiceJpa;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class CommonParametersControllerTest extends TestWithTestData {

    private final String fileRepoFilePath = String.format("src/test/config/project/%s/common_parameters.json",
            fileProjectName);
    private final String gitRepoFilePath = String.format("src/test/config/project/%s/common_parameters.json",
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
    CommonParametersServiceJpa commonParametersServiceJpa;
    @MockBean
    EurekaDiscoveryServiceImpl eurekaDiscoveryServiceImpl;

    @After
    public void afterEach() throws IOException {
        Files.deleteIfExists(Paths.get(fileRepoFilePath));
        Files.deleteIfExists(Paths.get(gitRepoFilePath));
    }

    // GENERAL CASES

//    @Test
//    public void updateCommonParameters_projectIsNotConfigured_status404() throws Exception {
//        String notConfiguredProjectId = "11111111-1111-1111-1111-11111111111";
//        String urlToUpdateCommonParametersForNotConfiguredProject =
//                String.format("/api/svp/project/%s/common-parameters/update", notConfiguredProjectId);
//        CommonParameterRequest request = new CommonParameterRequest();
//        request.setCommonParameters(Collections.emptyList());
//        String body = objectMapper.writeValueAsString(request);
//        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any())).thenThrow(ProjectConfigException.class);
//
//        mockMvc.perform(post(urlToUpdateCommonParametersForNotConfiguredProject)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().is(404));
//    }

    @Test
    public void getCommonParameters_correctRequest_fileRepo_returnsCommonParameters() throws Exception {
        String testFile = loadFileToString("src/test/resources/test_data/common-parameters"
                + "/common_parameters.json");
        writeToFile(fileRepoFilePath, testFile);
        List<SutParameter> expectedCommonParameters = Arrays.asList(objectMapper.readValue(testFile,
                SutParameter[].class));

        CommonParameterRequest request = new CommonParameterRequest();
        request.setFolder("Default");
        String body = objectMapper.writeValueAsString(request);

//        mock List<CommonParameterEntity> from DB
        List<CommonParameterEntity> commons = new ArrayList<>();
        for (int i = 0; i < expectedCommonParameters.size(); i++) {
            commons.add(new CommonParameterEntity(null, expectedCommonParameters.get(i), i));
        }
        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setCommonParameters(commons);
        when(commonParametersServiceJpa.getCommonParameter(any(), anyString())).thenReturn(commons);

        MvcResult response = mockMvc.perform(post("/api/svp/project/{project}/common-parameters", fileProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        List<SutParameter> actualCommonParameters = Arrays.asList(objectMapper
                .readValue(response.getResponse().getContentAsString(),
                        SutParameter[].class));

        Assert.assertEquals(expectedCommonParameters, actualCommonParameters);
    }

    // FILE REPO

    @Test
    public void updateCommonParameters_fileRepo_successfullyUpdated() throws Exception {
        String testFile = loadFileToString("src/test/resources/test_data/common-parameters"
                + "/common_parameters.json");
        writeToFile(fileRepoFilePath, testFile);
        List<SutParameter> commonParametersBeforeChange = Arrays.asList(objectMapper.readValue(testFile,
                SutParameter[].class));

        String CommonParameterEntity = readFileToString("src/test/resources/test_data/controllers"
                + "/commonParametersController/CommonParametersEntity.json");
        List<CommonParameterEntity> commons = Arrays.asList(objectMapper.readValue(CommonParameterEntity,
                CommonParameterEntity[].class));
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString(fileProject));
        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setProject(projectConfigs);
        folderEntity.setCommonParameters(commons);
        folderEntity.setName("Default");
        folderEntity.setCommonParameters(new ArrayList<>());
        when(folderServiceJpa.getFolderByProjectIdAndName(any(), any())).thenReturn(folderEntity);
        when(commonParametersServiceJpa.getCommonParameterEntitiesByFolderId(any())).thenReturn(commons);

        String newParamName = "Changed Param Name";
        commonParametersBeforeChange.get(0).setName(newParamName);
        CommonParameterRequest request = new CommonParameterRequest();
        request.setCommonParameters(commonParametersBeforeChange);
        request.setFolder("Default");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/svp/project/{project}/common-parameters/update", fileProject)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        List<SutParameter> commonParametersAfterChange = Arrays.asList(objectMapper.
                readValue(loadFileToString(fileRepoFilePath),
                        SutParameter[].class));

        Assert.assertEquals(commonParametersAfterChange.get(0).getName(), newParamName);
    }

    // GIT REPO

//    todo will implemented when be DB for testing
//    @Test
//    public void updateCommonParameters_gitRepo_successfullyUpdated() throws Exception {
//        String testFile = loadFileToString("src/test/resources/test_data/common-parameters"
//                + "/common_parameters.json");
//        writeToFile(gitRepoFilePath, testFile);
//        List<SutParameter> commonParametersBeforeChange = Arrays.asList(objectMapper.readValue(testFile,
//                SutParameter[].class));
//        getMockProjectConfigFromBd(gitProject, projectConfigService);
//        String newParamName = "Changed Param Name";
//        commonParametersBeforeChange.get(0).setName(newParamName);
//        CommonParameterRequest request = new CommonParameterRequest();
//        request.setCommonParameters(commonParametersBeforeChange);
//        String body = objectMapper.writeValueAsString(request);
//
//        mockMvc.perform(post("/api/svp/project/{project}/common-parameters/update", gitProject)
//                        .contentType(APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().isOk());
//        List<SutParameter> commonParametersAfterChange = Arrays.asList(objectMapper.
//                readValue(loadFileToString(gitRepoFilePath),
//                        SutParameter[].class));
//
//        Assert.assertEquals(commonParametersAfterChange.get(0).getName(), newParamName);
//        verify(gitRepository, times(1))
//                .gitCommitAndPush(Mockito.anyString(), Mockito.anyString());
//    }
}
