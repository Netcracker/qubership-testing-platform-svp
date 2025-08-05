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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.UpdateTimestamp;
import org.qubership.atp.svp.core.enums.DisplayType;
import org.qubership.atp.svp.core.enums.ValidationType;
import org.qubership.atp.svp.model.impl.ErConfig;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.impl.SutParameter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Entity
@Table(name = "sut_parameter_config")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "json", typeClass = JsonType.class)
@Accessors(chain = true)
public class SutParameterEntity {

    @Id
    @Column(name = "parameter_id", nullable = false)
    private UUID parameterId;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_type")
    private DisplayType displayType;

    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private Source source;

    private String component;

    @Type(type = "json")
    @Column(name = "additional_sources", columnDefinition = "jsonb")
    private List<Source> additionalSources;

    @Type(type = "json")
    @Column(name = "er_config", columnDefinition = "jsonb")
    private ErConfig erConfig;

    @Column(name = "preconfigured", nullable = false)
    private boolean preconfigured;

    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "group_id", foreignKey = @ForeignKey(name = "FK_group_id__group"))
    private GroupEntity groupEntity;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime lastUpdateDateTime;

    @Column(name = "parameter_order", nullable = false)
    private int parameterOrder;

    /**
     * Constructor converter SutParameter to SutParameterEntity.
     *
     * @param parameter SutParameter
     * @param groupEntity GroupEntity
     */
    public SutParameterEntity(SutParameter parameter, GroupEntity groupEntity, int order) {
        this.name = parameter.getName();
        if (Objects.nonNull(parameter.getParameterId())) {
            this.parameterId = parameter.getParameterId();
        } else {
            this.parameterId = UUID.randomUUID();
        }
        this.displayType = parameter.getDisplayType();
        this.source = parameter.getDataSource();
        this.component = parameter.getComponent();
        this.additionalSources = parameter.getAdditionalSources();
        this.erConfig = parameter.getEr();
        this.preconfigured = parameter.getIsPreconfigured();
        this.groupEntity = groupEntity;
        this.parameterOrder = order;
    }

    /**
     * Constructor converter SutParameter to SutParameterEntity for CommonParameterEntity.
     *
     * @param parameter SutParameter
     */
    public SutParameterEntity(SutParameter parameter, int order) {
        if (Objects.nonNull(parameter.getParameterId())) {
            this.parameterId = parameter.getParameterId();
        } else {
            this.parameterId = UUID.randomUUID();
        }
        this.name = parameter.getName();
        this.displayType = parameter.getDisplayType();
        this.source = parameter.getDataSource();
        this.component = parameter.getComponent();
        this.additionalSources = parameter.getAdditionalSources();
        this.erConfig = parameter.getEr();
        this.preconfigured = parameter.getIsPreconfigured();
        this.parameterOrder = order;
    }

    /**
     * Check validation status.
     *
     * @return true if parameter allows validation, otherwise - false.
     *         Validation is considered allowed in the case
     *         when validation type is not {@link ValidationType#NONE}.
     */
    public boolean allowsValidation() {
        return erConfig.getType() != ValidationType.NONE;
    }

    /**
     * Update entity by imported entity.
     */
    public void updateEntity(SutParameterEntity importedCommonParameter) {
        this.name = importedCommonParameter.getName();
        this.displayType = importedCommonParameter.getDisplayType();
        this.source = importedCommonParameter.getSource();
        this.component = importedCommonParameter.getComponent();
        this.additionalSources = importedCommonParameter.getAdditionalSources();
        this.erConfig = importedCommonParameter.getErConfig();
        this.preconfigured = importedCommonParameter.isPreconfigured();
        this.parameterOrder = importedCommonParameter.getParameterOrder();
    }
}
