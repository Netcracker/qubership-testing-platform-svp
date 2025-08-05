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

package org.qubership.atp.svp.model.bulkvalidator;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class ComparingProcessRequest {

    private List<ComparingTestCase> testCases;

    /**
     * Creates a new compare request for test runs.
     * @param trIds - Test Run IDs in Bulk Validator.
     * @return a new {@link ComparingProcessRequest} for test runs.
     */
    public static ComparingProcessRequest createRequestForTestRuns(List<UUID> trIds) {
        ComparingProcessRequest request = new ComparingProcessRequest();
        request.setTestCases(trIds.stream().map(ComparingTestCase::new).collect(Collectors.toList()));
        return request;
    }
}
