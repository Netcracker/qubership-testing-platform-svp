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
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionTabEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TabDto {

    private String name;
    private ValidationStatus status;
    private List<GroupDto> groups;

    /**
     * Create TabDto Object.
     *
     * @param tab               PotSessionTab
     * @param pageConfiguration PageConfiguration
     */
    public TabDto(PotSessionTabEntity tab, PageConfigurationEntity pageConfiguration) {
        this.name = tab.getName();
        this.status = tab.getValidationStatus();
        this.groups = creatGroupsDto(tab, pageConfiguration);
    }

    private List<GroupDto> creatGroupsDto(PotSessionTabEntity tab, PageConfigurationEntity pageConfiguration) {
        List<GroupDto> groups =
                pageConfiguration.getTabEntities()
                        .stream()
                        .filter(confTab -> confTab.getName().equals(tab.getName()))
                        .flatMap(foundConfTab -> foundConfTab.getGroupEntities().stream())
                        .filter(confGroup -> !confGroup.isHide())
                        .map(confGroup -> new GroupDto(confGroup.getName()))
                        .collect(Collectors.toList());
        fillGroups(tab, groups);
        return groups;
    }

    private void fillGroups(PotSessionTabEntity tab, List<GroupDto> groups) {
        groups.forEach(groupDto -> tab.getPotSessionParameterEntities()
                .stream()
                .filter(potSessionParameter -> potSessionParameter.getGroup().equals(groupDto.getName()))
                .forEach(groupDto::addSutParameter)
        );
    }
}
