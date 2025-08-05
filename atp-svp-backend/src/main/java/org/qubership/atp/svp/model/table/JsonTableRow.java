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
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JsonTableRow implements Serializable {

    /**
     * Order is used to form a hierarchy of table rows in the plane of a single array.
     */
    private int nestingDepth;

    /**
     * This List contains an array of cells simple and grouped which represents a table row.
     */
    private List<JsonCell> cells;

    public JsonTableRow(List<JsonCell> cells) {
        this.cells = cells;
    }

    public JsonTableRow(List<JsonCell> cells, int nestingDepth) {
        this.cells = cells;
        this.nestingDepth = nestingDepth;
    }

    /**
     * Add list cells to JsonTableRow.
     */
    public void addAllCells(List<JsonCell> cells) {
        this.cells.addAll(cells);
    }
}
