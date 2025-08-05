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

package org.qubership.atp.svp.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.core.enums.ResultType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.kafka.SvpKafkaMessage;
import org.qubership.atp.svp.repo.jpa.SessionRepository;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionParameterRepository;
import org.qubership.atp.svp.service.direct.EurekaDiscoveryServiceImpl;
import org.qubership.atp.svp.service.direct.KafkaMessagingService;
import org.qubership.atp.svp.service.direct.SessionServiceImpl;
import org.qubership.atp.svp.service.direct.WebSocketMessagingService;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = SvpKafkaListener.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false",
                "kafka.svp.event.enable= true"})
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class SvpKafkaListenerTest extends TestWithTestData {

    @Autowired
    SvpKafkaListener svpKafkaListener;
    @SpyBean
    SessionServiceImpl sessionServiceImpl;
    @MockBean
    PotSessionParameterRepository repository;
    @SpyBean
    WebSocketMessagingService webSocketMessagingService;
    @MockBean
    KafkaMessagingService kafkaMessagingService;
    @MockBean
    EurekaDiscoveryServiceImpl eurekaDiscoveryServiceImpl;
    @MockBean
    SessionRepository sessionRepository;

    @MockBean
    SimpMessagingTemplate messagingTemplate;
    @MockBean
    KafkaSendlerService kafkaSendlerService;

    @Test
    public void listen_SvpKafkaMessageParameterType_SendToWebSocket() throws IOException {
        UUID sessionId = UUID.randomUUID();
        String testPodName = "testName";
        when(sessionRepository.containsSession(any(), anyString())).thenReturn(true);
        when(eurekaDiscoveryServiceImpl.getCurrentPodName()).thenReturn(testPodName);

        String file = loadFileToString("src/test/resources/test_data/webSocket/PotSessionParameter.json");
        PotSessionParameterEntity parameter = objectMapper.readValue(file, PotSessionParameterEntity.class);
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.PARAMETER, null,
                null, parameter.getParameterId().toString(), 0, null);
        when(repository.findByParameterId(UUID.fromString(kafkaMessage.getParameterId()))).thenReturn(parameter);

        svpKafkaListener.listen(sessionId, kafkaMessage);

        verify(webSocketMessagingService, times(1)).sendSutParameterResult(sessionId, parameter);
    }

    @Test
    public void listen_SvpKafkaMessageTabType_SendToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        String testPodName = "testName";
        when(sessionRepository.containsSession(any(), anyString())).thenReturn(true);
        when(eurekaDiscoveryServiceImpl.getCurrentPodName()).thenReturn(testPodName);

        String pageName = "testPage";
        String tabName = "testTab";
        ValidationStatus tabStatus = ValidationStatus.NONE;
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.TAB, pageName,
                tabName, null, 0, tabStatus);

        svpKafkaListener.listen(sessionId, kafkaMessage);

        verify(webSocketMessagingService, times(1)).sendValidationStatusForTab(sessionId, pageName, tabName, tabStatus);
    }

    @Test
    public void listen_SvpKafkaMessagePageType_SendToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        String testPodName = "testName";
        when(sessionRepository.containsSession(any(), anyString())).thenReturn(true);
        when(eurekaDiscoveryServiceImpl.getCurrentPodName()).thenReturn(testPodName);

        String pageName = "testPage";
        ValidationStatus status = ValidationStatus.NONE;
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.PAGE, pageName,
                null, null, 0, status);

        svpKafkaListener.listen(sessionId, kafkaMessage);

        verify(webSocketMessagingService, times(1)).sendValidationStatusForPage(sessionId, pageName, status);
    }

    @Test
    public void listen_SvpKafkaMessageSessionType_SendToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        String testPodName = "testName";
        when(sessionRepository.containsSession(any(), anyString())).thenReturn(true);
        when(eurekaDiscoveryServiceImpl.getCurrentPodName()).thenReturn(testPodName);

        ValidationStatus status = ValidationStatus.PASSED;
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.SESSION, null,
                null, null, 0, status);

        svpKafkaListener.listen(sessionId, kafkaMessage);

        verify(webSocketMessagingService, times(1)).sendValidationStatusForSession(sessionId, status);
    }

    @Test
    public void listen_SvpKafkaMessageCountType_SendToWebSocket() {
        UUID sessionId = UUID.randomUUID();//
        String testPodName = "testName";
        when(sessionRepository.containsSession(any(), anyString())).thenReturn(true);
        when(eurekaDiscoveryServiceImpl.getCurrentPodName()).thenReturn(testPodName);

        int countParameters = 1;
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.COUNT_PARAMETER, null,
                null, null, countParameters, null);

        svpKafkaListener.listen(sessionId, kafkaMessage);

        verify(webSocketMessagingService, times(1)).sendCountDownloadingParameters(sessionId, countParameters);
    }

    @Test
    public void listen_SessionNotExist_SendToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.SESSION, null,
                null, null, 0, null);

        svpKafkaListener.listen(sessionId, kafkaMessage);

        verify(webSocketMessagingService, never()).sendSutParameterResult(any(), any());
        verify(webSocketMessagingService, never()).sendValidationStatusForTab(any(), anyString(), anyString(), any());
        verify(webSocketMessagingService, never()).sendValidationStatusForPage(any(), anyString(), any());
        verify(webSocketMessagingService, never()).sendValidationStatusForSession(any(), any());
        verify(webSocketMessagingService, never()).sendCountDownloadingParameters(any(), anyInt());
        verify(webSocketMessagingService, never()).sendSessionExpiredMessage(any());
    }
}
