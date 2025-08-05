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

import java.util.Set;

import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.core.exceptions.SourceSettingNotFoundException;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Source {

    private String system;
    private String connection;
    private EngineType engineType;
    private String script;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private Set<SourceSettings> settings;

    /**
     * Search in the Settings array and returns the SourceSettings of the specified corresponding class.
     */
    public <T extends SourceSettings> SourceSettings getSettingsByType(Class<T> type) {
        return settings.stream().filter(setting -> setting.getClass().equals(type))
                .findFirst().<RuntimeException>orElseThrow(() -> {
                    throw new SourceSettingNotFoundException("Not found settings for type: " + type.getName());
                });
    }
}
