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

package org.qubership.atp.svp.repo.feign;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.auth.springbootstarter.entities.Project;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "${feign.atp.users.name}", url = "${feign.atp.users.url}",
        configuration = FeignConfiguration.class)
public interface UsersFeignClient {

    /**
     * Get all projects from atp-users.
     */
    @RequestMapping(method = RequestMethod.GET,
            value = "${feign.atp.users.route}/api/v1/users/projects")
    List<Project> getAllProjects();

    /**
     * Get users from project by project id.
     */
    @RequestMapping(method = RequestMethod.GET,
            value = "${feign.atp.users.route}/api/v1/users/projects/{projectId}/users")
    Project getProjectsUsersByUuid(@PathVariable("projectId") UUID projectId);

}
