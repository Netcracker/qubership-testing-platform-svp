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

package org.qubership.atp.svp.model.api.tsg;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PreconfiguredValidationTest {

    // TODO implement and fix this

    @Mock
    Component component;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Ignore
    public void addResultToComponent_componentDidNotExist_componentWithParameterResultWasAdded() {
        PreconfiguredValidation preconfiguredValidation = new PreconfiguredValidation();

        preconfiguredValidation.addResultToComponent("parameterName", null,
                null, null, "componentName");

        Mockito.verify(component).addParameterResult(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Assert.assertTrue(preconfiguredValidation.getComponents().contains(component));
    }

    @Test
    public void addResultToComponent_componentAlreadyExists_parameterResultWasAddedToExistsComponent() {

    }
}
