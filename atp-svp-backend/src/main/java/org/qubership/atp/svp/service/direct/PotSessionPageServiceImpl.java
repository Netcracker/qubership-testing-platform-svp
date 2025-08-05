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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Transactional;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.StoringSessionPageException;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionPotSessionPageNotFoundException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionPageEntity;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionPageRepository;
import org.qubership.atp.svp.service.AbstractMessagingService;
import org.qubership.atp.svp.service.PotSessionPageService;
import org.qubership.atp.svp.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PotSessionPageServiceImpl extends AbstractMessagingService implements PotSessionPageService {

    private final PotSessionServiceImpl potSessionService;
    private final ValidationService validationService;
    private final PotSessionPageRepository potSessionPageRepository;
    private final MetricsService metricsService;

    /**
     * Constructor for {@link PotSessionPageServiceImpl} instance.
     */
    @Autowired
    public PotSessionPageServiceImpl(PotSessionServiceImpl potSessionService,
                                     ValidationService validationService,
                                     PotSessionPageRepository potSessionPageRepository,
                                     MetricsService metricsService) {
        this.potSessionService = potSessionService;
        this.validationService = validationService;
        this.potSessionPageRepository = potSessionPageRepository;
        this.metricsService = metricsService;
    }

    @Override
    @Transactional
    public Optional<PotSessionPageEntity> findPageByNameAndSessionId(String pageName, UUID sessionId) {
        return potSessionPageRepository.findByPotSessionEntitySessionIdAndName(sessionId, pageName);
    }

    @Override
    public PotSessionPageEntity findPageById(UUID pageId) {
        return potSessionPageRepository.findById(pageId).orElseThrow(ExecutionPotSessionPageNotFoundException::new);
    }

    @Override
    public SessionExecutionConfiguration getExecutionConfigurationForSession(UUID sessionId) {
        return potSessionService.findSessionById(sessionId).getExecutionConfiguration();
    }

    @Override
    public ConcurrentHashMap<String, ExecutionVariable> getExecutionVariablesForSession(UUID sessionId) {
        return potSessionService.findSessionById(sessionId).getExecutionVariables();
    }

    @Override
    public OffsetDateTime getSessionStartedDate(UUID sessionId) {
        return potSessionService.findSessionById(sessionId).getStarted();
    }

    @Override
    public void validatePage(UUID sessionId, String pageName, PotSessionPageEntity page)
            throws StoringSessionPageException {
        Set<ValidationStatus> tabStatuses = potSessionPageRepository.getImpactingValidationStatus(page.getId());
        page.setValidationStatus(validationService.calculateStatusForPage(tabStatuses));
        page.setAlreadyValidated(true);
        addValidationStatusForPage(sessionId, page.getName(), page.getValidationStatus());
        updatePotSessionPage(page);
        metricsService.recordValidationRequestDuration(
                Duration.between(page.getStarted(), OffsetDateTime.now()),
                page.getProjectId());
    }

    @Transactional
    @Override
    public void addPageToSession(UUID sessionId, PotSessionPageEntity page) throws StoringSessionPageException {
        PotSessionEntity session = potSessionService.findSessionById(sessionId);
        try {
            page.setPotSessionEntity(session);
            page.setProjectId(session.getExecutionConfiguration().getProjectId());
            potSessionPageRepository.deleteOldPageForSession(page.getName(), sessionId);
            potSessionPageRepository.saveAndFlush(page);
            getMessageService(sessionId).sendPageInProgress(sessionId, page.getName());
        } catch (Exception ex) {
            throw new StoringSessionPageException(ex);
        }
    }

    private void addValidationStatusForPage(UUID sessionId, String pageName, ValidationStatus pageStatus) {
        log.info("Session: {}. Got status {} for page {}", sessionId, pageStatus, pageName);
        getMessageService(sessionId).sendValidationStatusForPage(sessionId, pageName, pageStatus);
    }

    @Override
    public void updatePotSessionPage(PotSessionPageEntity page) throws StoringSessionPageException {
        try {
            potSessionPageRepository.saveAndFlush(page);
        } catch (Exception ex) {
            throw new StoringSessionPageException(ex);
        }
    }
}
