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

import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.kafka.SvpKafkaMessage;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionParameterRepository;
import org.qubership.atp.svp.service.AbstractMessagingService;
import org.qubership.atp.svp.service.direct.SessionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "kafka.svp.event.enable", havingValue = "true")
@Slf4j
public class SvpKafkaListener extends AbstractMessagingService {

    @Value("${service.pod-name}")
    private String groupId;

    private final SessionServiceImpl sessionServiceImpl;
    private final PotSessionParameterRepository repository;

    /**
     * The constructor of SvpKafkaListener.
     */
    @Autowired
    public SvpKafkaListener(SessionServiceImpl sessionServiceImpl,
                            PotSessionParameterRepository repository) {
        this.sessionServiceImpl = sessionServiceImpl;
        this.repository = repository;
    }

    /**
     * Kafka SVP listener.
     */

    @KafkaListener(id = "${service.pod-name}",
            topics = "${kafka.topic.end.svp}",
            containerFactory = "stringSvpKafkaListenerContainerFactory",
            groupId = "${service.pod-name}"
    )
    public void listen(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) UUID sessionId,
                       @Payload @Valid @NonNull SvpKafkaMessage svpKafkaMessage) {
        log.debug("[{} consumer group] get a message from kafka", groupId);
        if (sessionServiceImpl.hasSession(sessionId)) {
            sendResultToWs(sessionId, svpKafkaMessage);
        }
    }

    private void sendResultToWs(UUID sessionId, SvpKafkaMessage svpKafkaMessage) {
        switch (svpKafkaMessage.getType()) {
            case PARAMETER:
                UUID parameterId = UUID.fromString(svpKafkaMessage.getParameterId());
                PotSessionParameterEntity parameter = repository.findByParameterId(parameterId);
                getMessageService(sessionId).sendSutParameterResult(sessionId, parameter);
                break;
            case TAB:
                String tabName = svpKafkaMessage.getTabName();
                String tabPageName = svpKafkaMessage.getPageName();
                ValidationStatus status = svpKafkaMessage.getStatus();
                getMessageService(sessionId).sendValidationStatusForTab(sessionId, tabPageName, tabName, status);
                break;
            case PAGE:
                String pageName = svpKafkaMessage.getPageName();
                ValidationStatus pageStatus = svpKafkaMessage.getStatus();
                getMessageService(sessionId).sendValidationStatusForPage(sessionId, pageName, pageStatus);
                break;
            case SESSION:
                getMessageService(sessionId).sendValidationStatusForSession(sessionId, svpKafkaMessage.getStatus());
                break;
            case COUNT_PARAMETER:
                int countParameter = svpKafkaMessage.getCountParameter();
                getMessageService(sessionId).sendCountDownloadingParameters(sessionId, countParameter);
                break;
            case PAGE_IN_PROGRESS:
                String pageInProgressName = svpKafkaMessage.getPageName();
                getMessageService(sessionId).sendPageInProgress(sessionId, pageInProgressName);
                break;
            case EXPIRED_SESSION:
                getMessageService(sessionId).sendSessionExpiredMessage(sessionId);
                break;
            default:
                throw new RuntimeException();
        }
    }
}
