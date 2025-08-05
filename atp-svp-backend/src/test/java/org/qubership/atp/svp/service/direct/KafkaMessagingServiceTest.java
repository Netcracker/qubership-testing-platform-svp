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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.core.enums.ResultType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.kafka.KafkaSendlerService;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.kafka.SvpKafkaMessage;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = KafkaMessagingService.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class KafkaMessagingServiceTest extends TestWithTestData {

    @Autowired
    KafkaMessagingService kafkaMessagingService;
    @MockBean
    KafkaSendlerService kafkaSendlerService;


    @Test
    public void sendSutParameterResult_sessionNotExistInPod_SenToKafka() throws IOException {
        UUID sessionId = UUID.randomUUID();
        String file = loadFileToString("src/test/resources/test_data/webSocket/PotSessionParameter.json");
        PotSessionParameterEntity parameter = objectMapper.readValue(file, PotSessionParameterEntity.class);
        SvpKafkaMessage expectedKafkaMessage = new SvpKafkaMessage(ResultType.PARAMETER, null,
                null, parameter.getParameterId().toString(), 0, null);

        kafkaMessagingService.sendSutParameterResult(sessionId, parameter);

        verify(kafkaSendlerService, times(1)).sendMessage(sessionId, expectedKafkaMessage);
    }

    @Test
    public void sendValidationStatusForTab_sessionNotExistInPod_SenToKafka() {
        UUID sessionId = UUID.randomUUID();
        String pageName = "testPage";
        String tabName = "testTab";
        ValidationStatus tabStatus = ValidationStatus.NONE;
        boolean needCheck = true;
        SvpKafkaMessage message = new SvpKafkaMessage(ResultType.TAB, pageName,
                tabName, null, 0, tabStatus);

        kafkaMessagingService.sendValidationStatusForTab(sessionId, pageName, tabName, tabStatus);

        verify(kafkaSendlerService, times(1)).sendMessage(sessionId, message);
    }

    @Test
    public void sendValidationStatusForPage_sessionNotExistInPod_SenTKafka() {
        UUID sessionId = UUID.randomUUID();
        String pageName = "testPage";
        ValidationStatus status = ValidationStatus.NONE;
        boolean needCheck = true;
        SvpKafkaMessage message =  new SvpKafkaMessage(ResultType.PAGE, pageName,
                null, null, 0, status);

        kafkaMessagingService.sendValidationStatusForPage(sessionId, pageName, status);

        verify(kafkaSendlerService, times(1)).sendMessage(sessionId, message);
    }

    @Test
    public void sendValidationStatusForSession_sessionNotExistInPod_SenTKafka() {
        UUID sessionId = UUID.randomUUID();
        ValidationStatus status = ValidationStatus.PASSED;
        boolean needCheck = true;
        SvpKafkaMessage message = new SvpKafkaMessage(ResultType.SESSION, null,
                null, null, 0, status);

        kafkaMessagingService.sendValidationStatusForSession(sessionId, status);

        verify(kafkaSendlerService, times(1)).sendMessage(sessionId, message);
    }

    @Test
    public void sendCountDownloadingParameters_sessionNotExistInPod_SenTKafka() {
        UUID sessionId = UUID.randomUUID();
        int countParameters = 1;
        boolean needCheck = true;
        SvpKafkaMessage message = new SvpKafkaMessage(ResultType.COUNT_PARAMETER, null,
                null, null, countParameters, null);

        kafkaMessagingService.sendCountDownloadingParameters(sessionId, countParameters);

        verify(kafkaSendlerService, times(1)).sendMessage(sessionId, message);
    }
}
