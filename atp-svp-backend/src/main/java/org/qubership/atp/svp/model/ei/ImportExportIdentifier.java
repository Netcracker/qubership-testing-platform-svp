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

package org.qubership.atp.svp.model.ei;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.svp.core.exceptions.folder.FolderNotExistException;
import org.qubership.atp.svp.model.db.FolderEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public abstract class ImportExportIdentifier<T> implements ExportImportToEntity<T> {

    protected UUID id;
    protected String name;
    protected UUID sourceId;
    protected UUID folderId;
    @JsonIgnore
    protected transient FolderEntity folder;

    /**
     * Find and set folder from folders.
     *
     * @param folders folders
     */
    public void setFolder(List<FolderEntity> folders) {
        folder = folders.stream().filter(f -> {
                    if (f.getSourceId() == null) {
                        return folderId.equals(f.getFolderId());
                    } else {
                        return folderId.equals(f.getSourceId());
                    }
                })
                .findAny().orElseThrow(FolderNotExistException::new);
    }

    /**
     * Check name on duplicates and set new if needed with '_copy'.
     */
    public void makeNewName(List<String> existingNames) {
        StringBuilder result = new StringBuilder(this.name);

        int count = 0;
        int length = 0;
        while (existingNames.contains(result.toString())) {
            if (count == 0) {
                result.append("_Copy");
                length = result.length();
                count++;
            } else {
                result.setLength(length);
                result.append('(').append(count).append(')');
                count++;
            }
        }
        this.name = result.toString();
        existingNames.add(this.name);
    }
}
