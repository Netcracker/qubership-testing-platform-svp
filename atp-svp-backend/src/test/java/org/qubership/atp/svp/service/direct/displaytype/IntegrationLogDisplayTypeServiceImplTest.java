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

package org.qubership.atp.svp.service.direct.displaytype;

import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.environments.System;
import org.qubership.atp.svp.model.impl.BulkValidatorValidation;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.logcollector.SearchResult;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.LogCollectorValueObject;
import org.qubership.atp.svp.repo.impl.BulkValidatorRepository;
import org.qubership.atp.svp.repo.impl.LogCollectorRepository;
import org.qubership.atp.svp.service.direct.BulkValidatorValidationService;
import org.qubership.atp.svp.service.direct.DeferredSearchServiceImpl;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.qubership.atp.svp.service.direct.PotSessionServiceImpl;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = IntegrationLogDisplayTypeServiceImpl.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
public class IntegrationLogDisplayTypeServiceImplTest extends TestWithTestData {

    @SpyBean
    IntegrationLogDisplayTypeServiceImpl integrationLogDisplayTypeService;
    @SpyBean
    ExecutionVariablesServiceImpl executionVariablesService;

    @MockBean
    DeferredSearchServiceImpl deferredSearchService;
    @MockBean
    LogCollectorRepository logCollectorRepository;
    @MockBean
    BulkValidatorRepository bulkValidatorRepository;
    @MockBean
    PotSessionServiceImpl potSessionServiceImpl;
    @MockBean
    BulkValidatorValidationService bulkValidatorValidationService;

    @Mock
    Environment environment;
    @Mock
    System system;
    @Mock
    Server server;
    @Mock
    SutParameterExecutionContext parameterExecutionContext;
    @Mock
    SessionExecutionConfiguration sessionExecutionConfiguration;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(environment.getSystem(Mockito.anyString())).thenReturn(system);
        Mockito.when(system.getServer(Mockito.anyString())).thenReturn(server);
        Mockito.when(parameterExecutionContext.getSessionConfiguration()).thenReturn(sessionExecutionConfiguration);
        Mockito.when(sessionExecutionConfiguration.getEnvironment()).thenReturn(environment);
        Mockito.when(parameterExecutionContext.getExecutionVariables()).thenReturn(new ConcurrentHashMap<>());
    }

    /* GETTING VALUE PROCESS */

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_errorWhileGettingData_throwsGettingValueException() throws GettingValueException {
        Source source = new Source("", "", EngineType.LOG_COLLECTOR, "", new HashSet<>());
        Mockito.doThrow(new GettingValueException("Error while getting data!")).when(integrationLogDisplayTypeService)
                .getLogCollectorValueObject(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito
                        .any());

        integrationLogDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test()
    public void getValueFromSource_logCollectorDataWithErrors_returnsLogCollectorValueObject()
            throws GettingValueException {
        Source source = new Source("", "", EngineType.LOG_COLLECTOR, "", new HashSet<>());

        LogCollectorValueObject logCollectorValueObject = generateLogCollectorValueObjectWithErrorsOnEachLevels();
        LogCollectorValueObject expectedResult = generateLogCollectorValueObjectWithErrorsOnEachLevels();

        Mockito.doReturn(logCollectorValueObject).when(integrationLogDisplayTypeService)
                .getLogCollectorValueObject(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito
                        .any());
        AbstractValueObject actualResult = integrationLogDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof LogCollectorValueObject);
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test()
    public void getValueFromSource_logCollectorDataWithoutErrors_returnsLogCollectorValueObject()
            throws GettingValueException {
        Source source = new Source("", "", EngineType.LOG_COLLECTOR, "", new HashSet<>());

        SearchResult searchResult = new SearchResult();
        searchResult.setComponentSearchResults(Collections.emptyList());
        LogCollectorValueObject expectedResult = new LogCollectorValueObject(searchResult);

        Mockito.doReturn(expectedResult).when(integrationLogDisplayTypeService)
                .getLogCollectorValueObject(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        AbstractValueObject actualResult = integrationLogDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof LogCollectorValueObject);
        Assert.assertEquals(expectedResult, actualResult);
    }

    /* VALIDATION PROCESS */
}
