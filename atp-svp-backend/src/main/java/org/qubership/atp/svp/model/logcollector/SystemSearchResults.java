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
public class SystemSearchResults {

    private static final String IGNORED_NO_DATA_FOUND_ERROR_CODE = "LC-1031";
    private static final String IGNORED_NO_DATA_FOUND_ERROR_CODE_FILEBASE = "LC-1005";

    private String errorCode;
    private String errorDetails;
    private String logsLink;
    private List<SearchThreadFindResult> searchThreadResult;
    private String systemName;
    private UUID systemSearchId;
    private SearchThreadStatus status;

    /**
     * Check {@link SystemSearchResults} on error.
     * @return true if response from LogCollector has error.
     */
    public boolean hasError() {
        return Objects.nonNull(errorCode) && !errorCode.equals(IGNORED_NO_DATA_FOUND_ERROR_CODE)
                && !errorCode.equals(IGNORED_NO_DATA_FOUND_ERROR_CODE_FILEBASE)
                && !status.equals(SearchThreadStatus.COMPLETED);
    }

    public boolean hasThreads() {
        return Objects.nonNull(searchThreadResult) && !searchThreadResult.isEmpty();
    }
}
