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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JsonHierarchyNodeNames {

    /**
     * The name of the node id in the Json script for building a hierarchical sort.
     */
    private String id;

    /**
     * The name of the node rootId in the Json script for building a hierarchical sort.
     */
    private String rootId;

    /**
     * The name of the node parentId in the Json script for building a hierarchical sort.
     */
    private String parentId;

    /**
     * Checking whether all fields are filled in.
     */
    public boolean hasEmptyNodeNames() {
        return id.isEmpty() || rootId.isEmpty() || parentId.isEmpty();
    }
}
