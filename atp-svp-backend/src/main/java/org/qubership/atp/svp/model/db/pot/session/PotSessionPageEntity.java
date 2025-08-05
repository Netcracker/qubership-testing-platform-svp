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

import java.time.OffsetDateTime;
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
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TypeDef;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pot_session_page", indexes = {
        @Index(name = "pot_session_page_pot_session_id_index", columnList = "pot_session_id")
})
@Getter
@Setter
@NoArgsConstructor
@TypeDef(name = "json", typeClass = JsonType.class)
public class PotSessionPageEntity {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @Column(name = "validation_status")
    private ValidationStatus validationStatus;

    @Column(name = "already_validated")
    private boolean alreadyValidated;

    @Column(name = "tabs_loading_already_started")
    private boolean tabsLoadingAlreadyStarted;

    @OneToMany(mappedBy = "potSessionPageEntity", cascade = CascadeType.ALL, orphanRemoval =
            true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PotSessionTabEntity> potSessionTabs;

    @ManyToOne
    @JoinColumn(name = "pot_session_id", foreignKey = @ForeignKey(name = "FK_pot_session_id"))
    private PotSessionEntity potSessionEntity;

    @ManyToOne
    @JoinColumn(name = "page_config_id", foreignKey = @ForeignKey(name = "FK_page_configuration_id"))
    private PageConfigurationEntity pageConfiguration;

    @CreationTimestamp
    @Column(name = "started", nullable = false)
    private OffsetDateTime started;

    @Column(name = "project_id")
    private UUID projectId;

    /**
     * Static Method for create {@link PotSessionPageEntity}.
     */
    public static PotSessionPageEntity createPotSessionPage(PageConfigurationEntity pageConfiguration) {
        PotSessionPageEntity potSessionPageEntity = new PotSessionPageEntity();
        potSessionPageEntity.setName(pageConfiguration.getName());
        potSessionPageEntity.setPageConfiguration(pageConfiguration);
        potSessionPageEntity.potSessionTabs = pageConfiguration.getTabEntities().stream().map(tabEntity ->
                PotSessionTabEntity.createPotSessionTab(potSessionPageEntity, tabEntity)
        ).collect(Collectors.toList());
        potSessionPageEntity.setValidationStatus(potSessionPageEntity.getInitialValidationStatus());
        return potSessionPageEntity;
    }

    private ValidationStatus getInitialValidationStatus() {
        return potSessionTabs.stream().anyMatch(PotSessionTabEntity::hasInProgressStatus)
                ? ValidationStatus.IN_PROGRESS
                : ValidationStatus.NONE;
    }

    /**
     * check parameters on synchronous loading.
     * @return true if page contains any {@link PotSessionParameterEntity} with true synchronousLoading flag.
     */
    public boolean containsSynchronousLoadingParameters() {
        return potSessionTabs.stream().anyMatch(potSessionTab ->
                potSessionTab.getPotSessionParameterEntities().stream()
                        .anyMatch(PotSessionParameterEntity::isSynchronousLoading));
    }

    public Optional<PotSessionTabEntity> findTabByName(@NotNull String name) {
        return potSessionTabs.stream().filter(page -> name.equals(page.getName())).findFirst();
    }

    /**
     * Searches for specific SUT parameter in session.
     */
    public Optional<PotSessionParameterEntity> findParameter(String tabName,
                                                             String groupName,
                                                             String parameterName) {
        return potSessionTabs.stream()
                .filter(tab -> tab.getName().equals(tabName))
                .findFirst()
                .flatMap(tab -> tab.findParameterByNameAndGroup(parameterName, groupName));
    }
}
