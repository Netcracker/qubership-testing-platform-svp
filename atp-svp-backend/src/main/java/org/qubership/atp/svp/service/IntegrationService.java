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
import java.util.UUID;

import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.environments.LazyEnvironment;
import org.qubership.atp.svp.model.environments.Project;
import org.qubership.atp.svp.model.environments.System;
import org.qubership.atp.svp.model.logcollector.LogCollectorConfiguration;

public interface IntegrationService {

    List<Project> getProjectsFromEnvironments();

    Project getProjectFromEnvironments(UUID projectId);

    List<LazyEnvironment> getEnvironmentsByProjectIdFromEnvironments(UUID projectId);

    Environment getEnvironmentById(UUID environmentId);

    List<String> getSystemsNamesFromEnvironments(UUID projectId);

    List<String> getConnectionsNamesFromEnvironments(UUID projectId);

    List<System> getSystemsByEnvironmentIdFromEnvironments(UUID environmentId);

    List<LogCollectorConfiguration> getConfigurationsFromLogCollector(UUID projectId);
}
