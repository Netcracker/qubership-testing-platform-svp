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

package org.qubership.atp.svp.model.pot.values;

import org.qubership.atp.svp.core.exceptions.InvalidLogCollectorApiUsageException;
import org.qubership.atp.svp.model.logcollector.SearchResult;
import org.qubership.atp.svp.model.logcollector.SystemSearchResults;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LogCollectorValueObject extends AbstractValueObject {

    private SearchResult searchResult;

    @Getter(AccessLevel.NONE)
    private boolean hasResults;

    /**
     * Constructor for LogCollectorValueObject with searchResult from LogCollector.
     */
    public LogCollectorValueObject(SearchResult searchResult) {
        this.searchResult = searchResult;
        this.hasResults = false;
    }

    public boolean hasResults() {
        return hasResults;
    }

    /**
     * Return first log message from Log Collector's search result.
     */
    public String findFirstLogResult() {
        String componentSearchResultMessage = "Couldn't get ComponentSearchResults for this LogCollector request";
        String systemSearchResultMessage = "Couldn't get Systems for this LogCollector request";
        String searchThreadResultMessage = "Couldn't get Search Threads for this LogCollector request";
        return searchResult.getComponentSearchResults().stream().findFirst()
                .orElseThrow(() -> new InvalidLogCollectorApiUsageException(componentSearchResultMessage))
                .getSystemSearchResults().stream().findFirst()
                .orElseThrow(() -> new InvalidLogCollectorApiUsageException(systemSearchResultMessage))
                .getSearchThreadResult().stream().findFirst()
                .orElseThrow(() -> new InvalidLogCollectorApiUsageException(searchThreadResultMessage))
                .getMessageAsSingleString();
    }

    /**
     * Return true if we Have any Error on any levels.
     */
    public boolean hasError() {
        return searchResult.hasError()
                || searchResult.getComponentSearchResults().stream().anyMatch(componentSearchResults ->
                componentSearchResults.hasError()
                        || componentSearchResults.getSystemSearchResults().stream()
                        .anyMatch(SystemSearchResults::hasError));
    }

    /**
     * Return true if response from LogCollector is not empty and contain logs.
     */
    @Override
    public boolean hasData() {
        return searchResult.getComponentSearchResults().stream().anyMatch(componentSearchResults ->
                componentSearchResults.getSystemSearchResults().stream().anyMatch(SystemSearchResults::hasThreads));
    }
}
