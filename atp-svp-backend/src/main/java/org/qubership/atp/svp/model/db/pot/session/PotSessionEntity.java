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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.utils.converters.ListConverter;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pot_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "json", typeClass = JsonType.class)
public class PotSessionEntity {

    @Id
    @GeneratedValue
    @Column(name = "session_id")
    private UUID sessionId;

    private OffsetDateTime started;

    @Column(name = "already_validated")
    private boolean alreadyValidated;

    @Column(name = "session_pages_loading_already_started")
    private boolean sessionPagesLoadingAlreadyStarted;

    @Type(type = "json")
    @Column(columnDefinition = "jsonb", name = "execution_configuration")
    private SessionExecutionConfiguration executionConfiguration;

    @Column(columnDefinition = "jsonb", name = "execution_variables")
    @Type(type = "json")
    private ConcurrentHashMap<String, ExecutionVariable> executionVariables = new ConcurrentHashMap<>();

    @Type(type = "json")
    @Column(columnDefinition = "jsonb", name = "key_parameter")
    private Map<String, String> keyParameters = new HashMap<>();

    @OneToMany(mappedBy = "potSessionEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PotSessionPageEntity> potSessionPageEntities;

    @OneToMany(mappedBy = "potSessionEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PotSessionParameterEntity> commonParameters;


    @Column(name = "page_order", columnDefinition = "TEXT")
    @Convert(converter = ListConverter.class)
    private List<String> pageOrder;

    /**
     * The constructor of PotSessionEntity.
     */
    public PotSessionEntity(SessionExecutionConfiguration executionConfiguration,
                            Map<String, String> keyParameters,
                            ConcurrentHashMap<String, ExecutionVariable> executionVariables,
                            List<String> pageOrder) {
        this.started = OffsetDateTime.now();
        this.executionConfiguration = executionConfiguration;
        this.keyParameters = keyParameters;
        this.executionVariables = executionVariables;
        this.pageOrder = pageOrder;
    }

    public Optional<PotSessionPageEntity> findPageByName(@NotNull String name) {
        return potSessionPageEntities.stream().filter(page -> name.equals(page.getName())).findFirst();
    }

    /**
     * Searches for specific common parameter in session.
     */
    public Optional<PotSessionParameterEntity> findCommonParameter(String name) {
        return commonParameters.stream().filter(parameter -> parameter.getName().equals(name)).findFirst();
    }

    /**
     * Searches for specific SUT parameter in session.
     */
    public Optional<PotSessionParameterEntity> findParameter(String pageName,
                                                             String tabName,
                                                             String groupName,
                                                             String parameterName) {
        return findPageByName(pageName)
                .flatMap(page -> page.findTabByName(tabName)
                        .flatMap(tab -> tab.findParameterByNameAndGroup(parameterName, groupName)));
    }
}
