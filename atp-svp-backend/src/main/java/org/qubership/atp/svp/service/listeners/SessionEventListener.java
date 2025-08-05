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

package org.qubership.atp.svp.service.listeners;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.qubership.atp.svp.core.exceptions.execution.ExecutionSessionNotFoundException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.events.GetInfoForCommonParametersEvent;
import org.qubership.atp.svp.model.events.GetInfoForSessionEvent;
import org.qubership.atp.svp.model.events.GetInfoForSessionPagesEvent;
import org.qubership.atp.svp.model.events.ValidateSessionEvent;
import org.qubership.atp.svp.service.PotSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SessionEventListener {

    private final ApplicationEventPublisher eventPublisher;
    private final PotSessionService potSessionService;

    @Autowired
    public SessionEventListener(ApplicationEventPublisher eventPublisher, PotSessionService potSessionService) {
        this.eventPublisher = eventPublisher;
        this.potSessionService = potSessionService;
    }

    /**
     * Handler for {@link GetInfoForSessionEvent}.
     * Starts process of getting info for session.
     * The first step in events chain.
     * <br>
     * Event is processed asynchronously on thread pool 'GettingInfoProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link GetInfoForCommonParametersEvent} if Common Parameters is lack under session (not loaded yet)
     * or forcedLoadingCommonParameters session flag is true
     * <br>
     * - {@link GetInfoForSessionPagesEvent} if Common Parameters was already loaded successfully and stored to session
     * and forcedLoadingCommonParameters session flag is false
     */
    @Async("GettingInfoProcessExecutor")
    @EventListener(condition = "#getInfoForSessionEvent.onlyForPreconfiguredParams == false")
    public void handleGetInfoForSessionEvent(GetInfoForSessionEvent getInfoForSessionEvent) {
        startGettingInfoForSession(getInfoForSessionEvent.getSessionId(),
                getInfoForSessionEvent.getOnlyForPreconfiguredParams());
    }

    /**
     * Handler for {@link GetInfoForSessionEvent}.
     * Starts process of getting info for session in case loading of preconfigured parameters only.
     * The first step in events chain.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link GetInfoForCommonParametersEvent} if Common Parameters is lack under session (not loaded yet)
     * or forcedLoadingCommonParameters session flag is true
     * <br>
     * - {@link GetInfoForSessionPagesEvent} if Common Parameters was already loaded successfully and stored to session
     * and forcedLoadingCommonParameters session flag is false
     */
    @EventListener(condition = "#getInfoForSessionEvent.onlyForPreconfiguredParams == true")
    public void handleGetInfoForSessionEventSynchronously(GetInfoForSessionEvent getInfoForSessionEvent) {
        startGettingInfoForSession(getInfoForSessionEvent.getSessionId(),
                getInfoForSessionEvent.getOnlyForPreconfiguredParams());
    }

    private void startGettingInfoForSession(UUID sessionId, boolean isOnlyPreconfiguredParams) {
        try {
            log.info("[Session - {}] Getting info for session started.", sessionId);
            if (potSessionService.shouldCommonParametersLoadedForSession(sessionId)) {
                startGettingInfoForCommonParameters(sessionId, isOnlyPreconfiguredParams);
                log.info("[Session - {}] Event for getting info for Common Parameters under Session was published.",
                        sessionId);
            } else {
                log.info("[Session - {}] Getting info for Common Parameters was skipped (Not needed).", sessionId);
                startGettingInfoForSessionPages(sessionId, isOnlyPreconfiguredParams);
                log.info("[Session - {}] Event for getting info for Pages under Session was published.", sessionId);
            }
        } catch (ExecutionSessionNotFoundException sessionNotFoundEx) {
            log.error("Unexpected end of session: " + sessionId + "!", sessionNotFoundEx);
        } catch (Exception ex) {
            log.error("Unexpected error occurred during the getting info for session: " + sessionId + "!", ex);
        }
    }

    private void startGettingInfoForCommonParameters(UUID sessionId, boolean isOnlyPreconfiguredParams) {
        GetInfoForCommonParametersEvent getInfoForCommonParametersEvent =
                GetInfoForCommonParametersEvent.builder()
                        .sessionId(sessionId)
                        .onlyCommonParametersExecuted(false)
                        .onlyForPreconfiguredParams(isOnlyPreconfiguredParams)
                        .build();
        eventPublisher.publishEvent(getInfoForCommonParametersEvent);
    }

    private void startGettingInfoForSessionPages(UUID sessionId, boolean onlyForPreconfiguredParams) {
        GetInfoForSessionPagesEvent getInfoForSessionPagesEvent = GetInfoForSessionPagesEvent.builder()
                .sessionId(sessionId)
                .countOfUnprocessedCommonParameters(new AtomicInteger())
                .onlyCommonParametersExecuted(false)
                .onlyForPreconfiguredParams(onlyForPreconfiguredParams)
                .build();
        eventPublisher.publishEvent(getInfoForSessionPagesEvent);
    }

    /**
     * Handler for {@link ValidateSessionEvent}.
     * Starts process of validate info for session if all pages under session was already processed
     * (validation process for all pages was finished).
     * The last step in events chain.
     * <br>
     * Event is processed asynchronously on thread pool 'ValidationProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * Calculates ValidationStatus for session and sent it to WebSocket topic "/session-results".
     */
    @Async("ValidationProcessExecutor")
    @Transactional
    @EventListener(condition = "#validateSessionEvent.countOfUnprocessedPages.get() == 0 "
            + "&& #validateSessionEvent.onlyForPreconfiguredParams == false")
    public void handleValidateSessionEvent(ValidateSessionEvent validateSessionEvent) {
        validateSessionByEventMultiThreadSafe(validateSessionEvent);
    }

    /**
     * Handler for {@link ValidateSessionEvent}.
     * Starts process of validate info for session if all pages under session was already processed
     * (validation process for all pages was finished) in case loading of preconfigured parameters only.
     * The last step in events chain.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * Calculates ValidationStatus for session and sent it to WebSocket topic "/session-results".
     */
    @EventListener(condition = "#validateSessionEvent.countOfUnprocessedPages.get() == 0 "
            + "&& #validateSessionEvent.onlyForPreconfiguredParams == true")
    @Transactional
    public void handleValidateSessionEventSynchronously(ValidateSessionEvent validateSessionEvent) {
        PotSessionEntity potSession = potSessionService.findSessionById(validateSessionEvent.getSessionId());
        validateSession(potSession);
    }

    private void validateSessionByEventMultiThreadSafe(ValidateSessionEvent validateSessionEvent) {
        UUID sessionId = validateSessionEvent.getSessionId();
        synchronized (validateSessionEvent.getCountOfUnprocessedPages()) {
            PotSessionEntity session = potSessionService.findSessionById(sessionId);
            if (!session.isAlreadyValidated()) {
                validateSession(session);
            }
        }
    }

    private void validateSession(PotSessionEntity potSession) {
        UUID sessionId = potSession.getSessionId();
        try {
            log.info("[Session - {}] Validation process for session started", sessionId);
            potSessionService.addValidationStatusForSession(potSession);
            log.info("[Session - {}] Validation process for session was finished successfully", sessionId);
        } catch (ExecutionSessionNotFoundException sessionNotFoundEx) {
            log.error("Unexpected end of session: " + sessionId + "!", sessionNotFoundEx);
        } catch (Exception ex) {
            log.error("Unexpected error occurred during the validation process for session: " + sessionId + "!", ex);
        }
    }
}
