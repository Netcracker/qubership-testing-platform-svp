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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qubership.atp.auth.springbootstarter.entities.Operation;
import org.qubership.atp.auth.springbootstarter.security.permissions.PolicyEnforcement;
import org.qubership.atp.svp.core.exceptions.InvalidEnvironmentApiUsageException;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.environments.LazyEnvironment;
import org.qubership.atp.svp.model.environments.Project;
import org.qubership.atp.svp.model.environments.System;
import org.qubership.atp.svp.model.logcollector.LogCollectorConfiguration;
import org.qubership.atp.svp.repo.feign.UsersFeignClient;
import org.qubership.atp.svp.repo.impl.EnvironmentRepository;
import org.qubership.atp.svp.repo.impl.LogCollectorRepository;
import org.qubership.atp.svp.service.IntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IntegrationServiceImpl implements IntegrationService {

    private final LogCollectorRepository logCollectorRepository;
    private final EnvironmentRepository environmentRepository;
    private final UsersFeignClient usersFeignClient;
    @Lazy
    private final PolicyEnforcement policyEnforcement;

    @Value("${keycloak.enabled}")
    private boolean isKeycloakEnabled;

    @Value("${spring.profiles.active}")
    private String springProfilesActive;

    /**
     * IntegrationServiceImpl default constructor.
     */
    @Autowired
    public IntegrationServiceImpl(LogCollectorRepository logCollectorRepository,
                                  EnvironmentRepository environmentRepository,
                                  UsersFeignClient usersFeignClient,
                                  PolicyEnforcement policyEnforcement) {
        this.logCollectorRepository = logCollectorRepository;
        this.environmentRepository = environmentRepository;
        this.usersFeignClient = usersFeignClient;
        this.policyEnforcement = policyEnforcement;
    }

    @Override
    public List<Project> getProjectsFromEnvironments() {
        log.info("Getting projects from atp-environment service.");
        List<Project> projects = environmentRepository.getProjects();
        if (isKeycloakEnabled && springProfilesActive.equals("default")) {
            log.info("Getting filtered projects for current user from atp-environment service.");
            projects = getFilteredProjectsForCurrentUser(projects);
        }
        log.info("Successfully received projects from atp-environment service.");
        return projects;
    }

    private List<Project> getFilteredProjectsForCurrentUser(List<Project> projects) {
        Map<UUID, org.qubership.atp.auth.springbootstarter.entities.Project> projectsFromUserService =
                getProjectsFromUsersService();
        return projects.stream()
                .filter(project -> {
                    if (projectsFromUserService.containsKey(project.getId())) {
                        return policyEnforcement.checkPoliciesForOperation(
                                projectsFromUserService.get(project.getId()), Operation.READ);
                    }
                    return policyEnforcement.isAdmin() || policyEnforcement.isSupport();
                }).collect(Collectors.toList());
    }

    private Map<UUID, org.qubership.atp.auth.springbootstarter.entities.Project> getProjectsFromUsersService() {
        return usersFeignClient.getAllProjects()
                .stream()
                .collect(Collectors.toMap(
                        org.qubership.atp.auth.springbootstarter.entities.Project::getUuid, Function.identity()));
    }

    @Override
    public Project getProjectFromEnvironments(UUID projectId) {
        log.info("Getting project from atp-environment service.");
        try {
            Project project = environmentRepository.getProject(projectId);
            log.info("Successfully received project from atp-environment service.");
            return project;
        } catch (Throwable e) {
            String message = "Failed to received project from atp-environment service.";
            log.error(message, e);
            throw new InvalidEnvironmentApiUsageException(message, e);
        }
    }

    @Override
    public List<LazyEnvironment> getEnvironmentsByProjectIdFromEnvironments(UUID projectId) {
        log.info("Getting environments from atp-environments for projectId: {} "
                + "from atp-environment service.", projectId);
        List<LazyEnvironment> environments = environmentRepository.getEnvironmentsByProjectId(projectId);
        log.info("Successfully received environments for projectId: {} "
                + "from atp-environment service.", projectId);
        return environments;
    }

    @Override
    public Environment getEnvironmentById(UUID environmentId) {
        log.info("Getting environment with id: {} from atp-environment service.", environmentId);
        try {
            Environment environment = environmentRepository.getEnvironmentById(environmentId);
            log.info("Successfully received environment with id: {} from atp-environment service.", environmentId);
            return environment;
        } catch (Throwable e) {
            String message = "Failed to received environment with id: " + environmentId
                    + " from atp-environment service.";
            log.error(message, e);
            throw new InvalidEnvironmentApiUsageException(message, e);
        }
    }

    @Override
    public List<String> getSystemsNamesFromEnvironments(UUID projectId) {
        log.info("Getting systems names for projectId: {} from atp-environment service.", projectId);
        List<String> systemsNames = environmentRepository.getSystemsName(projectId);
        log.info("Successfully received systems names for projectId: {} from atp-environment service.", projectId);
        return systemsNames;
    }

    @Override
    public List<String> getConnectionsNamesFromEnvironments(UUID projectId) {
        log.info("Getting connections names for projectId: {} from atp-environment service.", projectId);
        List<String> connectionsNames = environmentRepository.getConnectionsName(projectId);
        log.info("Successfully received connections names for projectId: {} "
                + "from atp-environment service.", projectId);
        return connectionsNames;
    }

    @Override
    public List<System> getSystemsByEnvironmentIdFromEnvironments(UUID environmentId) {
        log.info("Getting systems for environmentId {} from atp-environments.", environmentId);
        List<System> systems = environmentRepository.getSystemsByEnvironmentId(environmentId);
        log.info("Successfully received systems for environmentId: {} "
                + "from atp-environment service.", environmentId);
        return systems;
    }

    @Override
    public List<LogCollectorConfiguration> getConfigurationsFromLogCollector(UUID projectId) {
        return logCollectorRepository.getConfigurations(projectId);
    }
}
