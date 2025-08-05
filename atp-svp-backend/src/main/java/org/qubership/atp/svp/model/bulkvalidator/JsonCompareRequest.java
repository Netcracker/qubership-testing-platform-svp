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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class JsonCompareRequest {

    private List<UUID> objectIds;
    private UUID tcId;
    private UUID trId;
    private boolean isSnapshot;

    /**
     * JsonCompareRequest constructor.
     */
    public JsonCompareRequest(UUID tcId, UUID trId, List<UUID> objectIds) {
        this.tcId = tcId;
        this.trId = trId;
        this.isSnapshot = false;
        this.objectIds = objectIds;
    }

    /**
     * Creates a new request for getting highlight.
     * @param tcId - Test case IDs in Bulk Validator
     * @param trId - Test Run IDs in Bulk Validator
     * @param objectId - object IDs in Bulk Validator
     * @return a new {@link JsonCompareRequest} for highlights request.
     */
    public static JsonCompareRequest createRequestForGettingHighlight(UUID tcId, UUID trId, UUID objectId) {
        return new JsonCompareRequest(tcId, trId, Collections.singletonList(objectId));
    }
}
