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

package org.qubership.atp.svp.model.configuration;

import java.util.UUID;

import org.qubership.atp.svp.model.db.KeyParameterEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeyParameterConfiguration {

    private UUID id;
    private String name;
    private int order;
    private UUID folderId;
    private UUID sourceId;

    /**
     * Constructor for creating KeyParameterConfiguration from KeyParameterEntity.
     *
     * @param keyParameter KeyParameterEntity
     */
    public KeyParameterConfiguration(KeyParameterEntity keyParameter) {
        this.id = keyParameter.getKeyParameterId();
        this.name = keyParameter.getName();
        this.order = keyParameter.getKeyOrder();
        this.folderId = keyParameter.getFolder().getFolderId();
        this.sourceId = keyParameter.getSourceId();
    }

    /**
     * Constructor for creating KeyParameterConfiguration by String name.
     *
     * @param name String
     */
    public KeyParameterConfiguration(String name) {
        this.name = name;
    }
}
