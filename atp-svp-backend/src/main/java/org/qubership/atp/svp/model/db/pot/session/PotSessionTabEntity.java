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

package org.qubership.atp.svp.model.db.pot.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TypeDef;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.TabEntity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pot_session_tab", indexes = {
        @Index(name = "pot_session_tab_pot_session_page_id_index", columnList = "pot_session_page_id")
})
@Getter
@Setter
@NoArgsConstructor
@TypeDef(name = "json", typeClass = JsonType.class)
public class PotSessionTabEntity {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @Column(name = "synchronous_loading")
    private boolean synchronousLoading;

    @Column(name = "already_validated")
    private boolean alreadyValidated;

    @Column(name = "validation_status")
    private ValidationStatus validationStatus;

    @OneToMany(mappedBy = "potSessionTabEntity", cascade = CascadeType.ALL, orphanRemoval =
            true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PotSessionParameterEntity> potSessionParameterEntities;

    @ManyToOne
    @JoinColumn(name = "pot_session_page_id", foreignKey = @ForeignKey(name = "FK_pot_session_page_id"))
    private PotSessionPageEntity potSessionPageEntity;

    public PotSessionTabEntity(TabEntity tab) {
        this.name = tab.getName();
        this.synchronousLoading = tab.isSynchronousLoading();
    }

    /**
     * The constructor of PotSessionTabEntity.
     */
    public static PotSessionTabEntity createPotSessionTab(PotSessionPageEntity page, TabEntity tab) {
        PotSessionTabEntity tabEntity = new PotSessionTabEntity(tab);
        tabEntity.setPotSessionPageEntity(page);
        tabEntity.setPotSessionParameterEntities(generateParameters(tab, page.getName(), tabEntity));
        tabEntity.setValidationStatus(tabEntity.getInitialValidationStatus());
        return tabEntity;
    }

    private static List<PotSessionParameterEntity> generateParameters(TabEntity tab, String pageName,
                                                                      PotSessionTabEntity tabEntity) {
        return tab.getGroupEntities().stream().flatMap(groupEntity -> groupEntity.getSutParameterEntities()
                        .stream().map(sutParameterEntity -> new PotSessionParameterEntity(sutParameterEntity,
                                tabEntity, pageName, groupEntity.getName(), groupEntity.isSynchronousLoading())))
                .collect(Collectors.toList());
    }

    public boolean hasInProgressStatus() {
        return validationStatus == ValidationStatus.IN_PROGRESS;
    }

    private ValidationStatus getInitialValidationStatus() {
        return potSessionParameterEntities.stream().anyMatch(PotSessionParameterEntity::hasInProgressStatus)
                ? ValidationStatus.IN_PROGRESS
                : ValidationStatus.NONE;
    }

    /**
     * Try to find {@link PotSessionParameterEntity} under group by group name and parameter name.
     *
     * @return optional of {@link PotSessionParameterEntity}.
     */
    public Optional<PotSessionParameterEntity> findParameterByNameAndGroup(String parameterName, String groupName) {
        return potSessionParameterEntities.stream()
                .filter(parameter -> groupName.equals(parameter.getGroup())
                        && parameterName.equals(parameter.getName()))
                .findFirst();
    }
}
