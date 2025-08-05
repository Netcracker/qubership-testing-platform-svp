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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.messages.PageValidationResultMessage;
import org.qubership.atp.svp.model.messages.SessionCountParametersMessage;
import org.qubership.atp.svp.model.messages.SessionExpiredMessage;
import org.qubership.atp.svp.model.messages.SessionValidationResultMessage;
import org.qubership.atp.svp.model.messages.SutParameterResultMessage;
import org.qubership.atp.svp.model.messages.TabValidationResultMessage;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = WebSocketMessagingService.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class WebSocketMessagingServiceTest extends TestWithTestData {

    @Autowired
    WebSocketMessagingService webSocketMessagingService;
    @SpyBean
    SimpMessagingTemplate messagingTemplate;
    @MockBean
    MessageChannel messageChannel;

    @Test
    public void sendSutParameterResult_sessionExistInPod_SenToWebSocket() throws IOException {
        UUID sessionId = UUID.randomUUID();
        String file = loadFileToString("src/test/resources/test_data/webSocket/PotSessionParameter.json");
        PotSessionParameterEntity parameter = objectMapper.readValue(file, PotSessionParameterEntity.class);

        String topic = "/parameter-results";
        SutParameterResultMessage mess = new SutParameterResultMessage(sessionId, parameter);

        webSocketMessagingService.sendSutParameterResult(sessionId, parameter);

        verify(messagingTemplate, times(1)).convertAndSend(topic, mess);
    }

    @Test
    public void sendSutParameterResult_needCheckIsFalse_SenToWebSocket() throws IOException {
        UUID sessionId = UUID.randomUUID();
        String file = loadFileToString("src/test/resources/test_data/webSocket/PotSessionParameter.json");
        PotSessionParameterEntity parameter = objectMapper.readValue(file, PotSessionParameterEntity.class);
        String topic = "/parameter-results";
        SutParameterResultMessage expectedMessage = new SutParameterResultMessage(sessionId, parameter);

        webSocketMessagingService.sendSutParameterResult(sessionId, parameter);

        verify(messagingTemplate, times(1)).convertAndSend(topic, expectedMessage);
    }

    @Test
    public void sendValidationStatusForTab_sessionExistInPod_SenToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        String pageName = "testPage";
        String tabName = "testTab";
        ValidationStatus tabStatus = ValidationStatus.NONE;
        String topic = "/tab-results";
        TabValidationResultMessage message = new TabValidationResultMessage(sessionId, pageName, tabName, tabStatus);

        webSocketMessagingService.sendValidationStatusForTab(sessionId, pageName, tabName, tabStatus);

        verify(messagingTemplate, times(1)).convertAndSend(topic, message);
    }

    @Test
    public void sendValidationStatusForTab_needCheckIsFalse_SenToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        String pageName = "testPage";
        String tabName = "testTab";
        ValidationStatus tabStatus = ValidationStatus.NONE;
        String topic = "/tab-results";
        TabValidationResultMessage message = new TabValidationResultMessage(sessionId, pageName, tabName, tabStatus);

        webSocketMessagingService.sendValidationStatusForTab(sessionId, pageName, tabName, tabStatus);

        verify(messagingTemplate, times(1)).convertAndSend(topic, message);
    }

    @Test
    public void sendValidationStatusForPage_sessionExistInPod_SenToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        String pageName = "testPage";
        ValidationStatus status = ValidationStatus.NONE;
        String topic = "/page-results";
        PageValidationResultMessage message = new PageValidationResultMessage(sessionId, pageName, status);

        webSocketMessagingService.sendValidationStatusForPage(sessionId, pageName, status);

        verify(messagingTemplate, times(1)).convertAndSend(topic, message);
    }

    @Test
    public void sendValidationStatusForPage_needCheckIsFalse_SenToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        String pageName = "testPage";
        ValidationStatus status = ValidationStatus.NONE;
        String topic = "/page-results";
        PageValidationResultMessage message = new PageValidationResultMessage(sessionId, pageName, status);

        webSocketMessagingService.sendValidationStatusForPage(sessionId, pageName, status);

        verify(messagingTemplate, times(1)).convertAndSend(topic, message);
    }

    @Test
    public void sendValidationStatusForSession_sessionExistInPod_SenToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        ValidationStatus status = ValidationStatus.PASSED;
        String topic = "/session-results";
        SessionValidationResultMessage message = new SessionValidationResultMessage(sessionId, status);

        webSocketMessagingService.sendValidationStatusForSession(sessionId, status);

        verify(messagingTemplate, times(1)).convertAndSend(topic, message);
    }

    @Test
    public void sendValidationStatusForSession_needCheckIsFalse_SenToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        ValidationStatus status = ValidationStatus.PASSED;
        String topic = "/session-results";
        SessionValidationResultMessage message = new SessionValidationResultMessage(sessionId, status);

        webSocketMessagingService.sendValidationStatusForSession(sessionId, status);

        verify(messagingTemplate, times(1)).convertAndSend(topic, message);
    }

    @Test
    public void sendSessionExpiredMessage_sessionId_SenToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        String topic = "/expired-sessions";
        SessionExpiredMessage message = new SessionExpiredMessage(sessionId);

        webSocketMessagingService.sendSessionExpiredMessage(sessionId);

        verify(messagingTemplate, times(1)).convertAndSend(topic, message);
    }

    @Test
    public void sendCountDownloadingParameters_sessionExistInPod_SenToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        int countParameters = 1;
        String topic = "/count-parameters";
        SessionCountParametersMessage message = new SessionCountParametersMessage(sessionId, countParameters);

        webSocketMessagingService.sendCountDownloadingParameters(sessionId, countParameters);

        verify(messagingTemplate, times(1)).convertAndSend(topic, message);
    }

    @Test
    public void sendCountDownloadingParameters_needCheckIsFalse_SenToWebSocket() {
        UUID sessionId = UUID.randomUUID();
        int countParameters = 1;
        String topic = "/count-parameters";
        SessionCountParametersMessage message = new SessionCountParametersMessage(sessionId, countParameters);

        webSocketMessagingService.sendCountDownloadingParameters(sessionId, countParameters);

        verify(messagingTemplate, times(1)).convertAndSend(topic, message);
    }
}
