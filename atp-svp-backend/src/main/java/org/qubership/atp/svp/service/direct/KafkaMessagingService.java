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

import java.util.UUID;

import org.qubership.atp.svp.core.enums.ResultType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.kafka.KafkaSendlerService;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.kafka.SvpKafkaMessage;
import org.qubership.atp.svp.service.MessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaMessagingService implements MessagingService {

    private final KafkaSendlerService kafkaSendlerService;

    /**
     * The constructor of WebSocketMessagingService.
     */
    @Autowired
    public KafkaMessagingService(KafkaSendlerService kafkaSendlerService) {
        this.kafkaSendlerService = kafkaSendlerService;
    }

    /**
     * Sends SUT parameter result message to Web Socket.
     */
    public void sendSutParameterResult(UUID sessionId, PotSessionParameterEntity parameter) {
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.PARAMETER, null,
                null, parameter.getParameterId().toString(), 0, null);
        sendToKafka(sessionId, kafkaMessage);
    }

    /**
     * Sends validation status of tab execution to Web Socket.
     */
    public void sendValidationStatusForTab(UUID sessionId, String pageName,
                                           String tabName, ValidationStatus tabStatus) {
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.TAB, pageName,
                tabName, null, 0, tabStatus);
        sendToKafka(sessionId, kafkaMessage);
    }

    /**
     * Sends validation status of page execution to Web Socket.
     */
    public void sendValidationStatusForPage(UUID sessionId, String pageName, ValidationStatus pageStatus) {
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.PAGE, pageName,
                null, null, 0, pageStatus);
        sendToKafka(sessionId, kafkaMessage);
    }

    /**
     * Sends validation status of session execution to Web Socket.
     */
    public void sendValidationStatusForSession(UUID sessionId, ValidationStatus sessionStatus) {
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.SESSION, null,
                null, null, 0, sessionStatus);
        sendToKafka(sessionId, kafkaMessage);
    }

    /**
     * Sends expired session to Web Socket.
     */
    public void sendSessionExpiredMessage(UUID sessionId) {
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.EXPIRED_SESSION, null,
                null, null, 0, null);
        sendToKafka(sessionId, kafkaMessage);
    }

    /**
     * Sends count of loading parameters message to Web Socket.
     */
    public void sendCountDownloadingParameters(UUID sessionId, int countParameters) {
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.COUNT_PARAMETER, null,
                null, null, countParameters, null);
        sendToKafka(sessionId, kafkaMessage);
    }

    /**
     * Send sessionId with page name that has InProgress status.
     */
    public void sendPageInProgress(UUID sessionId, String pageName) {
        SvpKafkaMessage kafkaMessage = new SvpKafkaMessage(ResultType.PAGE_IN_PROGRESS, pageName,
                null, null, 0, null);
        sendToKafka(sessionId, kafkaMessage);
    }

    private void sendToKafka(UUID sessionId, SvpKafkaMessage message) {
        log.debug("[{}] Sending message to kafka, message: {}.",
                sessionId, message);
        kafkaSendlerService.sendMessage(sessionId, message);
    }
}
