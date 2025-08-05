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

package org.qubership.atp.svp.service.direct;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Transactional;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.StoringSessionTabException;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionPotSessionTabNotFoundException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionTabEntity;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionTabRepository;
import org.qubership.atp.svp.service.AbstractMessagingService;
import org.qubership.atp.svp.service.PotSessionTabService;
import org.qubership.atp.svp.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PotSessionTabServiceImpl extends AbstractMessagingService implements PotSessionTabService {

    private final PotSessionServiceImpl potSessionService;
    private final ValidationService validationService;
    private final PotSessionTabRepository potSessionTabRepository;

    /**
     * Constructor for {@link PotSessionTabServiceImpl} instance.
     */
    @Autowired
    public PotSessionTabServiceImpl(PotSessionServiceImpl potSessionService,
                                    ValidationService validationService,
                                    PotSessionTabRepository potSessionTabRepository) {
        this.potSessionService = potSessionService;
        this.validationService = validationService;
        this.potSessionTabRepository = potSessionTabRepository;
    }

    @Override
    @Transactional
    public PotSessionTabEntity getTabById(UUID tabId) {
        return potSessionTabRepository.findById(tabId).orElseThrow(ExecutionPotSessionTabNotFoundException::new);
    }

    @Override
    public SessionExecutionConfiguration getExecutionConfigurationForSession(UUID sessionId) {
        return potSessionService.getSessionExecutionConfiguration(sessionId);
    }

    @Override
    public ConcurrentHashMap<String, ExecutionVariable> getExecutionVariablesForSession(UUID sessionId) {
        return potSessionService.getVariables(sessionId);
    }

    @Override
    public OffsetDateTime getSessionStartedDate(UUID sessionId) {
        return potSessionService.findSessionById(sessionId).getStarted();
    }

    @Override
    public void validateTab(UUID sessionId, String pageName, String tabName, PotSessionTabEntity tab)
            throws StoringSessionTabException {
        ValidationStatus tabStatus = validationService.calculateStatusForTab(tab);
        tab.setValidationStatus(tabStatus);
        tab.setAlreadyValidated(true);
        addValidationStatusForTab(sessionId, pageName, tabName, tabStatus);
        updatePotSessionTab(tab);
    }

    private void updatePotSessionTab(PotSessionTabEntity tab) throws StoringSessionTabException {
        try {
            potSessionTabRepository.saveAndFlush(tab);
        } catch (Exception ex) {
            throw new StoringSessionTabException(ex.getMessage());
        }
    }

    private void addValidationStatusForTab(UUID sessionId,
                                           String pageName,
                                           String tabName,
                                           ValidationStatus tabStatus) {
        log.info("Session: {}. Got status {} for tab {} under page {}", sessionId, tabStatus, tabName, pageName);
        getMessageService(sessionId).sendValidationStatusForTab(sessionId, pageName, tabName, tabStatus);
    }
}
