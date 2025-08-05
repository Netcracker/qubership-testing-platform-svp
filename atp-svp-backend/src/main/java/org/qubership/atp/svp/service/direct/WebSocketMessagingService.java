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

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.messages.PageInProgressMessage;
import org.qubership.atp.svp.model.messages.PageValidationResultMessage;
import org.qubership.atp.svp.model.messages.SessionCountParametersMessage;
import org.qubership.atp.svp.model.messages.SessionExpiredMessage;
import org.qubership.atp.svp.model.messages.SessionValidationResultMessage;
import org.qubership.atp.svp.model.messages.SutParameterResultMessage;
import org.qubership.atp.svp.model.messages.TabValidationResultMessage;
import org.qubership.atp.svp.service.MessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WebSocketMessagingService implements MessagingService {

    private final String parameterResultsDestination = "/parameter-results";
    private final String tabValidationStatusDestination = "/tab-results";
    private final String pageValidationStatusDestination = "/page-results";
    private final String sessionValidationStatusDestination = "/session-results";
    private final String expiredSessionDestination = "/expired-sessions";
    private final String countDownloadingParameters = "/count-parameters";
    private final String pageInProgressTopic = "/page-in-progress";

    private SimpMessagingTemplate messagingTemplate;

    /**
     * The constructor of WebSocketMessagingService.
     */
    @Autowired
    public WebSocketMessagingService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Sends SUT parameter result message to Web Socket.
     */
    public void sendSutParameterResult(UUID sessionId, PotSessionParameterEntity parameter) {
        sendMessage(sessionId, parameterResultsDestination, new SutParameterResultMessage(sessionId, parameter));
    }

    /**
     * Sends validation status of tab execution to Web Socket.
     */
    public void sendValidationStatusForTab(UUID sessionId, String pageName,
                                           String tabName, ValidationStatus tabStatus) {
        sendMessage(sessionId, tabValidationStatusDestination,
                new TabValidationResultMessage(sessionId, pageName, tabName, tabStatus));

    }

    /**
     * Sends validation status of page execution to Web Socket.
     */
    public void sendValidationStatusForPage(UUID sessionId, String pageName, ValidationStatus pageStatus) {
        sendMessage(sessionId, pageValidationStatusDestination, new PageValidationResultMessage(sessionId, pageName,
                pageStatus));
    }

    /**
     * Sends validation status of session execution to Web Socket.
     */
    public void sendValidationStatusForSession(UUID sessionId, ValidationStatus sessionStatus) {

        sendMessage(sessionId, sessionValidationStatusDestination, new SessionValidationResultMessage(sessionId,
                sessionStatus));
    }

    /**
     * Sends expired session to Web Socket.
     */
    public void sendSessionExpiredMessage(UUID sessionId) {
        sendMessage(sessionId, expiredSessionDestination, new SessionExpiredMessage(sessionId));
    }

    private void sendMessage(UUID sessionId, String topic, Object message) {
        try {
            log.debug("[{}] Sending message to socket topic: {}, message: {}.",
                    sessionId, topic, message);
            this.messagingTemplate.convertAndSend(topic, message);
        } catch (MessagingException ex) {
            log.error("[{}] Couldn't send message, topic: {}, message: {}.", sessionId, topic, message, ex);
        }
    }

    /**
     * Sends count of loading parameters message to Web Socket.
     */
    public void sendCountDownloadingParameters(UUID sessionId, int countParameters) {
        sendMessage(sessionId, countDownloadingParameters, new SessionCountParametersMessage(sessionId,
                countParameters));
    }

    /**
     * Send sessionId with page name that has InProgress status.
     */
    public void sendPageInProgress(UUID sessionId, String pageName) {
        sendMessage(sessionId, pageInProgressTopic, new PageInProgressMessage(sessionId, pageName));
    }
}
