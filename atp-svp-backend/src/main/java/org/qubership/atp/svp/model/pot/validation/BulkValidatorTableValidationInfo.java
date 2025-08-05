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

package org.qubership.atp.svp.model.pot.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BulkValidatorTableValidationInfo extends TableValidationInfo {

    /**
     * List of links to the BV test runs for each row by its index.
     */
    private Map<Integer, List<BulkValidatorTestRunInfo>> bvTestRunsInfo = new HashMap<>();

    public void addBulkValidatorTestRunsInfoForRow(int rowId, List<BulkValidatorTestRunInfo> testRunsInfo) {
        bvTestRunsInfo.put(rowId, testRunsInfo);
    }
}
