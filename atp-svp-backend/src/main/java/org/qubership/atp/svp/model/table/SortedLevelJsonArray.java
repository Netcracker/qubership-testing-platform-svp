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
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)

//TODO remove JsonElement inheritance and rewrite the methods used.
public class SortedLevelJsonArray extends JsonElement implements Iterable<JsonElementNestingDepth> {

    /**
     * Here you create a list of json elements sorted in hierarchical order.
     */
    private List<JsonElementNestingDepth> elements;

    public SortedLevelJsonArray() {
        this.elements = new ArrayList();
    }

    @Override
    public SortedLevelJsonArray deepCopy() {
        return null;
    }

    public void add(JsonElementNestingDepth obj) {
        this.elements.add(obj);
    }

    @NotNull
    @Override
    public Iterator<JsonElementNestingDepth> iterator() {
        return this.elements.iterator();
    }
}
