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

package org.qubership.atp.svp.model.api.ram;

import java.util.List;

import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.pot.validation.ValidationInfo;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SutParameterDto {

    private String name;
    private String status;
    private String groupName;
    private List<AbstractValueObject> arValues;
    private AbstractValueObject er;
    private ValidationInfo validationInfo;

    /**
     * Create SutParameterDto object.
     *
     * @param parameter PotSessionParameter
     */
    public SutParameterDto(PotSessionParameterEntity parameter) {
        this.name = parameter.getName();
        this.status = parameter.getValidationInfo().getStatus().getValidationStatusToString();
        this.groupName = parameter.getGroup();
        this.arValues = parameter.getArValues();
        this.er = parameter.getEr();
        this.validationInfo = parameter.getValidationInfo();
    }
}
