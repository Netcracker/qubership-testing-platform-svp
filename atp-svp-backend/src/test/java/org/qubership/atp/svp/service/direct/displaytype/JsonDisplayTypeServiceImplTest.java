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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.jcraft.jsch.JSchException;
import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.core.enums.JsonParseViewType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.enums.ValidationType;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.InvalidBulkValidatorApiUsageException;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.model.bulkvalidator.TestRunCreationResponse;
import org.qubership.atp.svp.model.db.SutParameterEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.Connection;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.environments.System;
import org.qubership.atp.svp.model.impl.BulkValidatorProjectSettings;
import org.qubership.atp.svp.model.impl.HttpSettings;
import org.qubership.atp.svp.model.impl.JsonDataColumnSettings;
import org.qubership.atp.svp.model.impl.JsonHierarchyNodeNames;
import org.qubership.atp.svp.model.impl.JsonJoinConditionSettings;
import org.qubership.atp.svp.model.impl.JsonParseSettings;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.impl.SourceSettings;
import org.qubership.atp.svp.model.impl.TableValidationSettings;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.SimpleExecutionVariable;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.model.pot.validation.ActualTablesValidationInfo;
import org.qubership.atp.svp.model.pot.validation.TableValidationInfo;
import org.qubership.atp.svp.model.pot.validation.TableVsTableValidationInfo;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.repo.feign.BulkValidatorApiFeignClient;
import org.qubership.atp.svp.repo.feign.BulkValidatorPublicFeignClient;
import org.qubership.atp.svp.repo.feign.BulkValidatorValidatorFeignClient;
import org.qubership.atp.svp.repo.impl.BulkValidatorRepository;
import org.qubership.atp.svp.repo.impl.CassandraRepository;
import org.qubership.atp.svp.repo.impl.RestRepositoryImpl;
import org.qubership.atp.svp.repo.impl.SqlRepository;
import org.qubership.atp.svp.repo.impl.SshRepository;
import org.qubership.atp.svp.service.direct.BulkValidatorValidationService;
import org.qubership.atp.svp.service.direct.CompareTablesService;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.qubership.atp.svp.service.direct.JsonDisplayTypeValidationService;
import org.qubership.atp.svp.service.direct.displaytype.jsonparse.CommonJsonParseService;
import org.qubership.atp.svp.service.direct.displaytype.jsonparse.CommonJsonParseTableService;
import org.qubership.atp.svp.service.direct.displaytype.jsonparse.HierarchyTableServiceImpl;
import org.qubership.atp.svp.service.direct.displaytype.jsonparse.HierarchyTreeObjectTableServiceImpl;
import org.qubership.atp.svp.service.direct.displaytype.jsonparse.JsonParseTypeFactoryImpl;
import org.qubership.atp.svp.service.direct.displaytype.jsonparse.RawSimpleServiceImpl;
import org.qubership.atp.svp.service.direct.displaytype.jsonparse.TableServiceImpl;
import org.qubership.atp.svp.tests.DbMockEntity;
import org.qubership.atp.svp.tests.TestWithTestData;
import org.qubership.atp.svp.utils.DtoConvertService;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = JsonDisplayTypeServiceImpl.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
@PrepareForTest(SqlRepository.class)
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties", properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest", "atp.bv.url=http://atp-bv:8080"
})
public class JsonDisplayTypeServiceImplTest extends TestWithTestData {

    private static final String MOCKED_SERVER_URL = "www.google.com";

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Autowired
    JsonDisplayTypeServiceImpl jsonDisplayTypeService;

    @SpyBean
    JsonParseTypeFactoryImpl jsonParsTypeFactory;

    @SpyBean
    RawSimpleServiceImpl rawSimpleService;

    @SpyBean
    TableServiceImpl tableService;

    @SpyBean
    HierarchyTableServiceImpl hierarchyTableService;

    @SpyBean
    HierarchyTreeObjectTableServiceImpl hierarchyTreeObjectTableService;

    @SpyBean
    CommonJsonParseService commonJsonParseService;

    @SpyBean
    CommonJsonParseTableService commonJsonParseTableService;

    @SpyBean
    ExecutionVariablesServiceImpl executionVariablesService;

    @SpyBean
    CompareTablesService compareTablesService;

    @MockBean
    RestRepositoryImpl restRepository;

    @MockBean
    CassandraRepository cassandraRepository;

    @SpyBean
    BulkValidatorValidationService bulkValidatorValidationService;

    @SpyBean
    BulkValidatorRepository bulkValidatorRepository;

    @SpyBean
    DtoConvertService dtoConvertService;

    @MockBean
    BulkValidatorApiFeignClient bulkValidatorApiFeignClient;

    @MockBean
    BulkValidatorValidatorFeignClient bulkValidatorValidatorFeignClient;

    @MockBean
    BulkValidatorPublicFeignClient bulkValidatorPublicFeignClient;

    @SpyBean
    JsonDisplayTypeValidationService jsonDisplayTypeValidationService;

    @MockBean
    SshRepository sshRepository;

    @Mock
    Environment environment;
    @Mock
    System system;
    @Mock
    Server server;
    @Mock
    Connection connection;

    @Mock
    SutParameterExecutionContext parameterExecutionContext;
    @Mock
    SessionExecutionConfiguration sessionExecutionConfiguration;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(SqlRepository.class);
        when(environment.getSystem(anyString())).thenReturn(system);
        when(system.getServer(anyString())).thenReturn(server);
        when(server.getConnection()).thenReturn(connection);
        when(connection.getParameters()).thenReturn(Collections.singletonMap("url", MOCKED_SERVER_URL));
        when(parameterExecutionContext.getSessionConfiguration()).thenReturn(sessionExecutionConfiguration);
        when(sessionExecutionConfiguration.getEnvironment()).thenReturn(environment);
        when(parameterExecutionContext.getExecutionVariables()).thenReturn(new ConcurrentHashMap<>());
    }

    /* GETTING VALUE PROCESS */

    // Check execution variables
    @Test
    public void getValueFromSource_executionVariablesExists_allExecutionVariablesAreSubstitute()
            throws GettingValueException {
        String mockedRequestQuery = "/request-query/";
        String keyParameterVariableName = "keyParameterName";
        String keyParameterVariableValue = "keyParameterValue";
        String sutParameterVariableName = "groupName.sutParameterName";
        String sutParameterVariableValue = "sutParameterValue";
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                Collections.emptyList(), null, null, false, null, false);
        HttpSettings arHttpSettings = new HttpSettings();
        arHttpSettings.setQuery(mockedRequestQuery + "${" + keyParameterVariableName + "}");
        arHttpSettings.setQueryParams(Collections.emptyMap());
        arHttpSettings.setRequestType(HttpMethod.GET);
        Map<String, String> headers = new HashMap<>();
        headers.put("${" + keyParameterVariableName + "}", "${" + sutParameterVariableName + "}");
        arHttpSettings.setHeaders(headers);

        Set<SourceSettings> settings = new HashSet<>();
        settings.add(jsonParseSettings);
        settings.add(arHttpSettings);
        Source source = new Source("", "", EngineType.REST, "", settings);

//        Set<ExecutionVariable> executionVariables = new HashSet<>();
        ConcurrentHashMap executionVariables = new ConcurrentHashMap();
