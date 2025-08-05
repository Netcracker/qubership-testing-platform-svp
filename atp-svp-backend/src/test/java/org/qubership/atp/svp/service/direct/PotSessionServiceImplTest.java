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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.core.exceptions.StoringSessionException;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionParameterNotFoundException;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionSessionNotFoundException;
import org.qubership.atp.svp.kafka.KafkaSendlerService;
import org.qubership.atp.svp.model.api.GetParameterResultRequest;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.repo.impl.LogCollectorRepository;
import org.qubership.atp.svp.repo.jpa.SessionRepository;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionRepository;
import org.qubership.atp.svp.tests.DbMockEntity;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = PotSessionServiceImpl.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class PotSessionServiceImplTest extends TestWithTestData {

    @SpyBean
    DeferredSearchServiceImpl deferredSearchService;

    @SpyBean
    ExecutionVariablesServiceImpl executionVariablesService;
    @SpyBean
    ValidationServiceImpl validationService;

    @MockBean
    WebSocketMessagingService messagingService;
    @MockBean
    KafkaMessagingService kafkaMessagingService;
    @MockBean
    EurekaDiscoveryServiceImpl eurekaDiscoveryServiceImpl;
    @MockBean
    SessionRepository sessionRepository;

    @MockBean
    PotSessionRepository potSessionRepository;

    @MockBean
    KafkaSendlerService kafkaSendlerService;
    @MockBean
    LogCollectorRepository logCollectorRepository;
    @SpyBean
    private SessionServiceImpl sessionServiceImpl;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void killExpiredSessions_twoSessions_oneIsExpired_expiredIsDeleted() throws
            StoringSessionException, IOException {

        UUID expiredSessionId = UUID.randomUUID();
        sessionServiceImpl.addNewSession(expiredSessionId);
        when(potSessionRepository.getExpiredSessionId(anyInt(), any()))
                .thenReturn(Collections.singletonList(expiredSessionId.toString()));
        PotSessionEntity session = DbMockEntity.generatePotSessionEntity(expiredSessionId);
        when(potSessionRepository.findBySessionId(expiredSessionId)).thenReturn(Optional.of(session));
        PotSessionServiceImpl service = new PotSessionServiceImpl(deferredSearchService,
                executionVariablesService, validationService, 30, potSessionRepository, sessionServiceImpl);

        service.setSocketService(messagingService);
        service.setKafkaService(kafkaMessagingService);
        service.setSessionService(sessionServiceImpl);

        service.killExpiredSessions();

        verify(potSessionRepository, times(1)).deleteBySessionId(expiredSessionId);
        Assert.assertFalse(sessionServiceImpl.hasSession(expiredSessionId));
    }

    @Test
    public void killExpiredSessions_twoSessions_noneIsExpired_noSessionsDeleted() throws StoringSessionException {
        when(potSessionRepository.getExpiredSessionId(anyInt(), any()))
                .thenReturn(Collections.emptyList());
        PotSessionServiceImpl service = new PotSessionServiceImpl(deferredSearchService,
                executionVariablesService, validationService, 30, potSessionRepository, sessionServiceImpl);

        service.killExpiredSessions();

        verify(potSessionRepository, never()).deleteBySessionId(any());
    }

    @Test(expected = ExecutionSessionNotFoundException.class)
    public void getParameterResult_noSession_throwsSessionNotFound()  {
        PotSessionServiceImpl service = new PotSessionServiceImpl(
                deferredSearchService, executionVariablesService, validationService, 30, potSessionRepository, sessionServiceImpl);

        service.getParameterResult(new GetParameterResultRequest(UUID.randomUUID(), "",
                "", "", "", false));
    }

    @Test(expected = ExecutionParameterNotFoundException.class)
    public void getParameterResult_noPageParameter_throwsParameterNotFound() throws IOException {
        UUID sessionId = UUID.randomUUID();
        PotSessionEntity session = DbMockEntity.generatePotSessionEntity(sessionId);
        PotSessionServiceImpl service = new PotSessionServiceImpl(
                deferredSearchService, executionVariablesService, validationService, 30, potSessionRepository, sessionServiceImpl);
        when(potSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(session));

        service.getParameterResult(new GetParameterResultRequest(sessionId, "",
                "", "", "", false));
    }

    @Test(expected = ExecutionParameterNotFoundException.class)
    public void getParameterResult_noCommonParameter_throwsParameterNotFound() throws IOException {
        UUID sessionId = UUID.randomUUID();
        PotSessionEntity session = DbMockEntity.generatePotSessionEntity(sessionId);
        PotSessionServiceImpl service = new PotSessionServiceImpl(
                deferredSearchService, executionVariablesService, validationService, 30, potSessionRepository, sessionServiceImpl);
        when(potSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(session));

        service.getParameterResult(new GetParameterResultRequest(sessionId, "",
                "", "", "", true));
    }
}
