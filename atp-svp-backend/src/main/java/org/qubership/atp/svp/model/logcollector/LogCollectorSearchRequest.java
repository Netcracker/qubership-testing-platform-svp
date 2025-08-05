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

package org.qubership.atp.svp.model.logcollector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class LogCollectorSearchRequest {

    private UUID projectId;
    private UUID environmentId;
    private List<UUID> configurations;
    private LogCollectorSearchRequestParameters parameters;
    private List<UUID> templates;
    private UUID requestId;
    private String requestTool;

    public LogCollectorSearchRequest() {
        configurations = new ArrayList<>();
        parameters = new LogCollectorSearchRequestParameters();
    }
}
