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
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.qubership.atp.svp.model.impl.SutParameter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Entity
@Table(name = "common_parameters")
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class CommonParameterEntity {

    @Id
    @Column(name = "parameter_id")
    private UUID commonParameterId;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "parameter_id", foreignKey = @ForeignKey(name = "FK_parameter_id__parameter"))
    private SutParameterEntity sutParameterEntity;

    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "folder_id", foreignKey = @ForeignKey(name = "FK_folder_id__folder"), nullable = false)
    private FolderEntity folder;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Column(name = "source_id")
    private UUID sourceId;

    /**
     * Constructor converter SutParameter to SutParameterEntity.
     *
     * @param parameter SutParameter
     * @param folder FolderEntity
     */
    public CommonParameterEntity(FolderEntity folder, SutParameter parameter, int order) {
        if (Objects.nonNull(parameter.getParameterId())) {
            this.commonParameterId = parameter.getParameterId();
        } else {
            this.commonParameterId = UUID.randomUUID();
        }
        this.folder = folder;
        this.sutParameterEntity = new SutParameterEntity(parameter, order);
        this.sourceId = parameter.getSourceId();
    }
}
