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

package org.qubership.atp.svp.model.api.tsg;

import java.util.HashSet;
import java.util.Set;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;

import lombok.Data;

@Data
public class PreconfiguredValidation {

    private Set<Component> components;

    public PreconfiguredValidation() {
        this.components = new HashSet<>();
    }

    /**
     * Adds the validation result to the component of preconfigured validation.
     *
     * @param parameterName - name of preconfigured parameter.
     * @param ar            - abstract AR (Table or String)
     * @param er            - abstract ER (Table or String)
     * @param validation    - status of the validation AR with ER
     * @param componentName - name of component
     */
    public void addResultToComponent(String parameterName, AbstractValueObject ar, AbstractValueObject er,
                                     ValidationStatus validation, String componentName) {
        components.add(new Component(componentName));
        components.stream().filter(component -> component.getName().equals(componentName))
                .findFirst().ifPresent(component -> component.addParameterResult(parameterName, ar, er, validation));
    }
}
