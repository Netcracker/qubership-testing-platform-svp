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

import java.util.UUID;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;

public interface MessagingService {

    void sendSutParameterResult(UUID sessionId, PotSessionParameterEntity parameter);

    void sendValidationStatusForTab(UUID sessionId, String pageName, String tabName, ValidationStatus tabStatus);

    void sendValidationStatusForPage(UUID sessionId, String pageName, ValidationStatus pageStatus);

    void sendValidationStatusForSession(UUID sessionId, ValidationStatus sessionStatus);

    void sendSessionExpiredMessage(UUID sessionId);

    void sendCountDownloadingParameters(UUID sessionId, int countParameters);

    void sendPageInProgress(UUID sessionId, String pageName);
}
