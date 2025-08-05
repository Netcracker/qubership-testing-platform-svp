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

import org.qubership.atp.svp.model.events.GenerateLogCollectorLinkToTemplateEvent;
import org.qubership.atp.svp.model.events.GetInfoForParameterEvent;
import org.qubership.atp.svp.model.events.GetInfoForTabsUnderPageEvent;
import org.qubership.atp.svp.model.events.ReloadSutParameterEvent;
import org.qubership.atp.svp.model.events.ValidateParameterEvent;
import org.qubership.atp.svp.model.events.ValidateTabEvent;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.service.DeferredSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ParameterEventListener extends AbstractParameterEventListener {

    private final DeferredSearchService deferredSearchService;

    @Autowired
    public ParameterEventListener(DeferredSearchService deferredSearchService) {
        this.deferredSearchService = deferredSearchService;
    }

    /**
     * Handler for {@link GetInfoForParameterEvent}.
     * Starts process of getting info for SUT parameter.
     * <br>
     * Event is processed asynchronously on thread pool 'GettingInfoProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link GenerateLogCollectorLinkToTemplateEvent} if SUT parameter should have
     * LogCollector link to temporary template;
     * <br>
     * - {@link ValidateParameterEvent} if SUT Parameter allows validation
     * (deferred result flag is false,
     * validation type of parameter is not {@link org.qubership.atp.svp.core.enums.ValidationType#NONE}
     * and parameter type is not JsonTable);
     * <br>
     * - {@link ValidateTabEvent} if SUT Parameter processed successfully (all other events-cases completed).
     */
    @Async("GettingInfoProcessExecutor")
    @EventListener(condition = "#getInfoForParameterEvent.getParameterExecutionContext()"
            + ".parameter.synchronousLoading == false "
            + "&& #getInfoForParameterEvent.getParameterExecutionContext()"
            + ".sessionConfiguration.onlyForPreconfiguredParams == false")
    public void handleGetInfoForParameterEvent(GetInfoForParameterEvent getInfoForParameterEvent) {
        SutParameterExecutionContext executionContext = getInfoForParameterEvent.getParameterExecutionContext();
        log.info("[Session - {}] Started getting info for parameter {}.",
                executionContext.getSessionId(), executionContext.getParameter().getPath());
        super.startGettingInfoForParameter(executionContext);
    }

    /**
     * Handler for {@link GetInfoForParameterEvent}.
     * Starts process of getting info for SUT parameter with synchronously loading
     * or in case only preconfigured params loading for session.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link GenerateLogCollectorLinkToTemplateEvent} if SUT parameter should have
     * LogCollector link to temporary template;
     * <br>
     * - {@link ValidateParameterEvent} if SUT Parameter allows validation
     * (deferred result flag is false,
     * validation type of parameter is not {@link org.qubership.atp.svp.core.enums.ValidationType#NONE}
     * and parameter type is not JsonTable);
     * <br>
     * - {@link GetInfoForTabsUnderPageEvent} if SUT Parameter processed successfully
     * (all other events-cases completed).
     */
    @EventListener(condition = "#getInfoForParameterEvent.getParameterExecutionContext()"
            + ".parameter.synchronousLoading == true "
            + "|| #getInfoForParameterEvent.getParameterExecutionContext()"
            + ".sessionConfiguration.onlyForPreconfiguredParams == true")
    public void handleGetInfoForParameterEventSynchronously(GetInfoForParameterEvent getInfoForParameterEvent) {
        SutParameterExecutionContext executionContext = getInfoForParameterEvent.getParameterExecutionContext();
        log.info("[Session - {}] Started getting info for parameter {} with synchronous loading.",
                executionContext.getSessionId(), executionContext.getParameter().getPath());
        super.startGettingInfoForParameter(executionContext);
    }

    /**
     * Listener of event {@link ReloadSutParameterEvent} for rerun getting info process
     * for parameters with deferred results.
     * <br>
     * Event is processed asynchronously on thread pool 'GettingInfoProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     */
    @Async("GettingInfoProcessExecutor")
    @EventListener
    public void handleReloadSutParameterEvent(ReloadSutParameterEvent reloadSutParameterEvent) {
        try {
            boolean deferredSearchResultFlag = false;
            reloadSutParameterEvent.getParameterExecutionContext().setDeferredSearchResult(deferredSearchResultFlag);
            log.info("[Session- {}] Event processing has started, "
                            + "SUT parameter loading thread will be restarted for collect the results",
                    reloadSutParameterEvent.getParameterExecutionContext().getSessionId());
            startGettingInfoForParameter(reloadSutParameterEvent.getParameterExecutionContext());
        } catch (Exception e) {
            AbstractParameterExecutionContext parameterContext = reloadSutParameterEvent.getParameterExecutionContext();
            String errorMessage = "[Session- " + parameterContext.getSessionId() + "] Failed to restart "
                    + "SutParameterExecutor thread for collect the results. Parameter execution context: "
                    + parameterContext;
            log.error(errorMessage);
            deferredSearchService.killDeferredSearchResult(reloadSutParameterEvent.getRequestId(),
                    errorMessage);
        }
    }

    /**
     * Handler for {@link ValidateParameterEvent}.
     * Starts process of validation for SUT parameter.
     * <br>
     * Event is processed asynchronously on thread pool 'ValidationProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * Calculates ValidationStatus for SUT parameter and sent it to WebSocket topic "/parameter-results".
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link GetInfoForTabsUnderPageEvent} if SUT Parameter with synchronous loading
     * processed successfully (all other events-cases completed).
     * <br>
     * - {@link ValidateTabEvent} if SUT Parameter without synchronous loading
     * processed successfully (all other events-cases completed).
     */
    @Async("ValidationProcessExecutor")
    @EventListener(condition = "#validateParameterEvent.getParameterExecutionContext()"
            + ".parameter.synchronousLoading == false "
            + "&& #validateParameterEvent.getParameterExecutionContext()"
            + ".sessionConfiguration.onlyForPreconfiguredParams == false")
    public void handleValidateParameterEvent(ValidateParameterEvent validateParameterEvent) {
        SutParameterExecutionContext executionContext = validateParameterEvent.getParameterExecutionContext();
        log.info("[Session - {}] Started validation for parameter {}.", executionContext.getSessionId(),
                executionContext.getParameter().getPath());
        super.startParameterValidationProcess(validateParameterEvent.getParameterExecutionContext());
    }

    /**
     * Handler for {@link ValidateParameterEvent}.
     * Starts process of validation for SUT parameter with synchronously loading
     * or in case only preconfigured params loading for session.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * Calculates ValidationStatus for SUT parameter and sent it to WebSocket topic "/parameter-results".
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link GetInfoForTabsUnderPageEvent} if SUT Parameter with synchronous loading
     * processed successfully (all other events-cases completed).
     * <br>
     * - {@link ValidateTabEvent} if SUT Parameter without synchronous loading
     * processed successfully (all other events-cases completed).
     */
    @EventListener(condition = "#validateParameterEvent.getParameterExecutionContext()"
            + ".parameter.synchronousLoading == true "
            + "|| #validateParameterEvent.getParameterExecutionContext()"
            + ".sessionConfiguration.onlyForPreconfiguredParams == true")
    public void handleValidateParameterEventSynchronously(ValidateParameterEvent validateParameterEvent) {
        SutParameterExecutionContext executionContext = validateParameterEvent.getParameterExecutionContext();
        log.info("[Session - {}] Started validation for parameter {} with synchronous loading.",
                executionContext.getSessionId(),
                executionContext.getParameter().getPath());
        super.startParameterValidationProcess(validateParameterEvent.getParameterExecutionContext());
    }

    @Override
    protected void startParameterValidation(AbstractParameterExecutionContext executionContext) {
        ValidateParameterEvent validateParameterEvent = ValidateParameterEvent.builder()
                .parameterExecutionContext((SutParameterExecutionContext) executionContext)
                .build();
        super.getEventPublisher().publishEvent(validateParameterEvent);
    }

    @Override
    protected void processParameterResults(AbstractParameterExecutionContext executionContext) {
        SutParameterExecutionContext sutParameterExecutionContext = (SutParameterExecutionContext) executionContext;
        super.processParameterResults(sutParameterExecutionContext);
        if (sutParameterExecutionContext.getParameter().isSynchronousLoading()) {
            startGettingInfoForTabsUnderPage(executionContext.getSessionId(),
                    sutParameterExecutionContext.getParameter().getPage(),
                    sutParameterExecutionContext.getSessionConfiguration().getOnlyForPreconfiguredParams(),
                    sutParameterExecutionContext.getCountOfUnprocessedPagesUnderSession(),
                    sutParameterExecutionContext.getCountOfUnprocessedSynchronousParametersUnderPage());
            log.info("[Session - {}] load and process synchronous parameter: {} was finished successfully. "
                            + "Event for started getting info for Tabs under page {} was published.",
                    executionContext.getSessionId(), sutParameterExecutionContext.getParameter().getPath(),
                    sutParameterExecutionContext.getParameter().getPage());
        } else {
            startTabValidation(sutParameterExecutionContext);
            log.info("[Session - {}] load and process parameter: {} was finished successfully. "
                            + "Event for validation for Tab {} was published.",
                    executionContext.getSessionId(), sutParameterExecutionContext.getParameter().getPath(),
                    sutParameterExecutionContext.getParameter().getTab());
        }
    }

    private void startGettingInfoForTabsUnderPage(UUID sessionId,
                                                  String pageName,
                                                  boolean onlyForPreconfiguredParams,
                                                  AtomicInteger countOfUnprocessedPagesUnderSession,
                                                  AtomicInteger countOfUnprocessedSynchronousParametersUnderPage) {
        GetInfoForTabsUnderPageEvent getInfoForTabsUnderPageEvent = GetInfoForTabsUnderPageEvent.builder()
                .sessionId(sessionId)
                .pageName(pageName)
                .onlyForPreconfiguredParams(onlyForPreconfiguredParams)
                .countOfUnprocessedPagesUnderSession(countOfUnprocessedPagesUnderSession)
                .countOfUnprocessedSynchronousParametersUnderPage(countOfUnprocessedSynchronousParametersUnderPage)
                .build();
        super.getEventPublisher().publishEvent(getInfoForTabsUnderPageEvent);
    }

    private void startTabValidation(SutParameterExecutionContext executionContext) {
        ValidateTabEvent validateTabEvent = ValidateTabEvent.builder()
                .sessionId(executionContext.getSessionId())
                .pageName(executionContext.getParameter().getPage())
                .tabId(executionContext.getParameter().getPotSessionTabEntity().getId())
                .tabName(executionContext.getParameter().getTab())
                .onlyForPreconfiguredParams(executionContext.getSessionConfiguration().getOnlyForPreconfiguredParams())
                .countOfUnprocessedParameters(executionContext.getCountOfUnprocessedParametersUnderTab())
                .countOfUnprocessedTabsUnderPage(executionContext.getCountOfUnprocessedTabsUnderPage())
                .countOfUnprocessedPagesUnderSession(executionContext.getCountOfUnprocessedPagesUnderSession())
                .build();
        super.getEventPublisher().publishEvent(validateTabEvent);
    }
}
