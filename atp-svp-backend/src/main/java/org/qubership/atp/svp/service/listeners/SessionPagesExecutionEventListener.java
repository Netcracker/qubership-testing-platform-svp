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

package org.qubership.atp.svp.service.listeners;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.Transactional;

import org.qubership.atp.svp.core.exceptions.execution.ExecutionSessionNotFoundException;
import org.qubership.atp.svp.model.db.GroupEntity;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.TabEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.events.GetInfoForPageEvent;
import org.qubership.atp.svp.model.events.GetInfoForSessionPagesEvent;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.service.AbstractMessagingService;
import org.qubership.atp.svp.service.PotSessionService;
import org.qubership.atp.svp.service.jpa.PageConfigurationServiceJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SessionPagesExecutionEventListener extends AbstractMessagingService {

    private final ApplicationEventPublisher eventPublisher;
    private final PotSessionService potSessionService;
    private final PageConfigurationServiceJpa pageConfigurationServiceJpa;

    /**
     * Constructor for {@link SessionPagesExecutionEventListener} instance.
     */
    @Autowired
    public SessionPagesExecutionEventListener(ApplicationEventPublisher eventPublisher,
                                              PotSessionService potSessionService,
                                              PageConfigurationServiceJpa pageConfigurationServiceJpa) {
        this.eventPublisher = eventPublisher;
        this.potSessionService = potSessionService;
        this.pageConfigurationServiceJpa = pageConfigurationServiceJpa;
    }

    /**
     * Handler for {@link GetInfoForSessionPagesEvent}.
     * Gets Page configurations and starts process of getting info for pages under session
     * in case of lack of the unprocessed Common Parameters
     * and if not only Common Parameters need to be executed.
     * <br>
     * Event is processed asynchronously on thread pool 'GettingInfoProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * As a result for each page the {@link GetInfoForPageEvent} event are published in the events chain.
     */
    @Transactional
    @Async("GettingInfoProcessExecutor")
    @EventListener(condition = "#getInfoForSessionPagesEvent.countOfUnprocessedCommonParameters.get() == 0 "
            + "&& #getInfoForSessionPagesEvent.onlyCommonParametersExecuted == false "
            + "&& #getInfoForSessionPagesEvent.onlyForPreconfiguredParams == false")
    public void handleGetInfoForSessionPagesEvent(GetInfoForSessionPagesEvent getInfoForSessionPagesEvent) {
        startGettingInfoForPagesByEventMultiThreadSafe(getInfoForSessionPagesEvent);
    }

    /**
     * Handler for {@link GetInfoForSessionPagesEvent}.
     * Gets Page configurations and starts process of getting info for pages under session
     * in case of lack of the unprocessed Common Parameters,
     * if not only Common Parameters need to be executed
     * and loading of preconfigured parameters only.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * As a result for each page the {@link GetInfoForPageEvent} event are published in the events chain.
     */
    @Transactional
    @EventListener(condition = "#getInfoForSessionPagesEvent.countOfUnprocessedCommonParameters.get() == 0 "
            + "&& #getInfoForSessionPagesEvent.onlyCommonParametersExecuted == false "
            + "&& #getInfoForSessionPagesEvent.onlyForPreconfiguredParams == true")
    public void handleGetInfoForSessionPagesEventSynchronously(
            GetInfoForSessionPagesEvent getInfoForSessionPagesEvent) {
        startGettingInfoForPages(getInfoForSessionPagesEvent.getSessionId());
    }

    private void startGettingInfoForPagesByEventMultiThreadSafe(GetInfoForSessionPagesEvent getInfoForPagesEvent) {
        UUID sessionId = getInfoForPagesEvent.getSessionId();
        synchronized (getInfoForPagesEvent.getCountOfUnprocessedCommonParameters()) {
            if (!potSessionService.isSessionPagesLoadingAlreadyStarted(sessionId)) {
                startGettingInfoForPages(sessionId);
            }
        }
    }

    private void startGettingInfoForPages(UUID sessionId) {
        try {
            log.info("[Session - {}] Getting info for pages was started.", sessionId);
            PotSessionEntity session = potSessionService.findSessionById(sessionId);
            SessionExecutionConfiguration sessionExecutionConfiguration = session.getExecutionConfiguration();
            List<PageConfigurationEntity> pageConfigurations = getPageConfigurationsForExecution(
                    sessionExecutionConfiguration.getPagesName(),
                    sessionExecutionConfiguration.getFolder(),
                    sessionExecutionConfiguration.getIsPotGenerationMode());

            startGettingInfoForPagesByConfiguration(pageConfigurations, sessionId,
                    sessionExecutionConfiguration.getOnlyForPreconfiguredParams());

            session.setSessionPagesLoadingAlreadyStarted(true);
            potSessionService.updateSession(session);
            log.info("[Session - {}] Successfully started getting info for each executable Page "
                    + "(events was published).", sessionId);
        } catch (ExecutionSessionNotFoundException sessionNotFoundEx) {
            log.error("Unexpected end of session: " + sessionId + "!", sessionNotFoundEx);
        } catch (Exception ex) {
            log.error("Unexpected error occurred during the getting info for pages: " + sessionId + "!", ex);
        }
    }

    private List<PageConfigurationEntity> getPageConfigurationsForExecution(List<String> pagesName,
                                                                            UUID folder,
                                                                            boolean isPotGenerationMode) {
        List<PageConfigurationEntity> pageConfigurations;
        if (isPotGenerationMode && pagesName.isEmpty()) {
            pageConfigurations = getAllPagesForExecution(folder);
        } else {
            pageConfigurations = getPageConfigurations(folder, pagesName);
        }
        return pageConfigurations;
    }

    private List<PageConfigurationEntity> getAllPagesForExecution(UUID folder) {
        return pageConfigurationServiceJpa.getAllPagesEntityInFolder(folder);
    }

    private List<PageConfigurationEntity> getPageConfigurations(UUID folderId, List<String> names) {
        return pageConfigurationServiceJpa.getPageConfigurationEntity(folderId, names);
    }

    private void startGettingInfoForPagesByConfiguration(List<PageConfigurationEntity> pageConfigurations,
                                                         UUID sessionId, boolean onlyForPreconfiguredParams) {
        AtomicInteger countOfUnprocessedPages = new AtomicInteger(pageConfigurations.size());
        int countParameters = 0;
        for (PageConfigurationEntity pageConfiguration : pageConfigurations) {
            for (TabEntity tab : pageConfiguration.getTabEntities()) {
                for (GroupEntity group : tab.getGroupEntities()) {
                    countParameters = countParameters + group.getSutParameterEntities().size();
                }
            }
            checkConfigurationOnSynchronous(pageConfiguration);
            GetInfoForPageEvent getInfoForPageEvent = GetInfoForPageEvent.builder()
                    .sessionId(sessionId)
                    .pageConfiguration(pageConfiguration)
                    .onlyForPreconfiguredParams(onlyForPreconfiguredParams)
                    .countOfUnprocessedPagesUnderSession(countOfUnprocessedPages)
                    .build();
            eventPublisher.publishEvent(getInfoForPageEvent);
        }
        getMessageService(sessionId).sendCountDownloadingParameters(sessionId, countParameters);
    }

    private void checkConfigurationOnSynchronous(PageConfigurationEntity pageConfiguration) {
        if (!pageConfiguration.isSynchronousLoading()) {
            pageConfiguration.getTabEntities().forEach(tab -> {
                if (!tab.isSynchronousLoading()) {
                    boolean isAllGroupsSynchronous =
                            tab.getGroupEntities().stream().allMatch(GroupEntity::isSynchronousLoading);
                    if (isAllGroupsSynchronous) {
                        tab.setSynchronousLoading(true);
                    }
                }
            });

            boolean isAllTabsSynchronous =
                    pageConfiguration.getTabEntities().stream().allMatch(TabEntity::isSynchronousLoading);
            if (isAllTabsSynchronous) {
                pageConfiguration.setSynchronousLoading(true);
            }
        }
    }
}
