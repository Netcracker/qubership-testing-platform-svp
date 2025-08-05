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
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.UpdateTimestamp;
import org.qubership.atp.svp.model.configuration.KeyParameterConfiguration;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Entity
@Table(name = "key_parameters", uniqueConstraints = {@UniqueConstraint(name = "KEY_NAME_AND_FOLDER_ID_CONSTRAINT",
        columnNames = {"name", "folder_id"})})
@Data
@AllArgsConstructor
@Accessors(chain = true)
@NoArgsConstructor
public class KeyParameterEntity {

    @Id
    @Column(name = "key_parameter_id")
    private UUID keyParameterId;

    private String name;

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

    @Column(name = "key_order", nullable = false)
    private int keyOrder;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Column(name = "source_id")
    private UUID sourceId;

    /**
     * Constructor for convert KeyParameterConfiguration to KeyParameterEntity.
     *
     * @param folder folder entity
     * @param keyParameter key parameter from file
     */
    public KeyParameterEntity(FolderEntity folder, KeyParameterConfiguration keyParameter, int order) {
        this.folder = folder;
        if (Objects.nonNull(keyParameter.getId())) {
            this.keyParameterId = keyParameter.getId();
        } else {
            this.keyParameterId = UUID.randomUUID();
        }
        this.name = keyParameter.getName();
        this.keyOrder = order;
        this.sourceId = keyParameter.getSourceId();
    }
}
