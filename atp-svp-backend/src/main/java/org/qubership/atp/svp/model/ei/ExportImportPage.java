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

package org.qubership.atp.svp.model.ei;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.TabEntity;
import org.qubership.atp.svp.model.impl.Tab;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ExportImportPage extends ImportExportIdentifier<PageConfigurationEntity> implements Serializable {

    private static final long serialVersionUID = 3659869867489838000L;
    private List<Tab> tabs;
    private Boolean isSynchronousLoading;
    private int order;

    /**
     * Constructor converter PageConfigurationEntity to PageConfiguration.
     *
     * @param pageConfigurationEntity PageConfigurationEntity
     */
    public ExportImportPage(PageConfigurationEntity pageConfigurationEntity) {
        this.id = pageConfigurationEntity.getPageId();
        this.name = pageConfigurationEntity.getName();
        this.isSynchronousLoading = pageConfigurationEntity.isSynchronousLoading();
        this.tabs = pageConfigurationEntity.getTabEntities().stream()
                .map(Tab::new)
                .collect(Collectors.toList());
        this.folderId = pageConfigurationEntity.getFolder().getFolderId();
        this.order = pageConfigurationEntity.getOrder();
    }

    @Override
    public PageConfigurationEntity toEntity() {
        PageConfigurationEntity pageConfigurationEntity = new PageConfigurationEntity();
        List<TabEntity> tabEntities = new ArrayList<>();
        for (int order = 0; order < tabs.size(); order++) {
            TabEntity tab = TabEntity.createTab(pageConfigurationEntity, tabs.get(order), order);
            tabEntities.add(tab);
        }
        return pageConfigurationEntity
                .setPageId(id)
                .setName(name)
                .setSynchronousLoading(isSynchronousLoading)
                .setFolder(folder)
                .setTabEntities(tabEntities)
                .setSourceId(sourceId);
    }
}
