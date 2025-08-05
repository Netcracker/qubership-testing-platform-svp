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

package org.qubership.atp.svp.model.pot;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class SutParameterExecutionContext extends AbstractParameterExecutionContext {

    @NonNull
    private AtomicInteger countOfUnprocessedSynchronousParametersUnderPage;
    @NonNull
    private AtomicInteger countOfUnprocessedParametersUnderTab;
    @NonNull
    private AtomicInteger countOfUnprocessedTabsUnderPage;
    @NonNull
    private AtomicInteger countOfUnprocessedPagesUnderSession;

    /**
     * Fill object sut parameter execution context.
     *
     * @param sessionId session id from {@link PotSessionEntity#getSessionId()} ()}
     * @param parameterStarted parameter started date and time
     * @param sessionConfiguration session execution configuration
     *         from {@link PotSessionEntity#getExecutionConfiguration()}
     * @param executionVariables execution variable stored in {@link PotSessionEntity#getExecutionVariables()}
     * @param parameter {@link PotSessionParameter}
     * @param isDeferredSearchResult flag that checks the parameter for postpone the results
     * @param countOfUnprocessedSynchronousParametersUnderPage count of unprocessed
     *         synchronous loading Parameters under Page.
     * @param countOfUnprocessedParametersUnderTab count of unprocessed SUT Parameters under Tab.
     * @param countOfUnprocessedTabsUnderPage count of unprocessed Tabs under Page.
     * @param countOfUnprocessedPagesUnderSession count of unprocessed Pages under PotSession.
     * @param responseSearchId search id obtained from the response to the start search method or from the Kafka key.
     */
    @Builder
    public SutParameterExecutionContext(@NonNull UUID sessionId,
                                        @NonNull OffsetDateTime parameterStarted,
                                        @NonNull SessionExecutionConfiguration sessionConfiguration,
                                        @NonNull ConcurrentHashMap<String, ExecutionVariable> executionVariables,
                                        @NonNull PotSessionParameterEntity parameter,
                                        boolean isDeferredSearchResult,
                                        @NonNull AtomicInteger countOfUnprocessedSynchronousParametersUnderPage,
                                        @NonNull AtomicInteger countOfUnprocessedParametersUnderTab,
                                        @NonNull AtomicInteger countOfUnprocessedTabsUnderPage,
                                        @NonNull AtomicInteger countOfUnprocessedPagesUnderSession,
                                        @Nullable UUID responseSearchId) {
        super(sessionId, parameterStarted, sessionConfiguration, executionVariables,
                parameter, isDeferredSearchResult, responseSearchId);
        this.countOfUnprocessedSynchronousParametersUnderPage = countOfUnprocessedSynchronousParametersUnderPage;
        this.countOfUnprocessedParametersUnderTab = countOfUnprocessedParametersUnderTab;
        this.countOfUnprocessedTabsUnderPage = countOfUnprocessedTabsUnderPage;
        this.countOfUnprocessedPagesUnderSession = countOfUnprocessedPagesUnderSession;
    }

    @Override
    public void decrementCountOfUnprocessedParameters() {
        if (super.getParameter().isSynchronousLoading()) {
            countOfUnprocessedSynchronousParametersUnderPage.decrementAndGet();
        } else {
            countOfUnprocessedParametersUnderTab.decrementAndGet();
        }
    }

    /**
     * Fill execution variables with SUT parameter result which has
     * {@link SimpleValueObject}, has not contains errors
     * and has synchronous loading flag true.
     * <br>
     * Add {@link ExecutionVariable} with next format:
     * "group_name.parameter_name" as variable name
     * and first actual result value as variable value
     * to {@link SutParameterExecutionContext#getExecutionVariables()}
     */
    @Override
    public void setParameterResultAsVariable(ExecutionVariablesServiceImpl executionVariablesService) {
        Function<PotSessionParameterEntity, String> getSutParameterVariableName = parameter ->
                parameter.getGroup() + "." + parameter.getName();
        if (!super.getParameter().hasErrors() && super.getParameter().isSynchronousLoading()) {
            Optional<ExecutionVariable> sutParameterAsVariable = executionVariablesService
                    .getVariableFromParameter(getSutParameterVariableName, getParameter());
            sutParameterAsVariable.ifPresent(variable -> executionVariablesService.addExecutionVariables(variable,
                    super.getExecutionVariables()));
        }
    }

    @Override
    public String toString() {
        return "SutParameterExecutionContext{"
                + "sessionId=" + super.getSessionId()
                + ", parameterStarted=" + super.getParameterStarted()
                + ", sessionConfiguration=" + super.getSessionConfiguration()
                + ", executionVariables=" + super.getExecutionVariables()
                + ", parameter=" + super.getParameter().getPath()
                + ", isDeferredSearchResult=" + super.isDeferredSearchResult()
                + ", countOfUnprocessedSynchronousParametersUnderPage="
                + countOfUnprocessedSynchronousParametersUnderPage
                + ", countOfUnprocessedParametersUnderTab=" + countOfUnprocessedParametersUnderTab
                + ", countOfUnprocessedTabsUnderPage=" + countOfUnprocessedTabsUnderPage
                + ", countOfUnprocessedPagesUnderSession=" + countOfUnprocessedPagesUnderSession
                + ", responseSearchId=" + super.getResponseSearchId()
                + '}';
    }
}
