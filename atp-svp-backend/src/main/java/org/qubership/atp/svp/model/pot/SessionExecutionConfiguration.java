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

package org.qubership.atp.svp.model.pot;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.impl.BulkValidatorProjectSettings;
import org.qubership.atp.svp.model.impl.LogCollectorSearchPeriod;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SessionExecutionConfiguration {

    @NonNull
    private Environment environment;
    @NonNull
    private List<String> pagesName;
    @Nullable // not for all projects (Filled from the project configuration or UI)
    @Setter
    private LogCollectorSearchPeriod logCollectorSearchPeriod;
    @Nullable // not for all projects (Filled from the project configuration)
    private BulkValidatorProjectSettings bulkValidatorSettings;
    @NonNull
    private List<UUID> logCollectorConfigurations;
    @NonNull
    private Boolean shouldHighlightDiffs;
    @NonNull
    private Boolean shouldSendSessionResults;
    @NonNull
    private Boolean isFullInfoNeededInPot;
    @NonNull
    private Boolean onlyForPreconfiguredParams;
    @NonNull
    private Boolean isPotGenerationMode;
    @NonNull
    private Boolean onlyCommonParametersExecuted;
    @NonNull
    private Boolean forcedLoadingCommonParameters;
    @Nullable
    private UUID folder;

    public boolean shouldHighlightDiffs() {
        return shouldHighlightDiffs;
    }

    public boolean shouldSendSessionResults() {
        return shouldSendSessionResults;
    }

    @JsonIgnore
    public UUID getProjectId() {
        return environment.getProjectId();
    }
}
