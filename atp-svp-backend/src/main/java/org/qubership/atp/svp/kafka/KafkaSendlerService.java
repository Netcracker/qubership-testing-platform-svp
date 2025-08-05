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

import org.qubership.atp.svp.model.kafka.SvpKafkaMessage;
import org.qubership.atp.svp.utils.Utils;
import org.springframework.kafka.core.KafkaTemplate;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class KafkaSendlerService {

    private final KafkaTemplate<UUID, String> kafkaTemplate;
    private final String topicName;

    /**
     * Constructor for KafkaSandlerService.
     */
    public KafkaSendlerService(String topicName, KafkaTemplate<UUID, String> kafkaTemplate) {
        this.topicName = topicName;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send message to Kafka.
     */
    public void sendMessage(UUID sessionId, SvpKafkaMessage msg) {
        try {
            String payload = Utils.mapper.writeValueAsString(msg);
            kafkaTemplate.send(topicName, sessionId, payload);
        } catch (Exception e) {
            log.error(String.format("Cannot put terminate event to kafka for session - {}", sessionId), e);
            throw new RuntimeException(e);
        }
    }
}
