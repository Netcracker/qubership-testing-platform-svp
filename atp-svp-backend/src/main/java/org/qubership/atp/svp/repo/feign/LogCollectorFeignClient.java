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
import org.qubership.atp.svp.clients.api.logcollector.ApiSearchLogsControllerApi;
import org.qubership.atp.svp.model.logcollector.LogCollectorConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${feign.atp.logcollector.name}", url = "${feign.atp.logcollector.url}",
        path = "${feign.atp.logcollector.route}", configuration = FeignConfiguration.class)
public interface LogCollectorFeignClient extends ApiSearchLogsControllerApi {

    /**
     * Get systems under environment.
     */
    @GetMapping(value = "${feign.atp.logcollector.route}/api/configurations/project/{projectId}")
    List<LogCollectorConfiguration> getConfigurations(@PathVariable("projectId") UUID projectId);
}
