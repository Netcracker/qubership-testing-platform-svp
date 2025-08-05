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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.qubership.atp.svp.model.table.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActualTablesValidationInfo extends ValidationInfo {

    /**
     * Set of all possible headers in all tables.
     */
    private Set<String> combinedHeaders = new HashSet<>();

    private List<TableValidationInfo> tableValidations = new ArrayList<>();

    /**
     * Adds all table headers into combined set.
     */
    public void addTableHeaders(Table table) {
        if (!table.getRows().isEmpty()) {
            combinedHeaders.addAll(table.getHeaders());
        }
    }

    /**
     * Adds a new table validation for actual result table.
     */
    public void addTableValidation(TableValidationInfo tableValidation) {
        tableValidations.add(tableValidation);
    }
}
