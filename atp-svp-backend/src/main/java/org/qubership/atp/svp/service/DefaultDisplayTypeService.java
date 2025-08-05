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

package org.qubership.atp.svp.service;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;

public abstract class DefaultDisplayTypeService extends DefaultValidationDisplayTypeService
        implements DisplayTypeService {

    /**
     * Runs validations for parameter value ans sets validation status.
     */
    public void validateParameter(AbstractParameterExecutionContext context)
            throws ValidationException {
        PotSessionParameterEntity parameter = context.getParameter();
        parameter.setValidationStatus(ValidationStatus.PASSED);
        for (AbstractValueObject arValue : parameter.getArValues()) {
            if (!parameter.getEr().equals(arValue)) {
                parameter.setValidationStatus(ValidationStatus.FAILED);
                break;
            }
        }
    }
}
