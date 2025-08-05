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

import static org.keycloak.util.JsonSerialization.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.assertj.core.util.Strings;
import org.qubership.atp.svp.core.exceptions.VariableException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.impl.HttpSettings;
import org.qubership.atp.svp.model.impl.JsonDataColumnSettings;
import org.qubership.atp.svp.model.impl.JsonHierarchyNodeNames;
import org.qubership.atp.svp.model.impl.JsonParseSettings;
import org.qubership.atp.svp.model.impl.LogCollectorSettings;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.JasonTableExecutionVariable;
import org.qubership.atp.svp.model.pot.SimpleExecutionVariable;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.service.ExecutionVariablesService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

@Service
public class ExecutionVariablesServiceImpl implements ExecutionVariablesService {

    private static final String VARIABLE_REGEXP_MATCHER = ".*?\\$\\{.*?}.*?";
    private static final String VARIABLE_REGEXP_PATTERN = "\\$\\{[^\\=\\[\\]\\&\\',]+\\}";

    /**
     * Returns source string with key parameters, common parameters and execution actual results as variable values.
     * <br>
     * Pattern for replace key and common parameters variables to its value is: ${key_parameter_name}
     * Pattern for replace SUT parameters variables to its value is: ${group_name.sut_parameter_name}.
     */
    @Override
    public String getSourceWithExecutionVariables(String sourceStr,
                                                  ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        if (!Strings.isNullOrEmpty(sourceStr)) {
            for (Map.Entry<String, ExecutionVariable> entry : executionVariables.entrySet()) {
                sourceStr = substituteVariable(sourceStr, entry.getValue());
            }
            checkSourceOnVariable(sourceStr);
        }
        return sourceStr;
    }

