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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.keycloak.util.JsonSerialization.mapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.skyscreamer.jsonassert.JSONAssert;

import org.qubership.atp.svp.core.enums.JsonParseViewType;
import org.qubership.atp.svp.core.exceptions.VariableException;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.impl.HttpSettings;
import org.qubership.atp.svp.model.impl.JsonDataColumnSettings;
import org.qubership.atp.svp.model.impl.JsonHierarchyNodeNames;
import org.qubership.atp.svp.model.impl.JsonParseSettings;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.JasonTableExecutionVariable;
import org.qubership.atp.svp.model.pot.SimpleExecutionVariable;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.service.direct.displaytype.DisplayTypeTestConstants;

public class ExecutionVariablesServiceImplTest {

    @Spy
    ExecutionVariablesServiceImpl executionVariablesService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getSourceWithExecutionVariables_withKeyAndCommonParameterInsensitiveVariables_returnsScriptWithParameterValues() {
        String initialScript = "select ${KeY1}, ${common1} from dual where param1 = ${key2} and param2 = ${common2}";
        String erScript = "select keyValue1, commonValue1 from dual where param1 = keyValue2 and param2 = commonValue2";
        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();
        executionVariables.put("key1",new SimpleExecutionVariable("key1", "keyValue1"));
        executionVariables.put("KEy2",new SimpleExecutionVariable("KEy2", "keyValue2"));
        executionVariables.put("COMMON1",new SimpleExecutionVariable("COMMON1", "commonValue1"));
        executionVariables.put("common2",new SimpleExecutionVariable("common2", "commonValue2"));
        String arScript = executionVariablesService.getSourceWithExecutionVariables(initialScript, executionVariables);
        Assert.assertEquals(erScript, arScript);
    }

    @Test(expected = VariableException.class)
    public void getSourceWithExecutionVariables_withoutVariables_returnsInitialScript() {
        String initialScript = "select ${key1}, ${common1} from dual where param1 = ${key2} and param2 = ${common2}";
        String arScript = executionVariablesService.getSourceWithExecutionVariables(initialScript,
                new ConcurrentHashMap<>());
        Assert.assertEquals(initialScript, arScript);
    }

    @Test
    public void getHttpSettingsWithExecutionVariables_HttpSettingsWithAllFieldsVariables_returnsHttpSettingsWithParameterValues() {
        String mapKeyErParam = "key = commonValue1";
        String mapValueErParam = "value = commonValue2";
        HttpSettings erHttpSettings = new HttpSettings();
        erHttpSettings.setBody("body = keyValue1 row$=keyValue2 ");
        erHttpSettings.setQuery("Query = commonValue1, commonValue2");
        erHttpSettings.setUrlEncodedBody(new HashMap<String, String>() {{
            put(mapKeyErParam, mapValueErParam);
        }});
        erHttpSettings.setQueryParams(new HashMap<String, String>() {{
            put(mapKeyErParam, mapValueErParam);
        }});
        erHttpSettings.setHeaders(new HashMap<String, String>() {{
            put(mapKeyErParam, mapValueErParam);
        }});

        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();
        executionVariables.put("key1",new SimpleExecutionVariable("key1", "keyValue1"));
        executionVariables.put("KEy2",new SimpleExecutionVariable("KEy2", "keyValue2"));
        executionVariables.put("COMMON1",new SimpleExecutionVariable("COMMON1", "commonValue1"));
        executionVariables.put("common2",new SimpleExecutionVariable("common2", "commonValue2"));

        String mapKeyParam = "key = ${common1}";
        String mapValueParam = "value = ${common2}";
        HttpSettings httpSettings = new HttpSettings();
        httpSettings.setBody("body = ${key1} row$=${key2} ");
        httpSettings.setQuery("Query = ${common1}, ${common2}");
        httpSettings.setUrlEncodedBody(new HashMap<String, String>() {{
            put(mapKeyParam, mapValueParam);
        }});
        httpSettings.setQueryParams(new HashMap<String, String>() {{
            put(mapKeyParam, mapValueParam);
        }});
        httpSettings.setHeaders(new HashMap<String, String>() {{
            put(mapKeyParam, mapValueParam);
        }});

        HttpSettings arHttpSettings =
                executionVariablesService.getHttpSettingsWithExecutionVariables(executionVariables, httpSettings);

        assertThat(erHttpSettings, samePropertyValuesAs(arHttpSettings));
    }

