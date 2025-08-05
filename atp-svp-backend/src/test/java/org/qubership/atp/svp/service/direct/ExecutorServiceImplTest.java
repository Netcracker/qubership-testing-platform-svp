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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.auth.springbootstarter.security.permissions.PolicyEnforcement;
import org.qubership.atp.svp.clients.api.logcollector.dto.public_api.ConfigurationDto;
import org.qubership.atp.svp.model.api.GetInfoRequest;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.impl.LogCollectorSearchPeriod;
import org.qubership.atp.svp.model.logcollector.LogCollectorConfiguration;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.repo.feign.EnvironmentFeignClient;
import org.qubership.atp.svp.repo.feign.EnvironmentsProjectFeignClient;
import org.qubership.atp.svp.repo.feign.LogCollectorConfigurationFeignClient;
import org.qubership.atp.svp.repo.feign.LogCollectorFeignClient;
import org.qubership.atp.svp.repo.feign.LogCollectorQueueFeignClient;
import org.qubership.atp.svp.repo.feign.UsersFeignClient;
import org.qubership.atp.svp.repo.impl.EnvironmentRepository;
import org.qubership.atp.svp.repo.impl.FilePageConfigurationRepository;
import org.qubership.atp.svp.repo.impl.GitPageConfigurationRepository;
import org.qubership.atp.svp.repo.impl.LogCollectorRepository;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionRepository;
import org.qubership.atp.svp.service.PotGenerationEngine;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.utils.DtoConvertService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ExecutorServiceImpl.class)
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties",
        properties = "svp.session.lifespan=30")
public class ExecutorServiceImplTest {

    @Autowired
    ExecutorServiceImpl executorService;

    @SpyBean
    IntegrationServiceImpl integrationService;
    @SpyBean
    PotSessionServiceImpl potSessionService;
    @SpyBean
    LogCollectorRepository logCollectorRepository;

    @MockBean
    WebSocketMessagingService webSocketMessagingService;
    @MockBean
    KafkaMessagingService kafkaMessagingService;
    @MockBean
    DeferredSearchServiceImpl deferredSearchService;
    @MockBean
    ExecutionVariablesServiceImpl executionVariablesService;
    @MockBean
    ProjectConfigService configService;
    @MockBean
    EnvironmentFeignClient environmentFeignClient;
    @MockBean
    LogCollectorFeignClient logCollectorFeignClient;
    @MockBean
    PotGenerationEngine potGenerationEngine;
    @MockBean
    GitPageConfigurationRepository gitPageConfigurationRepository;
    @MockBean
    FilePageConfigurationRepository filePageConfigurationRepository;
    @MockBean
    ValidationServiceImpl validationService;
    @MockBean
    private DtoConvertService dtoConvertService;
    @MockBean
    private EnvironmentsProjectFeignClient environmentsProjectFeignClient;
    @MockBean
    private EnvironmentRepository environmentRepository;
    @MockBean
    private LogCollectorQueueFeignClient logCollectorQueueFeignClient;
    @MockBean
    private LogCollectorConfigurationFeignClient logCollectorConfigurationFeignClient;
    @MockBean
    private SessionDtoProcessorService sessionDtoProcessorService;
    @MockBean
    private UsersFeignClient usersFeignClient;
    @MockBean
    private PolicyEnforcement policyEnforcement;
    @MockBean
    PotSessionRepository potSessionRepository;
    @MockBean
    FolderServiceJpa folderServiceJpa;
    @MockBean
    SessionServiceImpl sessionServiceImpl;

