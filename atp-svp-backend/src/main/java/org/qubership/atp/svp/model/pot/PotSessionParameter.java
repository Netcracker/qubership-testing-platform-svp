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

package org.qubership.atp.svp.model.pot;

import java.util.ArrayList;
import java.util.List;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.impl.SutParameter;
import org.qubership.atp.svp.model.pot.validation.ValidationInfo;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PotSessionParameter {

    private String page;
    private String tab;
    private String group;
    private SutParameter parameterConfig;
    private boolean isSynchronousLoading;

    private AbstractValueObject er;
    private List<AbstractValueObject> arValues = new ArrayList<>();

    private ValidationInfo validationInfo;

    /**
     * The constructor of PotSessionParameter.
     */
    public PotSessionParameter(PotSessionParameterEntity parameter) {
        this.page = parameter.getPage();
        this.tab = parameter.getTab();
        this.group = parameter.getGroup();
        this.parameterConfig = new SutParameter(parameter.getParameterConfig());
        this.arValues = parameter.getArValues();
        this.isSynchronousLoading = parameter.isSynchronousLoading();
        this.validationInfo = parameter.getValidationInfo();
        this.er = parameter.getEr();
    }

    /**
     * Initializes POT session parameter with initial validation info.
     */
    public PotSessionParameter(String page, String tab, String group,
                               SutParameter parameterConfig,
                               boolean isSynchronousLoading) {
        this.page = page;
        this.tab = tab;
        this.group = group;
        this.parameterConfig = parameterConfig;
        this.isSynchronousLoading = isSynchronousLoading;
        this.validationInfo = getInitialValidationInfo();
    }

    /**
     * Initializes top session parameter.
     */
    public PotSessionParameter(SutParameter parameterConfig) {
        this.parameterConfig = parameterConfig;
        this.validationInfo = getInitialValidationInfo();
    }

    public String getName() {
        return parameterConfig.getName();
    }

    public String getComponentName() {
        return parameterConfig.getComponent();
    }

    public boolean getIsSynchronousLoading() {
        return isSynchronousLoading;
    }

    public void setIsSynchronousLoading(boolean synchronousLoading) {
        isSynchronousLoading = synchronousLoading;
    }

    /**
     * Gets parameter's path representation separated by slashes.
     */
    public String getPath() {
        return page + "_" + tab + "_" + group + "_" + getName();
    }

    private ValidationInfo getInitialValidationInfo() {
        return new ValidationInfo(ValidationStatus.IN_PROGRESS);
    }
}
