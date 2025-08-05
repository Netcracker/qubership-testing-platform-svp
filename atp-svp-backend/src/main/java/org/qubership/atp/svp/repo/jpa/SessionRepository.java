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

package org.qubership.atp.svp.repo.jpa;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.svp.model.db.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {

    @Query(value = "select cast(id as varchar) id from sessions where pod_name = ?1", nativeQuery = true)
    List<String> getSessionIds(String podName);

    @Modifying
    void deleteById(UUID sessionId);

    @Query(value = "SELECT EXISTS(SELECT id FROM sessions s  WHERE id = ?1 and pod_name = ?2)", nativeQuery = true)
    boolean containsSession(UUID sessionId, String podName);

    @Query(value = "delete from pot_sessions ps, sessions s where ps.session_id = s.id and s.pod_name not in ?2",
            nativeQuery = true)
    void removeLostSessions(List<String> podNames);
}
