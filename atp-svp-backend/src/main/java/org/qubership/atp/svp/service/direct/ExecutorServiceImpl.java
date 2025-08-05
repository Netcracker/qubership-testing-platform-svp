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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionCheckNamesException;
import org.qubership.atp.svp.core.exceptions.execution.GetPotSyncTimeoutException;
import org.qubership.atp.svp.model.api.GetInfoRequest;
import org.qubership.atp.svp.model.api.ram.SessionDto;
import org.qubership.atp.svp.model.api.tsg.PreconfiguredValidation;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.events.GetInfoForCommonParametersEvent;
import org.qubership.atp.svp.model.events.GetInfoForSessionEvent;
import org.qubership.atp.svp.model.impl.LogCollectorSearchPeriod;
import org.qubership.atp.svp.model.logcollector.LogCollectorConfiguration;
import org.qubership.atp.svp.model.pot.PotFile;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.service.AbstractRepositoryConfigService;
import org.qubership.atp.svp.service.ExecutorService;
import org.qubership.atp.svp.service.IntegrationService;
import org.qubership.atp.svp.service.PotGenerationEngine;
import org.qubership.atp.svp.service.PotSessionService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExecutorServiceImpl extends AbstractRepositoryConfigService implements ExecutorService {

    private final PotSessionService potSessionService;
    private final IntegrationService integrationService;
    private final PotGenerationEngine potEngine;
    private final ApplicationEventPublisher eventPublisher;
    private final SessionDtoProcessorService sessionDtoProcessorService;
    private final FolderServiceJpa folderServiceJpa;
    private final ProjectConfigService projectConfigService;
    private static final int DEFAULT_TIMEOUT_RANGE = 900;
    private static final int MAX_TIMEOUT_RANGE = 1500;
    public static final String DEFAULT_FOLDER_NAME = "Default";

    /**
     * Main constructor for initializing ExecutorService.
     */
    @Autowired
    public ExecutorServiceImpl(PotSessionService potSessionService,
                               IntegrationService integrationService,
                               PotGenerationEngine potEngine,
                               ApplicationEventPublisher eventPublisher,
                               SessionDtoProcessorService sessionDtoProcessorService,
                               FolderServiceJpa folderServiceJpa,
                               ProjectConfigService projectConfigService) {
        this.potSessionService = potSessionService;
        this.integrationService = integrationService;
        this.potEngine = potEngine;
        this.eventPublisher = eventPublisher;
        this.sessionDtoProcessorService = sessionDtoProcessorService;
        this.folderServiceJpa = folderServiceJpa;
        this.projectConfigService = projectConfigService;
    }

    /**
     * Returns generated POT report file or zip archive with session parameters results.
     * File will be generated from PotSession which find by sessionId cache.
     *
     * @return POT report as docx file or zip archive.
     */
    @Override
    public PotFile getPotReportForSession(UUID sessionId) {
        return potEngine.generatePot(potSessionService.findSessionById(sessionId));
    }

    /**
     * Gets info for selected pages and common parameters.
     */
    @Override
    public UUID getInfo(UUID projectId, GetInfoRequest request) {
        UUID sessionId = getOrCreateSession(projectId, request, false);
        GetInfoForSessionEvent getInfoForSessionEvent = GetInfoForSessionEvent.builder()
                .sessionId(sessionId)
                .onlyForPreconfiguredParams(false)
                .build();
        eventPublisher.publishEvent(getInfoForSessionEvent);
        return sessionId;
    }

    /**
     * Synchronize getting info and generation POT
     * Returns generated POT report file or zip archive with session parameters results.
     * File will be generated from PotSession which find by sessionId cache.
     *
     * @return POT report as docx file or zip archive.
     */
    @Override
    public PotFile getPotSync(UUID projectId, GetInfoRequest request) {
        log.info("ExecutorServiceImpl - getPotSync for project: {}, request: {}  Started", projectId, request);
        PotSessionEntity session = getSessionSync(projectId, request);
        return potEngine.generatePot(session);
    }

    /**
     * Synchronize getting info and generation SessionDto.
     *
     * @return SessionDto
     */
    @Override
    public SessionDto getInfoSessionDtoRam(UUID projectId, GetInfoRequest request) {
        log.info("ExecutorServiceImpl - getInfoSessionDtoRam for project: {}, request: {}  Started",
                projectId, request);
        PotSessionEntity session = getSessionSync(projectId, request);
        return sessionDtoProcessorService.potSessionParameterConverterToDto(session);
    }

    private PotSessionEntity getSessionSync(UUID projectId, GetInfoRequest request) {
        checkPagesName(projectId, request);
        UUID sessionId = getInfo(projectId, request);
        log.info("[Session - {}] Get info synchronize was started", sessionId);
        PotSessionEntity session = potSessionService.findSessionById(sessionId);
        int timeOutRange = request.getTimeOutRange();
        waitSession(session, timeOutRange);
        log.info("[Session - {}] finished get info synchronize", sessionId);
        return session;
    }

    private void checkPagesName(UUID projectId, GetInfoRequest request) {
        List<String> pageNames = folderServiceJpa.getFolderByProjectIdAndName(projectId, request.getFolder())
                .getPages().stream().map(PageConfigurationEntity::getName).collect(Collectors.toList());
        List<String> notFoundNames = new ArrayList<>();
        request.getPagesName().forEach(pageName -> {
            if (!pageNames.contains(pageName)) {
                notFoundNames.add(pageName);
            }
        });
        if (!notFoundNames.isEmpty()) {
            log.error("Names for synchronize get info didn't find. Names: {}", notFoundNames);
            throw new ExecutionCheckNamesException(notFoundNames);
        }
    }

    private void waitSession(PotSessionEntity session, int timeOutRange) {
        if (timeOutRange <= 0) {
            timeOutRange = DEFAULT_TIMEOUT_RANGE;
        } else if (timeOutRange > MAX_TIMEOUT_RANGE) {
            timeOutRange = MAX_TIMEOUT_RANGE;
        }
        UUID sessionId = session.getSessionId();
        log.info("[Session - {}] Start waiting for the session to continue", sessionId);
        do {
            try {
                Thread.sleep(1000);
                if (Duration.between(session.getStarted(), OffsetDateTime.now()).getSeconds() > timeOutRange) {
                    throw new GetPotSyncTimeoutException();
                }
            } catch (InterruptedException e) {
                log.error("Unexpected error in waitSession", e);
            }
        } while (!potSessionService.isAlreadyValidated(sessionId));
        log.info("[Session - {}] Finished waiting for the session", sessionId);
    }

    /**
     * Returns session id. If not exists, session will be created.
     */
    @Override
    public UUID getOrCreateSession(UUID projectId, GetInfoRequest request, boolean isOnlyPreconfiguredParams) {
        ProjectConfigsEntity config = projectConfigService.getProjectConfig(projectId);
        request.setPagesName(removeDoublePages(request));
        checkDefaultFolder(request);
        LogCollectorSearchPeriod logCollectorSearchPeriod =
                LogCollectorSearchPeriod.createFromProjectConfigAndGetInfoRequest(config, request);

        SessionExecutionConfiguration executionConfiguration = createSessionExecutionConfiguration(projectId, request,
                config, logCollectorSearchPeriod,
                isOnlyPreconfiguredParams);

        return Objects.nonNull(request.getSessionId())
                ? getSession(request.getSessionId(), executionConfiguration)
                : potSessionService.startSession(executionConfiguration, request);
    }

    private void checkDefaultFolder(GetInfoRequest request) {
        if (Objects.isNull(request.getFolder())) {
            request.setFolder(DEFAULT_FOLDER_NAME);
        }
    }

    @NotNull
    private List<String> removeDoublePages(GetInfoRequest request) {
        if (Objects.isNull(request.getPagesName())) {
            return Collections.emptyList();
        }
        return request.getPagesName().stream().distinct().collect(Collectors.toList());
    }

    private UUID getSession(UUID sessionId, SessionExecutionConfiguration executionConfiguration) {
        potSessionService.updateSessionForGetInfo(sessionId, executionConfiguration);
        return sessionId;
    }

    private SessionExecutionConfiguration createSessionExecutionConfiguration(UUID projectId,
                                                                              GetInfoRequest request,
                                                                              ProjectConfigsEntity config,
                                                                              LogCollectorSearchPeriod lcSearchPeriod,
                                                                              boolean isOnlyPreconfiguredParams) {
        return SessionExecutionConfiguration.builder()
                .environment(integrationService.getEnvironmentById(request.getEnvironmentId()))
                .pagesName(request.getPagesName())
                .logCollectorSearchPeriod(lcSearchPeriod)
                .logCollectorConfigurations(getConfigurationsFromLogCollectorErrorSafe(projectId))
                .shouldHighlightDiffs(request.shouldHighlightDiffs())
                .shouldSendSessionResults(request.shouldSendSessionResults())
                .isFullInfoNeededInPot(config.isFullInfoNeededInPot())
                .onlyForPreconfiguredParams(isOnlyPreconfiguredParams)
                .isPotGenerationMode(request.isPotGenerationMode())
                .onlyCommonParametersExecuted(request.isOnlyCommonParametersExecuted())
                .forcedLoadingCommonParameters(request.isForcedLoadingCommonParameters())
                .folder(folderServiceJpa.getFolderByProjectIdAndName(projectId, request.getFolder()).getFolderId())
                .build();
    }

    private List<UUID> getConfigurationsFromLogCollectorErrorSafe(UUID projectId) {
        try {
            return integrationService.getConfigurationsFromLogCollector(projectId)
                    .stream().map(LogCollectorConfiguration::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not get LogCollector configurations "
                    + "(not necessarily for projects without integration with LogCollector). "
                    + "Message: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get info for common parameters.
     */
    @Override
    public void getInfoForCommonParameters(UUID projectId, GetInfoRequest request) {
        UUID sessionId = getOrCreateSession(projectId, request, false);
        GetInfoForCommonParametersEvent getInfoForCommonParametersEvent = GetInfoForCommonParametersEvent.builder()
                .sessionId(sessionId)
                .onlyCommonParametersExecuted(true)
                .onlyForPreconfiguredParams(false)
                .build();
        eventPublisher.publishEvent(getInfoForCommonParametersEvent);
    }

    /**
     * Returns validation result for preconfigured parameters in all pages (API for TSG).
     */
    @Override
    public PreconfiguredValidation getPreconfiguredValidationResults(UUID projectId, GetInfoRequest request) {
        UUID sessionId = getOrCreateSession(projectId, request, true);
        GetInfoForSessionEvent getInfoForSessionEvent = GetInfoForSessionEvent.builder()
                .sessionId(sessionId)
                .onlyForPreconfiguredParams(true)
                .build();
        eventPublisher.publishEvent(getInfoForSessionEvent);
        return potSessionService.getPreconfiguredValidations(sessionId);
    }
}
