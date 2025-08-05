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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "className", defaultImpl = HttpSettings.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HttpSettings.class, name = "HttpSettings"),
        @JsonSubTypes.Type(value = TableSettings.class, name = "TableSettings"),
        @JsonSubTypes.Type(value = LogCollectorSettings.class, name = "LogCollectorSettings"),
        @JsonSubTypes.Type(value = JsonParseSettings.class, name = "JsonParseSettings"),
        @JsonSubTypes.Type(value = GenerateLinkSettings.class, name = "GenerateLinkSettings")
})
public interface SourceSettings {

}
