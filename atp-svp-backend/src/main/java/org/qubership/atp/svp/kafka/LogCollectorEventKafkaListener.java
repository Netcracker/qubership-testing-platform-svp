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

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.svp.mdc.MdcField;
import org.qubership.atp.svp.model.events.ReloadSutParameterEvent;
import org.qubership.atp.svp.model.kafka.LogCollectorKafkaMessage;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.service.DeferredSearchService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "kafka.logcollector.event.enable", havingValue = "true")
@Slf4j
public class LogCollectorEventKafkaListener implements KafkaEventListener<String, LogCollectorKafkaMessage> {

    @Getter
    private static final String GROUP_ID_KAFKA_LC = "atp-svp_" + UUID.randomUUID();
    private final ApplicationEventPublisher eventPublisher;
    private final DeferredSearchService deferredSearchService;

    @Autowired
    public LogCollectorEventKafkaListener(ApplicationEventPublisher eventPublisher,
                                          DeferredSearchService deferredSearchService) {
        this.eventPublisher = eventPublisher;
        this.deferredSearchService = deferredSearchService;
    }

    /**
     * Kafka listener for events from log collector about completed search.
     * Before as listener start processing event be validated object of payload.
     * If {@link DeferredSearchService} contain {@link AbstractParameterExecutionContext} for search id from event
     * then run(publish event {@link ReloadSutParameterEvent} via {@link ApplicationEventPublisher})
     * getting and processing search result else do nothing.
     *
     * @param logCollectorKafkaMessage {@link LogCollectorKafkaMessage}.
     */
    @Override
    @KafkaListener(topics = "${kafka.topic.end.logcollector}",
            containerFactory = "stringLogCollectorEventConcurrentKafkaListenerContainerFactory",
            errorHandler = "defaultKafkaValidationErrorHandler",
            id = "#{T(org.qubership.atp.svp.kafka.LogCollectorEventKafkaListener).getGROUP_ID_KAFKA_LC()}",
            groupId = "#{T(org.qubership.atp.svp.kafka.LogCollectorEventKafkaListener).getGROUP_ID_KAFKA_LC()}"
    )
    public void listen(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String responseSearchIdAsString,
                       @Payload @Valid @NonNull LogCollectorKafkaMessage logCollectorKafkaMessage) {
        MDC.clear();
        UUID responseSearchId = UUID.fromString(responseSearchIdAsString);
        log.debug("Received Kafka event from LogCollector, ResponseSearchId: {} ,Message: {}", responseSearchId,
                logCollectorKafkaMessage);
        Optional<AbstractParameterExecutionContext> contextByLogCollectorRequestSearchId =
                deferredSearchService.findContextByRequestSearchId(logCollectorKafkaMessage.getRequestId());
        if (contextByLogCollectorRequestSearchId.isPresent()) {
            MdcUtils.put(MdcField.SESSION_ID.toString(),
                    contextByLogCollectorRequestSearchId.get().getSessionId());
            //In responseSearchId we pass the key from kafka by which we will take the results from LC
            reloadSutParameterEventForEndedSearch(responseSearchId, logCollectorKafkaMessage.getRequestId(),
                    contextByLogCollectorRequestSearchId.get(), logCollectorKafkaMessage);
            deferredSearchService.evictContextByRequestSearchId(logCollectorKafkaMessage.getRequestId());
        } else {
            log.debug("ParameterExecutionContext for the search was not found ResponseSearchId: {} ,Message: {}",
                    responseSearchId, logCollectorKafkaMessage);
        }
    }

    private void reloadSutParameterEventForEndedSearch(UUID responseSearchId, UUID requestSearchId,
                                                       AbstractParameterExecutionContext parameterContext,
                                                       LogCollectorKafkaMessage logCollectorKafkaMessage) {
        if (logCollectorKafkaMessage.hasEndedStatus()) {
            addResponseSearchIdToParameterContext(responseSearchId, parameterContext);
            ReloadSutParameterEvent reloadSutParameterEvent = ReloadSutParameterEvent.builder()
                    .requestId(requestSearchId)
                    .parameterExecutionContext(parameterContext)
                    .build();
            eventPublisher.publishEvent(reloadSutParameterEvent);
        } else {
            log.warn("LogCollectorKafkaEvent has incorrect (not ended) status ResponseSearchId: {} ,Message: {}",
                    responseSearchId, logCollectorKafkaMessage);
        }
    }

    private void addResponseSearchIdToParameterContext(UUID responseSearchId,
                                                       AbstractParameterExecutionContext parameterContext) {
        if (Objects.isNull(parameterContext.getResponseSearchId())) {
            parameterContext.setResponseSearchId(responseSearchId);
        }
    }
}
