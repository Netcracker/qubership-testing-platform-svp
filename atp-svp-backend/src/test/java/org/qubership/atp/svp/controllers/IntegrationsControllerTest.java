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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.atp.auth.springbootstarter.entities.Operation;
import org.qubership.atp.auth.springbootstarter.security.permissions.PolicyEnforcement;
import org.qubership.atp.svp.clients.api.logcollector.dto.public_api.ConfigurationDto;
import org.qubership.atp.svp.model.environments.LazyEnvironment;
import org.qubership.atp.svp.model.environments.Project;
import org.qubership.atp.svp.model.environments.System;
import org.qubership.atp.svp.model.logcollector.LogCollectorConfiguration;
import org.qubership.atp.svp.repo.feign.EnvironmentFeignClient;
import org.qubership.atp.svp.repo.feign.EnvironmentsProjectFeignClient;
import org.qubership.atp.svp.repo.feign.LogCollectorConfigurationFeignClient;
import org.qubership.atp.svp.repo.feign.LogCollectorFeignClient;
import org.qubership.atp.svp.repo.feign.LogCollectorQueueFeignClient;
import org.qubership.atp.svp.repo.feign.UsersFeignClient;
import org.qubership.atp.svp.repo.impl.EnvironmentRepository;
import org.qubership.atp.svp.service.direct.EurekaDiscoveryServiceImpl;
import org.qubership.atp.svp.service.direct.IntegrationServiceImpl;
import org.qubership.atp.svp.tests.TestWithTestData;
import org.qubership.atp.svp.utils.DtoConvertService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class IntegrationsControllerTest extends TestWithTestData {

    private final String PROJECTS_FILE_PATH = "src/test/resources/test_data"
            + "/integrations/projects.json";
    private final String ENVIRONMENTS_FILE_PATH = "src/test/resources/test_data"
            + "/integrations/environments.json";
    private final String SYSTEMS_FILE_PATH = "src/test/resources/test_data"
            + "/integrations/systems.json";
    private final String LOG_COLLECTOR_CONFIGS_FILE_PATH = "src/test/resources/test_data"
            + "/integrations/log-collector-configs.json";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnvironmentRepository environmentRepository;

    @SpyBean
    private IntegrationServiceImpl integrationService;
    @MockBean
    private UsersFeignClient usersFeignClient;
    @MockBean
    private PolicyEnforcement policyEnforcement;

    @MockBean
    private LogCollectorFeignClient logCollectorFeignClient;

    @SpyBean
    private DtoConvertService dtoConvertService;

    @MockBean
    private EnvironmentsProjectFeignClient environmentsProjectFeignClient;

    @MockBean
    private EnvironmentFeignClient environmentFeignClient;
    @MockBean
    private LogCollectorQueueFeignClient logCollectorQueueFeignClient;
    @MockBean
    private LogCollectorConfigurationFeignClient logCollectorConfigurationFeignClient;
    @MockBean
    EurekaDiscoveryServiceImpl eurekaDiscoveryServiceImpl;

    private ObjectMapper objectMapper;

    @Before
    public void beforeClass() {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void getProjectsFromEnvironments_mockedClient_returnsListOfProjects() throws Exception {
        List<Project> expectedProjects = Arrays.asList(objectMapper
                .readValue(loadFileToString(PROJECTS_FILE_PATH), Project[].class));
        when(environmentRepository.getProjects()).thenReturn(expectedProjects);

        MvcResult response = mockMvc.perform(get("/api/svp/integrations/environments/projects",
                UUID.randomUUID()))
                .andExpect(status().isOk())
                .andReturn();
        List<Project> actualProjects = Arrays.asList(objectMapper.readValue(response.getResponse().getContentAsString(),
                Project[].class));

        Assert.assertEquals(expectedProjects.size(), actualProjects.size());
        Assert.assertTrue(expectedProjects.containsAll(actualProjects));
    }

    @Test
    public void getProjectsFromEnvironments_mockedClientAndUserManager_returnsFilteredListOfProjects() throws Exception {
        List<Project> allEnvProjects = Arrays.asList(objectMapper
                .readValue(loadFileToString(PROJECTS_FILE_PATH), Project[].class));
        when(environmentRepository.getProjects()).thenReturn(allEnvProjects);
        ReflectionTestUtils.setField(integrationService, "isKeycloakEnabled", true);
        ReflectionTestUtils.setField(integrationService, "springProfilesActive", "default");
        org.qubership.atp.auth.springbootstarter.entities.Project entityProject =
                new org.qubership.atp.auth.springbootstarter.entities.Project();
        UUID idProject = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        entityProject.setUuid(idProject);
        when(usersFeignClient.getAllProjects()).thenReturn(Collections.singletonList(entityProject));
        when(policyEnforcement.checkPoliciesForOperation(entityProject, Operation.READ)).thenReturn(true);
        when(policyEnforcement.isAdmin()).thenReturn(false);
        when(policyEnforcement.isSupport()).thenReturn(false);

        MvcResult response = mockMvc.perform(get("/api/svp/integrations/environments/projects",
                        UUID.randomUUID()))
                .andExpect(status().isOk())
                .andReturn();

        List<Project> actualProjects = Arrays.asList(objectMapper.readValue(response.getResponse().getContentAsString(),
                Project[].class));
        Assert.assertEquals(1, actualProjects.size());
        List<Project> expectedProjects = Collections.singletonList(allEnvProjects.get(0));
        Assert.assertTrue(expectedProjects.containsAll(actualProjects));
    }

    @Test
    public void getEnvironmentsByProjectIdFromEnvironments_mockedClient_returnsListOfEnvironments() throws Exception {
        List<LazyEnvironment> expectedEnvironments = Arrays.asList(objectMapper
                .readValue(loadFileToString(ENVIRONMENTS_FILE_PATH), LazyEnvironment[].class));
        when(environmentRepository.getEnvironmentsByProjectId(Mockito.any(UUID.class)))
                .thenReturn(expectedEnvironments);

        MvcResult response = mockMvc.perform(get("/api/svp/integrations"
                        + "/environments/projects/{projectId}/environments",
                UUID.randomUUID()))
                .andExpect(status().isOk())
                .andReturn();
        List<LazyEnvironment> actualEnvironments = Arrays.asList(objectMapper.readValue(response.getResponse()
                .getContentAsString(), LazyEnvironment[].class));

        Assert.assertEquals(expectedEnvironments.size(), actualEnvironments.size());
        Assert.assertTrue(expectedEnvironments.containsAll(actualEnvironments));
    }

    @Test
    public void getSystemsByEnvironmentIdFromEnvironments_mockedClient_returnsListOfSystems() throws Exception {
        List<System> expectedSystems = Arrays.asList(objectMapper
                .readValue(loadFileToString(SYSTEMS_FILE_PATH), System[].class));
        when(environmentRepository.getSystemsByEnvironmentId(Mockito.any(UUID.class)))
                .thenReturn(expectedSystems);

        MvcResult response = mockMvc.perform(get("/api/svp/integrations"
                + "/environments/environments/{environmentId}/systems", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andReturn();
        List<System> actualSystems = Arrays.asList(objectMapper.readValue(response.getResponse()
                .getContentAsString(), System[].class));

        Assert.assertEquals(expectedSystems.size(), actualSystems.size());
        Assert.assertTrue(expectedSystems.containsAll(actualSystems));
    }

    @Test
    public void getSystemsNamesFromEnvironments_mockedClient_returnsListOfSystems() throws Exception {
        List<String> expectedSystems = Arrays.asList("VFHU", "Oman");
        when(environmentRepository.getSystemsName(Mockito.any(UUID.class))).thenReturn(expectedSystems);

        MvcResult response = mockMvc.perform(get("/api/svp/integrations"
                        + "/environments/projects/{projectId}/systems-names",
                UUID.randomUUID()))
                .andExpect(status().isOk())
                .andReturn();
        List<String> servicesList = Arrays.asList(objectMapper.readValue(response.getResponse().getContentAsString(),
                String[].class));

        Assert.assertEquals(expectedSystems.size(), servicesList.size());
        Assert.assertTrue(expectedSystems.containsAll(servicesList));
    }

    @Test
    public void getConnectionsNamesFromEnvironments_mockedClient_returnsListOfSystems() throws Exception {
        List<String> expectedConnections = Arrays.asList("HTTP", "DB");
        when(environmentRepository.getConnectionsName(Mockito.any(UUID.class))).thenReturn(expectedConnections);

        MvcResult response = mockMvc.perform(get("/api/svp/integrations"
                        + "/environments/projects/{projectId}/connections-names",
                UUID.randomUUID()))
                .andExpect(status().isOk())
                .andReturn();
        List<String> connectionsList = Arrays.asList(objectMapper.readValue(response.getResponse().getContentAsString(),
                String[].class));

        Assert.assertEquals(expectedConnections.size(), connectionsList.size());
        Assert.assertTrue(expectedConnections.containsAll(connectionsList));
    }

    @Test
    public void getConfigurationsFromLogCollector_mockedClient_returnsListOfConfigurations() throws Exception {
        List<LogCollectorConfiguration> expectedConfigurations = Arrays.asList(objectMapper
                .readValue(loadFileToString(LOG_COLLECTOR_CONFIGS_FILE_PATH), LogCollectorConfiguration[].class));
        ResponseEntity<List<ConfigurationDto>> logCollectorConfigurationDto =
                new ResponseEntity<>(dtoConvertService.convertList(expectedConfigurations, ConfigurationDto.class)
                        , HttpStatus.OK);
        when(logCollectorConfigurationFeignClient.getConfigurationsByProjectId(Mockito.any(UUID.class))).thenReturn(logCollectorConfigurationDto);

        MvcResult response = mockMvc.perform(get("/api/svp/integrations"
                + "/log-collector/projects/{projectId}/configurations", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andReturn();
        List<LogCollectorConfiguration> configurations =
                Arrays.asList(objectMapper.readValue(response.getResponse().getContentAsString(),
                        LogCollectorConfiguration[].class));

        Assert.assertEquals(expectedConfigurations.size(), configurations.size());
        Assert.assertTrue(expectedConfigurations.containsAll(configurations));
    }
}
