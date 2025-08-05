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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.qubership.atp.svp.core.exceptions.execution.ExecutionSessionNotFoundException;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionPageEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.events.GetInfoForPageEvent;
import org.qubership.atp.svp.model.events.GetInfoForParameterEvent;
import org.qubership.atp.svp.model.events.GetInfoForTabsUnderPageEvent;
import org.qubership.atp.svp.model.events.ValidatePageEvent;
import org.qubership.atp.svp.model.events.ValidateSessionEvent;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.service.PotSessionPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PageEventListener {

    private final ApplicationEventPublisher eventPublisher;
    private final PotSessionPageService potSessionPageService;

    @Autowired
    public PageEventListener(ApplicationEventPublisher eventPublisher, PotSessionPageService potSessionPageService) {
        this.eventPublisher = eventPublisher;
        this.potSessionPageService = potSessionPageService;
    }

    /**
     * Handler for {@link GetInfoForPageEvent}.
     * Starts process of getting info for page by page configuration.
     * <br>
     * Event is processed asynchronously on thread pool 'GettingInfoProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link GetInfoForParameterEvent} for each synchronous parameters under page
     * if Page contains parameters with synchronously loading;
     * <br>
     * - {@link GetInfoForTabsUnderPageEvent} if Page does not contain parameters with synchronously loading.
     */
    @Async("GettingInfoProcessExecutor")
    @EventListener(condition = "#getInfoForPageEvent.onlyForPreconfiguredParams == false")
    public void handleGetInfoForPageEvent(GetInfoForPageEvent getInfoForPageEvent) {
        startGettingInfoForPage(getInfoForPageEvent.getSessionId(),
                getInfoForPageEvent.getPageConfiguration(),
                getInfoForPageEvent.getOnlyForPreconfiguredParams(),
                getInfoForPageEvent.getCountOfUnprocessedPagesUnderSession());
    }

    /**
     * Handler for {@link GetInfoForPageEvent}.
     * Starts process of getting info for page by page configuration
     * in case loading of preconfigured parameters only.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * As a result the following events are published in the events chain:
     * <br>
     * - {@link GetInfoForParameterEvent} for each synchronous parameters under page
     * if Page contains parameters with synchronously loading;
     * <br>
     * - {@link GetInfoForTabsUnderPageEvent} if Page does not contain parameters with synchronously loading.
     */
    @EventListener(condition = "#getInfoForPageEvent.onlyForPreconfiguredParams == true")
    public void handleGetInfoForPageEventSynchronously(GetInfoForPageEvent getInfoForPageEvent) {
        startGettingInfoForPage(getInfoForPageEvent.getSessionId(),
                getInfoForPageEvent.getPageConfiguration(),
                getInfoForPageEvent.getOnlyForPreconfiguredParams(),
                getInfoForPageEvent.getCountOfUnprocessedPagesUnderSession());
    }

    private void startGettingInfoForPage(UUID sessionId,
                                         PageConfigurationEntity pageConfiguration,
                                         boolean onlyForPreconfiguredParams,
                                         AtomicInteger countOfUnprocessedPagesUnderSession) {
        try {
            log.info("[Session - {}] Started getting info for page {}.", sessionId, pageConfiguration.getName());

            PotSessionPageEntity page = PotSessionPageEntity.createPotSessionPage(pageConfiguration);
            potSessionPageService.addPageToSession(sessionId, page);
            if (page.containsSynchronousLoadingParameters()) {
                startGettingInfoForSynchronousLoadingParametersUnderPage(sessionId, page,
                        onlyForPreconfiguredParams, countOfUnprocessedPagesUnderSession);
                log.info("[Session - {}] Successfully started getting info for synchronous loading params under page "
                        + "(events for each synchronous parameter was published).", sessionId);
            } else {
                startGettingInfoForTabsUnderPage(sessionId, page.getName(),
                        onlyForPreconfiguredParams, countOfUnprocessedPagesUnderSession);
                log.info("[Session - {}] Successfully started getting info for Tabs under page: {} "
                        + "(event was published).", sessionId, page.getName());
            }
        } catch (ExecutionSessionNotFoundException sessionNotFoundEx) {
            log.error("Unexpected end of session: " + sessionId + "!", sessionNotFoundEx);
            countOfUnprocessedPagesUnderSession.decrementAndGet();
        } catch (Exception ex) {
            log.error("Unexpected error occurred during the getting info process for session: " + sessionId
                    + " for page: " + pageConfiguration.getName() + "!", ex);
            countOfUnprocessedPagesUnderSession.decrementAndGet();
        }
    }

    private void startGettingInfoForSynchronousLoadingParametersUnderPage(UUID sessionId,
                                                                          PotSessionPageEntity page,
                                                                          boolean onlyForPreconfiguredParams,
                                                                          AtomicInteger countOfUnprocessedPages) {
        SessionExecutionConfiguration executionConfigurationForSession =
                potSessionPageService.getExecutionConfigurationForSession(sessionId);
        ConcurrentHashMap<String, ExecutionVariable> executionVariablesForSession =
                potSessionPageService.getExecutionVariablesForSession(sessionId);
        List<PotSessionParameterEntity> synchronousParameters =
                getSynchronousParametersForPage(page, onlyForPreconfiguredParams);
        AtomicInteger countOfUnprocessedSynchronousParameters = new AtomicInteger(synchronousParameters.size());
        AtomicInteger zeroCounter = new AtomicInteger();
        for (PotSessionParameterEntity parameter : synchronousParameters) {
            SutParameterExecutionContext parameterExecutionContext = SutParameterExecutionContext.builder()
                    .sessionId(sessionId)
                    .parameterStarted(OffsetDateTime.now())
                    .sessionConfiguration(executionConfigurationForSession)
                    .executionVariables(executionVariablesForSession)
                    .parameter(parameter)
                    .isDeferredSearchResult(parameter.hasDeferredResults())
                    .countOfUnprocessedSynchronousParametersUnderPage(countOfUnprocessedSynchronousParameters)
                    .countOfUnprocessedParametersUnderTab(zeroCounter)
                    .countOfUnprocessedTabsUnderPage(zeroCounter)
                    .countOfUnprocessedPagesUnderSession(countOfUnprocessedPages)
                    .build();
            GetInfoForParameterEvent getInfoForParameterEvent = GetInfoForParameterEvent.builder()
                    .parameterExecutionContext(parameterExecutionContext)
                    .build();
            eventPublisher.publishEvent(getInfoForParameterEvent);
        }
    }

    private List<PotSessionParameterEntity> getSynchronousParametersForPage(PotSessionPageEntity page,
                                                                            boolean onlyForPreconfiguredParams) {
        return page.getPotSessionTabs()
                .stream().flatMap(tab -> tab.getPotSessionParameterEntities().stream()
                        .filter(PotSessionParameterEntity::isSynchronousLoading)
                        .filter(parameter -> parameter.isAllowedForExecution(onlyForPreconfiguredParams)))
                .collect(Collectors.toList());
    }

    private void startGettingInfoForTabsUnderPage(UUID sessionId,
                                                  String pageName,
                                                  boolean onlyForPreconfiguredParams,
                                                  AtomicInteger countOfUnprocessedPagesUnderSession) {
        GetInfoForTabsUnderPageEvent getInfoForTabsUnderPageEvent = GetInfoForTabsUnderPageEvent.builder()
                .sessionId(sessionId)
                .pageName(pageName)
                .onlyForPreconfiguredParams(onlyForPreconfiguredParams)
                .countOfUnprocessedPagesUnderSession(countOfUnprocessedPagesUnderSession)
                .countOfUnprocessedSynchronousParametersUnderPage(new AtomicInteger())
                .build();
        eventPublisher.publishEvent(getInfoForTabsUnderPageEvent);
    }

    /**
     * Handler for {@link ValidatePageEvent}.
     * Starts process of validate info for page if all tabs under page was already processed
     * (validation process for all tabs was finished).
     * <br>
     * Event is processed asynchronously on thread pool 'ValidationProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * Calculates ValidationStatus for page and sent it to WebSocket topic "/page-results".
     * Event {@link ValidateSessionEvent} for validate session was published.
     */
    @Transactional
    @Async("ValidationProcessExecutor")
    @EventListener(condition = "#validatePageEvent.countOfUnprocessedTabs.get() == 0 "
            + "&& #validatePageEvent.onlyForPreconfiguredParams == false")
    public void handleValidatePageEvent(ValidatePageEvent validatePageEvent) {
        validatePageByEventMultiThreadSafe(validatePageEvent);
    }

    /**
     * Handler for {@link ValidatePageEvent}.
     * Starts process of validate info for page if all tabs under page was already processed
     * (validation process for all tabs was finished)
     * in case loading of preconfigured parameters only.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * Calculates ValidationStatus for page and sent it to WebSocket topic "/page-results".
     * Event {@link ValidateSessionEvent} for validate session was published.
     */
    @Transactional
    @EventListener(condition = "#validatePageEvent.countOfUnprocessedTabs.get() == 0 "
            + "&& #validatePageEvent.onlyForPreconfiguredParams == true")
    public void handleValidatePageEventSynchronously(ValidatePageEvent validatePageEvent) {
        UUID pageId = validatePageEvent.getPageId();
        PotSessionPageEntity page = potSessionPageService.findPageById(pageId);
        validatePage(validatePageEvent.getSessionId(),
                validatePageEvent.getPageName(),
                page,
                validatePageEvent.getOnlyForPreconfiguredParams(),
                validatePageEvent.getCountOfUnprocessedPagesUnderSession());
    }

    private void validatePageByEventMultiThreadSafe(ValidatePageEvent validatePageEvent) {
        UUID sessionId = validatePageEvent.getSessionId();
        String pageName = validatePageEvent.getPageName();
        UUID pageId = validatePageEvent.getPageId();
        synchronized (validatePageEvent.getCountOfUnprocessedTabs()) {
            PotSessionPageEntity page = potSessionPageService.findPageById(pageId);
            if (!page.isAlreadyValidated()) {
                validatePage(sessionId, pageName, page,
                        validatePageEvent.getOnlyForPreconfiguredParams(),
                        validatePageEvent.getCountOfUnprocessedPagesUnderSession());
            }
        }
    }

    private void validatePage(UUID sessionId,
                              String pageName,
                              PotSessionPageEntity page,
                              boolean onlyForPreconfiguredParams,
                              AtomicInteger countOfUnprocessedPagesUnderSession) {
        try {
            log.info("[Session - {}] Validation process for page: {} was started", sessionId, pageName);
            potSessionPageService.validatePage(sessionId, pageName, page);
            countOfUnprocessedPagesUnderSession.decrementAndGet();
            startSessionValidation(sessionId, onlyForPreconfiguredParams, countOfUnprocessedPagesUnderSession);
            log.info("[Session - {}] Validation process for page: {} was finished successfully. "
                    + "Event for validate session was published.", sessionId, pageName);
        } catch (ExecutionSessionNotFoundException sessionNotFoundEx) {
            log.error("[Session - {}] Page with name {} not found during validation process for page!",
                    sessionId, pageName);
        } catch (Exception ex) {
            log.error("Unexpected error occurred during the validation process for page: " + pageName
                    + " under session: " + sessionId + "!", ex);
        }
    }

    private void startSessionValidation(UUID sessionId,
                                        boolean onlyForPreconfiguredParams,
                                        AtomicInteger countOfUnprocessedPagesUnderSession) {
        ValidateSessionEvent validateSessionEvent = ValidateSessionEvent.builder()
                .sessionId(sessionId)
                .onlyForPreconfiguredParams(onlyForPreconfiguredParams)
                .countOfUnprocessedPages(countOfUnprocessedPagesUnderSession)
                .build();
        eventPublisher.publishEvent(validateSessionEvent);
    }
}