//        executionVariables.add(new SimpleExecutionVariable(keyParameterVariableName, keyParameterVariableValue));
//        executionVariables.add(new SimpleExecutionVariable(sutParameterVariableName, sutParameterVariableValue));
        executionVariables.put(keyParameterVariableName, new SimpleExecutionVariable(keyParameterVariableName,
                keyParameterVariableValue));
        executionVariables.put(sutParameterVariableName, new SimpleExecutionVariable(sutParameterVariableName,
                sutParameterVariableValue));
        Mockito.when(parameterExecutionContext.getExecutionVariables()).thenReturn(executionVariables);

        HttpSettings erHttpSettings = new HttpSettings();
        erHttpSettings.setQuery(mockedRequestQuery + keyParameterVariableValue);
        erHttpSettings.setQueryParams(Collections.emptyMap());
        erHttpSettings.setRequestType(HttpMethod.GET);
        erHttpSettings.setHeaders(Collections.singletonMap(keyParameterVariableValue, sutParameterVariableValue));

        Mockito.when(restRepository.executeRequest(MOCKED_SERVER_URL, erHttpSettings)).thenReturn("response");

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);

        Mockito.verify(restRepository).executeRequest(MOCKED_SERVER_URL, erHttpSettings);
    }

    // Json parsing to JsonParseViewType.RAW

    // Negative test cases
    @Test
    public void getValueFromSource_notJsonResponseAndEngineTypeNotTypical_throwsGettingValueException()
            throws IOException {
        String initialJsonAsString = loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH);
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, Collections.emptyList(),
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.LOG_COLLECTOR,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(initialJsonAsString);

        try {
            jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
            Assert.fail("Switch not go in default case");
        } catch (GettingValueException ex) {
            Assert.assertEquals("Unexpected EngineType value: LOG_COLLECTOR for DisplayType: JSON",
                    ex.getMessage());
        }
    }

    @Test
    public void getValueFromSource_jsonResponseAndEngineTypeCassandra_returnsInitialJsonAsSimpleValueObject()
            throws GettingValueException, IOException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "$.category",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, Collections.emptyList()
                , null, null, false, null, false);
        Source source = new Source("", "", EngineType.CASSANDRA,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(cassandraRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals("[Mobile] Consumer", ((SimpleValueObject) actualResult).getValue());
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_notJsonResponseAndRawViewAndJsonPathIsNotEmpty_throwsGettingValueException()
            throws GettingValueException, IOException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "$.category",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, Collections.emptyList(),
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.NOT_JSON_RESPONSE_FILE_PATH));

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    // Positive test cases form JsonParseViewType.RAW
    @Test
    public void getValueFromSource_jsonResponseAndRawViewAndJsonPathIsEmpty_returnsInitialJsonAsSimpleValueObject()
            throws GettingValueException, IOException {
        String initialJsonAsString = loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH);
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, Collections.emptyList(),
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(initialJsonAsString);

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(initialJsonAsString, ((SimpleValueObject) actualResult).getValue());
    }

    @Test
    public void getValueFromSource_jsonResponseAndRawViewAndJsonPathIsNotEmpty_returnsParsedJsonAsSimpleValueObject()
            throws GettingValueException, IOException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "$.category",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                Collections.emptyList(), null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals("[Mobile] Consumer", ((SimpleValueObject) actualResult).getValue());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndRawViewAndJsonPathValidAndNotTypical_returnsParsedJsonAsSimpleValueObject()
            throws GettingValueException, IOException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "category",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, Collections.emptyList(),
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals("[Mobile] Consumer", ((SimpleValueObject) actualResult).getValue());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndRawViewAndJsonPathIsNotEmpty_returnsParsedJsonAsNodeSimpleValueObject()
            throws GettingValueException, IOException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "$.validFor",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                Collections.emptyList(), null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_NODE_CORRECT_VALUE,
                ((SimpleValueObject) actualResult).getValue());
    }

    @Test
    public void getValueFromSource_whenSetFlagFirstValue_returnFirstValueByArray() throws GettingValueException, IOException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "$.fieldNames",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                Collections.emptyList(), null, null, false, null, true);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));

        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_FIRST_VALUE_FROM_JSON));
        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals("Not correctly return value", "action_id", ((SimpleValueObject) actualResult).getValue());
    }

    @Test
    public void getValueFromSource_whenSetFlagFirstValueAndNotArrayObject_expectedExceptionJson() throws GettingValueException, IOException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "$.fieldNames",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                Collections.emptyList(), null, null, false, null, true);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));

        exceptionRule.expect(GettingValueException.class);
        exceptionRule.expectMessage("Json path: $.fieldNames is not correctly");
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_FIRST_VALUE_FROM_JSON_WHEN_PRIMITIVE));

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }


    // Json parsing to JsonParseViewType.TABLE

    // Negative test cases
    @Test(expected = GettingValueException.class)
    public void getValueFromSource_notJsonResponseAndTableViewAndJsonPathIsNotEmpty_throwsGettingValueException()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.subcategory", Collections.emptyList()));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE, "$.category",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings
                , null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.NOT_JSON_RESPONSE_FILE_PATH));

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_jsonResponseAndTableViewAndColumnsDataNotSpecified_throwsGettingValueException()
            throws GettingValueException, IOException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE, "$.quoteItem",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, Collections.emptyList(),
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndTableViewAndColumnSettingsParametersJsonBadPath_returnsJsonTableWithEmptyRow()
            throws IOException, GettingValueException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.BadPath", Collections.emptyList()));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE, "$.relatedParty",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings
                , null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(0, ((JsonTable) ((TableValueObject) actualResult).getTable()).getRows().size());
    }

    //Positive test cases for JsonParseViewType.TABLE
    @Test
    public void getValueFromSource_jsonResponseAndTableViewAndParsedValueIsArray_returnsJsonTable()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.id", Collections.emptyList()));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE, "$.relatedParty",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings
                , null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_TABLE_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndTableViewAndParsedValueIsArrayAndGroupingJsonPaths_returnsJsonTable()
            throws GettingValueException, IOException {
        List<String> groupingJsonPaths = Arrays.asList("$.priceType", "$.name");
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$..pricesByOffering[1]", groupingJsonPaths));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE,
                "$.productOfferingRefByMarketRefs",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings,
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_TABLE_GROUPING_CELL_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndTableViewAndJsonPathsAndJsonPathValidAndNotTypical_returnsJsonTable()
            throws GettingValueException, IOException {
        List<String> groupingJsonPaths = Arrays.asList("priceType", "name");
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$..pricesByOffering[1]", groupingJsonPaths));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE,
                "productOfferingRefByMarketRefs",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings
                , null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_TABLE_GROUPING_CELL_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void getValueFromSource_jsonResponseAndTableViewAndParsedValueIsNotArray_returnsJsonTable()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.relatedParty[0].id", Collections.emptyList()));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE, "$",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings,
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);
        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_TABLE_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndGroupingJsonPathsAndColumnSettingsJsonPathNotExistent_returnsJsonTable()
            throws GettingValueException, IOException {
        List<String> groupingJsonPaths = Arrays.asList("$.priceType", "$.name");
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.JsonPathNotExistent", groupingJsonPaths));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE,
                "$.productOfferingRefByMarketRefs",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings,
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(
                DisplayTypeTestConstants
                        .JSON_TABLE_GROUPING_CELL_AND_COLUMN_SETTINGS_JSON_PATH_NOT_EXISTENT_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndGroupingJsonPathsAndColumnSettingsJsonPathNotArray_returnsJsonTable()
            throws GettingValueException, IOException {
        List<String> groupingJsonPaths = Arrays.asList("$.priceType", "$.name");
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.marketId", groupingJsonPaths));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE,
                "$.productOfferingRefByMarketRefs",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings,
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(
                DisplayTypeTestConstants
                        .JSON_TABLE_GROUPING_CELL_AND_COLUMN_SETTINGS_JSON_PATH_NOT_ARRAY_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndTableViewAndParsedMockJsonSwitcherIsTrueAndMockValue_returnsJsonTable()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.id", Collections.emptyList()));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE, "$.relatedParty",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER,
                loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH),
                true, jsonDataColumnSettings, null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_TABLE_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void getValueFromSource_jsonResponseAndTableViewAndParsedMockJsonSwitcherIsNullAndValue_returnsJsonTable()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.id", Collections.emptyList()));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE, "$.relatedParty",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER,
                loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH),
                null, jsonDataColumnSettings, null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_TABLE_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndTableViewAndParsedMockJsonSwitcherIsTrueAndEmptyValue_returnsJsonTable()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.id", Collections.emptyList()));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE, "$.relatedParty",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "",
                null, jsonDataColumnSettings, null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_TABLE_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    // Json parsing to JsonParseViewType.HIERARCHY_TABLE
    // Negative test cases
    @Test(expected = GettingValueException.class)
    public void
    getValueFromSource_notJsonResponseAndHierarchyTableViewAndJsonPathIsNotEmpty_throwsGettingValueException()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.subcategory", Collections.emptyList()));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE, "$.category",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings,
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.NOT_JSON_RESPONSE_FILE_PATH));

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void
    getValueFromSource_jsonResponseAndHierarchyTableViewAndParsedValueIsNotArray_throwsGettingValueException()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.subcategory", Collections.emptyList()));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE, "$.category",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings,
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void
    getValueFromSource_jsonResponseAndHierarchyTableViewAndColumnsDataNotSpecified_throwsGettingValueException()
            throws GettingValueException, IOException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE, "$.quoteItem",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, Collections.emptyList(),
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void
    getValueFromSource_jsonResponseAndHierarchyTableViewAndColumnsDataAndHierarchyNodeNamesIsNull_throwsGettingValueException()
            throws GettingValueException, IOException {
        List<String> groupingJsonPaths = Arrays.asList("$.priceType", "$.name");
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$..pricesByOffering[1]", groupingJsonPaths));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE,
                "$.productOfferingRefByMarketRefs",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings,
                null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));
        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void
    getValueFromSource_jsonResponseAndHierarchyTableViewAndColumnsDataAndHierarchyNodeNamesWithOneNullParameter_throwsGettingValueException()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.product.name", Collections.emptyList()));
        JsonHierarchyNodeNames hierarchyNodeNames = new JsonHierarchyNodeNames("id", "rootQuoteItemId", null);
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE,
                ".quoteItem",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings,
                hierarchyNodeNames, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    // Positive test cases for JsonParseViewType.HIERARCHY_TABLE
    @Test
    public void
    getValueFromSource_jsonResponseAndHierarchyTableView_returnsJsonTableSortedHierarchyWithNestingDepthSet()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.product.name", Collections.emptyList()));
        JsonHierarchyNodeNames hierarchyNodeNames = new JsonHierarchyNodeNames("id", "rootQuoteItemId",
                "parentQuoteItemId");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE,
                "$.quoteItem", DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                jsonDataColumnSettings, hierarchyNodeNames, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL, "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_HIERARCHY_TABLE_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndHierarchyTableViewAndNodNamesJsonPath_returnsJsonTableSortedHierarchyWithNestingDepthSet()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.product.name", Collections.emptyList()));
        JsonHierarchyNodeNames hierarchyNodeNames = new JsonHierarchyNodeNames("$.id", "$.rootQuoteItemId",
                "$.parentQuoteItemId");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE,
                "$.quoteItem", DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                jsonDataColumnSettings, hierarchyNodeNames, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL, "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_HIERARCHY_TABLE_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    // Json parsing to JsonParseViewType.HIERARCHY_TREE_OBJECT_TABLE
    // Negative test cases
    @Test(expected = GettingValueException.class)
    public void
    getValueFromSource_jsonResponseAndHierarchyTreeObjectTableViewAndColumnsDataAndHierarchyNodeNamesIsEmpty_throwsGettingValueException()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.name", Collections.emptyList()));
        List<String> hierarchyTreeObjectNodeNames = Collections.emptyList();
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TREE_OBJECT_TABLE,
                "$", DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings,
                null, hierarchyTreeObjectNodeNames, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));
        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void
    getValueFromSource_jsonResponseAndHierarchyTreeObjectTableViewAndBadJsonValue_throwsGettingValueException()
            throws GettingValueException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.name", Collections.emptyList()));
        List<String> hierarchyTreeObjectNodeNames = Arrays.asList("relatedOrders", "orderItems");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TREE_OBJECT_TABLE,
                "$", DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, jsonDataColumnSettings,
                null, hierarchyTreeObjectNodeNames, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(DisplayTypeTestConstants.BAD_JSON_RESPONSE_VALUE);
        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    // Positive test cases for JsonParseViewType.HIERARCHY_TREE_OBJECT_TABLE

    @Test
    public void
    getValueFromSource_jsonResponseAndHierarchyTreeObjectTableViewAndOneNodeNames_returnsJsonTableSortedHierarchyWithNestingDepthSet()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.name", Collections.emptyList()));
        List<String> hierarchyTreeObjectNodeNames = Arrays.asList("relatedOrders");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TREE_OBJECT_TABLE,
                "$", DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                jsonDataColumnSettings, null, hierarchyTreeObjectNodeNames, false, null, false);
        Source source = new Source("", "", EngineType.SQL, "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_TREE_OBJECT_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_HIERARCHY_TREE_OBJECT_TABLE_ONE_NODE_NAMES_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndHierarchyTreeObjectTableViewAndOneNodeNamesNotTypical_returnsJsonTableSortedHierarchyWithNestingDepthSet()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.name", Collections.emptyList()));
        List<String> hierarchyTreeObjectNodeNames = Arrays.asList("$test");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TREE_OBJECT_TABLE,
                "$", DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                jsonDataColumnSettings, null, hierarchyTreeObjectNodeNames, false, null, false);
        Source source = new Source("", "", EngineType.SQL, "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_TREE_OBJECT_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(
                DisplayTypeTestConstants.JSON_HIERARCHY_TREE_OBJECT_TABLE_ONE_NODE_NAMES_NOT_TYPICAL_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndHierarchyTreeObjectTableViewAndTwoNodeNames_returnsJsonTableSortedHierarchyWithNestingDepthSet()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.name", Collections.emptyList()));
        List<String> hierarchyTreeObjectNodeNames = Arrays.asList("relatedOrders", "orderItems");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TREE_OBJECT_TABLE,
                "$", DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                jsonDataColumnSettings, null, hierarchyTreeObjectNodeNames, false, null, false);
        Source source = new Source("", "", EngineType.SQL, "", Collections.singleton(jsonParseSettings));
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_TREE_OBJECT_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_HIERARCHY_TREE_OBJECT_TABLE_TWO_NODE_NAMES_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void getValueFromSource_jsonResponseAndTableViewAndParsedValueIsArrayEmpty_returnsEmptyJsonTable()
            throws GettingValueException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER", "$.id", Collections.emptyList()));
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE, "$",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "[]", true, jsonDataColumnSettings
                , null, null, false, null, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);

        Assert.assertEquals(DisplayTypeTestConstants.JSON_TABLE_EMPTY_TABLE_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    // Positive test cases for JsonParseViewType.TABLE mod JOIN Table

    @Test
    public void
    getValueFromSource_jsonResponseAndJoinTwoRefTableAndIsSwitcherTrueAndConditionalSettingsNotEmpty_returnsJoinJsonTable()
            throws GettingValueException, IOException {

        List<JsonDataColumnSettings> jsonDataColumnSettings = new ArrayList<>();
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("rootQuoteItem3", "$.rootQuoteItemId", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("parentQuoteItem", "$.parentQuoteItemId", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("id", "$.id", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Product.name", "$.product.name", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Action", "$.action", Collections.emptyList()));

        String pathTableRef1 = "group.tableRef1";
        String pathTableRef2 = "group.tableRef2";

        JsonJoinConditionSettings jsonJoinConditionSettings1 = new JsonJoinConditionSettings();
        jsonJoinConditionSettings1.getIdxPrimaryHeaderNames().add(0);
        jsonJoinConditionSettings1.getIdxPrimaryHeaderNames().add(4);
        jsonJoinConditionSettings1.setPathReferenceSutParameterName(pathTableRef1);
        jsonJoinConditionSettings1.getIdxReferenceHeaderNames().add(0);
        jsonJoinConditionSettings1.getIdxReferenceHeaderNames().add(2);
        JsonJoinConditionSettings jsonJoinConditionSettings2 = new JsonJoinConditionSettings();
        jsonJoinConditionSettings2.getIdxPrimaryHeaderNames().add(0);
        jsonJoinConditionSettings2.setPathReferenceSutParameterName(pathTableRef2);
        jsonJoinConditionSettings2.getIdxReferenceHeaderNames().add(3);

        List<JsonJoinConditionSettings> jsonJoinConditionSettings = new ArrayList<>();
        jsonJoinConditionSettings.add(jsonJoinConditionSettings1);
        jsonJoinConditionSettings.add(jsonJoinConditionSettings2);

        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE, "$.quoteItem",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER,
                loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_TABLE_PRIMARY_FILE_PATH),
                true, jsonDataColumnSettings, null, null,
                true, jsonJoinConditionSettings, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));

        JsonTable jsonModelTableRef1 = generateTableRef1ForJoin();
        JsonTable jsonModelTableRef2 = generateTableRef2ForJoin();

        Mockito.when(executionVariablesService.getSourceWithJsonTableVariables(eq(pathTableRef1), any()))
                .thenReturn(Optional.of(jsonModelTableRef1));
        Mockito.when(executionVariablesService.getSourceWithJsonTableVariables(eq(pathTableRef2), any()))
                .thenReturn(Optional.of(jsonModelTableRef2));

        SutParameterEntity sutParameter = new SutParameterEntity();
        sutParameter.setName("tablePrim");
        PotSessionParameterEntity parameter = new PotSessionParameterEntity();
        parameter.setParameterConfig(sutParameter);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_TABLE_JOIN_2REF_TABLE_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    @Test
    public void
    getValueFromSource_jsonResponseAndPrimeTableHierarchyAndRefTableHierarchyAndIsSwitcherTrueAndConditionalSettingsNotEmpty_returnsJoinJsonTable()
            throws GettingValueException, IOException {

        List<JsonDataColumnSettings> jsonDataColumnSettings = new ArrayList<>();
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Product.name", "$.product.name", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("id", "$.id", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Action", "$.action", Collections.emptyList()));

        String pathTableRef1 = "group.tableRef1";

        JsonJoinConditionSettings jsonJoinConditionSettings1 = new JsonJoinConditionSettings();
        jsonJoinConditionSettings1.getIdxPrimaryHeaderNames().add(0);
        jsonJoinConditionSettings1.setPathReferenceSutParameterName(pathTableRef1);
        jsonJoinConditionSettings1.getIdxReferenceHeaderNames().add(0);

        List<JsonJoinConditionSettings> jsonJoinConditionSettings = new ArrayList<>();
        jsonJoinConditionSettings.add(jsonJoinConditionSettings1);
        JsonHierarchyNodeNames hierarchyNodeNames = new JsonHierarchyNodeNames("$.id", "$.rootQuoteItemId",
                "$.parentQuoteItemId");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE, "$.quoteItem",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER,
                loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_TABLE_HIERARCHY_PRIMARY_FILE_PATH),
                true, jsonDataColumnSettings, hierarchyNodeNames, null,
                true, jsonJoinConditionSettings, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));

        JsonTable jsonModelTableRef1 = generateTableHierarchyForJoin();

        Mockito.when(executionVariablesService.getSourceWithJsonTableVariables(eq(pathTableRef1), any()))
                .thenReturn(Optional.of(jsonModelTableRef1));

        SutParameterEntity sutParameter = new SutParameterEntity();
        sutParameter.setName("tablePrim");
        PotSessionParameterEntity parameter = new PotSessionParameterEntity();
        parameter.setParameterConfig(sutParameter);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
                parameterExecutionContext);

        Assert.assertTrue(((TableValueObject) actualResult).getTable() instanceof JsonTable);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_TABLE_HIERARCHY_JOIN_1REF_TABLE_CORRECT_VALUE,
                ((TableValueObject) actualResult).getTable().toString());
    }

    // Negative test cases for JsonParseViewType.TABLE mod JOIN Table

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_jsonResponseAndJoinTableAndNotExistIndexPrimHeader_returnsException()
            throws GettingValueException, IOException {

        List<JsonDataColumnSettings> jsonDataColumnSettings = new ArrayList<>();
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Product.name", "$.product.name", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("id", "$.id", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Action", "$.action", Collections.emptyList()));

        String pathTableRef1 = "group.tableRef1";

        JsonJoinConditionSettings jsonJoinConditionSettings1 = new JsonJoinConditionSettings();
        // index outside
        jsonJoinConditionSettings1.getIdxPrimaryHeaderNames().add(4);
        jsonJoinConditionSettings1.setPathReferenceSutParameterName(pathTableRef1);
        jsonJoinConditionSettings1.getIdxReferenceHeaderNames().add(0);

        List<JsonJoinConditionSettings> jsonJoinConditionSettings = new ArrayList<>();
        jsonJoinConditionSettings.add(jsonJoinConditionSettings1);
        JsonHierarchyNodeNames hierarchyNodeNames = new JsonHierarchyNodeNames("$.id", "$.rootQuoteItemId",
                "$.parentQuoteItemId");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE, "$.quoteItem",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER,
                loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_TABLE_HIERARCHY_PRIMARY_FILE_PATH),
                true, jsonDataColumnSettings, hierarchyNodeNames, null,
                true, jsonJoinConditionSettings, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));

        JsonTable jsonModelTableRef1 = generateTableHierarchyForJoin();

        Mockito.when(executionVariablesService.getSourceWithJsonTableVariables(eq(pathTableRef1), any()))
                .thenReturn(Optional.of(jsonModelTableRef1));

        SutParameterEntity sutParameter = new SutParameterEntity();
        sutParameter.setName("tablePrim");
        PotSessionParameterEntity parameter = new PotSessionParameterEntity();
        parameter.setParameterConfig(sutParameter);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_jsonResponseAndJoinTableAndNotExistIndexRefHeader_returnsException()
            throws GettingValueException, IOException {

        List<JsonDataColumnSettings> jsonDataColumnSettings = new ArrayList<>();
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Product.name", "$.product.name", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("id", "$.id", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Action", "$.action", Collections.emptyList()));

        String pathTableRef1 = "group.tableRef1";

        JsonJoinConditionSettings jsonJoinConditionSettings1 = new JsonJoinConditionSettings();
        jsonJoinConditionSettings1.getIdxPrimaryHeaderNames().add(2);
        jsonJoinConditionSettings1.setPathReferenceSutParameterName(pathTableRef1);
        // index outside
        jsonJoinConditionSettings1.getIdxReferenceHeaderNames().add(4);

        List<JsonJoinConditionSettings> jsonJoinConditionSettings = new ArrayList<>();
        jsonJoinConditionSettings.add(jsonJoinConditionSettings1);
        JsonHierarchyNodeNames hierarchyNodeNames = new JsonHierarchyNodeNames("$.id", "$.rootQuoteItemId",
                "$.parentQuoteItemId");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE, "$.quoteItem",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER,
                loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_TABLE_HIERARCHY_PRIMARY_FILE_PATH),
                true, jsonDataColumnSettings, hierarchyNodeNames, null,
                true, jsonJoinConditionSettings, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));

        JsonTable jsonModelTableRef1 = generateTableHierarchyForJoin();

        Mockito.when(executionVariablesService.getSourceWithJsonTableVariables(eq(pathTableRef1), any()))
                .thenReturn(Optional.of(jsonModelTableRef1));

        SutParameterEntity sutParameter = new SutParameterEntity();
        sutParameter.setName("tablePrim");
        PotSessionParameterEntity parameter = new PotSessionParameterEntity();
        parameter.setParameterConfig(sutParameter);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_jsonResponseAndJoinTableAndGroupingValue_returnsException()
            throws GettingValueException, IOException {
        List<String> groupingJsonPaths = Arrays.asList("$.product.name", "$.product");
        List<JsonDataColumnSettings> jsonDataColumnSettings = new ArrayList<>();
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Product.name", "$.product.name", groupingJsonPaths));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("id", "$.id", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Action", "$.action", Collections.emptyList()));

        String pathTableRef1 = "group.tableRef1";

        JsonJoinConditionSettings jsonJoinConditionSettings1 = new JsonJoinConditionSettings();
        jsonJoinConditionSettings1.getIdxPrimaryHeaderNames().add(0);
        jsonJoinConditionSettings1.setPathReferenceSutParameterName(pathTableRef1);
        jsonJoinConditionSettings1.getIdxReferenceHeaderNames().add(0);

        List<JsonJoinConditionSettings> jsonJoinConditionSettings = new ArrayList<>();
        jsonJoinConditionSettings.add(jsonJoinConditionSettings1);
        JsonHierarchyNodeNames hierarchyNodeNames = new JsonHierarchyNodeNames("$.id", "$.rootQuoteItemId",
                "$.parentQuoteItemId");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE, "$.quoteItem",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER,
                loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_TABLE_HIERARCHY_PRIMARY_FILE_PATH),
                true, jsonDataColumnSettings, hierarchyNodeNames, null,
                true, jsonJoinConditionSettings, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));

        JsonTable jsonModelTableRef1 = generateTableHierarchyForJoin();

        Mockito.when(executionVariablesService.getSourceWithJsonTableVariables(eq(pathTableRef1), any()))
                .thenReturn(Optional.of(jsonModelTableRef1));

        SutParameterEntity sutParameter = new SutParameterEntity();
        sutParameter.setName("tablePrim");
        PotSessionParameterEntity parameter = new PotSessionParameterEntity();
        parameter.setParameterConfig(sutParameter);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_jsonResponseAndJoinTableAndDifferentNumberKeyIndexes_returnsException()
            throws GettingValueException, IOException {
        List<JsonDataColumnSettings> jsonDataColumnSettings = new ArrayList<>();
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Product.name", "$.product.name", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("id", "$.id", Collections.emptyList()));
        jsonDataColumnSettings.add(
                new JsonDataColumnSettings("Action", "$.action", Collections.emptyList()));

        String pathTableRef1 = "group.tableRef1";

        JsonJoinConditionSettings jsonJoinConditionSettings1 = new JsonJoinConditionSettings();
        jsonJoinConditionSettings1.getIdxPrimaryHeaderNames().add(0);
        jsonJoinConditionSettings1.setPathReferenceSutParameterName(pathTableRef1);
        jsonJoinConditionSettings1.getIdxReferenceHeaderNames().add(0);
        jsonJoinConditionSettings1.getIdxReferenceHeaderNames().add(1);

        List<JsonJoinConditionSettings> jsonJoinConditionSettings = new ArrayList<>();
        jsonJoinConditionSettings.add(jsonJoinConditionSettings1);
        JsonHierarchyNodeNames hierarchyNodeNames = new JsonHierarchyNodeNames("$.id", "$.rootQuoteItemId",
                "$.parentQuoteItemId");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.HIERARCHY_TABLE, "$.quoteItem",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER,
                loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_TABLE_HIERARCHY_PRIMARY_FILE_PATH),
                true, jsonDataColumnSettings, hierarchyNodeNames, null,
                true, jsonJoinConditionSettings, false);
        Source source = new Source("", "", EngineType.SQL,
                "", Collections.singleton(jsonParseSettings));

        JsonTable jsonModelTableRef1 = generateTableHierarchyForJoin();

        Mockito.when(executionVariablesService.getSourceWithJsonTableVariables(eq(pathTableRef1), any()))
                .thenReturn(Optional.of(jsonModelTableRef1));

        SutParameterEntity sutParameter = new SutParameterEntity();
        sutParameter.setName("tablePrim");
        PotSessionParameterEntity parameter = new PotSessionParameterEntity();
        parameter.setParameterConfig(sutParameter);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

