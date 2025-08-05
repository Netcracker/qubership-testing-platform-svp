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

package org.qubership.atp.svp.service.direct.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionTabEntity;
import org.qubership.atp.svp.model.pot.validation.ValidationInfo;
import org.qubership.atp.svp.service.direct.ValidationServiceImpl;

@RunWith(Parameterized.class)
public class ValidationServiceImplTabValidationParametrizedTest {

    private final ValidationServiceImpl validationService;

    private final ValidationStatus expectedValidationStatus;
    private final PotSessionTabEntity tab;

    public ValidationServiceImplTabValidationParametrizedTest(ValidationStatus validationStatus1,
                                                              ValidationStatus validationStatus2,
                                                              ValidationStatus validationStatus3,
                                                              ValidationStatus expectedValidationStatus) {
        validationService = new ValidationServiceImpl();
        this.expectedValidationStatus = expectedValidationStatus;
        PotSessionParameterEntity parameter1 = new PotSessionParameterEntity();
        parameter1.setValidationInfo(new ValidationInfo());
        parameter1.setValidationStatus(validationStatus1);

        PotSessionParameterEntity parameter2 = new PotSessionParameterEntity();
        parameter2.setValidationInfo(new ValidationInfo());
        parameter2.setValidationStatus(validationStatus2);

        PotSessionParameterEntity parameter3 = new PotSessionParameterEntity();
        parameter3.setValidationInfo(new ValidationInfo());
        parameter3.setValidationStatus(validationStatus3);

        List<PotSessionParameterEntity> parameters = Arrays.asList(parameter1, parameter2, parameter3);
        PotSessionTabEntity tab = new PotSessionTabEntity();
        tab.setPotSessionParameterEntities(parameters);
        this.tab = tab;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> pageValidationStatusByParameterValidationStatuses() {
        // The first three statuses are the actual status of the parameters,
        // the fourth status is the expected status of the page
        return Arrays.asList(new ValidationStatus[][]{
                // Result page status - NONE
                // If all parameter statuses are NONE, then the page status is also NONE
                {ValidationStatus.NONE, ValidationStatus.NONE, ValidationStatus.NONE, ValidationStatus.NONE},
                {ValidationStatus.MANUAL, ValidationStatus.MANUAL, ValidationStatus.MANUAL, ValidationStatus.NONE},
                {ValidationStatus.MANUAL, ValidationStatus.NONE, ValidationStatus.MANUAL, ValidationStatus.NONE},
                // Result page status - PASSED
                {ValidationStatus.PASSED, ValidationStatus.PASSED, ValidationStatus.PASSED, ValidationStatus.PASSED},
                {ValidationStatus.PASSED, ValidationStatus.NONE, ValidationStatus.PASSED, ValidationStatus.PASSED},
                {ValidationStatus.PASSED, ValidationStatus.MANUAL, ValidationStatus.PASSED, ValidationStatus.PASSED},
                // Result page status - WARNING
                {ValidationStatus.WARNING, ValidationStatus.WARNING, ValidationStatus.WARNING,
                        ValidationStatus.WARNING},
                {ValidationStatus.WARNING, ValidationStatus.NONE, ValidationStatus.WARNING, ValidationStatus.WARNING},
                {ValidationStatus.WARNING, ValidationStatus.PASSED, ValidationStatus.WARNING, ValidationStatus.WARNING},
                {ValidationStatus.WARNING, ValidationStatus.MANUAL, ValidationStatus.WARNING, ValidationStatus.WARNING},
                {ValidationStatus.WARNING, ValidationStatus.PASSED, ValidationStatus.MANUAL, ValidationStatus.WARNING},
                {ValidationStatus.WARNING, ValidationStatus.IN_PROGRESS, ValidationStatus.PASSED,
                        ValidationStatus.WARNING},
                {ValidationStatus.IN_PROGRESS, ValidationStatus.IN_PROGRESS, ValidationStatus.PASSED,
                        ValidationStatus.WARNING},
                {ValidationStatus.IN_PROGRESS, ValidationStatus.IN_PROGRESS, ValidationStatus.IN_PROGRESS,
                        ValidationStatus.WARNING},
                // Result page status - FAILED
                {ValidationStatus.FAILED, ValidationStatus.FAILED, ValidationStatus.FAILED, ValidationStatus.FAILED},
                {ValidationStatus.FAILED, ValidationStatus.NONE, ValidationStatus.FAILED, ValidationStatus.FAILED},
                {ValidationStatus.FAILED, ValidationStatus.PASSED, ValidationStatus.FAILED, ValidationStatus.FAILED},
                {ValidationStatus.FAILED, ValidationStatus.WARNING, ValidationStatus.FAILED, ValidationStatus.FAILED},
                {ValidationStatus.FAILED, ValidationStatus.MANUAL, ValidationStatus.FAILED, ValidationStatus.FAILED},
                {ValidationStatus.FAILED, ValidationStatus.IN_PROGRESS, ValidationStatus.IN_PROGRESS,
                        ValidationStatus.FAILED},
                {ValidationStatus.FAILED, ValidationStatus.PASSED, ValidationStatus.IN_PROGRESS,
                        ValidationStatus.FAILED}
        });
    }

    @Test
    public void calculateValidationStatus_differentStatuses_pageHasCorrectStatus() {
        ValidationStatus actualValidationStatus = validationService.calculateStatusForTab(tab);

        Assert.assertEquals(expectedValidationStatus, actualValidationStatus);
    }
}
