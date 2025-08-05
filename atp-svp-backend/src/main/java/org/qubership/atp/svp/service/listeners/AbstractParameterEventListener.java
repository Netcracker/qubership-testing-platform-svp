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


import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.service.PotSessionParameterService;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public abstract class AbstractParameterEventListener {

    private PotSessionParameterService potSessionParameterService;
    private ApplicationEventPublisher eventPublisher;
    private ExecutionVariablesServiceImpl executionVariablesService;

    @Autowired
    public final void setPotSessionParameterService(PotSessionParameterService potSessionParameterService) {
        this.potSessionParameterService = potSessionParameterService;
    }

    @Autowired
    public final void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Autowired
    public final void setExecutionVariablesServiceImpl(ExecutionVariablesServiceImpl executionVariablesService) {
        this.executionVariablesService = executionVariablesService;
    }

    protected ApplicationEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    protected PotSessionParameterService getPotSessionParameterService() {
        return potSessionParameterService;
    }

    protected void startGettingInfoForParameter(AbstractParameterExecutionContext executionContext) {
        try {
            potSessionParameterService.startGettingInfoForParameter(executionContext);
            if (!executionContext.isDeferredSearchResult()) {
                validateOrProcessParameterResults(executionContext);
            }
        } catch (Exception e) {
            log.error("Unexpected error occurred during the getting info for parameter: "
                    + executionContext.getParameter().getPath() + " under session: "
                    + executionContext.getSessionId() + "!", e);
            executionContext.decrementCountOfUnprocessedParameters();
        }
    }

    private void validateOrProcessParameterResults(AbstractParameterExecutionContext executionContext) {
        if (executionContext.allowsParameterValidation()) {
            startParameterValidation(executionContext);
            log.info("[Session - {}] Event of validation process for parameter {} was published.",
                    executionContext.getSessionId(),
                    executionContext.getParameter().getPath());
        } else {
            processParameterResults(executionContext);
        }
    }

    protected abstract void startParameterValidation(AbstractParameterExecutionContext executionContext);

    protected void processParameterResults(AbstractParameterExecutionContext executionContext) {
        executionContext.setNoneValidationStatusInsteadInProgressForParameter();
        potSessionParameterService.updatePotSessionParameter(executionContext);
        potSessionParameterService.sendParameterResultToSession(executionContext);
        executionContext.setParameterResultAsVariable(executionVariablesService);
        potSessionParameterService.addVariable(executionContext);
        executionContext.decrementCountOfUnprocessedParameters();
    }

    protected void startParameterValidationProcess(AbstractParameterExecutionContext executionContext) {
        try {
            log.info("[Session - {}] Validation process for parameter: {} was started",
                    executionContext.getSessionId(), executionContext.getParameter().getPath());
            potSessionParameterService.validateParameter(executionContext);
            processParameterResults(executionContext);
            log.info("[Session - {}] Validation process for parameter: {} was finished successfully. "
                            + "Event for validate tab was published (only for SUT parameters).",
                    executionContext.getSessionId(), executionContext.getParameter().getPath());
        } catch (Exception e) {
            log.error("Unexpected error occurred during the validation process for parameter: "
                            + executionContext.getParameter().getPath()
                            + " under session: " + executionContext.getSessionId() + "!", e);
        }
    }
}
