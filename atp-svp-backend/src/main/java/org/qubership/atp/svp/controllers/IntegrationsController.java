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

package org.qubership.atp.svp.controllers;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.svp.model.environments.LazyEnvironment;
import org.qubership.atp.svp.model.environments.Project;
import org.qubership.atp.svp.model.environments.System;
import org.qubership.atp.svp.model.logcollector.LogCollectorConfiguration;
import org.qubership.atp.svp.service.IntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RequestMapping("/api/svp/integrations")
@RestController()
public class IntegrationsController /*implements IntegrationsControllerApi*/ {

    private final IntegrationService integrationService;

    @Autowired
    public IntegrationsController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    /**
     * Gets list of projects from atp-environments.
     */
    @GetMapping("environments/projects")
    @AuditAction(auditAction = "Get projects from atp-environments")
    @PreAuthorize("@entityAccess.isAuthenticated()")
    public List<Project> getProjectsFromEnvironments() {
        return integrationService.getProjectsFromEnvironments();
    }

    /**
     * Gets list of environments for the current project from atp-environments.
     */
    @GetMapping("environments/projects/{projectId}/environments")
    @AuditAction(auditAction = "Get get environments for project: {{#projectId}} ")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"READ\")")
    public List<LazyEnvironment> getEnvironmentsByProjectIdFromEnvironments(@PathVariable(value = "projectId")
                                                                            UUID projectId) {
        return integrationService.getEnvironmentsByProjectIdFromEnvironments(projectId);
    }

    /**
     * Gets list of systems for the current project from atp-environments.
     */
    @GetMapping("environments/environments/{environmentId}/systems")
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @AuditAction(auditAction = "Get systems of environment: {{#environmentId}}")
    public List<System> getSystemsByEnvironmentIdFromEnvironments(@PathVariable(value = "environmentId")
                                                                  UUID environmentId) {
        return integrationService.getSystemsByEnvironmentIdFromEnvironments(environmentId);
    }

    /**
     * Gets list of possible system names for the current project from atp-environments.
     */
    @GetMapping("environments/projects/{projectId}/systems-names")
    @AuditAction(auditAction = "Get system names of environment {{#environmentId}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"READ\")")
    public List<String> getSystemsNamesFromEnvironments(@PathVariable(value = "projectId") UUID projectId) {
        return integrationService.getSystemsNamesFromEnvironments(projectId);
    }

    /**
     * Gets list of possible connection names for the current project from atp-environments.
     */
    @GetMapping("environments/projects/{projectId}/connections-names")
    @AuditAction(auditAction = "Get connection names of environment: {{#environmentId}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"READ\")")
    public List<String> getConnectionsNamesFromEnvironments(@PathVariable(value = "projectId") UUID projectId) {
        return integrationService.getConnectionsNamesFromEnvironments(projectId);
    }

    /**
     * Gets list of possible configurations for the current project from atp-log-collector.
     */
    @GetMapping("log-collector/projects/{projectId}/configurations")
    @AuditAction(auditAction = "Get log collector configurations")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"READ\")")
    public List<LogCollectorConfiguration> getConfigurationsFromLogCollector(@PathVariable(value = "projectId")
                                                                             UUID projectId) {
        return integrationService.getConfigurationsFromLogCollector(projectId);
    }
}
