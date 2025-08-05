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

import org.qubership.atp.svp.model.events.GetInfoForCommonParameterEvent;
import org.qubership.atp.svp.model.events.GetInfoForSessionPagesEvent;
import org.qubership.atp.svp.model.events.ValidateCommonParameterEvent;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.CommonParameterExecutionContext;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommonParameterEventListener extends AbstractParameterEventListener {

    /**
     * Handler for {@link GetInfoForCommonParameterEvent}.
     * Starts process of getting info for common parameter.
     * <br>
     * Event is processed asynchronously on thread pool 'GettingInfoProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link ValidateCommonParameterEvent} if Common Parameter allows validation
     * (deferred result flag is false,
     * validation type of parameter is not {@link org.qubership.atp.svp.core.enums.ValidationType#NONE}
     * and parameter type is not JsonTable.)
     * <br>
     * - {@link GetInfoForSessionPagesEvent} if Common Parameter does not allow validation.
     */
    @Async("GettingInfoProcessExecutor")
    @EventListener(condition = "#getInfoForCommonParameterEvent.getParameterExecutionContext()"
            + ".sessionConfiguration.onlyForPreconfiguredParams == false")
    public void handleGetInfoForCommonParameterEvent(GetInfoForCommonParameterEvent getInfoForCommonParameterEvent) {
        startGettingInfoForParameter(getInfoForCommonParameterEvent.getParameterExecutionContext());
    }

    /**
     * Handler for {@link GetInfoForCommonParameterEvent}.
     * Starts process of getting info for common parameter
     * in case loading of preconfigured parameters only.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link ValidateCommonParameterEvent} if Common Parameter allows validation
     * (deferred result flag is false,
     * validation type of parameter is not {@link org.qubership.atp.svp.core.enums.ValidationType#NONE}
     * and parameter type is not JsonTable.)
     * <br>
     * - {@link GetInfoForSessionPagesEvent} if Common Parameter does not allow validation.
     */
    @EventListener(condition = "#getInfoForCommonParameterEvent.getParameterExecutionContext()"
            + ".sessionConfiguration.onlyForPreconfiguredParams == true")
    public void handleGetInfoForCommonParameterEventSynchronously(
            GetInfoForCommonParameterEvent getInfoForCommonParameterEvent) {
        startGettingInfoForParameter(getInfoForCommonParameterEvent.getParameterExecutionContext());
    }

    @Override
    protected void startGettingInfoForParameter(AbstractParameterExecutionContext executionContext) {
        log.info("[Session - {}] Started getting info for common parameter {}.", executionContext.getSessionId(),
                executionContext.getParameter().getName());
        super.startGettingInfoForParameter(executionContext);
    }

    /**
     * Handler for {@link ValidateCommonParameterEvent}.
     * Starts process of validation for common parameter.
     * <br>
     * Event is processed asynchronously on thread pool 'ValidationProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * Calculates ValidationStatus for common parameter and sent it to WebSocket topic "/parameter-results".
     * <br>
     * {@link GetInfoForSessionPagesEvent} was published to events chain.
     */
    @Async("ValidationProcessExecutor")
    @EventListener(condition = "#validateCommonParameterEvent.getParameterExecutionContext()"
            + ".sessionConfiguration.onlyForPreconfiguredParams == false")
    public void handleValidateCommonParameterEvent(ValidateCommonParameterEvent validateCommonParameterEvent) {
        CommonParameterExecutionContext executionContext = validateCommonParameterEvent.getParameterExecutionContext();
        log.info("[Session - {}] Started validation for common parameter {}.", executionContext.getSessionId(),
                executionContext.getParameter().getName());
        super.startParameterValidationProcess(validateCommonParameterEvent.getParameterExecutionContext());
    }

    /**
     * Handler for {@link ValidateCommonParameterEvent}.
     * Starts process of validation for common parameter
     * in case loading of preconfigured parameters only.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * Calculates ValidationStatus for common parameter and sent it to WebSocket topic "/parameter-results".
     * <br>
     * {@link GetInfoForSessionPagesEvent} was published to events chain.
     */
    @EventListener(condition = "#validateCommonParameterEvent.getParameterExecutionContext()"
            + ".sessionConfiguration.onlyForPreconfiguredParams == true")
    public void handleValidateCommonParameterEventSynchronously(
            ValidateCommonParameterEvent validateCommonParameterEvent) {
        CommonParameterExecutionContext executionContext = validateCommonParameterEvent.getParameterExecutionContext();
        log.info("[Session - {}] Started validation for common parameter {}.",
                executionContext.getSessionId(), executionContext.getParameter().getName());
        super.startParameterValidationProcess(validateCommonParameterEvent.getParameterExecutionContext());
    }

    @Override
    protected void startParameterValidation(AbstractParameterExecutionContext executionContext) {
        ValidateCommonParameterEvent validateParameterEvent = ValidateCommonParameterEvent.builder()
                .parameterExecutionContext((CommonParameterExecutionContext) executionContext)
                .build();
        super.getEventPublisher().publishEvent(validateParameterEvent);
    }

    @Override
    protected void processParameterResults(AbstractParameterExecutionContext executionContext) {
        super.processParameterResults(executionContext);
        startGettingInfoForSessionPages((CommonParameterExecutionContext) executionContext);
    }

    private void startGettingInfoForSessionPages(CommonParameterExecutionContext executionContext) {
        SessionExecutionConfiguration sessionConfiguration = executionContext.getSessionConfiguration();
        GetInfoForSessionPagesEvent getInfoForSessionPagesEvent = GetInfoForSessionPagesEvent.builder()
                .sessionId(executionContext.getSessionId())
                .countOfUnprocessedCommonParameters(executionContext.getCountOfUnprocessedCommonParameters())
                .onlyCommonParametersExecuted(sessionConfiguration.getOnlyCommonParametersExecuted())
                .onlyForPreconfiguredParams(executionContext.getSessionConfiguration().getOnlyForPreconfiguredParams())
                .build();
        super.getEventPublisher().publishEvent(getInfoForSessionPagesEvent);
    }
}
