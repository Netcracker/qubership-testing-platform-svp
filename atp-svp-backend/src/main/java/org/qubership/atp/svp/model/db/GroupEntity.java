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
import javax.persistence.FetchType;
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
import org.qubership.atp.svp.model.impl.SutParameter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "group_configs", uniqueConstraints = {@UniqueConstraint(name = "GROUP_NAME_AND_TAB_ID_CONSTRAINT",
        columnNames = {"name", "tab_id"})})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupEntity {

    @Id
    @GeneratedValue
    @Column(name = "group_id")
    private UUID groupId;

    private String name;

    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "tab_id", foreignKey = @ForeignKey(name = "FK_tab_id__tab"))
    private TabEntity tabEntity;

    @Column(name = "synchronous_loading", nullable = false)
    private boolean synchronousLoading;

    @Column(name = "hidden", nullable = false)
    private boolean hide;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime lastUpdateDateTime;

    @Column(name = "group_order", nullable = false)
    private int groupOrder;
    @JsonManagedReference
    @OrderBy("parameterOrder")
    @OneToMany(mappedBy = "groupEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<SutParameterEntity> sutParameterEntities;

    /**
     * Static method converter Group to GroupEntity.
     *
     * @param group Group
     * @param tab TabEntity
     * @return GroupEntity
     */
    public static GroupEntity createGroup(Group group, TabEntity tab, int groupOrder) {
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setGroupId(group.getGroupId());
        groupEntity.setName(group.getName());
        groupEntity.setSynchronousLoading(group.getIsSynchronousLoading());
        groupEntity.setTabEntity(tab);
        groupEntity.setHide(group.getIsHide());
        groupEntity.setGroupOrder(groupOrder);
        List<SutParameter> parameters = group.getSutParameters();
        List<SutParameterEntity> parameterEntities = new ArrayList<>();
        for (int order = 0; order < parameters.size(); order++) {
            SutParameterEntity parameter = new SutParameterEntity(parameters.get(order), groupEntity, order);
            parameterEntities.add(parameter);
        }
        groupEntity.setSutParameterEntities(parameterEntities);
        return groupEntity;
    }
}
