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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Table extends AbstractTable {

    /**
     * Each row is represented by a map of header as key and value as value.
     */
    private List<Map<String, String>> rows;

    /**
     * Creates table instance.
     */
    public Table(List<String> headers, List<Map<String, String>> rows) {
        super(headers);
        this.rows = rows;
    }

    /**
     * Creates table instance.
     */
    public Table(String name, List<String> headers, List<Map<String, String>> rows) {
        super(name, headers);
        this.rows = rows;
    }

    /**
     * Contains table rows.
     */
    @Override
    public boolean containsRows() {
        return Objects.nonNull(rows) && !rows.isEmpty();
    }

    /**
     * Gets row by index.
     */
    public Map<String, String> getRow(int index) {
        return rows.get(index);
    }

    /**
     * Gets table grouping values mapped by row index. Removes grouping headers and their values in all rows.
     */
    public Map<Integer, List<String>> getGroupingValues(@Nonnull List<String> groupingColumns) {
        Map<Integer, List<String>> groupingValues = new HashMap<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> currRow = getRow(i);
            List<String> rowGroupingValues = new ArrayList<>();
            groupingColumns.stream()
                    .map(String::toUpperCase)
                    .forEach(groupingColumn -> {
                        if (currRow.containsKey(groupingColumn)) {
                            rowGroupingValues.add(currRow.get(groupingColumn));
                            currRow.remove(groupingColumn);
                            super.getHeaders().remove(groupingColumn);
                        }
                    });
            groupingValues.put(i, rowGroupingValues);
        }
        return groupingValues;
    }
}
