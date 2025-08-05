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
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
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
import org.qubership.atp.svp.model.impl.Group;
import org.qubership.atp.svp.model.impl.Tab;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "tab_configs", uniqueConstraints = {@UniqueConstraint(name = "TAB_NAME_AND_PAGE_ID_CONSTRAINT",
        columnNames = {"name", "page_id"})})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TabEntity {

    @Id
    @GeneratedValue
    @Column(name = "tab_id", nullable = false)
    private UUID tabId;

    private String name;

    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne()
    @JoinColumn(name = "page_id", foreignKey = @ForeignKey(name = "FK_page_id__page"))
    private PageConfigurationEntity pageConfiguration;

    @Column(name = "synchronous_loading", nullable = false)
    private boolean synchronousLoading;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime lastUpdateDateTime;

    @Column(name = "tab_order", nullable = false)
    private int tabOrder;

    @JsonManagedReference
    @OrderBy("groupOrder")
    @OneToMany(mappedBy = "tabEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<GroupEntity> groupEntities;

    /**
     * Static method converter Tab to TabEntity.
     *
     * @param page PageConfigurationEntity
     * @param tab Tab
     * @return TabEntity
     */
    public static TabEntity createTab(PageConfigurationEntity page, Tab tab, int tabOrder) {
        TabEntity tabEntity = new TabEntity();
        tabEntity.setTabId(tab.getTabId());
        tabEntity.setName(tab.getName());
        tabEntity.setSynchronousLoading(tab.getIsSynchronousLoading());
        tabEntity.setPageConfiguration(page);
        tabEntity.setTabOrder(tabOrder);
        List<Group> groups = tab.getGroups();
        List<GroupEntity> groupEntities = new ArrayList<>();
        for (int order = 0; order < groups.size(); order++) {
            GroupEntity group = GroupEntity.createGroup(groups.get(order), tabEntity, order);
            groupEntities.add(group);
        }
        tabEntity.setGroupEntities(groupEntities);
        return tabEntity;
    }
}
