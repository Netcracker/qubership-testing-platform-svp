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

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.model.pot.validation.ValidationInfo;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.tests.DbMockEntity;

public class DefaultDisplayTypeServiceTest {

    @Spy
    DefaultDisplayTypeService defaultDisplayTypeService;

    @Mock
    SutParameterExecutionContext sutParameterExecutionContext;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void validateParameter_parameterHasNoIncorrectActualResults_setsPassedStatus() throws ValidationException, IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterEntity();
        parameter.setArValues(Arrays.asList(new SimpleValueObject("correctValue"),
                new SimpleValueObject("correctValue"),
                new SimpleValueObject("correctValue")));
        parameter.setEr(new SimpleValueObject("correctValue"));
        parameter.setValidationInfo(new ValidationInfo());
        Mockito.when(sutParameterExecutionContext.getParameter()).thenReturn(parameter);

        defaultDisplayTypeService.validateParameter(sutParameterExecutionContext);

        Assert.assertEquals(parameter.getValidationInfo().getStatus(), ValidationStatus.PASSED);
    }

    @Test
    public void validateParameter_parameterHasOneIncorrectActualResult_setsFailedStatus() throws ValidationException,
            IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterEntity();
        parameter.setArValues(Arrays.asList(new SimpleValueObject("correctValue"),
                new SimpleValueObject("incorrectValue"),
                new SimpleValueObject("correctValue")));
        parameter.setEr(new SimpleValueObject("correctValue"));
        parameter.setValidationInfo(new ValidationInfo());
        Mockito.when(sutParameterExecutionContext.getParameter()).thenReturn(parameter);

        defaultDisplayTypeService.validateParameter(sutParameterExecutionContext);

        Assert.assertEquals(parameter.getValidationInfo().getStatus(), ValidationStatus.FAILED);
    }

    @Test
    public void validateParameter_parameterHasMultipleIncorrectActualResults_setsFailedStatusAndWillBeCalledSetMethodOnce() throws ValidationException, IOException {
        PotSessionParameterEntity parameter = Mockito.spy(new PotSessionParameterEntity());
        parameter.setArValues(Arrays.asList(new SimpleValueObject("correctValue"),
                new SimpleValueObject("incorrectValue"),
                new SimpleValueObject("incorrectValue")));
        parameter.setEr(new SimpleValueObject("correctValue"));
        parameter.setValidationInfo(new ValidationInfo());
        Mockito.when(sutParameterExecutionContext.getParameter()).thenReturn(parameter);

        defaultDisplayTypeService.validateParameter(sutParameterExecutionContext);

        Mockito.verify(parameter).setValidationStatus(ValidationStatus.FAILED);
        Assert.assertEquals(parameter.getValidationInfo().getStatus(), ValidationStatus.FAILED);
    }
}
