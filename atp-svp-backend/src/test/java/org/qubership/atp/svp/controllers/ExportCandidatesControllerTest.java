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

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

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
import org.springframework.test.web.servlet.MvcResult;

import org.qubership.atp.svp.service.direct.EurekaDiscoveryServiceImpl;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.tests.DbMockEntity;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class ExportCandidatesControllerTest extends TestWithTestData {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    FolderServiceJpa folderServiceJpa;

    @MockBean
    EurekaDiscoveryServiceImpl eurekaDiscoveryServiceImpl;

    @Test
    public void getCandidates_foldersAndPagesNotExist_EmptyList() throws Exception {
        UUID projectId = UUID.randomUUID();
        String url = String.format("/api/svp/project/%s/candidates", projectId);
        Mockito.when(folderServiceJpa.getFoldersByProjectId(any())).thenReturn(Collections.emptyList());
        MvcResult response = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();

        Assert.assertEquals("[]", response.getResponse().getContentAsString());
    }

    @Test
    public void getCandidates_foldersAndPagesExist_FoldersAndPages() throws Exception {
        UUID projectId = UUID.randomUUID();
        String url = String.format("/api/svp/project/%s/candidates", projectId);

        Mockito.when(folderServiceJpa.getFoldersByProjectId(any()))
                .thenReturn(DbMockEntity.generateFoldersForExportCandidates());
        MvcResult response = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();

        Assert.assertEquals(expectedExportCandidates(), response.getResponse().getContentAsString());
    }
}
