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
import org.qubership.atp.svp.model.db.GroupEntity;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionPageEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PageDto {

    private String name;
    private ValidationStatus status;
    private List<TabDto> tabs;

    /**
     * Create PageDto object.
     *
     * @param page PotSessionPage
     */
    public PageDto(PotSessionPageEntity page) {
        this.name = page.getName();
        this.status = page.getValidationStatus();
        this.tabs = createTabsDto(page);
    }

    private List<TabDto> createTabsDto(PotSessionPageEntity page) {
        PageConfigurationEntity pageConfiguration = page.getPageConfiguration();
        return page.getPotSessionTabs()
                .stream()
                .filter(potTab -> pageConfiguration.getTabEntities()
                        .stream()
                        .filter(confTab -> confTab.getName().equals(potTab.getName()))
                        .noneMatch(confTab -> confTab.getGroupEntities()
                                .stream()
                                .allMatch(GroupEntity::isHide)))
                .map(tab -> new TabDto(tab, pageConfiguration)).collect(Collectors.toList());
    }
}
