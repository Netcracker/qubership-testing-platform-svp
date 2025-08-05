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

package org.qubership.atp.svp.model.folder;

import lombok.Data;

@Data
public class FolderRequest {

    private String name;
    private String oldName;

    public void setName(String name) {
        this.name = (name != null) ? name.trim() : null;
    }

    public void setOldName(String oldName) {
        this.oldName = (oldName != null) ? oldName.trim() : null;
    }
}
