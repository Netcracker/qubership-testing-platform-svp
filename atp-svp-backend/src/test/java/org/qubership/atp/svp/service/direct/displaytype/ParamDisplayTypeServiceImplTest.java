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

import java.util.Arrays;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.core.exceptions.ConnectionDbException;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.SqlScriptExecuteException;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.environments.System;
import org.qubership.atp.svp.model.impl.LogCollectorSettings;
import org.qubership.atp.svp.model.impl.LogCollectorSettingsParameters;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.repo.impl.CassandraRepository;
import org.qubership.atp.svp.repo.impl.LogCollectorRepository;
import org.qubership.atp.svp.repo.impl.SqlRepository;
import org.qubership.atp.svp.service.direct.DeferredSearchServiceImpl;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = ParamDisplayTypeServiceImpl.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
@PrepareForTest(SqlRepository.class)
public class ParamDisplayTypeServiceImplTest extends TestWithTestData {

    @SpyBean
    ParamDisplayTypeServiceImpl paramDisplayTypeService;
    @SpyBean
    ExecutionVariablesServiceImpl executionVariablesService;

    @MockBean
    DeferredSearchServiceImpl deferredSearchService;
    @MockBean
    LogCollectorRepository logCollectorRepository;

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
    @MockBean
    CassandraRepository cassandraRepository;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(SqlRepository.class);
        Mockito.when(environment.getSystem(Mockito.anyString())).thenReturn(system);
        Mockito.when(system.getServer(Mockito.anyString())).thenReturn(server);
        Mockito.when(parameterExecutionContext.getSessionConfiguration()).thenReturn(sessionExecutionConfiguration);
        Mockito.when(sessionExecutionConfiguration.getEnvironment()).thenReturn(environment);
        Mockito.when(parameterExecutionContext.getExecutionVariables()).thenReturn(new ConcurrentHashMap<>());
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_errorWhileGettingDataEngineTypeSQL_throwsGettingValueException() throws GettingValueException {
        Source source = new Source("", "", EngineType.SQL, "", new HashSet<>());
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), Mockito.anyString()))
                .thenThrow(new RuntimeException("Error while getting data!"));

        paramDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_errorWhileGettingDataEngineTypeCassandra_throwsGettingValueException() throws GettingValueException {
        Source source = new Source("", "", EngineType.CASSANDRA, "", new HashSet<>());
        Mockito.when(cassandraRepository.executeQueryAndGetFirstValue(Mockito.any(), Mockito.anyString()))
                .thenThrow(new RuntimeException("Error while getting data!"));

        paramDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test()
    public void getValueFromSource_tableDataAndEngineTypeSQL_returnsSimpleValueObjectWithFirstCellData() throws GettingValueException {
        Source source = new Source("", "", EngineType.SQL, "", new HashSet<>());
        String erValue = "erValue";
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), Mockito.anyString()))
                .thenReturn(erValue);

        AbstractValueObject actualResult = paramDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(erValue, ((SimpleValueObject) actualResult).getValue());
    }

    @Test()
    public void getValueFromSource_tableDataAndEngineTypeCassandra_returnsSimpleValueObjectWithFirstCellData() throws GettingValueException {
        Source source = new Source("", "", EngineType.CASSANDRA, "", new HashSet<>());
        String erValue = "erValue";
        Mockito.when(cassandraRepository.executeQueryAndGetFirstValue(Mockito.any(), Mockito.anyString()))
                .thenReturn(erValue);

        AbstractValueObject actualResult = paramDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(erValue, ((SimpleValueObject) actualResult).getValue());
    }

    @Test()
    public void getValueFromSource_logCollectorData_returnsSimpleValueObjectWithFirstLogCollectorSearchResult()
            throws GettingValueException {
        // Preparing LogCollectorSettings
        LogCollectorSettingsParameters lcSettingsParameters = new LogCollectorSettingsParameters();
        lcSettingsParameters.setPreviewParameters(Arrays.asList("operation_name", "payload", "integration_scenario"));
        LogCollectorSettings lcSettings = new LogCollectorSettings(Collections.emptyList(), lcSettingsParameters);
        Source source = new Source("", "", EngineType.LOG_COLLECTOR,
                "", Collections.singleton(lcSettings));

        Mockito.doReturn(generateLogCollectorValueObject()).when(paramDisplayTypeService)
                .getLogCollectorValueObject(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        String erValue = "logMessage1\nlogMessage2\nlogMessage3\n";

        AbstractValueObject actualResult =
                paramDisplayTypeService.getValueFromSource(source, parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(erValue, ((SimpleValueObject) actualResult).getValue());
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_errorWhileGettingDataEngineTypeSQL_throwsConnectionDbException() throws GettingValueException {
        Source source = new Source("", "", EngineType.SQL, "", new HashSet<>());
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), Mockito.anyString()))
                .thenThrow(new ConnectionDbException("Error connect to DB"));

        paramDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_errorWhileGettingDataEngineTypeSQL_throwsSqlScriptExecuteException() throws GettingValueException {
        Source source = new Source("", "", EngineType.SQL, "", new HashSet<>());
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), Mockito.anyString()))
                .thenThrow(new SqlScriptExecuteException("Error execute query"));

        paramDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

}
