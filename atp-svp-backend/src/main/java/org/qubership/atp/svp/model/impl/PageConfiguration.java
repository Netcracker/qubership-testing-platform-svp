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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageConfiguration {

    private UUID pageId;
    private String name;
    private List<Tab> tabs;
    private Boolean isSynchronousLoading;
    private UUID sourceId;
    private int order;

    /**
     * Constructor converter PageConfigurationEntity to PageConfiguration.
     * @param pageConfigurationEntity PageConfigurationEntity
     */
    public PageConfiguration(PageConfigurationEntity pageConfigurationEntity) {
        this.pageId = pageConfigurationEntity.getPageId();
        this.name = pageConfigurationEntity.getName();
        this.isSynchronousLoading = pageConfigurationEntity.isSynchronousLoading();
        this.tabs = pageConfigurationEntity.getTabEntities().stream()
                .map(Tab::new)
                .collect(Collectors.toList());
        this.order = pageConfigurationEntity.getOrder();
    }

    public Boolean getIsSynchronousLoading() {
        return Optional.ofNullable(isSynchronousLoading).orElse(false);
    }

    /**
     * Checks if page has any preconfigured parameter.
     */
    public boolean hasPreconfiguredParameters() {
        return tabs.stream().anyMatch(tab ->
                tab.getGroups().stream().anyMatch(group ->
                        group.getSutParameters().stream().anyMatch(SutParameter::getIsPreconfigured)));
    }

    /**
     * Checks if page has any LOG_COLLECTOR EngineType.
     */
    public boolean hasLogCollectorType() {
        return tabs.stream()
                .anyMatch(tab -> tab.getGroups().stream()
                        .anyMatch(group -> group.getSutParameters().stream()
                                .anyMatch(sutParameter -> sutParameter
                                        .getDataSource().getEngineType()
                                        .equals(EngineType.LOG_COLLECTOR))));
    }
}