// =================================================================================================================
    /* VALIDATION PROCESS */

    @Test(expected = ValidationException.class)
    public void validateParameter_correctValueNotSpecified_throwValidationException() throws ValidationException,
            IOException {
        PotSessionParameterEntity parameter =
                DbMockEntity.generateJsonParameterForValidation(DisplayTypeTestConstants.JSON_CORRECT_VALUE,
                        ValidationType.CUSTOM, 1, null, 0);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);
    }

    @Test()
    public void validateParameter_passedValidation_parameterValidationStatusIsPassed() throws ValidationException,
            IOException {
        PotSessionParameterEntity parameter =
                DbMockEntity.generateJsonParameterForValidation(DisplayTypeTestConstants.JSON_CORRECT_VALUE,
                        ValidationType.CUSTOM, 1, DisplayTypeTestConstants.JSON_INCORRECT_VALUE);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void
    validateParameter_oneFailedValidationAndNoHighlightDifference_parameterValidationStatusIsFailedWithoutDiffMessages()
            throws ValidationException, IOException {
        PotSessionParameterEntity parameter =
                DbMockEntity.generateJsonParameterForValidation(DisplayTypeTestConstants.JSON_CORRECT_VALUE,
                        ValidationType.CUSTOM, 3, DisplayTypeTestConstants.JSON_INCORRECT_VALUE,
                        1);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertTrue(parameter.getArValues()
                .stream().allMatch(ar -> checkSimpleParameterValueForDiffMessages(ar, null, null)));
        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void
    validateParameter_allFailedValidationsAndNoHighlightDifference_parameterValidationStatusIsFailedWithoutDiffMessages()
            throws ValidationException, IOException {
        PotSessionParameterEntity parameter =
                DbMockEntity.generateJsonParameterForValidation(DisplayTypeTestConstants.JSON_CORRECT_VALUE,
                        ValidationType.CUSTOM, 3, DisplayTypeTestConstants.JSON_INCORRECT_VALUE,
                        0, 1, 2);

        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertTrue(parameter.getArValues()
                .stream().allMatch(ar -> checkSimpleParameterValueForDiffMessages(ar, null, null)));
        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    @Ignore
    public void
    validateParameter_oneFailedValidationAndHighlightDifference_parameterValidationStatusIsFailedWithDiffMessages()
            throws ValidationException, IOException {
        int incorrectResultIdx = 1;
        PotSessionParameterEntity parameter =
                DbMockEntity.generateJsonParameterForValidation(DisplayTypeTestConstants.JSON_CORRECT_VALUE,
                        ValidationType.CUSTOM, 3, DisplayTypeTestConstants.JSON_INCORRECT_VALUE,
                        incorrectResultIdx);

        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        List<AbstractValueObject> parameterValues = parameter.getArValues();
        for (int i = 0; i < parameterValues.size(); i++) {
            String arDiffs;
            if (i == incorrectResultIdx) {
                arDiffs = DisplayTypeTestConstants.HIGHLIGHTED_JSON_AR_WITH_DIFF_MESSAGES;
            } else {
                arDiffs = DisplayTypeTestConstants.HIGHLIGHTED_JSON_AR_WITH_NORMAL_DIFF_MESSAGES;
            }
            Assert.assertTrue(checkSimpleParameterValueForDiffMessages(parameterValues.get(i), arDiffs, null));
        }
        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    @Ignore
    public void
    validateParameter_allFailedValidationsAndHighlightDifference_parameterValidationStatusIsFailedWithDiffMessages()
            throws ValidationException, IOException {
        PotSessionParameterEntity parameter =
                DbMockEntity.generateJsonParameterForValidation(DisplayTypeTestConstants.JSON_CORRECT_VALUE,
                        ValidationType.CUSTOM, 3, DisplayTypeTestConstants.JSON_INCORRECT_VALUE,
                         0, 1, 2);

        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertTrue(parameter.getArValues().stream().allMatch(ar -> checkSimpleParameterValueForDiffMessages(ar,
                DisplayTypeTestConstants.HIGHLIGHTED_JSON_AR_WITH_DIFF_MESSAGES, null)));
        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void
    validateParameter_customValidationJsonTableWithTableSettings_ValidationStatusIsPassedWithoutDiffMessages()
            throws ValidationException, IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForJsonTableCustom("d8bc2dcbfae5",
                "d8bc2dcbfae6", "d8bc2dcbfae5", "d8bc2dcbfae6");

        TableValidationSettings tableValidationSettings = objectMapper.readValue("{\"keyColumns\":[\"iD\"],"
                + "\"groupingColumns\":[]}", TableValidationSettings.class);
        parameter.getParameterConfig().getErConfig().setTableValidationSettings(tableValidationSettings);
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        TableVsTableValidationInfo validationInfo = (TableVsTableValidationInfo) parameter.getValidationInfo();
        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
        Assert.assertTrue(validationInfo.getTableValidations().stream()
                .allMatch(validation -> validation.getDiffs().isEmpty()));
    }

    @Test()
    public void
    validateParameter_customValidationJsonTableWithTableSettingsBigRegister_ValidationStatusIsPassedWithoutDiffMessages()
            throws ValidationException, IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForJsonTableCustom("d8bc2dcbfae5",
                "d8bc2dcbfae6", "d8bc2dcbfae5", "d8bc2dcbfae6");
        TableValidationSettings tableValidationSettings = objectMapper.readValue("{\"keyColumns\":[\"Id\"],"
                + "\"groupingColumns\":[]}", TableValidationSettings.class);
        parameter.getParameterConfig().getErConfig().setTableValidationSettings(tableValidationSettings);
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        TableVsTableValidationInfo validationInfo = (TableVsTableValidationInfo) parameter.getValidationInfo();
        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
        Assert.assertTrue(validationInfo.getTableValidations().stream()
                .allMatch(validation -> validation.getDiffs().isEmpty()));
    }

    @Test()
    public void
    validateParameter_customValidationJsonTableAndHighlightDifference_parameterValidationStatusIsPassedWithoutDiffMessages()
            throws IOException, ValidationException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForJsonTableCustom("d8bc2dcbfae5",
                "d8bc2dcbfae5", "d8bc2dcbfae5", "d8bc2dcbfae5");
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        TableVsTableValidationInfo validationInfo = (TableVsTableValidationInfo) parameter.getValidationInfo();
        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
        Assert.assertTrue(validationInfo.getTableValidations().stream()
                .allMatch(validation -> validation.getDiffs().isEmpty()));
    }

    @Test()
    public void
    validateParameter_customValidationJsonTableAndHighlightDifference_parameterValidationStatusIsFailedWithDiffMessages()
            throws IOException, ValidationException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForJsonTableCustom("d8bc2dcbfae5",
                "d8bc2dcbfae6", "d8bc2dcbfae6", "d8bc2dcbfae6");
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        TableVsTableValidationInfo validationInfo = (TableVsTableValidationInfo) parameter.getValidationInfo();
        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
        Assert.assertTrue(validationInfo.getTableValidations().stream()
                .allMatch(validation -> Objects.nonNull(validation.getDiffs())));
    }

    @Test()
    public void validateParameter_plainValidationJsonTableAndHighlightDifference_parameterValidationStatusIsPassed
    () throws IOException, ValidationException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForJsonTablePlain(
                "passed", "passed", "ID");
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();
        Assert.assertTrue(validationInfo.getTableValidations().stream()
                .allMatch(validation -> validation.getDiffs().isEmpty()));
        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void
    validateParameter_plainValidationJsonTableAndHighlightDifference_parameterValidationStatusIsFailedWithDiffMessages() throws IOException, ValidationException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForJsonTablePlain(
                "failed", "passed", "ID");
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        ActualTablesValidationInfo validationInfo = (ActualTablesValidationInfo) parameter.getValidationInfo();
        List<TableValidationInfo> tableValidations = validationInfo.getTableValidations();
        for (TableValidationInfo tableValidationInfo : tableValidations) {
            Assert.assertEquals(1, tableValidationInfo.getDiffs().size());
        }
        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
    }

    @Test(expected = ValidationException.class)
    public void
    validateParameter_plainValidationJsonTableAndHighlightDifference_ValidationExceptionWhenValidateGroupCell()
    throws IOException, ValidationException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForJsonTablePlain(
                "failed", "passed", "groupColumnName");
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);
    }

    @Test()
    public void validateParameter_plainValidationJsonRawAndHighlightDifference_parameterValidationStatusIsPassed()
    throws IOException, ValidationException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForJsonRawPlain("PASSED",
                "PASSED");
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void validateParameter_plainValidationJsonRawAndHighlightDifference_parameterValidationStatusIsFailed()
    throws IOException, ValidationException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForJsonRawPlain("PASSED",
                "FAILED");
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
    }

    @Test(expected = ValidationException.class)
    public void
    validateParameter_plainValidationJsonRawAndHighlightDifference_ValidationExceptionWhenErValueIsEmptyOrNull()
    throws IOException, ValidationException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForJsonRawPlain("PASSED",
                null);
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);
    }

    @Test()
    public void validateParameter_BvValidationJsonParametersJson_statusFailed() throws IOException,
            ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testCaseId = "61abd5ac-efbe-49cc-b7f5-925a7f543481";
        PotSessionParameterEntity parameter = DbMockEntity.generateJsonRawBvValidationParameter(false,
                "json message", testCaseId);

        when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        when(parameterExecutionContext.getSessionConfiguration().getProjectId())
                .thenReturn(projectId);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "json message";


        PotSessionParameterEntity expectedParameter = DbMockEntity.generateExpectedJsonRawBvValidationParameter(false,
                ValidationStatus.FAILED, "json message", testCaseId);
        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);

        String compareResponse = getBvCompareResponse("SIMILAR", testRunId);

        String highLightsResponse = getBvGetHighLightsResponse(validationObjectName,
                "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48cHJlPns8YnI+ICAidGVzdCIgOiA8c3BhbiBkYXRhLWJ"
                        + "sb2NrLWlkPSJwYy1oaWdobGlnaHQtYmxvY2siIGNsYXNzPSJTSU1JTEFSIiB0aXRsZT0iU2ltaWxhciBwcm9wZXJ0"
                        + "eSBvciBvYmplY3QiPiIxMjg4Ijwvc3Bhbj4sPGJyPiAgInN0YXR1czAiIDogImFjdGl2ZSI8YnI+fTwvcHJlPg==",
                "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48cHJlPns8YnI+ICAidGVzdCIgOiA8c3BhbiBkYXRhLWJsb2"
                        +
                        "NrLWlkPSJwYy1oaWdobGlnaHQtYmxvY2siIGNsYXNzPSJTSU1JTEFSIiB0aXRsZT0iU2ltaWxhciBwcm9wZXJ0eSBvci"
                        + "BvYmplY3QiPiIxMjg4OCI8L3NwYW4+LDxicj4gICJzdGF0dXMwIiA6ICJhY3RpdmUiPGJyPn08L3ByZT4=",
                "ignorePropertiesV2");

        TestRunCreationResponse responseTr = objectMapper.readValue(createResponse, TestRunCreationResponse.class);
        ResponseEntity<Object> responseTrEntity = new ResponseEntity<>(responseTr, HttpStatus.ACCEPTED);
        when(bulkValidatorPublicFeignClient.createTr(any(), anyString())).thenReturn(responseTrEntity);

        ResponseEntity<String>  compareRsEntity = new ResponseEntity<>(compareResponse, HttpStatus.ACCEPTED);
        when(bulkValidatorApiFeignClient.compare(any(), anyString())).thenReturn(compareRsEntity);

        ResponseEntity<String>  highLightsResponseEntity = new ResponseEntity<>(highLightsResponse, HttpStatus.ACCEPTED);
        when(bulkValidatorValidatorFeignClient.highlightByIds(any(), anyString())).thenReturn(highLightsResponseEntity);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
        Assert.assertEquals(objectMapper.writeValueAsString(expectedParameter),
                objectMapper.writeValueAsString(parameter));
    }

    @Test()
    public void validateParameter_BvValidationJsonParametersJson_statusPassed() throws IOException,
            ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testCaseId = "61abd5ac-efbe-49cc-b7f5-925a7f543481";
        PotSessionParameterEntity parameter = DbMockEntity.generateJsonRawBvValidationParameter(false,
                "json message", testCaseId);

        when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        when(parameterExecutionContext.getSessionConfiguration().getProjectId())
                .thenReturn(projectId);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "json message";

        PotSessionParameterEntity expectedParameter = DbMockEntity.generateExpectedJsonRawBvValidationParameter(false,
                ValidationStatus.PASSED, "json message", testCaseId);

        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);

        String compareResponse = getBvCompareResponse("IDENTICAL", testRunId);

        String highLightsResponse = getBvGetHighLightsResponse(validationObjectName,
                "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48cHJlPns8YnI+ICAidGVzdCIgOiA8c3BhbiBkYXRhLWJ"
                        + "sb2NrLWlkPSJwYy1oaWdobGlnaHQtYmxvY2siIGNsYXNzPSJTSU1JTEFSIiB0aXRsZT0iU2ltaWxhciBwcm9wZXJ0"
                        + "eSBvciBvYmplY3QiPiIxMjg4Ijwvc3Bhbj4sPGJyPiAgInN0YXR1czAiIDogImFjdGl2ZSI8YnI+fTwvcHJlPg==",
                "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48cHJlPns8YnI+ICAidGVzdCIgOiA8c3BhbiBkYXRhLWJsb2"
                        +
                        "NrLWlkPSJwYy1oaWdobGlnaHQtYmxvY2siIGNsYXNzPSJTSU1JTEFSIiB0aXRsZT0iU2ltaWxhciBwcm9wZXJ0eSBvci"
                        + "BvYmplY3QiPiIxMjg4OCI8L3NwYW4+LDxicj4gICJzdGF0dXMwIiA6ICJhY3RpdmUiPGJyPn08L3ByZT4=",
                "ignorePropertiesV2");

        TestRunCreationResponse responseTr = objectMapper.readValue(createResponse, TestRunCreationResponse.class);
        ResponseEntity<Object> responseTrEntity = new ResponseEntity<>(responseTr, HttpStatus.ACCEPTED);
        when(bulkValidatorPublicFeignClient.createTr(any(), anyString())).thenReturn(responseTrEntity);

        ResponseEntity<String>  compareRsEntity = new ResponseEntity<>(compareResponse, HttpStatus.ACCEPTED);
        when(bulkValidatorApiFeignClient.compare(any(), anyString())).thenReturn(compareRsEntity);

        ResponseEntity<String>  highLightsResponseEntity = new ResponseEntity<>(highLightsResponse, HttpStatus.ACCEPTED);
        when(bulkValidatorValidatorFeignClient.highlightByIds(any(), anyString())).thenReturn(highLightsResponseEntity);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
        Assert.assertEquals(objectMapper.writeValueAsString(expectedParameter),
                objectMapper.writeValueAsString(parameter));
    }

    @Test()
    public void validateParameter_BvValidationJsonParametersJsonScheme_statusPassed() throws IOException,
            ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testCaseId = "61abd5ac-efbe-49cc-b7f5-925a7f543481";
        PotSessionParameterEntity parameter = DbMockEntity.generateJsonRawBvValidationParameter(true,
                "json validSchema", testCaseId);

        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "json validSchema";

        PotSessionParameterEntity expectedParameter = DbMockEntity.generateExpectedJsonRawBvValidationParameter(true,
                ValidationStatus.PASSED, "json validSchema", testCaseId);

        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);

        String compareResponse = getBvCompareResponse("IDENTICAL", testRunId);

        String highLightsResponse = getBvGetHighLightsResponse(validationObjectName,
                "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48cHJlPns8YnI+ICAicm93cyIgOiBbIFsgIjcxNjA5NjM3Mz"
                        +
                        "Y0NzAyMjY4NDIiLCAiMjAyMS8wNy8xOSIsICJBUVVPUyBSNUcgRWFydGggQmx1ZSIgXSwgWyAiNzE2MDk2MzczNjQ3M"
                        +
                        "DIyNjg0MiIsICIyMDIxLzA3LzE5IiwgIkdhbGF4eSBBNyBCbHVlIiBdLCBbICIzMTYwOTYzODg4NTIwMjI5NDA1Iiwg"
                        + "IjIwMjEvMDcvMTkiLCAiQVFVT1MgUjVHIEVhcnRoIEJsdWUiIF0gXTxicj59PC9wcmU+",
                "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48cHJlPns8YnI"
                +"ICAidGVzdCIgOiAiZGZmIjxicj59PC9wcmU",
                "validateSchema");

        TestRunCreationResponse responseTr = objectMapper.readValue(createResponse, TestRunCreationResponse.class);
        ResponseEntity<Object> responseTrEntity = new ResponseEntity<>(responseTr, HttpStatus.ACCEPTED);
        when(bulkValidatorPublicFeignClient.createTr(any(), anyString())).thenReturn(responseTrEntity);

        ResponseEntity<String>  compareRsEntity = new ResponseEntity<>(compareResponse, HttpStatus.ACCEPTED);
        when(bulkValidatorApiFeignClient.compare(any(), anyString())).thenReturn(compareRsEntity);

        ResponseEntity<String>  highLightsResponseEntity = new ResponseEntity<>(highLightsResponse, HttpStatus.ACCEPTED);
        when(bulkValidatorValidatorFeignClient.highlightByIds(any(), anyString())).thenReturn(highLightsResponseEntity);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
        Assert.assertEquals(objectMapper.writeValueAsString(expectedParameter),
                objectMapper.writeValueAsString(parameter));
    }

    @Test(expected = ValidationException.class)
    public void validateParameter_BvValidationWithoutTestCaseId_validationException() throws IOException,
            ValidationException {
        String testCaseId = "";
        PotSessionParameterEntity parameter = DbMockEntity.generateJsonRawBvValidationParameter(true,
                "json validSchema", testCaseId);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(parameterExecutionContext.getSessionConfiguration().getBulkValidatorSettings())
                .thenReturn(objectMapper.readValue(BV_SETTINGS, BulkValidatorProjectSettings.class));
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);
    }

    @Test(expected = ValidationException.class)
    public void validateParameter_BvValidationWithoutValidationObjectTest_validationException() throws IOException,
            ValidationException {
        String testCaseId = "61abd5ac-efbe-49cc-b7f5-925a7f543481";
        PotSessionParameterEntity parameter = DbMockEntity.generateJsonRawBvValidationParameter(false,
                "", testCaseId);

        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(parameterExecutionContext.getSessionConfiguration().getBulkValidatorSettings())
                .thenReturn(objectMapper.readValue(BV_SETTINGS, BulkValidatorProjectSettings.class));
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);
    }

    @Test(expected = ValidationException.class)
    public void validateParameter_BvValidationWrongTestCaseId_validationException() throws IOException,
            ValidationException {
        String testCaseId = "61abd5ac-efbe-49cc-b7f5-925a7f543481";
        PotSessionParameterEntity parameter = DbMockEntity.generateJsonRawBvValidationParameter(true,
                "json validSchema", testCaseId);

        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(parameterExecutionContext.getSessionConfiguration().getBulkValidatorSettings())
                .thenReturn(objectMapper.readValue(BV_SETTINGS, BulkValidatorProjectSettings.class));
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        String createResponse = "{\"message\":\"Testrun isn't created. Please check tcId (tcName) attribute.\"}";

        Object responseTr = objectMapper.readValue(createResponse, Object.class);
        ResponseEntity<Object> responseTrEntity = new ResponseEntity<>(responseTr, HttpStatus.ACCEPTED);
        when(bulkValidatorPublicFeignClient.createTr(any(), anyString())).thenReturn(responseTrEntity);


        jsonDisplayTypeService.validateParameter(parameterExecutionContext);
    }

    @Test(expected = ValidationException.class)
    public void validateParameter_BvValidationWrongValidationObjectName_validationException() throws IOException,
            ValidationException {
        String testCaseId = "61abd5ac-efbe-49cc-b7f5-925a7f543481";
        PotSessionParameterEntity parameter = DbMockEntity.generateJsonRawBvValidationParameter(true,
                "json validSchema", testCaseId);

        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(parameterExecutionContext.getSessionConfiguration().getBulkValidatorSettings())
                .thenReturn(objectMapper.readValue(BV_SETTINGS, BulkValidatorProjectSettings.class));
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        String createResponse = "{\n"
                + "\t\"trId\": \"0c5d1ea8-d32f-4a24-a758-1695dcf61350\",\n"
                + "\t\"context\": {\n"
                + "\t\t\"inputParameters\": [],\n"
                + "\t\t\"values\": [],\n"
                + "\t\t\"dataSource\": [],\n"
                + "\t\t\"usedSources\": [],\n"
                + "\t\t\"parameterStatuses\": null,\n"
                + "\t\t\"buildInfo\": null,\n"
                + "\t\t\"readObjects\": [],\n"
                + "\t\t\"errors\": [],\n"
                + "\t\t\"rules\": [],\n"
                + "\t\t\"oldProcessContext\": false\n"
                + "\t},\n"
                + "\t\"created\": null,\n"
                + "\t\"tcId\": \"61abd5ac-efbe-49cc-b7f5-925a7f543481\",\n"
                + "\t\"trStatus\": null,\n"
                + "\t\"run\": null,\n"
                + "\t\"validationContext\": null,\n"
                + "\t\"summaryValidationResult\": null,\n"
                + "\t\"contextAsString\": \"{\\\"inputParameters\\\":[],\\\"values\\\":[],\\\"dataSource\\\":[],"
                + "\\\"usedSources\\\":[],\\\"readObjects\\\":[],\\\"errors\\\":[],\\\"rules\\\":[]}\"\n"
                + "}";

        doReturn(objectMapper.readValue(createResponse, TestRunCreationResponse.class))
                .when(bulkValidatorRepository).createTestRun(any(), any(), any());

        Object responseTr = objectMapper.readValue(createResponse, Object.class);
        ResponseEntity<Object> responseTrEntity = new ResponseEntity<>(responseTr, HttpStatus.ACCEPTED);
        when(bulkValidatorPublicFeignClient.createTr(any(), anyString())).thenReturn(responseTrEntity);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);
    }

    @Test
    public void validateParameter_BvValidationWrongValidationObjectNameByName_statusPassed() throws IOException,
            ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testCaseId = "${Test}";
        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "json validSchema";
        PotSessionParameterEntity parameter = DbMockEntity.generateJsonRawBvValidationParameter(true,
                "json validSchema", testCaseId);

        PotSessionParameterEntity expectedParameter = DbMockEntity.generateExpectedJsonRawBvValidationParameter(true,
                ValidationStatus.PASSED, "json validSchema", testCaseId);

        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);
        String compareResponse = getBvCompareResponse("IDENTICAL", testRunId);
        String highLightsResponse = getBvGetHighLightsResponse(validationObjectName,
                "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48cHJlPns8YnI+ICAicm93cyIgOiBbIFsgIjcxNjA5NjM3Mz"
                        +
                        "Y0NzAyMjY4NDIiLCAiMjAyMS8wNy8xOSIsICJBUVVPUyBSNUcgRWFydGggQmx1ZSIgXSwgWyAiNzE2MDk2MzczNjQ3M"
                        +
                        "DIyNjg0MiIsICIyMDIxLzA3LzE5IiwgIkdhbGF4eSBBNyBCbHVlIiBdLCBbICIzMTYwOTYzODg4NTIwMjI5NDA1Iiwg"
                        + "IjIwMjEvMDcvMTkiLCAiQVFVT1MgUjVHIEVhcnRoIEJsdWUiIF0gXTxicj59PC9wcmU+",
                "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48cHJlPns8YnI"
                +"ICAidGVzdCIgOiAiZGZmIjxicj59PC9wcmU",
                "validateSchema");


        when(bulkValidatorPublicFeignClient.getTcIdsByTcNames(any(), any()))
                .thenReturn(new ResponseEntity<>(createGettingResponse(true, false, false), HttpStatus.OK));
        when(executionVariablesService.getSourceWithExecutionVariables(any(), any())).thenReturn("TestCaseName");
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        TestRunCreationResponse responseTr = objectMapper.readValue(createResponse, TestRunCreationResponse.class);
        ResponseEntity<Object> responseTrEntity = new ResponseEntity<>(responseTr, HttpStatus.ACCEPTED);
        when(bulkValidatorPublicFeignClient.createTr(any(), anyString())).thenReturn(responseTrEntity);

        ResponseEntity<String>  compareRsEntity = new ResponseEntity<>(compareResponse, HttpStatus.ACCEPTED);
        when(bulkValidatorApiFeignClient.compare(any(), anyString())).thenReturn(compareRsEntity);

        ResponseEntity<String>  highLightsResponseEntity = new ResponseEntity<>(highLightsResponse, HttpStatus.ACCEPTED);
        when(bulkValidatorValidatorFeignClient.highlightByIds(any(), anyString())).thenReturn(highLightsResponseEntity);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
        Assert.assertEquals(objectMapper.writeValueAsString(expectedParameter),
                objectMapper.writeValueAsString(parameter));
    }

    @Test
    public void validateParameter_BvValidationWrongValidationObjectNameByName_StatusPassedGetFirstId() throws
    IOException,
            ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testCaseId = "${Test}";
        String testRunId = "0d7df690-0a3a-48cc-8c73-5974ba2faee5";
        String externalId = "a7e120ef-7f09-4054-8c00-980cc744aa95";
        String validationObjectName = "json validSchema";
        PotSessionParameterEntity parameter = DbMockEntity.generateJsonRawBvValidationParameter(true,
                "json validSchema", testCaseId);
        PotSessionParameterEntity expectedParameter = DbMockEntity.generateExpectedJsonRawBvValidationParameter(true,
                ValidationStatus.PASSED, "json validSchema", testCaseId);

        String createResponse = getBvCreateTestRunResponse(testRunId, externalId, validationObjectName);
        String compareResponse = getBvCompareResponse("IDENTICAL", testRunId);
        String highLightsResponse = getBvGetHighLightsResponse(validationObjectName,
                "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48cHJlPns8YnI+ICAicm93cyIgOiBbIFsgIjcxNjA5NjM3Mz"
                        +
                        "Y0NzAyMjY4NDIiLCAiMjAyMS8wNy8xOSIsICJBUVVPUyBSNUcgRWFydGggQmx1ZSIgXSwgWyAiNzE2MDk2MzczNjQ3M"
                        +
                        "DIyNjg0MiIsICIyMDIxLzA3LzE5IiwgIkdhbGF4eSBBNyBCbHVlIiBdLCBbICIzMTYwOTYzODg4NTIwMjI5NDA1Iiwg"
                        + "IjIwMjEvMDcvMTkiLCAiQVFVT1MgUjVHIEVhcnRoIEJsdWUiIF0gXTxicj59PC9wcmU+",
                "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48cHJlPns8YnI"
                +"ICAidGVzdCIgOiAiZGZmIjxicj59PC9wcmU",
                "validateSchema");

        when(bulkValidatorPublicFeignClient.getTcIdsByTcNames(any(), any()))
                .thenReturn(new ResponseEntity<>(createGettingResponse(true, false, true), HttpStatus.OK));
        when(executionVariablesService.getSourceWithExecutionVariables(any(), any())).thenReturn("TestCaseName");
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        TestRunCreationResponse responseTr = objectMapper.readValue(createResponse, TestRunCreationResponse.class);
        ResponseEntity<Object> responseTrEntity = new ResponseEntity<>(responseTr, HttpStatus.ACCEPTED);
        when(bulkValidatorPublicFeignClient.createTr(any(), anyString())).thenReturn(responseTrEntity);

        ResponseEntity<String>  compareRsEntity = new ResponseEntity<>(compareResponse, HttpStatus.ACCEPTED);
        when(bulkValidatorApiFeignClient.compare(any(), anyString())).thenReturn(compareRsEntity);

        ResponseEntity<String>  highLightsResponseEntity = new ResponseEntity<>(highLightsResponse, HttpStatus.ACCEPTED);
        when(bulkValidatorValidatorFeignClient.highlightByIds(any(), anyString())).thenReturn(highLightsResponseEntity);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
        Assert.assertEquals(objectMapper.writeValueAsString(expectedParameter),
                objectMapper.writeValueAsString(parameter));
    }

    @Test(expected = ValidationException.class)
    public void validateParameter_BvValidationWrongValidationObjectNameByName_ValidationExceptionNotFoundId()
    throws IOException,
            ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testCaseId = "${Test}";
        PotSessionParameterEntity parameter = DbMockEntity.generateJsonRawBvValidationParameter(true,
                "json validSchema", testCaseId);
        when(executionVariablesService.getSourceWithExecutionVariables(any(), any())).thenReturn("TestCaseName");
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);
        when(bulkValidatorPublicFeignClient.getTcIdsByTcNames(any(), any()))
                .thenReturn(new ResponseEntity<>(createGettingResponse(false, true, false), HttpStatus.OK));

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);
    }

    @Test(expected = ValidationException.class)
    public void validateParameter_ExceptionInFeignClient_ValidationException()
            throws IOException,
            ValidationException {
        UUID projectId = UUID.fromString("e75c4d6c-3814-48e7-b5a2-d8636ab91d38");
        String testCaseId = "${Test}";
        PotSessionParameterEntity parameter = DbMockEntity.generateJsonRawBvValidationParameter(true,
                "json validSchema", testCaseId);

        when(bulkValidatorPublicFeignClient.getTcIdsByTcNames(any(), any()))
                .thenReturn(new ResponseEntity<>(createGettingResponse(true, false, true), HttpStatus.OK));
        when(executionVariablesService.getSourceWithExecutionVariables(any(), any())).thenReturn("TestCaseName");
        when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        when(parameterExecutionContext.getSessionConfiguration().getProjectId()).thenReturn(projectId);
        when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);
        when(bulkValidatorPublicFeignClient.createTr(any(), anyString())).thenThrow(InvalidBulkValidatorApiUsageException.class);

        jsonDisplayTypeService.validateParameter(parameterExecutionContext);
    }

    @Test
    public void getValueFromSource_jsonResponseAndEngineTypeSsh_returnsParsedJsonAsSimpleValue()
            throws GettingValueException, IOException, JSchException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "$.validFor",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, Collections.emptyList()
                , null, null, false, null, false);
        Source source = new Source("", "", EngineType.SSH,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(sshRepository.executeCommandSsh(any(), anyString()))
                .thenReturn(loadFileToString(DisplayTypeTestConstants.JSON_RESPONSE_FILE_PATH));

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
        parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(DisplayTypeTestConstants.JSON_NODE_CORRECT_VALUE,
                ((SimpleValueObject) actualResult).getValue());
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_jsonResponseAndEngineTypeSsh_throwsGettingValueException()
            throws GettingValueException, JSchException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "$.validFor",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, Collections.emptyList()
                , null, null, false, null, false);
        Source source = new Source("", "", EngineType.SSH,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(sshRepository.executeCommandSsh(any(), anyString()))
                .thenReturn("Simple string");

        jsonDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test()
    public void getValueFromSource_jsonResponseAndEngineTypeSsh_returnsStringAsSimpleValue()
            throws GettingValueException, JSchException {
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.RAW, "$",
                DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false, Collections.emptyList()
                , null, null, false, null, false);
        Source source = new Source("", "", EngineType.SSH,
                "", Collections.singleton(jsonParseSettings));
        Mockito.when(sshRepository.executeCommandSsh(any(), anyString()))
                .thenReturn("Simple string");
        String erValue = "Simple string";

        AbstractValueObject actualResult = jsonDisplayTypeService.getValueFromSource(source,
        parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(erValue, ((SimpleValueObject) actualResult).getValue());
    }
}
