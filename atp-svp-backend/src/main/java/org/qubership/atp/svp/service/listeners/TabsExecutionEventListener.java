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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionSessionNotFoundException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionPageEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionTabEntity;
import org.qubership.atp.svp.model.events.GetInfoForTabEvent;
import org.qubership.atp.svp.model.events.GetInfoForTabsUnderPageEvent;
import org.qubership.atp.svp.model.events.ValidatePageEvent;
import org.qubership.atp.svp.service.PotSessionPageService;
import org.qubership.atp.svp.service.direct.PotSessionTabServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TabsExecutionEventListener {

    private final ApplicationEventPublisher eventPublisher;
    private final PotSessionPageService potSessionPageService;

    /**
     * Constructor of TabsExecutionEventListener.
     */
    @Autowired
    public TabsExecutionEventListener(ApplicationEventPublisher eventPublisher,
                                      PotSessionPageService potSessionPageService,
                                      PotSessionTabServiceImpl potSessionTabService) {
        this.eventPublisher = eventPublisher;
        this.potSessionPageService = potSessionPageService;
    }

    /**
     * Handler for {@link GetInfoForTabsUnderPageEvent}.
     * Gets {@link PotSessionTabEntity} instances for tabs under page
     * and starts process of getting info for each tab under page.
     * <br>
     * Event is processed asynchronously on thread pool 'GettingInfoProcessExecutor'
     * {@link org.qubership.atp.svp.config.AsyncConfig}.
     * <br>
     * <br>
     * As a result for each tab under page the {@link GetInfoForTabEvent} event
     * are published in the events chain.
     */
    @Transactional
    @Async("GettingInfoProcessExecutor")
    @EventListener(condition = "#getInfoForTabsEvent.countOfUnprocessedSynchronousParametersUnderPage.get() == 0 "
            + "&& #getInfoForTabsEvent.onlyForPreconfiguredParams == false")
    public void handleGetInfoForTabsUnderPageEventEvent(GetInfoForTabsUnderPageEvent getInfoForTabsEvent) {
        startGettingInfoForTabsByEventMultiThreadSafe(getInfoForTabsEvent);
    }

    /**
     * Handler for {@link GetInfoForTabsUnderPageEvent}.
     * Gets {@link PotSessionTabEntity} instances for tabs under page
     * and starts process of getting info for each tab under page
     * in case loading of preconfigured parameters only.
     * <br>
     * Event is processed synchronously.
     * <br>
     * <br>
     * As a result for each tab under page the {@link GetInfoForTabEvent} event
     * are published in the events chain.
     */
    @Transactional
    @EventListener(condition = "#getInfoForTabsEvent.countOfUnprocessedSynchronousParametersUnderPage.get() == 0 "
            + "&& #getInfoForTabsEvent.onlyForPreconfiguredParams == true")
    public void handleGetInfoForTabsUnderPageEventEventSynchronously(GetInfoForTabsUnderPageEvent getInfoForTabsEvent) {
        startGettingInfoForTabsByEventMultiThreadSafe(getInfoForTabsEvent);
    }

    private void startGettingInfoForTabsByEventMultiThreadSafe(GetInfoForTabsUnderPageEvent getInfoForTabsEvent) {
        UUID sessionId = getInfoForTabsEvent.getSessionId();
        String pageName = getInfoForTabsEvent.getPageName();
        Optional<PotSessionPageEntity> pageAsOptional = getPotSessionPageByNameAndSessionId(pageName, sessionId);
        pageAsOptional.ifPresent(page -> {
                    synchronized (getInfoForTabsEvent.getCountOfUnprocessedSynchronousParametersUnderPage()) {
                        if (!page.isTabsLoadingAlreadyStarted()) {
                            startGettingInfoForTabsUnderPage(sessionId, page,
                                    getInfoForTabsEvent.getOnlyForPreconfiguredParams(),
                                    getInfoForTabsEvent.getCountOfUnprocessedPagesUnderSession());
                        }
                    }
                }
        );
    }

    private Optional<PotSessionPageEntity> getPotSessionPageByNameAndSessionId(String pageName, UUID sessionId) {
        try {
            return potSessionPageService.findPageByNameAndSessionId(pageName, sessionId);
        } catch (ExecutionSessionNotFoundException e) {
            log.error("Unexpected end of session: " + sessionId + "!", e);
            return Optional.empty();
        }
    }

    private void startGettingInfoForTabsUnderPage(UUID sessionId,
                                                  PotSessionPageEntity page,
                                                  boolean onlyForPreconfiguredParams,
                                                  AtomicInteger countOfUnprocessedPagesUnderSession) {
        try {
            log.info("[Session - {}] Start getting Tabs under page: {}", sessionId, page.getName());
            List<PotSessionTabEntity> tabs = page.getPotSessionTabs();
            Map<Boolean, List<PotSessionTabEntity>> tabsBySynchronousLoading = tabs.stream()
                    .collect(Collectors.partitioningBy(PotSessionTabEntity::isSynchronousLoading));
            tabsBySynchronousLoading.get(true).stream()
                    .filter(tab -> ValidationStatus.IN_PROGRESS.equals(tab.getValidationStatus()))
                    .forEach(tab -> tab.setValidationStatus(ValidationStatus.NONE));
            List<PotSessionTabEntity> tabsWithAsynchronousLoading = tabsBySynchronousLoading.get(false);
            if (tabsWithAsynchronousLoading.isEmpty()) {
                log.info("[Session - {}] No tabs with asynchronous loading parameters "
                        + "were found under page: {} to get info for!", sessionId, page.getName());
                AtomicInteger zeroCounter = new AtomicInteger();
                startPageValidation(sessionId, page.getName(), page.getId(), onlyForPreconfiguredParams,
                        zeroCounter, countOfUnprocessedPagesUnderSession);
            } else {
                AtomicInteger countOfUnprocessedTabs = new AtomicInteger(tabsWithAsynchronousLoading.size());
                tabsWithAsynchronousLoading.forEach(tab -> startGettingInfoForTab(sessionId, tab,
                        onlyForPreconfiguredParams, countOfUnprocessedTabs,
                        countOfUnprocessedPagesUnderSession));
                page.setTabsLoadingAlreadyStarted(true);
                potSessionPageService.updatePotSessionPage(page);
                log.info("[Session - {}] Successfully started getting info for Tabs under page: {} "
                        + "(events for each tab was published).", sessionId, page.getName());
            }
        } catch (Exception ex) {
            log.error("[Session - " + sessionId + "] Unexpected error occurred during the "
                    + "getting info for Tabs under page: " + page.getName() + "!", ex);
        }
    }

    private void startPageValidation(UUID sessionId,
                                     String pageName,
                                     UUID pageId,
                                     boolean onlyForPreconfiguredParams,
                                     AtomicInteger countOfUnprocessedTabsUnderPage,
                                     AtomicInteger countOfUnprocessedPagesUnderSession) {
        ValidatePageEvent validatePageEvent = ValidatePageEvent.builder()
                .sessionId(sessionId)
                .pageName(pageName)
                .pageId(pageId)
                .onlyForPreconfiguredParams(onlyForPreconfiguredParams)
                .countOfUnprocessedTabs(countOfUnprocessedTabsUnderPage)
                .countOfUnprocessedPagesUnderSession(countOfUnprocessedPagesUnderSession)
                .build();
        eventPublisher.publishEvent(validatePageEvent);
    }

    private void startGettingInfoForTab(UUID sessionId,
                                        PotSessionTabEntity tab,
                                        boolean onlyForPreconfiguredParams,
                                        AtomicInteger countOfUnprocessedTabs,
                                        AtomicInteger countOfUnprocessedPagesUnderSession) {
        GetInfoForTabEvent getInfoForTabEvent = GetInfoForTabEvent.builder()
                .sessionId(sessionId)
                .tabId(tab.getId())
                .tabName(tab.getName())
                .onlyForPreconfiguredParams(onlyForPreconfiguredParams)
                .countOfUnprocessedTabsUnderPage(countOfUnprocessedTabs)
                .countOfUnprocessedPagesUnderSession(countOfUnprocessedPagesUnderSession)
                .build();
        eventPublisher.publishEvent(getInfoForTabEvent);
    }
}