    private SessionExecutionConfiguration sessionConfiguration;
    private LogCollectorSearchPeriod defaultLogCollectorSearchPeriodAlreadyStoredInSession
            = new LogCollectorSearchPeriod(9999999);

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(environmentRepository.getEnvironmentById(any())).thenReturn(new Environment());
        LogCollectorConfiguration lcConfiguration1 = new LogCollectorConfiguration();
        lcConfiguration1.setId(UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b1"));
        LogCollectorConfiguration lcConfiguration2 = new LogCollectorConfiguration();
        lcConfiguration2.setId(UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b2"));
        ResponseEntity<List<ConfigurationDto>> logCollectorConfigurationDto =
                new ResponseEntity<>(dtoConvertService.convertList(Arrays.asList(lcConfiguration1, lcConfiguration2),
                        ConfigurationDto.class)
                        , HttpStatus.OK);
        Mockito.when(logCollectorConfigurationFeignClient.getConfigurationsByProjectId(any())).thenReturn(
                logCollectorConfigurationDto);
        sessionConfiguration = SessionExecutionConfiguration.builder()
                .environment(new Environment())
                .pagesName(Collections.emptyList())
                .logCollectorConfigurations(Collections.emptyList())
                .shouldHighlightDiffs(false)
                .shouldSendSessionResults(false)
                .isFullInfoNeededInPot(false)
                .onlyForPreconfiguredParams(false)
                .isPotGenerationMode(false)
                .onlyCommonParametersExecuted(false)
                .forcedLoadingCommonParameters(false)
                .build();
    }

    @Test
    public void getOrCreateSession_noSessionIdInRequestAndLogCollectorSearchPeriodExistInRequest_createsNewSessionWithLogCollectorSearchPeriodFromRequest() {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        UUID environmentId = UUID.fromString("acce4dda-c9a0-40df-a26b-c243e257bf2d");
        // Create request to start session
        LogCollectorSearchPeriod expectedLcSearchPeriod = new LogCollectorSearchPeriod(10000);
        GetInfoRequest request = GetInfoRequest.builder()
                .environmentId(environmentId)
                .pagesName(Collections.emptyList())
                .logCollectorSearchPeriod(expectedLcSearchPeriod)
                .build();

        FolderEntity folder = new FolderEntity();
        folder.setFolderId(UUID.randomUUID());
        Mockito.when(folderServiceJpa.getFolderByProjectIdAndName(any(), anyString())).thenReturn(folder);

        // ProjectConfiguration mock
        ProjectConfigsEntity config = getProjectConfiguration(projectId);
        Mockito.when(configService.getProjectConfig(any())).thenReturn(config);

        PotSessionEntity sessionTest = new PotSessionEntity();
        sessionTest.setSessionId(UUID.randomUUID());
        Mockito.when(potSessionRepository.saveAndFlush(any())).thenReturn(sessionTest);
        ArgumentCaptor<PotSessionEntity> captorPotSessionEntity = ArgumentCaptor.forClass(PotSessionEntity.class);
        Mockito.when(potSessionRepository.saveAndFlush(any())).thenReturn(sessionTest);

        executorService.getOrCreateSession(projectId, request, false);

        Mockito.verify(potSessionRepository, Mockito.times(1)).saveAndFlush(captorPotSessionEntity.capture());
        PotSessionEntity session = captorPotSessionEntity.getValue();
        LogCollectorSearchPeriod actualLcSearchPeriod = session.getExecutionConfiguration().getLogCollectorSearchPeriod();
        Assert.assertEquals(expectedLcSearchPeriod, actualLcSearchPeriod);
    }

    @Test
    public void
    getOrCreateSession_sessionIdInRequestExistAndLogCollectorSearchPeriodExistInRequest_returnsExistingSessionIdAndSetLogCollectorSearchPeriodFromRequest() {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        UUID environmentId = UUID.fromString("acce4dda-c9a0-40df-a26b-c243e257bf2d");
        // Create and set preconfigured session
        UUID expectedSessionId = UUID.randomUUID();
        sessionConfiguration.setLogCollectorSearchPeriod(defaultLogCollectorSearchPeriodAlreadyStoredInSession);
        // Create request to start session
        LogCollectorSearchPeriod expectedLcSearchPeriod = new LogCollectorSearchPeriod(10000);
        GetInfoRequest request = GetInfoRequest.builder()
                .environmentId(environmentId)
                .sessionId(expectedSessionId)
                .pagesName(Collections.emptyList())
                .logCollectorSearchPeriod(expectedLcSearchPeriod)
                .build();

        FolderEntity folder = new FolderEntity();
        folder.setFolderId(UUID.randomUUID());
        Mockito.when(folderServiceJpa.getFolderByProjectIdAndName(any(), anyString())).thenReturn(folder);

        // ProjectConfiguration mock
        ProjectConfigsEntity config = getProjectConfiguration(projectId);
        Mockito.when(configService.getProjectConfig(Mockito.any())).thenReturn(config);

        PotSessionEntity sessionTest = new PotSessionEntity();
        sessionTest.setSessionId(expectedSessionId);
        Mockito.when(potSessionRepository.findBySessionId(any())).thenReturn(Optional.of(sessionTest));
        Mockito.when(potSessionRepository.saveAndFlush(any())).thenReturn(sessionTest);
        ArgumentCaptor<SessionExecutionConfiguration> captorPotSessionEntity = ArgumentCaptor.forClass(SessionExecutionConfiguration.class);


        UUID actualSessionId = executorService.getOrCreateSession(projectId, request, false);


        Mockito.verify(potSessionRepository, Mockito.times(1)).updateSession(any(), captorPotSessionEntity.capture());
        SessionExecutionConfiguration executionConfiguration = captorPotSessionEntity.getValue();
        LogCollectorSearchPeriod actualLcSearchPeriod = executionConfiguration.getLogCollectorSearchPeriod();
        Assert.assertEquals(expectedSessionId, actualSessionId);
        Assert.assertEquals(expectedLcSearchPeriod, actualLcSearchPeriod);
    }

    @Test
    public void
    getOrCreateSession_noSessionIdInRequestAndLogCollectorSearchPeriodNotExistInRequest_createsNewSessionWithLogCollectorSearchPeriodFromProjectConfig() {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        UUID environmentId = UUID.fromString("acce4dda-c9a0-40df-a26b-c243e257bf2d");
        // Create request to start session
        GetInfoRequest request = GetInfoRequest.builder()
                .environmentId(environmentId)
                .pagesName(Collections.emptyList())
                .build();
        // ProjectConfiguration mock
        ProjectConfigsEntity config = getProjectConfiguration(projectId);
        LogCollectorSearchPeriod expectedLcSearchPeriod =
                new LogCollectorSearchPeriod(config.getDefaultLogCollectorSearchTimeRange());
        Mockito.when(configService.getProjectConfig(Mockito.any())).thenReturn(config);

        FolderEntity folder = new FolderEntity();
        folder.setFolderId(UUID.randomUUID());
        Mockito.when(folderServiceJpa.getFolderByProjectIdAndName(any(), anyString())).thenReturn(folder);

        PotSessionEntity sessionTest = new PotSessionEntity();
        sessionTest.setSessionId(UUID.randomUUID());
        Mockito.when(potSessionRepository.saveAndFlush(any())).thenReturn(sessionTest);
        ArgumentCaptor<PotSessionEntity> captorPotSessionEntity = ArgumentCaptor.forClass(PotSessionEntity.class);


        UUID sessionId = executorService.getOrCreateSession(projectId, request, false);

        Mockito.verify(potSessionRepository, Mockito.times(1)).saveAndFlush(captorPotSessionEntity.capture());
        PotSessionEntity session = captorPotSessionEntity.getValue();
        LogCollectorSearchPeriod actualLcSearchPeriod = session.getExecutionConfiguration().getLogCollectorSearchPeriod();

        Assert.assertEquals(expectedLcSearchPeriod, actualLcSearchPeriod);
    }

    @Test
    public void
    getOrCreateSession_sessionIdInRequestExistAndLogCollectorSearchPeriodNotExistInRequest_returnsExistingSessionIdAndSetLogCollectorSearchPeriodFromProjectConfig() {
        UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");
        UUID environmentId = UUID.fromString("acce4dda-c9a0-40df-a26b-c243e257bf2d");
        // Create and set preconfigured session
        UUID expectedSessionId = UUID.randomUUID();
        // Create request to start session
        GetInfoRequest request = GetInfoRequest.builder()
                .environmentId(environmentId)
                .pagesName(Collections.emptyList())
                .sessionId(expectedSessionId)
                .build();
        // ProjectConfiguration mock
        ProjectConfigsEntity config = getProjectConfiguration(projectId);
        LogCollectorSearchPeriod expectedLcSearchPeriod =
                new LogCollectorSearchPeriod(config.getDefaultLogCollectorSearchTimeRange());
        Mockito.when(configService.getProjectConfig(Mockito.any())).thenReturn(config);

        FolderEntity folder = new FolderEntity();
        folder.setFolderId(UUID.randomUUID());
        Mockito.when(folderServiceJpa.getFolderByProjectIdAndName(any(), anyString())).thenReturn(folder);

        PotSessionEntity sessionTest = new PotSessionEntity();
        sessionTest.setSessionId(UUID.randomUUID());
        Mockito.when(potSessionRepository.saveAndFlush(any())).thenReturn(sessionTest);
        Mockito.when(potSessionRepository.findBySessionId(any())).thenReturn(Optional.of(sessionTest));
        ArgumentCaptor<SessionExecutionConfiguration> captorPotSessionEntity = ArgumentCaptor.forClass(SessionExecutionConfiguration.class);

        UUID actualSessionId = executorService.getOrCreateSession(projectId, request, false);


        Mockito.verify(potSessionRepository, Mockito.times(1)).updateSession(any(), captorPotSessionEntity.capture());
        SessionExecutionConfiguration executionConfiguration = captorPotSessionEntity.getValue();
        LogCollectorSearchPeriod actualLcSearchPeriod = executionConfiguration.getLogCollectorSearchPeriod();
        Assert.assertEquals(expectedSessionId, actualSessionId);
        Assert.assertEquals(expectedLcSearchPeriod, actualLcSearchPeriod);
    }

//    private ProjectConfiguration getProjectConfiguration(UUID projectId) {
//        return new ProjectConfiguration(projectId,
//                "Local test project",
//                true,
//                86400,
//                null);
//    }

    private ProjectConfigsEntity getProjectConfiguration(UUID projectId) {
        ProjectConfigsEntity config =  new ProjectConfigsEntity();
        config.setProjectId(projectId);
        config.setProjectName("Local test project");
        config.setDefaultLogCollectorSearchTimeRange(86400);
        config.setFullInfoNeededInPot(true);
        return config;

    }

    // TODO in plan:
    //  - getPreconfiguredValidationResults()
    //      - check that all the necessary methods are invoked and returned correct type
    //      - check negative cases of errors
}
