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
import java.util.Objects;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SearchResult {

    private static final String IGNORED_ARCHIVING_ERROR_CODE = "LC-1029";

    private String errorCode;
    private String errorDetails;
    private String logsLink;
    private UUID searchId;
    private SearchStatus status;
    private List<ComponentSearchResults> componentSearchResults;

    /**
     * We ignore the ErrorCode "LC-1029 cause "skipArchvation" in registerSearch request to LogCollector is always true.
     *
     * @return true if SearchResult has error.
     */
    public boolean hasError() {
        return Objects.nonNull(getErrorCode()) && !errorCode.equals(IGNORED_ARCHIVING_ERROR_CODE)
                && !status.equals(SearchStatus.COMPLETED);
    }
}
