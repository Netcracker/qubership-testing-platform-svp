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
import java.util.stream.Collectors;

import org.qubership.atp.svp.model.db.pot.session.PotSessionPageEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SessionDto {

    private List<SutParameterDto> commonParameters = new ArrayList<>();
    private List<PageDto> pages = new ArrayList<>();

    /**
     * Create SessionDto Object.
     */
    public SessionDto(List<PotSessionParameterEntity> parameterEntities, List<PotSessionPageEntity> pages) {
        this.commonParameters = parameterEntities.stream().map(SutParameterDto::new).collect(Collectors.toList());
        this.pages = pages.stream().map(PageDto::new).collect(Collectors.toList());
    }
}
