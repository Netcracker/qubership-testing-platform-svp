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

package org.qubership.atp.svp.repo.impl;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.environments.LazyEnvironment;
import org.qubership.atp.svp.model.environments.Project;
import org.qubership.atp.svp.model.environments.System;
import org.qubership.atp.svp.repo.feign.EnvironmentFeignClient;
import org.qubership.atp.svp.repo.feign.EnvironmentsProjectFeignClient;
import org.qubership.atp.svp.utils.DtoConvertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class EnvironmentRepository {

    private final DtoConvertService dtoConvertService;
    private final EnvironmentFeignClient environmentFeignClient;
    private final EnvironmentsProjectFeignClient environmentsProjectFeignClient;

    /**
     * Constructor for class.
     */
    @Autowired
    public EnvironmentRepository(DtoConvertService dtoConvertService,
                                 EnvironmentFeignClient environmentFeignClient,
                                 EnvironmentsProjectFeignClient environmentsProjectFeignClient) {
        this.dtoConvertService = dtoConvertService;
        this.environmentFeignClient = environmentFeignClient;
        this.environmentsProjectFeignClient = environmentsProjectFeignClient;
    }

    public List<Project> getProjects() {
        return dtoConvertService.convertList(
                environmentsProjectFeignClient.getAllShort(false).getBody(),
                Project.class);
    }

    /**
     * Get Project.
     */
    public Project getProject(UUID projectId) {
        return dtoConvertService.convert(environmentsProjectFeignClient.getShortProject(projectId, false)
                .getBody(), Project.class);
    }

    public List<LazyEnvironment> getEnvironmentsByProjectId(UUID projectId) {
        return dtoConvertService.convertList(environmentsProjectFeignClient.getEnvironmentsShort(projectId)
                .getBody(), LazyEnvironment.class);
    }

    public List<System> getSystemsByEnvironmentId(UUID environmentId) {
        return dtoConvertService.convertList(environmentFeignClient.getSystemV2(environmentId,
                "Business Solution", true).getBody(), System.class);
    }

    public Environment getEnvironmentById(UUID environmentId) {
        return dtoConvertService.convert(environmentFeignClient.getEnvironment(environmentId,
                true).getBody(), Environment.class);
    }

    public List<String> getSystemsName(UUID projectId) {
        return environmentsProjectFeignClient.getSystemsName(projectId).getBody();
    }

    public List<String> getConnectionsName(UUID projectId) {
        return environmentsProjectFeignClient.getConnectionsName(projectId).getBody();
    }
}
