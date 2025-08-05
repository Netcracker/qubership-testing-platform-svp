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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JsonTable extends AbstractTable implements Serializable {

    private List<JsonTableRow> rows = new ArrayList<>();

    /**
     * Creates table with json data grouped by {@link JsonTable}.
     */
    public JsonTable(List<String> headers, List<JsonTableRow> rows) {
        super(headers);
        this.rows = rows;
    }

    /**
     * Contains Json table rows.
     */
    @Override
    public boolean containsRows() {
        return Objects.nonNull(rows) && !rows.isEmpty();
    }

    /**
     * Creates named table with json data grouped by {@link JsonTable}.
     */
    public JsonTable(String name, List<String> headers, List<JsonTableRow> rows) {
        super(name, headers);
        this.rows = rows;
    }

    /**
     * Add row - list of {@link JsonCell} and nesting depth of current row in main hierarchy.
     */
    public void addJsonTableRow(JsonTableRow row) {
        rows.add(row);
    }

    /**
     * Add list rows to JsonTable.
     */
    public void addAllJsonTableRows(List<JsonTableRow> row) {
        rows.addAll(row);
    }
}
