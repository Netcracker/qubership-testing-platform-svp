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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.StoringSessionException;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionParameterNotFoundException;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionPreconfiguredException;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionSessionNotFoundException;
import org.qubership.atp.svp.model.api.GetInfoRequest;
import org.qubership.atp.svp.model.api.GetParameterResultRequest;
import org.qubership.atp.svp.model.api.tsg.PreconfiguredValidation;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionPageEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionTabEntity;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionRepository;
import org.qubership.atp.svp.service.AbstractMessagingService;
import org.qubership.atp.svp.service.PotSessionService;
import org.qubership.atp.svp.service.ValidationService;
import org.qubership.atp.svp.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PotSessionServiceImpl extends AbstractMessagingService implements PotSessionService {

    private final DeferredSearchServiceImpl deferredSearchService;
    private final ExecutionVariablesServiceImpl executionVariablesService;
    private final ValidationService validationService;
    private final PotSessionRepository potSessionRepository;
    private final SessionServiceImpl sessionServiceImpl;

    @Value("${svp.session.lifespan}")
    private Integer sessionLifespan;

    /**
     * Constructor for PotSessionServiceImpl.
     */
    @Autowired
    public PotSessionServiceImpl(DeferredSearchServiceImpl deferredSearchService,
                                 ExecutionVariablesServiceImpl executionVariablesService,
                                 ValidationService validationService,
                                 PotSessionRepository potSessionRepository,
                                 SessionServiceImpl sessionServiceImpl) {
        this.deferredSearchService = deferredSearchService;
        this.executionVariablesService = executionVariablesService;
        this.validationService = validationService;
        this.potSessionRepository = potSessionRepository;
        this.sessionServiceImpl = sessionServiceImpl;
    }

    /**
     * Constructor for TESTS ONLY.
     */
    public PotSessionServiceImpl(
            DeferredSearchServiceImpl deferredSearchService,
            ExecutionVariablesServiceImpl executionVariablesService,
            ValidationService validationService,
            Integer sessionLifespan,
            PotSessionRepository potSessionRepository,
            SessionServiceImpl sessionServiceImpl) {
        this.deferredSearchService = deferredSearchService;
        this.executionVariablesService = executionVariablesService;
        this.validationService = validationService;
        this.sessionLifespan = sessionLifespan;
        this.potSessionRepository = potSessionRepository;
        this.sessionServiceImpl = sessionServiceImpl;
    }

    @Override
    @Transactional
    public UUID startSession(SessionExecutionConfiguration sessionExecutionConfiguration, GetInfoRequest request) {
        Map<String, String> keyParameters = request.getKeyParameters();
        log.info("PotSessionServiceImpl - startSession - session execution configuration: {}, keyParameters: {}",
                sessionExecutionConfiguration, keyParameters);
        ConcurrentHashMap<String, ExecutionVariable> variables =
                executionVariablesService.getVariablesFromKeyParameters(keyParameters);
        executionVariablesService.getVariablesFromEnvironment(sessionExecutionConfiguration.getEnvironment(),
                variables);
        PotSessionEntity potSession = new PotSessionEntity(sessionExecutionConfiguration, keyParameters,
                variables, request.getPagesName());
        UUID sessionId = potSessionRepository.saveAndFlush(potSession).getSessionId();
        log.info("PotSessionServiceImpl - startSession - SUCCESS - sessionId: {}", sessionId);
        return sessionId;
    }

    @Override
    public PotSessionEntity findSessionById(UUID sessionId) {
        return potSessionRepository.findBySessionId(sessionId).orElseThrow(ExecutionSessionNotFoundException::new);
    }

    @Override
    public SessionExecutionConfiguration getSessionExecutionConfiguration(UUID sessionId) {
        String variables = potSessionRepository.getSessionExecutionConfiguration(sessionId);
        try {
            return Utils.mapper.readValue(variables, SessionExecutionConfiguration.class);
        } catch (JsonProcessingException e) {
            log.error("Unexpected end of session: " + sessionId + "!", e);
            throw new ExecutionSessionNotFoundException();
        }
    }

    @Override
    public ConcurrentHashMap<String, ExecutionVariable> getVariables(UUID sessionId) {
        return findSessionById(sessionId).getExecutionVariables();
    }

    @Override
    @Transactional
    public boolean isAlreadyValidated(UUID sessionId) {
        try {
            return potSessionRepository.isAlreadyValidated(sessionId);
        } catch (Exception exc) {
            throw new ExecutionSessionNotFoundException();
        }
    }

    @Override
    public ConcurrentHashMap<String, ExecutionVariable> getExecutionVariables(UUID sessionId) {
        return findSessionById(sessionId).getExecutionVariables();
    }

    @Override
    @Transactional
    public void addVariable(UUID sessionId, ConcurrentHashMap<String, ExecutionVariable> executionVariables) {
        potSessionRepository.updateVariables(executionVariables, sessionId);
        potSessionRepository.flush();
    }

    @Override
    @Transactional
    public PotSessionEntity updateSession(PotSessionEntity session) {
        return potSessionRepository.saveAndFlush(session);
    }

    @Override
    @Transactional
    public void updateSessionForGetInfo(UUID sessionId, SessionExecutionConfiguration executionConfiguration) {
        potSessionRepository.updateSession(sessionId, executionConfiguration);
        potSessionRepository.flush();
    }

    @Override
    @Transactional
    public boolean shouldCommonParametersLoadedForSession(UUID sessionId) {
        PotSessionEntity session = findSessionById(sessionId);
        return session.getExecutionConfiguration().getForcedLoadingCommonParameters()
                || session.getCommonParameters().isEmpty();
    }

    @Override
    @Transactional
    public synchronized PotSessionEntity addCommonParametersToSession(PotSessionEntity session,
                                                                      List<PotSessionParameterEntity>
                                                                              commonParameters) {
        session.setCommonParameters(commonParameters);
        return updateSession(session);
    }

    @Override
    @Transactional
    public void addValidationStatusForTab(UUID sessionId, String pageName, String tabName, ValidationStatus tabStatus) {
        log.info("Session: {}. Got status {} for tab {} in page {}", sessionId, tabStatus, tabName, pageName);
        getMessageService(sessionId).sendValidationStatusForTab(sessionId, pageName, tabName, tabStatus);
    }

    @Override
    @Transactional
    public void addValidationStatusForPage(UUID sessionId, String pageName, ValidationStatus pageStatus) {
        log.info("Session: {}. Got status {} for page {}", sessionId, pageStatus, pageName);
        getMessageService(sessionId).sendValidationStatusForPage(sessionId, pageName, pageStatus);
    }

    @Override
    @Transactional
    public void addValidationStatusForSession(PotSessionEntity potSession) {
        if (potSession.getExecutionConfiguration().shouldSendSessionResults()) {
            Set<ValidationStatus> pageStatuses =
                    potSessionRepository.getImpactingValidationStatus(potSession.getSessionId());
            ValidationStatus sessionStatus = validationService.calculateStatusForSession(pageStatuses);
            log.info("Session: {}. Got status {} for session", potSession.getSessionId(), sessionStatus);
            getMessageService(potSession.getSessionId())
                    .sendValidationStatusForSession(potSession.getSessionId(), sessionStatus);
            potSession.setAlreadyValidated(true);
            updateSession(potSession);
        }
    }

    @Override
    @Transactional
    public boolean isSessionPagesLoadingAlreadyStarted(UUID sessionId) {
        return potSessionRepository.isSessionPagesLoadingAlreadyStarted(sessionId);
    }

    @Override
    public synchronized void killSession(UUID sessionId) throws StoringSessionException {
        try {
            PotSessionEntity potSession = findSessionById(sessionId);
            deferredSearchService.killAllDeferredSearchResultsByExpiredSessionId(sessionId);
            setValidationStatusesForPagesAndTabsInExpiredSession(potSession);
            addValidationStatusForSession(potSession);
            getMessageService(sessionId).sendSessionExpiredMessage(sessionId);
            sessionServiceImpl.removeSession(sessionId);
            removeSession(sessionId);
            log.info("Session {} has been killed!", sessionId);
        } catch (Exception ex) {
            throw new StoringSessionException(ex);
        }
    }

    private void removeSession(UUID sessionId) throws StoringSessionException {
        try {
            potSessionRepository.deleteBySessionId(sessionId);
        } catch (Exception ex) {
            throw new StoringSessionException(ex);
        }
    }

    private void setValidationStatusesForPagesAndTabsInExpiredSession(PotSessionEntity session) {
        List<PotSessionPageEntity> pages = session.getPotSessionPageEntities();
        pages.stream()
                .filter(page -> page.getValidationStatus().equals(ValidationStatus.IN_PROGRESS))
                .forEach(page -> {
                    page.setValidationStatus(ValidationStatus.WARNING);
                    addValidationStatusForPage(session.getSessionId(), page.getName(), ValidationStatus.WARNING);
                    page.getPotSessionTabs().forEach(tab -> {
                        tab.setValidationStatus(ValidationStatus.WARNING);
                        addValidationStatusForTab(session.getSessionId(), page.getName(), tab.getName(),
                                ValidationStatus.WARNING);
                    });
                });
        updateSession(session);
    }

    @Override
    @Transactional
    public synchronized void killExpiredSessions() throws StoringSessionException {
        try {
            List<String> sessionsId = sessionServiceImpl.getSessions();
            List<UUID> ids = sessionsId.stream().map(UUID::fromString).collect(Collectors.toList());
            List<String> expiredSessions = potSessionRepository.getExpiredSessionId(sessionLifespan, ids);
            for (String sessionId : expiredSessions) {
                UUID sessionUuid = UUID.fromString(sessionId);
                killSession(sessionUuid);
            }
        } catch (Exception ex) {
            throw new StoringSessionException(ex);
        }
    }

    // TODO need to refactor this method for statuses instead not found exceptions
    @Override
    @Transactional
    public PotSessionParameterEntity getParameterResult(GetParameterResultRequest request) {
        PotSessionEntity session = findSessionById(request.getSessionId());
        if (request.isCommon()) {
            return session.findCommonParameter(request.getName())
                    .orElseThrow(() -> new ExecutionParameterNotFoundException(request.getName()));
        } else {
            return getSutParameterResult(session, request);
        }
    }

    private PotSessionParameterEntity getSutParameterResult(PotSessionEntity session,
                                                            GetParameterResultRequest request) {
        PotSessionParameterEntity parameter = session.findParameter(request.getPage(),
                        request.getTab(), request.getGroup(), request.getName())
                .orElseThrow(() -> new ExecutionParameterNotFoundException(request.getName()));

        return parameter;
    }

    @Override
    public PreconfiguredValidation getPreconfiguredValidations(UUID sessionId) {
        PreconfiguredValidation validation = new PreconfiguredValidation();
        for (PotSessionPageEntity page : findSessionById(sessionId).getPotSessionPageEntities()) {
            for (PotSessionTabEntity tab : page.getPotSessionTabs()) {
                for (PotSessionParameterEntity parameter : tab.getPotSessionParameterEntities()) {
                    if (parameter.getParameterConfig().isPreconfigured()) {
                        String errors = parameter.getErrors();
                        if (!errors.isEmpty()) {
                            throw new ExecutionPreconfiguredException();
                        }
                        validation.addResultToComponent(parameter.getName(),
                                parameter.getArValues().stream().findFirst().orElse(null),
                                parameter.getEr(),
                                parameter.getValidationInfo().getStatus(),
                                parameter.getComponentName());
                    }
                }
            }
        }
        return validation;
    }
}
