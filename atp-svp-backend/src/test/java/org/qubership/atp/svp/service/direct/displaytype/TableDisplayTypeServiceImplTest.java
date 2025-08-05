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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.enums.ValidationType;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.model.bulkvalidator.ComparingProcessResponse;
import org.qubership.atp.svp.model.bulkvalidator.TestRunCreationResponse;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.environments.System;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.impl.TableSettings;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.model.pot.validation.ActualTablesValidationInfo;
import org.qubership.atp.svp.model.pot.validation.TableValidationInfo;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.Table;
import org.qubership.atp.svp.repo.feign.BulkValidatorApiFeignClient;
import org.qubership.atp.svp.repo.feign.BulkValidatorPublicFeignClient;
import org.qubership.atp.svp.repo.feign.BulkValidatorValidatorFeignClient;
import org.qubership.atp.svp.repo.impl.BulkValidatorRepository;
import org.qubership.atp.svp.repo.impl.CassandraRepository;
import org.qubership.atp.svp.repo.impl.SqlRepository;
import org.qubership.atp.svp.service.direct.BulkValidatorValidationService;
import org.qubership.atp.svp.service.direct.CompareTablesService;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.qubership.atp.svp.tests.DbMockEntity;
import org.qubership.atp.svp.tests.TestWithTestData;
import org.qubership.atp.svp.utils.DtoConvertService;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = TableDisplayTypeServiceImpl.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
@PrepareForTest(SqlRepository.class)
public class TableDisplayTypeServiceImplTest extends TestWithTestData {

    @Autowired
    TableDisplayTypeServiceImpl tableDisplayTypeService;

    @SpyBean
    ExecutionVariablesServiceImpl executionVariablesService;

    @SpyBean
    CompareTablesService compareTablesService;

    @SpyBean
    BulkValidatorValidationService bulkValidatorValidationService;