    private void checkSourceOnVariable(String sourceStr) {
        Matcher matcher = Pattern.compile(VARIABLE_REGEXP_PATTERN).matcher(sourceStr);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            builder.append("\"").append(matcher.group()).append("\" ");
        }
        if (builder.length() != 0) {
            String errorMessage = "Input variable haven't been passed " + builder;
            throw new VariableException(errorMessage);
        }
    }

    private String substituteVariable(String sourceStr, ExecutionVariable variable) {
        if (variable instanceof SimpleExecutionVariable) {
            String varName = Pattern.quote(variable.getName());
            String varValue = Matcher.quoteReplacement(((SimpleExecutionVariable) variable).getSimpleValue());
            if (!Strings.isNullOrEmpty(varValue)) {
                sourceStr = sourceStr.replaceAll("(?i)\\$\\{" + varName + "}", varValue);
            }
        }
        return sourceStr;
    }

    /**
     * Returns {@link JsonParseSettings} with key parameters, common parameters
     * and execution actual results as variable values.
     * <br>
     * Method substitutes the values of variables in the request JsonPath, DataColumn, NodeNames, TreeObjectNodeNames.
     * <br>
     * Pattern for replace key and common parameters variables to its value is: ${key_parameter_name}
     * Pattern for replace SUT parameters variables to its value is: ${group_name.sut_parameter_name}.
     */
    @Override
    public JsonParseSettings getJsonParseSettingsWithExecutionVariables(JsonParseSettings settings,
                                                                        ConcurrentHashMap<String,
                                                                                ExecutionVariable> executionVariables) {
        settings.setJsonPath(getSourceWithExecutionVariables(settings.getJsonPath(), executionVariables));
        substituteVariablesToJsonDataColumn(settings.getColumnsData(), executionVariables);
        substituteVariablesToJsonHierarchyNodeNames(settings.getHierarchyNodeNames(), executionVariables);
        substituteVariablesToJsonHierarchyTreeObjectNodeNames(settings.getHierarchyTreeObjectNodeNames(),
                executionVariables);
        return settings;
    }

    /**
     * Take from the array of JsonTable variables in the Optional wrapper.
     *
     * @param pathReferenceToSutParameterName This is the key - the path to the parameter
     * @param executionVariables Collection of variables
     * @return JsonTable in a wrapper Optional.
     */
    public Optional<JsonTable> getSourceWithJsonTableVariables(
            String pathReferenceToSutParameterName,
            ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        if (!Strings.isNullOrEmpty(pathReferenceToSutParameterName)) {
            for (Map.Entry<String, ExecutionVariable> entry : executionVariables.entrySet()) {
                if (entry.getValue() instanceof JasonTableExecutionVariable
                        && entry.getValue().getName().equals(pathReferenceToSutParameterName.toUpperCase())) {
                    return Optional.ofNullable(((JasonTableExecutionVariable) entry.getValue()).getJsonTableValue());
                }
            }
        }
        return Optional.empty();
    }

    private void substituteVariablesToJsonDataColumn(List<JsonDataColumnSettings> columnsSettings,
                                                     ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        if (Objects.nonNull(columnsSettings) && !columnsSettings.isEmpty()) {
            columnsSettings.forEach(columnsSetting -> {
                columnsSetting.setHeader(getSourceWithExecutionVariables(columnsSetting.getHeader(),
                        executionVariables));
                columnsSetting.setJsonPath(getSourceWithExecutionVariables(columnsSetting.getJsonPath(),
                        executionVariables));
                substituteVariablesToList(columnsSetting.getGroupingJsonPaths(), executionVariables);
            });
        }
    }

    private void substituteVariablesToJsonHierarchyNodeNames(JsonHierarchyNodeNames nodeNames,
                                                             ConcurrentHashMap<String, ExecutionVariable> variables) {
        if (Objects.nonNull(nodeNames) && !nodeNames.hasEmptyNodeNames()) {
            nodeNames.setId(getSourceWithExecutionVariables(nodeNames.getId(), variables));
            nodeNames.setParentId(getSourceWithExecutionVariables(nodeNames.getParentId(), variables));
            nodeNames.setRootId(getSourceWithExecutionVariables(nodeNames.getRootId(), variables));
        }
    }

    private void substituteVariablesToJsonHierarchyTreeObjectNodeNames(List<String> treeObjectNodeNames,
                                                                       ConcurrentHashMap<String,
                                                                               ExecutionVariable> executionVariables) {
        substituteVariablesToList(treeObjectNodeNames, executionVariables);
    }

    private void substituteVariablesToList(List<String> params,
                                           ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        if (Objects.nonNull(params) && !params.isEmpty()) {
            for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
                String param = params.get(i);
                params.set(i, getSourceWithExecutionVariables(param, executionVariables));
            }
        }
    }

    /**
     * Returns {@link HttpSettings} with key parameters, common parameters
     * and execution actual results as variable values.
     * <br>
     * Method substitutes the values of variables in the request query and in headers.
     * <br>
     * Pattern for replace key and common parameters variables to its value is: ${key_parameter_name}
     * Pattern for replace SUT parameters variables to its value is: ${group_name.sut_parameter_name}.
     */
    @Override
    public HttpSettings getHttpSettingsWithExecutionVariables(ConcurrentHashMap<String, ExecutionVariable> variables,
                                                              HttpSettings settings) {
        settings.setQuery(getSourceWithExecutionVariables(settings.getQuery(), variables));
        settings.setBody(getSourceWithExecutionVariables(settings.getBody(), variables));
        substituteVariablesToMap(settings.getHeaders(), variables);
        substituteVariablesToMap(settings.getQueryParams(), variables);
        substituteVariablesToMap(settings.getUrlEncodedBody(), variables);

        return settings;
    }

    private void substituteVariablesToMap(Map<String, String> map,
                                          ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        if (Objects.nonNull(map) && !map.isEmpty()) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                fillEntryValueWithExecutionVariables(entry, executionVariables);
                fillEntryKeyWithExecutionVariablesByReplacingMapEntry(map, entry, executionVariables);
            }
        }
    }

    private void fillEntryValueWithExecutionVariables(Map.Entry<String, String> entry,
                                                      ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        if (entry.getValue().matches(VARIABLE_REGEXP_MATCHER)) {
            entry.setValue(getSourceWithExecutionVariables(entry.getValue(), executionVariables));
        }
    }

    private void fillEntryKeyWithExecutionVariablesByReplacingMapEntry(Map<String, String> map,
                                                                       Map.Entry<String, String> entry,
                                                                       ConcurrentHashMap<String,
                                                                               ExecutionVariable> executionVariables) {
        if (entry.getKey().matches(VARIABLE_REGEXP_MATCHER)) {
            map.remove(entry.getKey());
            String newKey = getSourceWithExecutionVariables(entry.getKey(), executionVariables);
            map.putIfAbsent(newKey, entry.getValue());
        }
    }

    /**
     * Returns {@link LogCollectorSettings} with key parameters, common parameters
     * and execution actual results as variable values.
     * <br>
     * Method substitutes the values of variables in the graylogSearchText and searchText related to fileBased search.
     * <br>
     * Pattern for replace key and common parameters variables to its value is: ${key_parameter_name}
     * Pattern for replace SUT parameters variables to its value is: ${group_name.sut_parameter_name}.
     */
    @Override
    public LogCollectorSettings getLogCollectorSettingsWithExecutionVariables(ConcurrentHashMap<String,
            ExecutionVariable> executionVariables,
                                                                              LogCollectorSettings settings) {
        String graylogSearchText = settings.getParameters().getGraylogSearchText();
        if (Objects.nonNull(graylogSearchText)) {
            settings.getParameters().setGraylogSearchText(getSourceWithExecutionVariables(
                    graylogSearchText, executionVariables));
        }
        String searchText = settings.getParameters().getSearchText();
        if (Objects.nonNull(searchText)) {
            settings.getParameters().setSearchText(getSourceWithExecutionVariables(searchText, executionVariables));
        }
        return settings;
    }

    /**
     * Returns execution variables from key parameters.
     *
     * @param keyParameters - key parameters from PotSession.
     * @return Set of {@link ExecutionVariable} with name of key parameters as variable name
     *         and value of key parameters as variable value.
     */
    @Override
    public ConcurrentHashMap<String, ExecutionVariable> getVariablesFromKeyParameters(
            Map<String, String> keyParameters) {
        ConcurrentHashMap<String, ExecutionVariable> variables = new ConcurrentHashMap<>();
        if (keyParameters != null) {
            keyParameters.forEach((key, value) -> variables.put(key.toUpperCase(),
                    new SimpleExecutionVariable(key.toUpperCase(), value)));
        }
        return variables;
    }

    /**
     * Returns execution variables from synchronous Parameter.
     *
     * @param getVarName Parameter Variable Name from PotSessionParameter
     * @param parameter PotSessionParameter
     * @return Optional of {@link ExecutionVariable} with name of parameters as variable name
     *         and value of parameters as variable value.
     */
    @Override
    public Optional<ExecutionVariable> getVariableFromParameter(Function<PotSessionParameterEntity, String> getVarName,
                                                                PotSessionParameterEntity parameter) {
        return parameter.getArValues().stream()
                .filter(arValue -> arValue instanceof SimpleValueObject || arValue instanceof TableValueObject)
                .findFirst()
                .map(firstSimpleArValue -> {
                    String variableName = getVarName.apply(parameter);
                    if (firstSimpleArValue instanceof SimpleValueObject) {
                        String variableValue = ((SimpleValueObject) firstSimpleArValue).getValue();
                        return new SimpleExecutionVariable(variableName.toUpperCase(), variableValue);
                    } else if (((TableValueObject) firstSimpleArValue).getTable() instanceof JsonTable) {
                        JsonTable jsonTable = (JsonTable) ((TableValueObject) firstSimpleArValue).getTable();
                        return new JasonTableExecutionVariable(variableName.toUpperCase(), jsonTable);
                    }
                    return null;
                });
    }

    /**
     * Returns execution variables from Environment.
     *
     * @param environment Environment
     *         Set of {@link ExecutionVariable} with all fields of the Environment object as a
     *         key - value in a flat view.
     */
    @Override
    public void getVariablesFromEnvironment(Environment environment,
                                            ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        String envPrefix = "ENV";
        JsonNode jsonNodeEnv = mapper.convertValue(environment, JsonNode.class);

        convertJsonNodeEnvToExVariables(envPrefix, jsonNodeEnv, executionVariables);
    }

    /**
     * Adding the value of the results to the Set array of the execution variables.
     * If there is a value, it is overwritten.
     *
     * @param variable ExecutionVariable
     */
    @Override
    public void addExecutionVariables(ExecutionVariable variable,
                                      ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        if (!executionVariables.containsKey(variable.getName().toUpperCase())) {
            executionVariables.remove(variable.getName().toUpperCase());
            executionVariables.put(variable.getName().toUpperCase(), variable);
        } else {
            executionVariables.put(variable.getName().toUpperCase(), variable);
        }
    }

    private void convertJsonNodeEnvToExVariables(String currentPath, JsonNode jsonNodeEnv,
                                                 ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        if (jsonNodeEnv.isObject()) {
            iteratingObject(currentPath, executionVariables, jsonNodeEnv);
        } else if (jsonNodeEnv.isArray()) {
            iteratingArray(currentPath, executionVariables, jsonNodeEnv);
        } else if (jsonNodeEnv.isValueNode()) {
            writeToSetExecutionVariable(currentPath, jsonNodeEnv, executionVariables);
        }
    }

    private void iteratingObject(String currentPath, ConcurrentHashMap<String, ExecutionVariable> executionVariables,
                                 JsonNode jsonNodeEnv) {
        ObjectNode objectNode = (ObjectNode) jsonNodeEnv;
        Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
        String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            String nameField = entry.getKey().contains(".") ? "\"" + entry.getKey() + "\"" : entry.getKey();
            String pathObject = nameField.equals("systems") ? currentPath : pathPrefix + nameField;
            convertJsonNodeEnvToExVariables(pathObject, entry.getValue(), executionVariables);
        }
    }

    private void iteratingArray(String currentPath, ConcurrentHashMap<String, ExecutionVariable> executionVariables,
                                JsonNode jsonNodeEnv) {
        ArrayNode arrayNode = (ArrayNode) jsonNodeEnv;
        for (int i = 0; i < arrayNode.size(); i++) {
            String pathArray = arrayNode.get(i).has("name")
                    ? currentPath + "." + arrayNode.get(i).get("name").asText()
                    : currentPath + "[" + i + "]." + arrayNode.get(i).asText();
            convertJsonNodeEnvToExVariables(pathArray, arrayNode.get(i), executionVariables);
        }
    }

    private void writeToSetExecutionVariable(String currentPath, JsonNode jsonNodeEnv,
                                             ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        ValueNode valueNode = (ValueNode) jsonNodeEnv;
        if (!valueNode.isNull()) {
            ExecutionVariable variable = new SimpleExecutionVariable(currentPath.toUpperCase(), valueNode.asText());
            addExecutionVariables(variable, executionVariables);
        }
    }

    /**
     * Get testCase names in variable.
     */
    @Override
    public List<String> getTestCaseNamesFromVariable(List<String> testCases,
                                                     ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        List<String> variableValues = new ArrayList<>();
        testCases.forEach(testCase -> {
            if (testCase.matches(VARIABLE_REGEXP_MATCHER)) {
                variableValues.addAll(Arrays.asList(getSourceWithExecutionVariables(testCase,
                        executionVariables).split("; ")));
            }
        });
        List<String> overVariables = testCases.stream().filter(testCase -> testCase.matches(VARIABLE_REGEXP_MATCHER))
                .collect(Collectors.toList());
        testCases.addAll(variableValues);
        testCases.removeAll(overVariables);
        return testCases;
    }
}
