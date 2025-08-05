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

package org.qubership.atp.svp.service;

import java.util.UUID;

import org.qubership.atp.svp.service.direct.KafkaMessagingService;
import org.qubership.atp.svp.service.direct.SessionServiceImpl;
import org.qubership.atp.svp.service.direct.WebSocketMessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public abstract class AbstractMessagingService {

    private SessionServiceImpl sessionServiceImpl;
    private KafkaMessagingService kafkaMessagingService;
    private WebSocketMessagingService webSocketMessagingService;


    @Autowired
    public final void setKafkaService(KafkaMessagingService kafkaMessagingService) {
        this.kafkaMessagingService = kafkaMessagingService;
    }

    @Autowired
    public final void setSocketService(WebSocketMessagingService webSocketMessagingService) {
        this.webSocketMessagingService = webSocketMessagingService;
    }

    @Autowired
    public final void setSessionService(SessionServiceImpl sessionServiceImpl) {
        this.sessionServiceImpl = sessionServiceImpl;
    }

    /**
     * Returns the instance of repository based on type set in configuration.
     */
    protected MessagingService getMessageService(UUID sessionId) {
        if (sessionServiceImpl.hasSession(sessionId)) {
            return webSocketMessagingService;
        } else {
            return kafkaMessagingService;
        }
    }
}
