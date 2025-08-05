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

package org.qubership.atp.svp.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.qubership.atp.svp.model.table.JsonTable;

import joptsimple.internal.Strings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JsonJoinConditionSettings {

    /**
     * Header name of the currently configuring SUT Parameter.
     */
    private List<Integer> idxPrimaryHeaderNames = new ArrayList<>();

    /**
     * Any SUT Parameter from the current group which will be joined.
     */
    private String pathReferenceSutParameterName;

    /**
     * The field in which we copy the clone of the object of the reference json table to join.
     */
    private JsonTable referenceTable;

    /**
     * Header name of the chosen SUT Parameter which will be joined.
     * Current field is a reference to bind tow tables.
     */
    private List<Integer> idxReferenceHeaderNames = new ArrayList<>();

    /**
     * Checking whether all fields are filled in.
     */
    public boolean hasEmptyJoinConditionSettings() {
        return idxPrimaryHeaderNames.isEmpty() || Strings.isNullOrEmpty(pathReferenceSutParameterName)
                || idxReferenceHeaderNames.isEmpty();
    }
}
