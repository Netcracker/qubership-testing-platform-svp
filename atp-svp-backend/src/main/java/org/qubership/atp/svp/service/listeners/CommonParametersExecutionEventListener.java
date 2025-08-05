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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.exceptions.StoringSessionException;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionSessionNotFoundException;
import org.qubership.atp.svp.model.db.CommonParameterEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.events.GetInfoForCommonParameterEvent;
import org.qubership.atp.svp.model.events.GetInfoForCommonParametersEvent;
import org.qubership.atp.svp.model.events.GetInfoForSessionPagesEvent;
import org.qubership.atp.svp.model.pot.CommonParameterExecutionContext;
import org.qubership.atp.svp.service.PotSessionService;
import org.qubership.atp.svp.service.jpa.CommonParametersServiceJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommonParametersExecutionEventListener {

    private final ApplicationEventPublisher eventPublisher;
    private final PotSessionService potSessionService;
    private final CommonParametersServiceJpa commonParametersServiceJpa;

    /**
     * Constructor for CommonParametersExecutionEventListener instance.
     */
    @Autowired
    public CommonParametersExecutionEventListener(ApplicationEventPublisher eventPublisher,
                                                  PotSessionService potSessionService,
                                                  CommonParametersServiceJpa commonParametersServiceJpa) {
        this.eventPublisher = eventPublisher;
        this.potSessionService = potSessionService;
        this.commonParametersServiceJpa = commonParametersServiceJpa;
    }

    /**
     * Handler for {@link GetInfoForCommonParametersEvent}.
     * Starts process of getting info for all of common parameters if they exist.
     * <br>
     * Event is processed asynchronously on thread pool 'GettingInfoProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link GetInfoForCommonParameterEvent} if Common Parameters exists in project configuration;
     * <br>
     * - {@link GetInfoForSessionPagesEvent} if Common Parameters does not exist in project configuration.
     */
    @Async("GettingInfoProcessExecutor")
    @EventListener(condition = "#getInfoForCommonParametersEvent.onlyForPreconfiguredParams == false")
    public void handleGetInfoForCommonParametersEvent(GetInfoForCommonParametersEvent getInfoForCommonParametersEvent) {
        startGettingInfoForCommonParametersIfExists(getInfoForCommonParametersEvent.getSessionId(),
                getInfoForCommonParametersEvent.getOnlyCommonParametersExecuted(),
                getInfoForCommonParametersEvent.getOnlyForPreconfiguredParams());
    }

    /**
     * Handler for {@link GetInfoForCommonParametersEvent}.
     * Starts process of getting info for all of common parameters if they exist
     * in case loading of preconfigured parameters only.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link GetInfoForCommonParameterEvent} if Common Parameters exists in project configuration;
     * <br>
     * - {@link GetInfoForSessionPagesEvent} if Common Parameters does not exist in project configuration.
     */
    @EventListener(condition = "#getInfoForCommonParametersEvent.onlyForPreconfiguredParams == true")
    public void handleGetInfoForCommonParametersEventSynchronously(
            GetInfoForCommonParametersEvent getInfoForCommonParametersEvent) {
        startGettingInfoForCommonParametersIfExists(getInfoForCommonParametersEvent.getSessionId(),
                getInfoForCommonParametersEvent.getOnlyCommonParametersExecuted(),
                getInfoForCommonParametersEvent.getOnlyForPreconfiguredParams());
    }

    private void startGettingInfoForCommonParametersIfExists(UUID sessionId,
                                                             boolean onlyCommonParametersExecuted,
                                                             boolean onlyForPreconfiguredParams) {
        try {
            log.info("[Session - {}] Start getting common parameters.", sessionId);
            PotSessionEntity session = potSessionService.findSessionById(sessionId);

            UUID folderId = session.getExecutionConfiguration().getFolder();
            List<CommonParameterEntity> commonParameters =
                    commonParametersServiceJpa.getCommonParameterEntitiesByFolderId(folderId);
            if (commonParameters.isEmpty()) {
                log.info("[Session - {}] No common parameters were found to get info for!", sessionId);
                startGettingInfoForSessionPages(sessionId, onlyCommonParametersExecuted, onlyForPreconfiguredParams);
                log.info("[Session - {}] Successfully started getting info for Pages "
                        + "(event was published).", sessionId);
            } else {
                startGettingInfoForCommonParameters(session, commonParameters);
                log.info("[Session - {}] Successfully started getting info for Common Parameters "
                        + "(events for each parameter was published).", sessionId);
            }
        } catch (ExecutionSessionNotFoundException | StoringSessionException sessionNotFoundEx) {
            log.error("Unexpected end of session: " + sessionId + "!", sessionNotFoundEx);
        } catch (Exception ex) {
            log.error("[Session - " + sessionId + "] Unexpected error occurred during the "
                    + "getting info for Common Parameters!", ex);
        }
    }

    private void startGettingInfoForSessionPages(UUID sessionId,
                                                 boolean onlyCommonParametersExecuted,
                                                 boolean onlyForPreconfiguredParams) {
        GetInfoForSessionPagesEvent getInfoForSessionPagesEvent = GetInfoForSessionPagesEvent.builder()
                .sessionId(sessionId)
                .countOfUnprocessedCommonParameters(new AtomicInteger())
                .onlyCommonParametersExecuted(onlyCommonParametersExecuted)
                .onlyForPreconfiguredParams(onlyForPreconfiguredParams)
                .build();
        eventPublisher.publishEvent(getInfoForSessionPagesEvent);
    }

    private void startGettingInfoForCommonParameters(PotSessionEntity session,
                                                     List<CommonParameterEntity> commonParameters)
            throws StoringSessionException {
        List<PotSessionParameterEntity> potSessionCommonParameters = commonParameters.stream()
                .map(commonParameter -> new PotSessionParameterEntity(session, commonParameter.getSutParameterEntity()))
                .collect(Collectors.toList());
        PotSessionEntity updatedSession = potSessionService.addCommonParametersToSession(session,
                potSessionCommonParameters);
        AtomicInteger commonParametersCount = new AtomicInteger(potSessionCommonParameters.size());
        updatedSession.getCommonParameters().forEach(parameter ->
                startGettingInfoForCommonParameter(session, parameter, commonParametersCount));
    }

    private void startGettingInfoForCommonParameter(PotSessionEntity session,
                                                    PotSessionParameterEntity parameter,
                                                    AtomicInteger commonParametersCount) {
        CommonParameterExecutionContext parameterExecutionContext = CommonParameterExecutionContext.builder()
                .sessionId(session.getSessionId())
                .parameterStarted(OffsetDateTime.now())
                .sessionConfiguration(session.getExecutionConfiguration())
                .executionVariables(session.getExecutionVariables())
                .parameter(parameter)
                .isDeferredSearchResult(parameter.hasDeferredResults())
                .countOfUnprocessedCommonParameters(commonParametersCount)
                .build();

        GetInfoForCommonParameterEvent getInfoForCommonParameterEvent =
                GetInfoForCommonParameterEvent.builder()
                        .parameterExecutionContext(parameterExecutionContext)
                        .build();
        eventPublisher.publishEvent(getInfoForCommonParameterEvent);
    }
}
