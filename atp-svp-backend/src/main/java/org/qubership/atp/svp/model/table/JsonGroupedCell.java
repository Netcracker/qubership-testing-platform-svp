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

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class JsonGroupedCell extends JsonCell implements Serializable {

    /**
     * A group cell represents a list of maps, where the key is
     * the result of a json request and the value is a json node.
     */
    private Map<String, String> groupedValue;

    /**
     * Setting the column header value and initializing the group column map.
     */
    public JsonGroupedCell(String columnHeader, Map<String, String> groupedValue) {
        super(columnHeader);
        this.groupedValue = groupedValue;
    }
}
