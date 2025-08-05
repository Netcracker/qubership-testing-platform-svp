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

package org.qubership.atp.svp.model.environments;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class System extends AbstractConfiguratorModel {

    private String environmentId;
    private List<Connection> connections;
    private UUID systemCategoryId;
    private Status status;
    private Long dateOfLastCheck;
    private String version;
    private Long dateOfCheckVersion;
    private ParametersGettingVersion parametersGettingVersion;
    private UUID parentSystemId;
    @JsonProperty("serverITF")
    private ServerItf serverItf;
    private Boolean mergeByName;
    private UUID linkToSystemId;
    private UUID externalId;

    /**
     * Get server by connection name.
     *
     * @param name connection name
     * @return Server
     * @throws IllegalArgumentException if connection not found
     */
    public Server getServer(String name) throws IllegalArgumentException {
        return new Server(
                connections.stream()
                        .filter(system -> name.equalsIgnoreCase(system.getName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Connection '" + name + "' not found")),
                name);
    }
}
