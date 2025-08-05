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

import org.qubership.atp.svp.model.db.FolderEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FolderConfiguration {

    private UUID id;
    private String name;
    private UUID sourceId;
    private UUID projectId;

    /**
     * Constructor for creating FolderConfiguration from FolderEntity.
     *
     * @param folder FolderEntity
     */
    public FolderConfiguration(FolderEntity folder) {
        this.id = folder.getFolderId();
        this.name = folder.getName();
        this.sourceId = folder.getSourceId();
        this.projectId = folder.getProject().getProjectId();
    }
}
