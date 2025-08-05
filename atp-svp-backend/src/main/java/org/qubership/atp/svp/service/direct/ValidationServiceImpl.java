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

package org.qubership.atp.svp.service.direct;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionTabEntity;
import org.qubership.atp.svp.service.ValidationService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ValidationServiceImpl implements ValidationService {

    @Override
    public ValidationStatus calculateStatusForTab(PotSessionTabEntity tab) {
        Set<ValidationStatus> impactingStatuses =
                getImpactingValidationStatusOfParameters(tab.getPotSessionParameterEntities());
        return impactingStatuses.isEmpty()
                ? ValidationStatus.NONE
                : calculateResultImpactingValidationStatus(impactingStatuses);
    }

    private Set<ValidationStatus> getImpactingValidationStatusOfParameters(List<PotSessionParameterEntity> parameters) {
        return parameters.stream()
                .filter(parameter -> {
                    ValidationStatus status = parameter.getValidationInfo().getStatus();
                    return !ValidationStatus.getStatusesNotImpactingCalculation().contains(status);
                })
                .map(parameter -> parameter.getValidationInfo().getStatus())
                .collect(Collectors.toSet());
    }

    @Override
    public ValidationStatus calculateStatusForPage(Set<ValidationStatus> tabStatuses) {
        return tabStatuses.isEmpty()
                ? ValidationStatus.NONE
                : calculateResultImpactingValidationStatus(tabStatuses);
    }

    @Override
    public ValidationStatus calculateStatusForSession(Set<ValidationStatus> pageStatuses) {
        return pageStatuses.isEmpty()
                ? ValidationStatus.PASSED
                : ValidationStatus.FAILED;
    }

    private ValidationStatus calculateResultImpactingValidationStatus(Set<ValidationStatus> impactingStatuses) {
        ValidationStatus statusResult;
        if (impactingStatuses.contains(ValidationStatus.FAILED)) {
            statusResult = ValidationStatus.FAILED;
        } else if (impactingStatuses.contains(ValidationStatus.WARNING)
                || impactingStatuses.contains(ValidationStatus.LC_WARNING)
                || impactingStatuses.contains(ValidationStatus.IN_PROGRESS)) {
            statusResult = ValidationStatus.WARNING;
        } else {
            statusResult = ValidationStatus.PASSED;
        }
        return statusResult;
    }
}
