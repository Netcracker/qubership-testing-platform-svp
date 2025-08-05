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
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.service.direct.ValidationServiceImpl;

@RunWith(Parameterized.class)
public class ValidationServiceImplPageValidationParametrizedTest {
    private final ValidationServiceImpl validationService;

    private final ValidationStatus expectedValidationStatus;
    private final Set<ValidationStatus> mock = new HashSet<>();

    public ValidationServiceImplPageValidationParametrizedTest(ValidationStatus validationStatus1,
                                                               ValidationStatus validationStatus2,
                                                               ValidationStatus validationStatus3,
                                                               ValidationStatus expectedValidationStatus) {
        validationService = new ValidationServiceImpl();
        this.expectedValidationStatus = expectedValidationStatus;
        mock.add(validationStatus1);
        mock.add(validationStatus2);
        mock.add(validationStatus3);

    }

    @Parameterized.Parameters
    public static Collection<Object[]> pageValidationStatusByParameterValidationStatuses() {
        // The first three statuses are the actual status of the parameters,
        // the fourth status is the expected status of the page
        return Arrays.asList(new ValidationStatus[][]{
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
        ValidationStatus actualValidationStatus = validationService.calculateStatusForPage(mock);

        Assert.assertEquals(expectedValidationStatus, actualValidationStatus);
    }
}
