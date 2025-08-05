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
import java.util.List;

import org.qubership.atp.svp.core.enums.DisplayType;
import org.qubership.atp.svp.model.db.CommonParameterEntity;
import org.qubership.atp.svp.model.db.SutParameterEntity;
import org.qubership.atp.svp.model.impl.ErConfig;
import org.qubership.atp.svp.model.impl.Source;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ExportImportCommonParameter extends ImportExportIdentifier<CommonParameterEntity> implements Serializable {

    private static final long serialVersionUID = 2925033595743126988L;
    private DisplayType displayType;
    private Source dataSource;
    private ErConfig er;
    private List<Source> additionalSources;
    private boolean isPreconfigured;
    private String component;

    /**
     * Constructor converter CommonParameterEntity to ExportImportCommonParameter.
     *
     * @param commonParameter CommonParameterEntity
     */
    public ExportImportCommonParameter(CommonParameterEntity commonParameter) {
        SutParameterEntity parameterEntity = commonParameter.getSutParameterEntity();
        this.id = parameterEntity.getParameterId();
        this.name = parameterEntity.getName();
        this.displayType = parameterEntity.getDisplayType();
        this.dataSource = parameterEntity.getSource();
        this.er = parameterEntity.getErConfig();
        this.additionalSources = parameterEntity.getAdditionalSources();
        this.isPreconfigured = parameterEntity.isPreconfigured();
        this.component = parameterEntity.getComponent();
        this.folderId = commonParameter.getFolder().getFolderId();
    }

    @Override
    public CommonParameterEntity toEntity() {
        return new CommonParameterEntity()
                .setCommonParameterId(id)
                .setSourceId(sourceId)
                .setFolder(folder)
                .setSutParameterEntity(new SutParameterEntity()
                        .setParameterId(id)
                        .setName(name)
                        .setDisplayType(displayType)
                        .setSource(dataSource)
                        .setErConfig(er)
                        .setAdditionalSources(additionalSources)
                        .setPreconfigured(isPreconfigured)
                        .setComponent(component));
    }
}
