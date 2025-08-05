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

package org.qubership.atp.svp.controllers;

import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.svp.service.direct.SessionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class WebSocketController {

    private final SessionServiceImpl sessionServiceImpl;

    @Autowired
    public WebSocketController(SessionServiceImpl sessionServiceImpl) {
        this.sessionServiceImpl = sessionServiceImpl;
    }

    @MessageMapping("/new-session")
    @AuditAction(auditAction = "Add session ID {{#sessionId}} that is subscribed from UI to BE by Web sockets")
    public void registerWbSession(@Payload UUID sessionId) {
        log.info("Got a new session id {}, and to save it to the list", sessionId);
        sessionServiceImpl.addNewSession(sessionId);
    }
}
