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

package org.qubership.atp.svp.model.impl;

import java.util.Objects;

import javax.annotation.Nullable;

import org.qubership.atp.svp.model.api.GetInfoRequest;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LogCollectorSearchPeriod {

    private String searchType;
    private String dateStart;
    private String dateEnd;
    private int timeRange;

    /**
     * Creates {@link LogCollectorSearchPeriod} instance with search period from project config,
     * or from {@link GetInfoRequest} which overrides it.
     *
     * @param projectConfig - project configuration
     * @param request - request for getting info (higher priority than project configuration)
     * @return new {@link LogCollectorSearchPeriod} instance.
     */
    @Nullable
    public static LogCollectorSearchPeriod createFromProjectConfigAndGetInfoRequest(ProjectConfigsEntity projectConfig,
                                                                                    GetInfoRequest request) {
        if (Objects.nonNull(request.getLogCollectorSearchPeriod())) {
            return request.getLogCollectorSearchPeriod();
        } else {
            return new LogCollectorSearchPeriod(
                    projectConfig.getDefaultLogCollectorSearchTimeRange());
        }
    }

    /**
     * Constructor for LogCollectorSearchPeriod with absolute search type.
     */
    public LogCollectorSearchPeriod(String dateStart, String dateEnd) {
        this.searchType = "ABSOLUTE";
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
    }

    /**
     * Constructor for LogCollectorSearchPeriod with relative search type.
     */
    public LogCollectorSearchPeriod(int timeRange) {
        this.searchType = "RELATIVE";
        this.timeRange = timeRange;
    }
}
