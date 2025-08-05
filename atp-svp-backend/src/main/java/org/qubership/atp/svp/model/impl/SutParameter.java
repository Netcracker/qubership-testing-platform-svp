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
import java.util.UUID;

import org.qubership.atp.svp.core.enums.DisplayType;
import org.qubership.atp.svp.core.enums.ValidationType;
import org.qubership.atp.svp.model.db.CommonParameterEntity;
import org.qubership.atp.svp.model.db.SutParameterEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SutParameter {

    private UUID parameterId;
    private String name;
    private DisplayType displayType;
    private Source dataSource;
    private ErConfig er;
    private List<Source> additionalSources = new ArrayList<>();
    private boolean isPreconfigured;
    private String component;
    private UUID folderId;
    private UUID sourceId;

    /**
     * Constructor converter SutParameterEntity to SutParameter.
     *
     * @param parameterEntity SutParameterEntity
     */
    public SutParameter(SutParameterEntity parameterEntity) {
        this.parameterId = parameterEntity.getParameterId();
        this.name = parameterEntity.getName();
        this.displayType = parameterEntity.getDisplayType();
        this.dataSource = parameterEntity.getSource();
        this.er = parameterEntity.getErConfig();
        this.additionalSources = parameterEntity.getAdditionalSources();
        this.isPreconfigured = parameterEntity.isPreconfigured();
        this.component = parameterEntity.getComponent();
    }

    /**
     * Constructor converter CommonParameterEntity to SutParameter.
     *
     * @param commonParameter CommonParameterEntity
     */
    public SutParameter(CommonParameterEntity commonParameter) {
        SutParameterEntity parameterEntity = commonParameter.getSutParameterEntity();
        this.parameterId = parameterEntity.getParameterId();
        this.name = parameterEntity.getName();
        this.displayType = parameterEntity.getDisplayType();
        this.dataSource = parameterEntity.getSource();
        this.er = parameterEntity.getErConfig();
        this.additionalSources = parameterEntity.getAdditionalSources();
        this.isPreconfigured = parameterEntity.isPreconfigured();
        this.component = parameterEntity.getComponent();
        this.folderId = commonParameter.getFolder().getFolderId();
        this.sourceId = commonParameter.getSourceId();
    }

    public boolean getIsPreconfigured() {
        return isPreconfigured;
    }

    public void setIsPreconfigured(boolean isPreconfigured) {
        this.isPreconfigured = isPreconfigured;
    }

    /**
     * Constructor for TESTS ONLY.
     */
    public SutParameter(String name, ErConfig er) {
        this.name = name;
        this.er = er;
    }

    /**
     * Check validation status.
     * @return true if parameter allows validation, otherwise - false.
     *         Validation is considered allowed in the case
     *         when validation type is not {@link ValidationType#NONE}.
     */
    public boolean allowsValidation() {
        return !er.getType().equals(ValidationType.NONE);
    }
}
