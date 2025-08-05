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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.svp.core.exceptions.InvalidLogCollectorApiUsageException;
import org.qubership.atp.svp.model.logcollector.LogCollectorSearchRequest;

import brave.internal.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LogCollectorSettings implements SourceSettings {

    private List<UUID> configurations;
    private LogCollectorSettingsParameters parameters;

    private static final String TOOL_NAME = "atp-svp";

    /**
     * Generates log collector request from existing settings.
     */
    public LogCollectorSearchRequest generateRequest(UUID projectId,
                                                     UUID environmentId,
                                                     List<UUID> configurations,
                                                     @Nullable LogCollectorSearchPeriod searchPeriod,
                                                     UUID requestId) {
        if (Objects.isNull(searchPeriod)) {
            throw new InvalidLogCollectorApiUsageException("Couldn't find the search period for the request to the "
                    + "Log Collector. Specify the defaultLogCollectorSearchTimeRange setting in the project "
                    + "configuration, or configure the period via the UI using Log Collector search period.");
        }
        LogCollectorSearchRequest request = new LogCollectorSearchRequest();
        request.setProjectId(projectId);
        request.setEnvironmentId(environmentId);
        request.setConfigurations(configurations);
        request.setRequestTool(TOOL_NAME);
        request.setRequestId(requestId);
        request.getParameters().setSkipArchivation(true);
        request.getParameters().setFilterText("LC_NO_FILTER");
        request.getParameters().setSearchPatternQuery(parameters.getGraylogSearchText());
        request.getParameters().setSearchPattern(parameters.getSearchText());
        request.getParameters().setSearchPatternType(parameters.isRegexp() ? "REGEXP" : "TEXT");
        request.getParameters().setStartDateFromString(searchPeriod.getDateStart());
        request.getParameters().setEndDateFromString(searchPeriod.getDateEnd());
        request.getParameters().setRangeType(searchPeriod.getSearchType());
        request.getParameters().setTimeRange(searchPeriod.getTimeRange());
        request.getParameters().setSearchStrategy(parameters.getSearchStrategy() != null
                ? parameters.getSearchStrategy() : "SURROUNDING_LINES");
        request.getParameters().setFileTrimmingStrategy("TRIM_BY_SEARCHING_TIME");
        request.getParameters().setSurroundingLinesQuantity(parameters.getLines());
        request.getParameters().setSurroundingMinutesQuantity(parameters.getMinutes());
        request.getParameters().setSearchPerOneDay(parameters.isSearchPerOneDay());
        request.getParameters().setReadOnlyMode(parameters.isReadOnlyMode());
        return request;
    }

    @Override
    public boolean equals(Object settings) {
        return settings instanceof LogCollectorSettings;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass().getName());
    }

    @Override
    public String toString() {
        return "LogCollectorSettings{"
                + "configurations=" + configurations
                + ", parameters=" + parameters
                + "}";
    }
}
