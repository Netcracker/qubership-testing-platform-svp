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

package org.qubership.atp.svp.model.table;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JsonElementNestingDepth extends JsonElement {

    /**
     * Order is used to form a hierarchy of table rows in the plane of a single array.
     */
    private int nestingDepth;

    /**
     * Json objects are copied here according to the nesting level.
     */
    private JsonObject jsonObject;

    public JsonElementNestingDepth(int nestingDepth, JsonObject jsonObject) {
        this.jsonObject = jsonObject;
        this.nestingDepth = nestingDepth;
    }

    @Override
    public JsonElement deepCopy() {
        return null;
    }
}
