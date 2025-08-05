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

package org.qubership.atp.svp.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.qubership.atp.svp.core.exceptions.StoringSessionPageException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionPageEntity;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;

public interface PotSessionPageService {

    Optional<PotSessionPageEntity> findPageByNameAndSessionId(String pageName, UUID sessionId);

    PotSessionPageEntity findPageById(UUID pageId);

    void validatePage(UUID sessionId, String pageName, PotSessionPageEntity page) throws StoringSessionPageException;

    void addPageToSession(UUID sessionId, PotSessionPageEntity page)
            throws StoringSessionPageException;

    OffsetDateTime getSessionStartedDate(UUID sessionId);

    SessionExecutionConfiguration getExecutionConfigurationForSession(UUID sessionId);

    ConcurrentHashMap<String, ExecutionVariable> getExecutionVariablesForSession(UUID sessionId);

    void updatePotSessionPage(PotSessionPageEntity page) throws StoringSessionPageException;

}
