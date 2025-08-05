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

package org.qubership.atp.svp.model.db;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.model.impl.Tab;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "page_configs", uniqueConstraints = {@UniqueConstraint(name = "PAGE_NAME_AND_FOLDER_ID_CONSTRAINT",
        columnNames = {"name", "folder_id"})})
public class PageConfigurationEntity {

    @Id
    @Column(name = "page_id")
    private UUID pageId;

    private String name;

    @Column(name = "synchronous_loading", nullable = false)
    private boolean synchronousLoading;

    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", foreignKey = @ForeignKey(name = "FK_folder_id__folder"), nullable = false)
    private FolderEntity folder;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime lastUpdateDateTime;

    @JsonManagedReference
    @OrderBy("tabOrder")
    @OneToMany(mappedBy = "pageConfiguration", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<TabEntity> tabEntities;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "\"order\"")
    private int order;

    /**
     * Static method converter PageConfiguration to PageConfigurationEntity.
     *
     * @param page PageConfiguration
     * @param folder FolderEntity
     * @return PageConfigurationEntity
     */
    public static PageConfigurationEntity createPageEntity(PageConfiguration page,
                                                           FolderEntity folder) {
        PageConfigurationEntity pageConfigurationEntity = new PageConfigurationEntity();
        if (Objects.nonNull(page.getPageId())) {
            pageConfigurationEntity.setPageId(page.getPageId());
        } else {
            pageConfigurationEntity.setPageId(UUID.randomUUID());
        }
        pageConfigurationEntity.setName(page.getName());
        pageConfigurationEntity.setSynchronousLoading(page.getIsSynchronousLoading());
        pageConfigurationEntity.setFolder(folder);
        List<Tab> tabs = page.getTabs();
        List<TabEntity> tabEntities = new ArrayList<>();
        for (int order = 0; order < tabs.size(); order++) {
            TabEntity tab = TabEntity.createTab(pageConfigurationEntity, tabs.get(order), order);
            tabEntities.add(tab);
        }
        pageConfigurationEntity.setTabEntities(tabEntities);
        pageConfigurationEntity.setOrder(page.getOrder());
        return pageConfigurationEntity;
    }

    /**
     * Updates the IDs of the page tree entities.
     */
    public void updateIds() {
        this.setPageId(UUID.randomUUID());
        this.getTabEntities().forEach(tab -> {
            tab.setTabId(UUID.randomUUID());
            tab.getGroupEntities().forEach(group -> {
                group.setGroupId(UUID.randomUUID());
                group.getSutParameterEntities().forEach(parameter ->
                        parameter.setParameterId(UUID.randomUUID()));
            });
        });
    }
}