    @SpyBean
    DtoConvertService dtoConvertService;
    @MockBean
    BulkValidatorApiFeignClient bulkValidatorApiFeignClient;
    @MockBean
    BulkValidatorValidatorFeignClient bulkValidatorValidatorFeignClient;
    @MockBean
    BulkValidatorPublicFeignClient bulkValidatorPublicFeignClient;
    @SpyBean
    BulkValidatorRepository bulkValidatorRepository;

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
        when(environment.getSystem(Mockito.anyString())).thenReturn(system);
        when(system.getServer(Mockito.anyString())).thenReturn(server);
        when(parameterExecutionContext.getSessionConfiguration()).thenReturn(sessionExecutionConfiguration);
        when(parameterExecutionContext.getExecutionVariables()).thenReturn(new ConcurrentHashMap<>());
        when(sessionExecutionConfiguration.getEnvironment()).thenReturn(environment);
    }

    /* GETTING VALUE PROCESS */

    // Negative test cases
    @Test(expected = GettingValueException.class)
    public void getValueFromSource_errorWhileGettingDataEngineTypeSQL_throwsGettingValueException() throws GettingValueException {
        Source source = new Source("", "", EngineType.SQL, "", new HashSet<>());
        when(SqlRepository.executeQuery(any(), Mockito.anyString()))
                .thenThrow(new RuntimeException("Error while getting data!"));

        tableDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_errorWhileGettingDataEngineTypeCassandra_throwsGettingValueException() throws GettingValueException {
        Source source = new Source("", "", EngineType.CASSANDRA, "", new HashSet<>());
        when(cassandraRepository.executeQuery(any(), Mockito.anyString()))
                .thenThrow(new RuntimeException("Error while getting data!"));

        tableDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test
    public void getValueFromSource_tableDataAndEngineTypeTypeNotSpecified_throwsGettingValueException() {
        String systemName = "Business Solution";
        Source source = new Source(systemName, "", EngineType.LOG_COLLECTOR,
                "", Collections.singleton(new TableSettings("")));
        try {
            tableDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
            Assert.fail("Switch not go in default case");
        } catch (GettingValueException ex) {
            Assert.assertEquals("Unexpected EngineType value: LOG_COLLECTOR for DisplayType: TABLE",
                    ex.getMessage());
        }
    }

    // Positive test cases
    @Test()
    public void getValueFromSource_tableDataAndKnownTableName_returnsTableValueObjectWithName()
            throws GettingValueException, IOException {

        String tableName = "tableName";
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(new TableSettings(tableName)));
        when(SqlRepository.executeQuery(any(), Mockito.anyString()))
                .thenReturn(objectMapper.readValue(loadFileToString(DisplayTypeTestConstants.TABLE_RESPONSE_FILE_PATH), Table.class));


        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.NONE, 1);
        parameter.getParameterConfig().setAdditionalSources(Collections.emptyList());
        Mockito.when(parameterExecutionContext.getParameter())
                .thenReturn(parameter);

        AbstractValueObject actualResult = tableDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof TableValueObject);
        Assert.assertEquals(tableName, ((TableValueObject) actualResult).getTable().getName());
    }

    @Test()
    public void getValueFromSource_tableDataAndEngineTypeCassandra_returnsTableValueObjectWithName()
            throws GettingValueException, IOException {
        String tableName = "tableName";
        Source source = new Source("", "", EngineType.CASSANDRA,
                "", Collections.singleton(new TableSettings(tableName)));
        when(cassandraRepository.executeQuery(any(), Mockito.anyString()))
                .thenReturn(objectMapper.readValue(loadFileToString(DisplayTypeTestConstants.TABLE_RESPONSE_FILE_PATH), Table.class));

        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.NONE, 1);
        parameter.getParameterConfig().setAdditionalSources(Collections.emptyList());
        Mockito.when(parameterExecutionContext.getParameter())
                .thenReturn(parameter);

        AbstractValueObject actualResult = tableDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof TableValueObject);
        Assert.assertEquals(tableName, ((TableValueObject) actualResult).getTable().getName());
    }

    @Test()
    public void getValueFromSource_tableDataAndUnknownTableName_returnsTableValueObjectWithSystemAsName()
            throws GettingValueException, IOException {
        String systemName = "Business Solution";
        Source source = new Source(systemName, "", EngineType.SQL,
                "", Collections.singleton(new TableSettings("")));
        when(SqlRepository.executeQuery(any(), Mockito.anyString()))
                .thenReturn(objectMapper.readValue(loadFileToString(DisplayTypeTestConstants.TABLE_RESPONSE_FILE_PATH), Table.class));

        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.NONE, 1);
        parameter.getParameterConfig().setAdditionalSources(Collections.emptyList());
        Mockito.when(parameterExecutionContext.getParameter())
                .thenReturn(parameter);

        AbstractValueObject actualResult = tableDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof TableValueObject);
        Assert.assertEquals("", ((TableValueObject) actualResult).getTable().getName());
    }

    @Test()
    public void getValueFromSource_tableDataAndKnownTableNameTypeCustom_returnsTableValueObjectWithSystemAsName()
            throws GettingValueException, IOException {
        String tableName = "tableName";
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(new TableSettings(tableName)));
        Mockito.when(SqlRepository.executeQuery(Mockito.any(), Mockito.anyString()))
                .thenReturn(objectMapper.readValue(loadFileToString(DisplayTypeTestConstants.TABLE_RESPONSE_FILE_PATH), Table.class));

        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.CUSTOM, 1);
        parameter.getParameterConfig().setAdditionalSources(Collections.emptyList());
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        AbstractValueObject actualResult = tableDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof TableValueObject);
        Assert.assertEquals(tableName, ((TableValueObject) actualResult).getTable().getName());
    }

    @Test()
    public void getValueFromSource_tableDataAndUnknownTableNameTypeCustom_returnsTableValueObjectWithSystemAsName()
            throws GettingValueException, IOException {
        String systemName = "Business Solution";
        Source source = new Source(systemName, "", EngineType.SQL,
                "", Collections.singleton(new TableSettings("")));
        Mockito.when(SqlRepository.executeQuery(Mockito.any(), Mockito.anyString()))
                .thenReturn(objectMapper.readValue(loadFileToString(DisplayTypeTestConstants.TABLE_RESPONSE_FILE_PATH), Table.class));

        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.CUSTOM, 1);
        parameter.getParameterConfig().setAdditionalSources(Collections.emptyList());
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        AbstractValueObject actualResult = tableDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof TableValueObject);
        Assert.assertEquals(systemName, ((TableValueObject) actualResult).getTable().getName());
    }

    @Test()
    public void getValueFromSource_tableDataAndUnknownTableNameAndAdditionalSource_returnsTableValueObjectWithSystemAsName()
            throws GettingValueException, IOException {
        String systemName = "Business Solution";
        Source source = new Source(systemName, "", EngineType.SQL,
                "", Collections.singleton(new TableSettings("")));
        Source secondSource = new Source(systemName, "", EngineType.SQL,
                "", Collections.singleton(new TableSettings(systemName)));

        Mockito.when(SqlRepository.executeQuery(Mockito.any(), Mockito.anyString()))
                .thenReturn(objectMapper.readValue(loadFileToString(DisplayTypeTestConstants.TABLE_RESPONSE_FILE_PATH), Table.class));
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.NONE, 1);
        parameter.getParameterConfig().setAdditionalSources(Collections.singletonList(secondSource));

        Mockito.when(parameterExecutionContext.getParameter())
                .thenReturn(parameter);

        AbstractValueObject actualResult = tableDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof TableValueObject);
        Assert.assertEquals(systemName, ((TableValueObject) actualResult).getTable().getName());
    }

    @Test()
    public void getValueFromSource_tableDataWithEngineTYpeCassandraAndKnownTableName_returnsTableValueObjectWithSystemAsName()
            throws GettingValueException, IOException {
        String systemName = "Business Solution";
        Source source = new Source(systemName, "", EngineType.CASSANDRA,
                "", Collections.singleton(new TableSettings("")));
        when(cassandraRepository.executeQuery(any(), Mockito.anyString()))
                .thenReturn(objectMapper.readValue(loadFileToString(DisplayTypeTestConstants.TABLE_RESPONSE_FILE_PATH), Table.class));

        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.NONE, 1);
        parameter.getParameterConfig().setAdditionalSources(Collections.emptyList());

        Mockito.when(parameterExecutionContext.getParameter())
                .thenReturn(parameter);

        AbstractValueObject actualResult = tableDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof TableValueObject);
        Assert.assertEquals("", ((TableValueObject) actualResult).getTable().getName());
    }

    /* VALIDATION PROCESS */

    // PLAIN

    @Test()
    public void validateParameter_plainValidationPassedAndNoHighlightDifference_parameterValidationStatusIsPassedWithoutDiffMessages()
            throws ValidationException, IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.PLAIN, 1);
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        tableDisplayTypeService.validateParameter(parameterExecutionContext);

        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();
        Assert.assertTrue(validationInfo.getTableValidations().stream()
                .allMatch(validation -> Objects.isNull(validation.getDiffs())));
        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void validateParameter_plainValidationPassedAndHighlightDifference_parameterValidationStatusIsPassedWithoutDiffMessages()
            throws IOException, ValidationException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.PLAIN, 1);
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);

        tableDisplayTypeService.validateParameter(parameterExecutionContext);

        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();
        Assert.assertTrue(validationInfo.getTableValidations().stream()
                .allMatch(validation -> validation.getDiffs().isEmpty()));
        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void validateParameter_plainValidationPassedAndNoHighlightDifference_multipleValuesForColoumnInEr()
            throws ValidationException, IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.PLAIN, 1);
        parameter.getParameterConfig().getErConfig().getTableValidations().get(0).setValue("InProgress, Cancelled, Completed");
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        tableDisplayTypeService.validateParameter(parameterExecutionContext);
        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();
        Assert.assertTrue(validationInfo.getTableValidations().stream()
                .allMatch(validation -> Objects.isNull(validation.getDiffs())));
        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
    }


    @Test()
    public void validateParameter_plainValidationFailedAndNoHighlightDifference_parameterValidationStatusIsFailedWithoutDiffMessages()
            throws IOException, ValidationException {
        int incorrectResultIdx = 1;
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.PLAIN, 3,
                incorrectResultIdx);
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        tableDisplayTypeService.validateParameter(parameterExecutionContext);

        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();
        Assert.assertTrue(validationInfo.getTableValidations().stream()
                .allMatch(validation -> Objects.isNull(validation.getDiffs())));
        Assert.assertEquals(ValidationStatus.FAILED, validationInfo.getStatus());
    }

    @Test()
    public void validateParameter_plainValidationFailedAndHighlightDifference_parameterValidationStatusIsFailedWithDiffMessages()
            throws IOException, ValidationException {
        int incorrectResultIdx = 1;
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForTable(ValidationType.PLAIN, 3,
                incorrectResultIdx);
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);

        tableDisplayTypeService.validateParameter(parameterExecutionContext);

        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();
        List<TableValidationInfo> tableValidations = validationInfo.getTableValidations();
        for (int i = 0; i < tableValidations.size(); i++) {
            if (i == incorrectResultIdx) {
                Assert.assertEquals(tableValidations.get(i).getDiffs().size(), 2);
            } else {
                Assert.assertTrue(tableValidations.get(i).getDiffs().isEmpty());
            }
        }
        Assert.assertEquals(ValidationStatus.FAILED, validationInfo.getStatus());
    }

    @Test()
    public void validateParameter_bvValidationPassedAndHighlightDifference_parameterValidationStatusIsPassedWithoutDiffMessages() throws IOException, ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "STATUS";

        PotSessionParameterEntity parameter = DbMockEntity.generateMockedParameterForBv(false);


        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);
        String compareResponse = getBvCompareResponse("IDENTICAL", testRunId);

        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        when(executionVariablesService.getTestCaseNamesFromVariable(anyList(), any())).thenReturn(Collections.singletonList(
                "61abd5ac-efbe-49cc-b7f5-925a7f543481"));
        doReturn(objectMapper.readValue(createResponse, TestRunCreationResponse.class))
                .when(bulkValidatorRepository).createTestRun(any(), any(), any());
        doReturn(Arrays.asList(objectMapper.readValue(compareResponse, ComparingProcessResponse[].class)))
                .when(bulkValidatorRepository).compare(any(), any());

        tableDisplayTypeService.validateParameter(parameterExecutionContext);
        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();

        Assert.assertEquals(ValidationStatus.PASSED, validationInfo.getStatus());
    }

    private ConcurrentHashMap<String, ExecutionVariable> anySet() {
        return null;
    }

    @Test()
    public void validateParameter_bvValidationFailedAndHighlightDifference_parameterValidationFailed() throws IOException, ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "STATUS";
        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);
        String compareResponse = getBvCompareResponse("FAILED", testRunId);
        PotSessionParameterEntity parameter = DbMockEntity.generateMockedParameterForBv(false);

        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        when(executionVariablesService.getTestCaseNamesFromVariable(anyList(), any())).thenReturn(Collections.singletonList(
                "61abd5ac-efbe-49cc-b7f5-925a7f543481"));
        doReturn(objectMapper.readValue(createResponse, TestRunCreationResponse.class))
                .when(bulkValidatorRepository).createTestRun(any(), any(), any());
        doReturn(Arrays.asList(objectMapper.readValue(compareResponse, ComparingProcessResponse[].class)))
                .when(bulkValidatorRepository).compare(any(), any());

        tableDisplayTypeService.validateParameter(parameterExecutionContext);
        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();

        Assert.assertEquals(ValidationStatus.FAILED, validationInfo.getStatus());
    }

    @Test()
    public void validateParameter_bvValidationFailedAndHighlightDifference_findTcIdByNameParameterValidationFailed() throws IOException, ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "STATUS";
        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);
        String compareResponse = getBvCompareResponse("FAILED", testRunId);
        PotSessionParameterEntity parameter = DbMockEntity.generateMockedParameterForBv(true);

        when(bulkValidatorPublicFeignClient.getTcIdsByTcNames(any(), any()))
                .thenReturn(new ResponseEntity<>(
                        createGettingResponse(true,false, false), HttpStatus.OK));
        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(executionVariablesService.getTestCaseNamesFromVariable(anyList(), any()))
                .thenReturn(new ArrayList<>(Collections.singletonList("testCaseName")));
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);

        doReturn(objectMapper.readValue(createResponse, TestRunCreationResponse.class))
                .when(bulkValidatorRepository).createTestRun(any(), any(), any());
        doReturn(Arrays.asList(objectMapper.readValue(compareResponse, ComparingProcessResponse[].class)))
                .when(bulkValidatorRepository).compare(any(), any());

        tableDisplayTypeService.validateParameter(parameterExecutionContext);
        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();

        Assert.assertEquals(ValidationStatus.FAILED, validationInfo.getStatus());
    }

    @Test()
    public void validateParameter_bvValidationPassedAndHighlightDifference_findTcIdByNameParameterValidationPassed() throws IOException, ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "STATUS";
        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);
        String compareResponse = getBvCompareResponse("IDENTICAL", testRunId);
        PotSessionParameterEntity parameter = DbMockEntity.generateMockedParameterForBv(true);

        when(bulkValidatorPublicFeignClient.getTcIdsByTcNames(any(), any()))
                .thenReturn(new ResponseEntity<>(
                        createGettingResponse(true,false, false), HttpStatus.OK));
        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(executionVariablesService.getTestCaseNamesFromVariable(anyList(), any()))
                .thenReturn(new ArrayList<>(Collections.singletonList("testCaseName")));
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        doReturn(objectMapper.readValue(createResponse, TestRunCreationResponse.class))
                .when(bulkValidatorRepository).createTestRun(any(), any(), any());
        doReturn(Arrays.asList(objectMapper.readValue(compareResponse, ComparingProcessResponse[].class)))
                .when(bulkValidatorRepository).compare(any(), any());

        tableDisplayTypeService.validateParameter(parameterExecutionContext);
        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();

        Assert.assertEquals(ValidationStatus.PASSED, validationInfo.getStatus());
    }

    @Test
    public void validateParameter_bvValidationPassedAndHighlightDifferenceWithNotFoundName_ValidationFailed() throws IOException,
            ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "STATUS";
        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);
        String compareResponse = getBvCompareResponse("IDENTICAL", testRunId);
        PotSessionParameterEntity parameter = DbMockEntity.generateMockedParameterForBv(true);

        when(bulkValidatorPublicFeignClient.getTcIdsByTcNames(any(), any()))
                .thenReturn(new ResponseEntity<>(
                        createGettingResponse(true,true, true), HttpStatus.OK));
        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(executionVariablesService.getTestCaseNamesFromVariable(anyList(), any()))
                .thenReturn(new ArrayList<>(Collections.singletonList("testCaseName")));
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        doReturn(objectMapper.readValue(createResponse, TestRunCreationResponse.class))
                .when(bulkValidatorRepository).createTestRun(any(), any(), any());
        doReturn(Arrays.asList(objectMapper.readValue(compareResponse, ComparingProcessResponse[].class)))
                .when(bulkValidatorRepository).compare(any(), any());

        tableDisplayTypeService.validateParameter(parameterExecutionContext);
        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();

        Assert.assertEquals(ValidationStatus.FAILED, validationInfo.getStatus());
        Assert.assertEquals("BV Test Cases below weren't found: lostName", validationInfo.getErrorDescription());
    }

    @Test
    public void validateParameter_bvValidationPassedAndHighlightDifferenceWithNotFoundName_twoTcIdFindValidationPassed() throws IOException,
            ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "STATUS";
        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);
        String compareResponse = getBvCompareResponse("IDENTICAL", testRunId);
        String testRunId2 = "0d7df690-48cc-0a3a-8c73-980cc744aa95";
        String externalId2 = "a7e120ef-7f09-4054-8c00-5974ba2faee5";
        String validationObjectName2 = "PRODUCT_ORDER_NAME";
        String createResponse2 = getBvCreateTestRunResponse(testRunId2, externalId2, validationObjectName2);
        String compareResponse2 = getBvCompareResponse("IDENTICAL", testRunId2);
        PotSessionParameterEntity parameter = DbMockEntity.generateMockedParameterForBv(true);

        when(bulkValidatorPublicFeignClient.getTcIdsByTcNames(any(), any()))
                .thenReturn(new ResponseEntity<>(
                        createGettingResponse(true, false, true), HttpStatus.OK));
        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(executionVariablesService.getTestCaseNamesFromVariable(anyList(), any()))
                .thenReturn(new ArrayList<>(Arrays.asList("test_name", "test_name2")));
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        doReturn(objectMapper.readValue(createResponse2, TestRunCreationResponse.class))
                .when(bulkValidatorRepository)
                .createTestRun(any(),
                        argThat(id -> id.equals(UUID.fromString("61abd5ac-efbe-b7f5-49cc-925a7f543481"))), any());
        doReturn(objectMapper.readValue(createResponse, TestRunCreationResponse.class))
                .when(bulkValidatorRepository)
                .createTestRun(any(),
                        argThat(id -> id.equals(UUID.fromString("61abd5ac-efbe-49cc-b7f5-925a7f543481"))), any());
        doReturn(Arrays.asList(objectMapper.readValue(compareResponse2, ComparingProcessResponse[].class)[0],
                objectMapper.readValue(compareResponse, ComparingProcessResponse[].class)[0]))
                .when(bulkValidatorRepository).compare(any(), any());

        tableDisplayTypeService.validateParameter(parameterExecutionContext);
        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();

        Assert.assertEquals(ValidationStatus.PASSED, validationInfo.getStatus());
    }

    @Test
    public void validateParameter_bvValidationPassedAndHighlightDifferenceWithNotFoundName_twoTcIdFindOneParameterIsFailedValidationFailed() throws IOException,
            ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "STATUS";
        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);
        String compareResponse = getBvCompareResponse("FAILED", testRunId);
        String testRunId2 = "0d7df690-48cc-0a3a-8c73-980cc744aa95";
        String externalId2 = "a7e120ef-7f09-4054-8c00-5974ba2faee5";
        String validationObjectName2 = "PRODUCT_ORDER_NAME";
        String createResponse2 = getBvCreateTestRunResponse(testRunId2, externalId2, validationObjectName2);
        String compareResponse2 = getBvCompareResponse("IDENTICAL", testRunId2);
        PotSessionParameterEntity parameter = DbMockEntity.generateMockedParameterForBv(true);

        when(bulkValidatorPublicFeignClient.getTcIdsByTcNames(any(), any()))
                .thenReturn(new ResponseEntity<>(
                        createGettingResponse(true, false, true), HttpStatus.OK));
        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(executionVariablesService.getTestCaseNamesFromVariable(anyList(), any()))
                .thenReturn(new ArrayList<>(Arrays.asList("test_name", "test_name2")));
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        doReturn(objectMapper.readValue(createResponse2, TestRunCreationResponse.class))
                .when(bulkValidatorRepository)
                .createTestRun(any(),
                        argThat(id -> id.equals(UUID.fromString("61abd5ac-efbe-b7f5-49cc-925a7f543481"))), any());
        doReturn(objectMapper.readValue(createResponse, TestRunCreationResponse.class))
                .when(bulkValidatorRepository)
                .createTestRun(any(),
                        argThat(id -> id.equals(UUID.fromString("61abd5ac-efbe-49cc-b7f5-925a7f543481"))), any());
        doReturn(Arrays.asList(objectMapper.readValue(compareResponse2, ComparingProcessResponse[].class)[0],
                objectMapper.readValue(compareResponse, ComparingProcessResponse[].class)[0]))
                .when(bulkValidatorRepository).compare(any(), any());

        tableDisplayTypeService.validateParameter(parameterExecutionContext);
        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();

        Assert.assertEquals(ValidationStatus.FAILED, validationInfo.getStatus());
    }
}
