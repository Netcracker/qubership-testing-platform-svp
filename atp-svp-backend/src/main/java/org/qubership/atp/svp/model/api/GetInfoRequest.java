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

package org.qubership.atp.svp.model.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.svp.model.impl.LogCollectorSearchPeriod;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetInfoRequest {

    private UUID environmentId;
    private UUID sessionId;
    private Map<String, String> keyParameters;
    @Getter(AccessLevel.NONE)
    @JsonProperty("shouldHighlightDiffs")
    private boolean shouldHighlightDiffs;
    @Getter(AccessLevel.NONE)
    @JsonProperty("shouldSendSessionResults")
    private boolean shouldSendSessionResults;
    @Setter
    private List<String> pagesName = new ArrayList<>();
    private LogCollectorSearchPeriod logCollectorSearchPeriod;
    private boolean forcedLoadingCommonParameters;
    private boolean onlyCommonParametersExecuted;
    @JsonProperty("isPotGenerationMode")
    private boolean isPotGenerationMode;
    @JsonProperty("timeOutRange")
    private int timeOutRange;
    @JsonProperty("folder")
    @Setter
    private String folder;

    public boolean shouldHighlightDiffs() {
        return shouldHighlightDiffs;
    }

    public boolean shouldSendSessionResults() {
        return shouldSendSessionResults;
    }
}
