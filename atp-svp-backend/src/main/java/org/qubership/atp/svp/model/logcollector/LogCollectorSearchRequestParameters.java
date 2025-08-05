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

import java.time.OffsetDateTime;

import org.assertj.core.util.Strings;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LogCollectorSearchRequestParameters {

    private String rangeType;
    private int timeRange;

    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private String searchPattern;
    private String searchPatternQuery;
    private String searchPatternType;
    private String searchStrategy;
    private String fileTrimmingStrategy;
    private String filterText;
    private int surroundingLinesQuantity;
    private int surroundingMinutesQuantity;

    private boolean readOnlyMode;
    private boolean searchPerOneDay;
    private boolean skipArchivation;

    /**
     * Set Start date from String.
     */
    public void setStartDateFromString(String startDate) {
        if (!Strings.isNullOrEmpty(startDate)) {
            this.startDate = convertToOffsetDateType(startDate);
        }
    }

    /**
     * Set End date from String.
     */
    public void setEndDateFromString(String endDate) {
        if (!Strings.isNullOrEmpty(endDate)) {
            this.endDate = convertToOffsetDateType(endDate);
        }
    }

    private OffsetDateTime convertToOffsetDateType(String date) {
        return OffsetDateTime.parse(date);
    }
}