    @Test
    public void getHttpSettingsWithVariables_HttpSettingsWithAllFieldsNull_returnsHttpSettingsNotException() {
        HttpSettings erHttpSettings = new HttpSettings();
        erHttpSettings.setBody("");
        erHttpSettings.setQuery(null);
        erHttpSettings.setUrlEncodedBody(null);
        erHttpSettings.setQueryParams(null);
        erHttpSettings.setHeaders(null);

        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();
        executionVariables.put("key1",new SimpleExecutionVariable("key1", "keyValue1"));

        HttpSettings httpSettings = new HttpSettings();
        httpSettings.setBody("");
        httpSettings.setQuery(null);
        httpSettings.setUrlEncodedBody(null);
        httpSettings.setQueryParams(null);
        httpSettings.setHeaders(null);

        HttpSettings arHttpSettings =
                executionVariablesService.getHttpSettingsWithExecutionVariables(executionVariables, httpSettings);

        assertThat(erHttpSettings, samePropertyValuesAs(arHttpSettings));
    }

    @Test
    public void getJsonParseSettingsWithExecutionVariables_JsonParseSettingsWithJsonPathVariables_returnsJsonParseSettingsWithJsonPathValues() {
        String initialPath = "$.select.${key1}.${common1}";
        String erPath = "$.select.keyValue1.commonValue1";

        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();
        executionVariables.put("key1",new SimpleExecutionVariable("key1", "keyValue1"));
        executionVariables.put("common1",new SimpleExecutionVariable("common1", "commonValue1"));

        JsonParseSettings erJsonParseSettings = new JsonParseSettings(null,
                erPath, null, "", false,
                null, null, null,
                false, null, false);
        JsonParseSettings jsonParseSettings = new JsonParseSettings(null,
                initialPath, null, "", false,
                null, null, null,
                false, null, false);

        JsonParseSettings arJsonParseSettings =
                executionVariablesService.getJsonParseSettingsWithExecutionVariables(jsonParseSettings,
                        executionVariables);

        assertThat(erJsonParseSettings, samePropertyValuesAs(arJsonParseSettings));
    }

    @Test
    public void getJsonParseSettingsWithExecutionVariables_JsonParseSettingsWithJsonPathVariablesWithSpecSymbol_returnsJsonParseSettingsWithJsonPathValues() {
        String initialPath = "$.select.${key1}.${$common1}";
        String erPath = "$.select.keyValue1.$commonValue1";

        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();
        executionVariables.put("key1",new SimpleExecutionVariable("key1", "keyValue1"));
        executionVariables.put("KEy2",new SimpleExecutionVariable("$common1", "$commonValue1"));

        JsonParseSettings erJsonParseSettings = new JsonParseSettings(null,
                erPath, null, "", false,
                null, null, null,
                false, null, false);
        JsonParseSettings jsonParseSettings = new JsonParseSettings(null,
                initialPath, null, "", false,
                null, null, null,
                false, null, false);

        JsonParseSettings arJsonParseSettings =
                executionVariablesService.getJsonParseSettingsWithExecutionVariables(jsonParseSettings,
                                executionVariables);

        assertThat(erJsonParseSettings, samePropertyValuesAs(arJsonParseSettings));
    }

