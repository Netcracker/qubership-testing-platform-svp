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
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Environment extends AbstractConfiguratorModel {

    private UUID projectId;
    private List<System> systems;
    private String graylogName;

    /**
     * Gets system by name.
     *
     * @param systemName system name
     * @return system name, throw RuntimeException if not found
     */
    public System getSystem(String systemName) {
        final Optional<System> system = systems.stream().filter(sys -> systemName.equals(sys.getName())).findFirst();
        if (system.isPresent()) {
            return system.get();
        }
        throw new RuntimeException("No system with name (" + systemName + ") in environment!");
    }

    /**
     * Check is exist system.
     *
     * @return boolean
     */
    public boolean isExistSystem(String systemName) {
        return systems.stream().anyMatch(sys -> systemName.equals(sys.getName()));
    }

    @Override
    public String toString() {
        return "Environment{"
                + "projectId=" + projectId
                + ", environmentId=" + super.getId()
                + ", environmentName=" + super.getName()
                + '}';
    }
}
