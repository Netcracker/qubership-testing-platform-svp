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
public class CommonParameterExecutionContext extends AbstractParameterExecutionContext {

    @NonNull
    private AtomicInteger countOfUnprocessedCommonParameters;

    /**
     * Fill object common parameter execution context.
     *
     * @param sessionId session id from {@link PotSessionEntity#getSessionId()}.
     * @param parameterStarted session started date and time.
     * @param sessionConfiguration session execution configuration from
     *      {@link PotSessionEntity#getExecutionConfiguration()}.
     * @param executionVariables execution variable stored in {@link PotSessionEntity#getExecutionVariables()}.
     * @param parameter session parameter {@link PotSessionParameterEntity}.
     * @param isDeferredSearchResult flag that checks the parameter for postpone the results.
     * @param countOfUnprocessedCommonParameters count of unprocessed Common Parameters under PotSession.
     * @param responseSearchId search id obtained from the response to the start search method or from the Kafka key.
     */
    @Builder
    public CommonParameterExecutionContext(@NonNull UUID sessionId,
                                           @NonNull OffsetDateTime parameterStarted,
                                           @NonNull SessionExecutionConfiguration sessionConfiguration,
                                           @NonNull ConcurrentHashMap<String, ExecutionVariable> executionVariables,
                                           @NonNull PotSessionParameterEntity parameter,
                                           boolean isDeferredSearchResult,
                                           @NonNull AtomicInteger countOfUnprocessedCommonParameters,
                                           @Nullable UUID responseSearchId) {
        super(sessionId, parameterStarted, sessionConfiguration, executionVariables,
                parameter, isDeferredSearchResult, responseSearchId);
        this.countOfUnprocessedCommonParameters = countOfUnprocessedCommonParameters;
    }

    @Override
    public void decrementCountOfUnprocessedParameters() {
        countOfUnprocessedCommonParameters.decrementAndGet();
    }

    /**
     * Fill execution variables with common parameter result which has
     * {@link SimpleValueObject} and has not contains errors.
     * <br>
     * Add {@link ExecutionVariable} with name of common parameters as variable name
     * and value of common parameters as variable value
     * to {@link CommonParameterExecutionContext#getExecutionVariables()}
     */
    @Override
    public void setParameterResultAsVariable(ExecutionVariablesServiceImpl executionVariablesService) {
        Function<PotSessionParameterEntity, String> getCommonParameterVariableName = PotSessionParameterEntity::getName;
        if (!super.getParameter().hasErrors()) {
            Optional<ExecutionVariable> sutParameterAsVariable = executionVariablesService
                    .getVariableFromParameter(getCommonParameterVariableName, getParameter());
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
                + ", countOfUnprocessedCommonParameters=" + countOfUnprocessedCommonParameters
                + ", responseSearchId=" + super.getResponseSearchId()
                + '}';
    }
}