    @Test
    public void getJsonParseSettingsWithExecutionVariables_JsonParseSettingsWithAllVariables_returnsJsonParseSettingsWithJsonPathValues() {
        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();
        executionVariables.put("key1",new SimpleExecutionVariable("key1", "keyValue1"));
        executionVariables.put("KEy2",new SimpleExecutionVariable("KEy2", "keyValue2"));
        executionVariables.put("COMMON1",new SimpleExecutionVariable("COMMON1", "commonValue1"));
        executionVariables.put("common2",new SimpleExecutionVariable("common2", "commonValue2"));

        String initialPath = "$.select.${key1}.${common1}";
        List<JsonDataColumnSettings> initJsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER.${key1}.${common1}",
                        "$.${key1}.${common1}", Collections.emptyList()));
        List<String> initHierarchyTreeObjectNodeNames = Arrays.asList("relatedOrders.${key1}.${common1}",
                "orderItems.${key1}.${common1}");
        JsonHierarchyNodeNames initHierarchyNodeNames = new JsonHierarchyNodeNames("$.id.${key1}.${common1}",
                "$.rootQuoteItemId.${key1}.${common1}",
                "$.parentQuoteItemId.${key1}.${common1}");
        JsonParseSettings jsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE,
                initialPath, DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                initJsonDataColumnSettings, initHierarchyNodeNames, initHierarchyTreeObjectNodeNames, false, null, false);

        String erPath = "$.select.keyValue1.commonValue1";
        List<JsonDataColumnSettings> erJsonDataColumnSettings = Collections.singletonList(
                new JsonDataColumnSettings("HEADER.keyValue1.commonValue1",
                        "$.keyValue1.commonValue1", Collections.emptyList()));
        List<String> erHierarchyTreeObjectNodeNames = Arrays.asList("relatedOrders.keyValue1.commonValue1",
                "orderItems.keyValue1.commonValue1");
        JsonHierarchyNodeNames erHierarchyNodeNames = new JsonHierarchyNodeNames("$.id.keyValue1.commonValue1",
                "$.rootQuoteItemId.keyValue1.commonValue1",
                "$.parentQuoteItemId.keyValue1.commonValue1");
        JsonParseSettings erJsonParseSettings = new JsonParseSettings(JsonParseViewType.TABLE,
                erPath, DisplayTypeTestConstants.GROUP_NAME_DIVIDER, "", false,
                erJsonDataColumnSettings, erHierarchyNodeNames, erHierarchyTreeObjectNodeNames, false, null, false);

        JsonParseSettings arJsonParseSettings =
                executionVariablesService.getJsonParseSettingsWithExecutionVariables(jsonParseSettings, executionVariables);

        assertThat(erJsonParseSettings, samePropertyValuesAs(arJsonParseSettings));
    }

    @Test
    public void getJsonParseSettingsWithExecutionVariables_JsonParseSettingsWithJsonPathNull_returnsJsonParseSettingsWithJsonPathNotException() {
        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();
        executionVariables.put("key1",new SimpleExecutionVariable("key1", "keyValue1"));
        executionVariables.put("KEy2",new SimpleExecutionVariable("common1", "commonValue1"));

        JsonParseSettings erJsonParseSettings = new JsonParseSettings(null,
                null, null, "", false,
                null, null, null,
                false, null, false);
        JsonParseSettings jsonParseSettings = new JsonParseSettings(null,
                null, null, "", false,
                null, null, null,
                false, null, false);
        JsonParseSettings arJsonParseSettings =
                executionVariablesService.getJsonParseSettingsWithExecutionVariables(jsonParseSettings,
                        executionVariables);

        assertThat(erJsonParseSettings, samePropertyValuesAs(arJsonParseSettings));
    }

    @Test
    public void getVariablesFromEnvironment_withEnvironment_returnsSetExecutionVariable() throws IOException,
            JSONException {
        String environments_file_path = "src/test/resources/test_data/executionVariables/environments-FullModel"
                + ".json";
        String executionVariable_file_path = "src/test/resources/test_data/executionVariables"
                + "/setExecutionVariablesEnv.json";
        String envResponseJson = new String(Files.readAllBytes(Paths.get(environments_file_path)));

        String erExecutionVariable = new String(Files.readAllBytes(Paths.get(executionVariable_file_path)));
        Environment environment = mapper.readValue(envResponseJson, Environment.class);
        ConcurrentHashMap<String, ExecutionVariable> arExecutionVariables = new ConcurrentHashMap<>();
        executionVariablesService.getVariablesFromEnvironment(environment, arExecutionVariables);

        String arExecutionVariable = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arExecutionVariables);
        JSONAssert.assertEquals(erExecutionVariable, arExecutionVariable, false);
    }

    @Test
    public void addExecutionVariables_withJsonTableObjectAndOverwriteVar_returnsVoid() {
        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();
        JsonTable jsonTableValue1 = new JsonTable("table1", new ArrayList<>(), new ArrayList<>());
        JsonTable jsonTableValue2 = new JsonTable("table2", new ArrayList<>(), new ArrayList<>());
        String nameVariable1 = "var1";
        ExecutionVariable variable1 = new JasonTableExecutionVariable(nameVariable1, jsonTableValue1);
        ExecutionVariable variable2 = new JasonTableExecutionVariable("var2", jsonTableValue2);

        executionVariablesService.addExecutionVariables(variable1, executionVariables);
        executionVariablesService.addExecutionVariables(variable2, executionVariables);

        Assert.assertEquals(2, executionVariables.size());
        Assert.assertTrue(executionVariables.values().stream()
                .allMatch(var -> var instanceof JasonTableExecutionVariable));
    }

    @Test
    public void addExecutionVariables_withJsonTableObjectAndDifferentVar_returnsVoid() {
        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();

        JsonTable jsonTableValue1 = new JsonTable("table1", new ArrayList<>(), new ArrayList<>());
        JsonTable jsonTableValue2 = new JsonTable("table2", new ArrayList<>(), new ArrayList<>());
        String nameVariable1 = "var1";
        String nameVariable2 = "var2";
        ExecutionVariable variable1 = new JasonTableExecutionVariable(nameVariable1, jsonTableValue1);
        ExecutionVariable variable2 = new JasonTableExecutionVariable(nameVariable2, jsonTableValue2);

        executionVariablesService.addExecutionVariables(variable1, executionVariables);
        executionVariablesService.addExecutionVariables(variable2, executionVariables);
        Assert.assertEquals(2, executionVariables.size());
    }

    @Test
    public void getSourceWithJsonTableVariables_withJsonTableObjectAndVariableExist_returnsOptionalJsonTable() {
        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();

        JsonTable jsonTableValue1 = new JsonTable("table1", new ArrayList<>(), new ArrayList<>());
        JsonTable jsonTableValue2 = new JsonTable("table2", new ArrayList<>(), new ArrayList<>());
        String nameVariable1 = "VAR1";
        String nameVariable2 = "VAR2";
        ExecutionVariable variable1 = new JasonTableExecutionVariable(nameVariable1, jsonTableValue1);
        ExecutionVariable variable2 = new JasonTableExecutionVariable(nameVariable2, jsonTableValue2);

        executionVariables.put("VAR1",variable1);
        executionVariables.put("VAR2",variable2);

        Optional<JsonTable> referenceJsonTable =
                executionVariablesService.getSourceWithJsonTableVariables(nameVariable1, executionVariables);
        Assert.assertEquals(2, executionVariables.size());
        Assert.assertEquals("table1", referenceJsonTable.get().getName());
    }

    @Test
    public void getTestCaseNamesFromVariable_twoNameInVariable_returnTwoNamesAndUuid() {
        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();
        executionVariables.put("key",new SimpleExecutionVariable("key", "name1; name2"));
        List<String> testCases = new ArrayList<>();
        testCases.add("${key}");
        testCases.add("0d7df690-0a3a-48cc-8c73-5974ba2faee5");

        List<String> result = executionVariablesService.getTestCaseNamesFromVariable(testCases, executionVariables);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("0d7df690-0a3a-48cc-8c73-5974ba2faee5",result.get(0));
        Assert.assertEquals("name1",result.get(1));
        Assert.assertEquals("name2",result.get(2));
    }

    @Test
    public void getTestCaseNamesFromVariable_withoutVariable_returnUuid() {
        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();
        executionVariables.put("key",new SimpleExecutionVariable("key", "name1; name2"));
        List<String> testCases = new ArrayList<>();
        testCases.add("0d7df690-0a3a-48cc-8c73-5974ba2faee5");

        List<String> result = executionVariablesService.getTestCaseNamesFromVariable(testCases, executionVariables);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("0d7df690-0a3a-48cc-8c73-5974ba2faee5",result.get(0));
    }

    @Test
    public void getTestCaseNamesFromVariable_UuidInVariable_returnUuid() {
        ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();
        executionVariables.put("key",new SimpleExecutionVariable("key", "0d7df690-0a3a-48cc-8c73-5974ba2faee5"));
        List<String> testCases = new ArrayList<>();
        testCases.add("${key}");

        List<String> result = executionVariablesService.getTestCaseNamesFromVariable(testCases, executionVariables);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("0d7df690-0a3a-48cc-8c73-5974ba2faee5",result.get(0));
    }
}
