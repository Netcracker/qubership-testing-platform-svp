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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;

import lombok.Data;

@Data
public class Component {

    private String name;
    private List<ParameterResult> parameters;

    public Component(String name) {
        this.name = name;
        this.parameters = new ArrayList<>();
    }

    /**
     * Adds the validation result to the component of preconfigured validation.
     *
     * @param name       - name of preconfigured parameter.
     * @param ar         - abstract AR (Table or String)
     * @param er         - abstract ER (Table or String)
     * @param validation - status of the validation AR with ER
     */
    public void addParameterResult(String name, AbstractValueObject ar,
                                   AbstractValueObject er, ValidationStatus validation) {
        ParameterResult parameter = new ParameterResult();
        parameter.setName(name);
        parameter.setAr(getResultFromAbstractValue(ar));
        parameter.setEr(getResultFromAbstractValue(er));
        if (Objects.nonNull(validation)) {
            parameter.setResult(validation.toString().toLowerCase());
        }
        parameters.add(parameter);
    }

    private Object getResultFromAbstractValue(AbstractValueObject abstractValue) {
        if (abstractValue instanceof SimpleValueObject) {
            return ((SimpleValueObject) abstractValue).getValue();
        }
        if (abstractValue instanceof TableValueObject) {
            return ((TableValueObject) abstractValue).getTable();
        }
        return null;
    }

    @Override
    public boolean equals(Object component) {
        return component instanceof Component
                && Objects.equals(name, ((Component) component).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
