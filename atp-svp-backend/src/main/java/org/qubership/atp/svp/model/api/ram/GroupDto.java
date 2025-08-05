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

import java.util.ArrayList;
import java.util.List;

import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GroupDto {

    private String name;
    private List<SutParameterDto> sutParameters;

    /**
     * Create GroupDto Object.
     *
     * @param nameGroup String
     */
    public GroupDto(String nameGroup) {
        this.name = nameGroup;
        this.sutParameters = new ArrayList<>();
    }

    /**
     * Add SutParameter to GroupDto.
     *
     * @param sutParameter PotSessionParameter
     */
    public void addSutParameter(PotSessionParameterEntity sutParameter) {
        this.sutParameters.add(new SutParameterDto(sutParameter));
    }
}
