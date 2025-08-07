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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@AllArgsConstructor
public abstract class AbstractParameterExecutionContext {

    @NonNull
    private UUID sessionId;
    @NonNull
    private OffsetDateTime parameterStarted;
    @NonNull
    private SessionExecutionConfiguration sessionConfiguration;
    @NonNull
    private ConcurrentHashMap<String, ExecutionVariable> executionVariables;
    @NonNull
    private PotSessionParameterEntity parameter;
    @Setter
    private boolean isDeferredSearchResult;
    @Setter
    @Nullable
    private UUID responseSearchId;

    /**
     * Get map of Execution Variables.
     * @return map of Execution Variables
     */
    public ConcurrentHashMap<String, ExecutionVariable> getExecutionVariables() {
        return executionVariables;
    }

    public abstract void decrementCountOfUnprocessedParameters();

    public abstract void setParameterResultAsVariable(ExecutionVariablesServiceImpl executionVariablesService);

    public boolean allowsParameterValidation() {
        return parameter.hasInProgressStatus() && parameter.getParameterConfig().allowsValidation();
    }

    /**
     * Set validation status {@link ValidationStatus#NONE} to in progress parameters.
     * This method invoked in case no-validations for parameter.
     */
    public void setNoneValidationStatusInsteadInProgressForParameter() {
        if (parameter.hasInProgressStatus()) {
            parameter.setValidationStatus(ValidationStatus.NONE);
        }
    }
}
