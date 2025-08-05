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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.StoringSessionException;
import org.qubership.atp.svp.model.api.GetInfoRequest;
import org.qubership.atp.svp.model.api.GetParameterResultRequest;
import org.qubership.atp.svp.model.api.tsg.PreconfiguredValidation;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;

public interface PotSessionService {

    UUID startSession(SessionExecutionConfiguration sessionExecutionConfiguration, GetInfoRequest request);

    PotSessionEntity findSessionById(UUID sessionId);

    SessionExecutionConfiguration getSessionExecutionConfiguration(UUID sessionId);

    ConcurrentHashMap<String, ExecutionVariable> getVariables(UUID sessionId);

    boolean isAlreadyValidated(UUID sessionId);

    boolean shouldCommonParametersLoadedForSession(UUID sessionId);

    PotSessionEntity addCommonParametersToSession(PotSessionEntity session,
                                                  List<PotSessionParameterEntity> commonParameters);

    void addValidationStatusForTab(UUID sessionId, String pageName, String tabName, ValidationStatus tabStatus);

    void addValidationStatusForPage(UUID sessionId, String pageName, ValidationStatus status);

    void addValidationStatusForSession(PotSessionEntity potSession);

    boolean isSessionPagesLoadingAlreadyStarted(UUID sessionId);

    void killSession(UUID sessionId) throws StoringSessionException;

    void killExpiredSessions() throws StoringSessionException;

    PotSessionParameterEntity getParameterResult(GetParameterResultRequest request);

    PreconfiguredValidation getPreconfiguredValidations(UUID sessionId);

    PotSessionEntity updateSession(PotSessionEntity session);

    void updateSessionForGetInfo(UUID sessionId, SessionExecutionConfiguration executionConfiguration);

    void addVariable(UUID sessionId, ConcurrentHashMap<String, ExecutionVariable> executionVariables);

    ConcurrentHashMap<String, ExecutionVariable> getExecutionVariables(UUID sessionId);
}
