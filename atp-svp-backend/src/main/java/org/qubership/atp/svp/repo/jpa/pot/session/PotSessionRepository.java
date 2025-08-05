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

package org.qubership.atp.svp.repo.jpa.pot.session;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PotSessionRepository extends JpaRepository<PotSessionEntity, UUID> {

    Optional<PotSessionEntity> findBySessionId(UUID sessionId);

    @Query(value = "select cast(session_id as varchar) session_id from pot_sessions where started  < now() "
            + "- make_interval(secs => :seconds) and session_id in :ids", nativeQuery = true)
    List<String> getExpiredSessionId(@Param("seconds") int seconds, @Param("ids") List<UUID> sessionIds);

    @Modifying
    void deleteBySessionId(UUID sessionId);

    @Query(value = "select alreadyValidated from PotSessionEntity where sessionId = ?1")
    boolean isAlreadyValidated(UUID sessionId);


    @Query(value = "select distinct validation_status from pot_session_page where pot_session_id =?1 and "
            + "validation_status != '0' and  validation_status != '5' and  validation_status != '3'", nativeQuery =
            true)
    Set<ValidationStatus> getImpactingValidationStatus(UUID sessionId);

    @Query(value = "SELECT cast(execution_configuration as varchar) FROM public.pot_sessions WHERE session_id=?1",
            nativeQuery = true)
    String getSessionExecutionConfiguration(UUID sessionId);

    @Query(value = "select sessionPagesLoadingAlreadyStarted from PotSessionEntity where sessionId = ?1")
    boolean isSessionPagesLoadingAlreadyStarted(UUID sessionId);

    @Modifying
    @Query("update PotSessionEntity p set p.executionVariables = :variables WHERE p.sessionId = :sessionId")
    void updateVariables(@Param("variables") ConcurrentHashMap<String, ExecutionVariable> variables,
                         @Param("sessionId") UUID sessionId);

    @Modifying
    @Query("UPDATE PotSessionEntity p SET  "
            + "p.executionConfiguration = :executionConfiguration, "
            + "p.alreadyValidated = 'false',  "
            + "p.sessionPagesLoadingAlreadyStarted = 'false' "
            + "WHERE p.sessionId = :sessionId")
    void updateSession(@Param("sessionId") UUID sessionId,
                       @Param("executionConfiguration") SessionExecutionConfiguration executionConfiguration);

}
