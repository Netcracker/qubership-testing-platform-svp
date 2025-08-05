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

import java.io.Serializable;

import org.qubership.atp.svp.model.db.KeyParameterEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ExportImportKeyParameter extends ImportExportIdentifier<KeyParameterEntity> implements Serializable {

    private static final long serialVersionUID = -6104946818460218660L;

    /**
     * Constructor converter KeyParameterEntity to KeyParameterConfiguration.
     *
     * @param keyParameterEntity KeyParameterEntity
     */
    public ExportImportKeyParameter(KeyParameterEntity keyParameterEntity) {
        this.id = keyParameterEntity.getKeyParameterId();
        this.folderId = keyParameterEntity.getFolder().getFolderId();
        this.name = keyParameterEntity.getName();
    }

    @Override
    public KeyParameterEntity toEntity() {
        return new KeyParameterEntity()
                .setKeyParameterId(id)
                .setName(name)
                .setSourceId(sourceId)
                .setFolder(folder);
    }
}
