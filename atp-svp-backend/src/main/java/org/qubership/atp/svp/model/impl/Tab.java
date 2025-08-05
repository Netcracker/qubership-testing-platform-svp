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

import org.qubership.atp.svp.model.db.TabEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tab {

    private UUID tabId;
    private String name;
    private List<Group> groups;
    private Boolean isSynchronousLoading;

    /**
     * Constructor converter TabEntity to Tab.
     * @param tabEntity TabEntity
     */
    public Tab(TabEntity tabEntity) {
        this.tabId = tabEntity.getTabId();
        this.name = tabEntity.getName();
        this.isSynchronousLoading = tabEntity.isSynchronousLoading();
        this.groups = tabEntity.getGroupEntities().stream().map(Group::new).collect(Collectors.toList());
    }

    public Boolean getIsSynchronousLoading() {
        return Optional.ofNullable(isSynchronousLoading).orElse(false);
    }
}
