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

package org.qubership.atp.svp.model.pot.values;

import java.io.Serializable;

import org.qubership.atp.svp.model.pot.PotFile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "TableValueObject", value = TableValueObject.class),
        @JsonSubTypes.Type(name = "ErrorValueObject", value = ErrorValueObject.class),
        @JsonSubTypes.Type(name = "LogCollectorValueObject", value = LogCollectorValueObject.class),
        @JsonSubTypes.Type(name = "SimpleValueObject", value = SimpleValueObject.class),
})
public abstract class AbstractValueObject implements Serializable {

    @JsonIgnore
    private transient PotFile valueAsFile;

    public abstract boolean hasData();

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    public String getType() {
        return this.getClass().getSimpleName();
    }
}
