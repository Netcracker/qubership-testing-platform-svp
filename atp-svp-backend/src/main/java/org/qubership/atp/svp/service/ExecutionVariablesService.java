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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.impl.HttpSettings;
import org.qubership.atp.svp.model.impl.JsonParseSettings;
import org.qubership.atp.svp.model.impl.LogCollectorSettings;
import org.qubership.atp.svp.model.pot.ExecutionVariable;

public interface ExecutionVariablesService {
    // TODO Move methods from the service to the factory and apply polymorphism with Spring mechanism

    String getSourceWithExecutionVariables(String sourceStr,
                                           ConcurrentHashMap<String, ExecutionVariable> executionVariables);

    JsonParseSettings getJsonParseSettingsWithExecutionVariables(JsonParseSettings settings,
                                                                 ConcurrentHashMap<String,
                                                                         ExecutionVariable> executionVariables);

    HttpSettings getHttpSettingsWithExecutionVariables(ConcurrentHashMap<String, ExecutionVariable> executionVariables,
                                                       HttpSettings settings);

    LogCollectorSettings getLogCollectorSettingsWithExecutionVariables(
            ConcurrentHashMap<String, ExecutionVariable> executionVariables,
            LogCollectorSettings settings);

    ConcurrentHashMap<String, ExecutionVariable> getVariablesFromKeyParameters(Map<String, String> keyParameters);

    void getVariablesFromEnvironment(Environment environment,
                                     ConcurrentHashMap<String, ExecutionVariable> executionVariables);

    Optional<ExecutionVariable> getVariableFromParameter(Function<PotSessionParameterEntity, String> getVarName,
                                                         PotSessionParameterEntity parameter);

    void addExecutionVariables(ExecutionVariable variable,
                               ConcurrentHashMap<String, ExecutionVariable> executionVariables);

    List<String> getTestCaseNamesFromVariable(List<String> testCases,
                                              ConcurrentHashMap<String, ExecutionVariable> executionVariables);
}
