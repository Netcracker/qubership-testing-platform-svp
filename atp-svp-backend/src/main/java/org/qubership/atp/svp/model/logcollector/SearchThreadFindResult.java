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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SearchThreadFindResult {

    private String foundInDirectory;
    private String logFileName;
    private String logsLink;
    private List<List<String>> messages;
    private Map<String, String> parameters;
    private String timestamp;

    /**
     * Returns messages as single string.
     */
    @JsonIgnore
    public String getMessageAsSingleString() {
        StringBuilder sb = new StringBuilder();
        messages.forEach(message -> {
            sb.append(String.join("\n", message));
            sb.append("\n");
        });
        return sb.toString();
    }

    public boolean hasLogParameter(String parameterName) {
        return Objects.nonNull(parameters) && parameters.containsKey(parameterName);
    }
}
