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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.Transactional;

import org.qubership.atp.svp.core.exceptions.StoringSessionTabException;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionSessionNotFoundException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionTabEntity;
import org.qubership.atp.svp.model.events.GetInfoForParameterEvent;
import org.qubership.atp.svp.model.events.GetInfoForTabEvent;
import org.qubership.atp.svp.model.events.ValidatePageEvent;
import org.qubership.atp.svp.model.events.ValidateTabEvent;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.service.PotSessionTabService;
import org.qubership.atp.svp.service.direct.PotSessionParameterServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TabEventListener {

    private final ApplicationEventPublisher eventPublisher;
    private final PotSessionTabService potSessionTabService;
    private final PotSessionParameterServiceImpl potSessionParameterService;

    /**
     * Constructor of TabEventListener.
     */
    @Autowired
    public TabEventListener(ApplicationEventPublisher eventPublisher,
                            PotSessionTabService potSessionTabService,
                            PotSessionParameterServiceImpl potSessionParameterService) {
        this.eventPublisher = eventPublisher;
        this.potSessionTabService = potSessionTabService;
        this.potSessionParameterService = potSessionParameterService;
    }

    /**
     * Handler for {@link GetInfoForTabEvent}.
     * Starts process of getting info for Tab and all parameters under it.
     * <br>
     * Event is processed asynchronously on thread pool 'GettingInfoProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * As a result for each parameter under tab the {@link GetInfoForParameterEvent} event
     * are published in the events chain.
     */
    @Async("GettingInfoProcessExecutor")
    @EventListener(condition = "#getInfoForTabEvent.onlyForPreconfiguredParams == false")
    public void handleGetInfoForTabEvent(GetInfoForTabEvent getInfoForTabEvent) {
        startGettingInfoForTab(getInfoForTabEvent.getSessionId(), getInfoForTabEvent.getTabId(),
                getInfoForTabEvent.getTabName(),
                getInfoForTabEvent.getCountOfUnprocessedTabsUnderPage(),
                getInfoForTabEvent.getCountOfUnprocessedPagesUnderSession());
    }

    /**
     * Handler for {@link GetInfoForTabEvent}.
     * Starts process of getting info for Tab and all parameters under it
     * in case loading of preconfigured parameters only.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * As a result for each parameter under tab the {@link GetInfoForParameterEvent} event
     * are published in the events chain.
     */
    @EventListener(condition = "#getInfoForTabEvent.onlyForPreconfiguredParams == true")
    public void handleGetInfoForTabEventSynchronously(GetInfoForTabEvent getInfoForTabEvent) {
        startGettingInfoForTab(getInfoForTabEvent.getSessionId(), getInfoForTabEvent.getTabId(),
                getInfoForTabEvent.getTabName(),
                getInfoForTabEvent.getCountOfUnprocessedTabsUnderPage(),
                getInfoForTabEvent.getCountOfUnprocessedPagesUnderSession());
    }

    private void startGettingInfoForTab(UUID sessionId, UUID tabId, String tabName,
                                        AtomicInteger countOfUnprocessedTabsUnderPage,
                                        AtomicInteger countOfUnprocessedPagesUnderSession) {
        try {
            log.info("[Session - {}] Started getting info for tab {}.", sessionId, tabName);
            SessionExecutionConfiguration executionConfigurationForSession =
                    potSessionTabService.getExecutionConfigurationForSession(sessionId);
            ConcurrentHashMap<String, ExecutionVariable> executionVariablesForSession =
                    potSessionTabService.getExecutionVariablesForSession(sessionId);
            AtomicInteger zeroCount = new AtomicInteger();
            List<PotSessionParameterEntity> list = potSessionParameterService.getPotSessionParameters(tabId, false);
            AtomicInteger parametersCount = new AtomicInteger(list.size());
            for (PotSessionParameterEntity parameter : list) {
                SutParameterExecutionContext parameterExecutionContext = SutParameterExecutionContext.builder()
                        .sessionId(sessionId)
                        .parameterStarted(OffsetDateTime.now())
                        .sessionConfiguration(executionConfigurationForSession)
                        .executionVariables(executionVariablesForSession)
                        .parameter(parameter)
                        .isDeferredSearchResult(parameter.hasDeferredResults())
                        .countOfUnprocessedParametersUnderTab(parametersCount)
                        .countOfUnprocessedTabsUnderPage(countOfUnprocessedTabsUnderPage)
                        .countOfUnprocessedPagesUnderSession(countOfUnprocessedPagesUnderSession)
                        .countOfUnprocessedSynchronousParametersUnderPage(zeroCount)
                        .build();
                GetInfoForParameterEvent getInfoForParameterEvent = GetInfoForParameterEvent.builder()
                        .parameterExecutionContext(parameterExecutionContext)
                        .build();
                eventPublisher.publishEvent(getInfoForParameterEvent);
            }
            log.info("[Session - {}] Successfully started getting info for parameters under tab "
                    + "(events for each parameter was published).", sessionId);
        } catch (ExecutionSessionNotFoundException sessionNotFoundEx) {
            log.error("Unexpected end of session: " + sessionId + "!", sessionNotFoundEx);
            countOfUnprocessedTabsUnderPage.decrementAndGet();
        } catch (Exception ex) {
            log.error("Unexpected error occurred during the getting info process for session: " + sessionId
                    + " for tab: " + tabName + "!", ex);
            countOfUnprocessedTabsUnderPage.decrementAndGet();
        }
    }

    /**
     * Handler for {@link ValidateTabEvent}.
     * Starts process of validate info for tab if all parameters under tab was already processed
     * (execution and validation processes for all parameters were finished).
     * <br>
     * Event is processed asynchronously on thread pool 'ValidationProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * Calculates ValidationStatus for page and sent it to WebSocket topic "/tab-results".
     * Event {@link ValidatePageEvent} for validate page was published.
     */
    @Async("ValidationProcessExecutor")
    @Transactional
    @EventListener(condition = "#validateTabEvent.countOfUnprocessedParameters.get() == 0 "
            + "&& #validateTabEvent.onlyForPreconfiguredParams == false")
    public void handleValidateTabEvent(ValidateTabEvent validateTabEvent) {
        validateTabByEventMultiThreadSafe(validateTabEvent);
    }

    /**
     * Handler for {@link ValidateTabEvent}.
     * Starts process of validate info for tab if all parameters under tab was already processed
     * (execution and validation processes for all parameters were finished)
     * in case loading of preconfigured parameters only.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * Calculates ValidationStatus for page and sent it to WebSocket topic "/tab-results".
     * Event {@link ValidatePageEvent} for validate page was published.
     */
    @Transactional
    @EventListener(condition = "#validateTabEvent.countOfUnprocessedParameters.get() == 0 "
            + "&& #validateTabEvent.onlyForPreconfiguredParams == true")
    public void handleValidateTabEventSynchronously(ValidateTabEvent validateTabEvent) {
        UUID tabId = validateTabEvent.getTabId();
        PotSessionTabEntity potSessionTabEntity = potSessionTabService.getTabById(tabId);
        validateTab(validateTabEvent.getSessionId(),
                validateTabEvent.getPageName(),
                validateTabEvent.getTabName(),
                potSessionTabEntity,
                validateTabEvent.getOnlyForPreconfiguredParams(),
                validateTabEvent.getCountOfUnprocessedTabsUnderPage(),
                validateTabEvent.getCountOfUnprocessedPagesUnderSession());
    }

    private void validateTabByEventMultiThreadSafe(ValidateTabEvent validateTabEvent) {
        UUID sessionId = validateTabEvent.getSessionId();
        UUID tabId = validateTabEvent.getTabId();
        String pageName = validateTabEvent.getPageName();
        String tabName = validateTabEvent.getTabName();
        synchronized (validateTabEvent.getCountOfUnprocessedParameters()) {
            PotSessionTabEntity potSessionTabEntity = potSessionTabService.getTabById(tabId);
            if (!potSessionTabEntity.isAlreadyValidated()) {
                validateTab(sessionId, pageName, tabName, potSessionTabEntity,
                        validateTabEvent.getOnlyForPreconfiguredParams(),
                        validateTabEvent.getCountOfUnprocessedTabsUnderPage(),
                        validateTabEvent.getCountOfUnprocessedPagesUnderSession());
            }
        }
    }

    private void validateTab(UUID sessionId,
                             String pageName,
                             String tabName,
                             PotSessionTabEntity potSessionTabEntity,
                             boolean onlyForPreconfiguredParams,
                             AtomicInteger countOfUnprocessedTabsUnderPage,
                             AtomicInteger countOfUnprocessedPagesUnderSession) {
        try {
            log.info("[Session - {}] Validation process for tab: {} under page: {} was started",
                    sessionId, tabName, pageName);
            potSessionTabService.validateTab(sessionId, pageName, tabName, potSessionTabEntity);
            countOfUnprocessedTabsUnderPage.decrementAndGet();
            startPageValidation(sessionId, pageName, potSessionTabEntity, onlyForPreconfiguredParams,
                    countOfUnprocessedTabsUnderPage, countOfUnprocessedPagesUnderSession);
            log.info("[Session - {}] Validation process for tab: {} under page: {} was finished successfully. "
                    + "Event for validate page was published.", sessionId, tabName, pageName);
        } catch (ExecutionSessionNotFoundException | StoringSessionTabException exception) {
            log.error("Unexpected end of session: " + sessionId + "!", exception);
        } catch (Exception ex) {
            log.error("Unexpected error occurred during the validation process for tab: " + tabName
                    + " under page: " + pageName + " under session: " + sessionId + "!", ex);
        }
    }

    private void startPageValidation(UUID sessionId,
                                     String pageName,
                                     PotSessionTabEntity potSessionTabEntity,
                                     boolean onlyForPreconfiguredParams,
                                     AtomicInteger countOfUnprocessedTabsUnderPage,
                                     AtomicInteger countOfUnprocessedPagesUnderSession) {
        ValidatePageEvent validatePageEvent = ValidatePageEvent.builder()
                .sessionId(sessionId)
                .pageName(pageName)
                .pageId(potSessionTabEntity.getPotSessionPageEntity().getId())
                .onlyForPreconfiguredParams(onlyForPreconfiguredParams)
                .countOfUnprocessedTabs(countOfUnprocessedTabsUnderPage)
                .countOfUnprocessedPagesUnderSession(countOfUnprocessedPagesUnderSession)
                .build();
        eventPublisher.publishEvent(validatePageEvent);
    }
}
