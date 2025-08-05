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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.events.ValidateTabEvent;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.model.pot.validation.ValidationInfo;
import org.qubership.atp.svp.repo.impl.LogCollectorRepository;
import org.qubership.atp.svp.service.AbstractMessagingService;
import org.qubership.atp.svp.service.DeferredSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DeferredSearchServiceImpl extends AbstractMessagingService implements DeferredSearchService {

    private final LogCollectorRepository logCollectorRepository;
    private final ApplicationEventPublisher eventPublisher;
    @Value("${svp.deferred-search-results.lifespan.sec}")
    private Integer deferredSearchResultsLifespan;

    /**
     * Constructor for initializing DeferredSearchService.
     */
    @Autowired
    public DeferredSearchServiceImpl(LogCollectorRepository logCollectorRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.logCollectorRepository = logCollectorRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Cache for every search contains necessary execution context of parameter.
     */
    private ConcurrentHashMap<UUID, AbstractParameterExecutionContext> deferredSearches = new ConcurrentHashMap<>();

    @Override
    public Optional<AbstractParameterExecutionContext> findContextByRequestSearchId(UUID requestSearchId) {
        return Optional.ofNullable(deferredSearches.get(requestSearchId));
    }

    @Override
    public void storeContextByRequestSearchId(UUID requestSearchId, AbstractParameterExecutionContext context) {
        deferredSearches.put(requestSearchId, context);
    }

    @Override
    public void evictContextByRequestSearchId(UUID requestSearchId) {
        deferredSearches.remove(requestSearchId);
    }

    @Override
    public synchronized void killAllDeferredSearchResultsByExpiredSessionId(UUID sessionId) {
        List<UUID> killedSearchesId = new ArrayList<>();
        deferredSearches.keySet().stream()
                .filter(requestSearchId -> isDeferredSearchResultBelongsToSession(requestSearchId, sessionId))
                .forEach(requestSearchId -> {
                    UUID responseSearchId = deferredSearches.get(requestSearchId).getResponseSearchId();
                    killedSearchesId.add(responseSearchId);
                    killDeferredSearchResult(requestSearchId,
                            "Session was expired and all deferred results was evicted from cache");
                });
        if (!killedSearchesId.isEmpty()) {
            logCollectorRepository.cancelSearches(killedSearchesId);
        }
    }

    @Override
    @Transactional
    public synchronized void killExpiredDeferredSearchResults() {
        List<UUID> killedSearchesId = new ArrayList<>();
        deferredSearches.keySet().stream()
                .filter(this::isDeferredSearchResultExpired)
                .forEach(requestSearchId -> {
                    UUID responseSearchId = deferredSearches.get(requestSearchId).getResponseSearchId();
                    killedSearchesId.add(responseSearchId);
                    killDeferredSearchResult(requestSearchId,
                            "Deferred result was expired and evicted from cache");
                });
        if (!killedSearchesId.isEmpty()) {
            logCollectorRepository.cancelSearches(killedSearchesId);
        }
    }

    @Override
    @Transactional
    public synchronized void killDeferredSearchResult(UUID requestSearchId, String messageAboutKilling) {
        findContextByRequestSearchId(requestSearchId).ifPresent(parameterContext -> {
            parameterContext.getParameter().getArValues().clear();
            ValidationInfo validationInfoForKilledResult =
                    new ValidationInfo(ValidationStatus.WARNING, messageAboutKilling);
            parameterContext.getParameter().setValidationInfo(validationInfoForKilledResult);
            getMessageService(parameterContext.getSessionId())
                    .sendSutParameterResult(parameterContext.getSessionId(), parameterContext.getParameter());
            parameterContext.decrementCountOfUnprocessedParameters();
            evictContextByRequestSearchId(requestSearchId);
            startTabValidation(parameterContext);
        });
    }

    private void startTabValidation(AbstractParameterExecutionContext parameterContext) {
        SutParameterExecutionContext executionContext = (SutParameterExecutionContext) parameterContext;
        ValidateTabEvent validateTabEvent = ValidateTabEvent.builder()
                .sessionId(executionContext.getSessionId())
                .tabId(executionContext.getParameter().getPotSessionTabEntity().getId())
                .pageName(executionContext.getParameter().getPage())
                .tabName(executionContext.getParameter().getTab())
                .onlyForPreconfiguredParams(executionContext.getSessionConfiguration().getOnlyForPreconfiguredParams())
                .countOfUnprocessedParameters(executionContext.getCountOfUnprocessedParametersUnderTab())
                .countOfUnprocessedTabsUnderPage(executionContext.getCountOfUnprocessedTabsUnderPage())
                .countOfUnprocessedPagesUnderSession(executionContext.getCountOfUnprocessedPagesUnderSession())
                .build();
        eventPublisher.publishEvent(validateTabEvent);
    }

    private boolean isDeferredSearchResultExpired(UUID requestSearchId) {
        try {
            Optional<AbstractParameterExecutionContext> parameterContext =
                    findContextByRequestSearchId(requestSearchId);
            return parameterContext.isPresent()
                    && Duration.between(parameterContext.get().getParameterStarted(), OffsetDateTime.now())
                    .getSeconds() > deferredSearchResultsLifespan;
        } catch (Exception e) {
            log.error("Comparison of the context creation date was failed from the deferred results cache "
                    + "with the current time. \nMessage: {}", e.getMessage());
            return false;
        }
    }

    private boolean isDeferredSearchResultBelongsToSession(UUID requestSearchId, UUID sessionId) {
        try {
            Optional<AbstractParameterExecutionContext> parameterContext =
                    findContextByRequestSearchId(requestSearchId);
            return parameterContext.isPresent()
                    && parameterContext.get().getSessionId().equals(sessionId);
        } catch (Exception e) {
            log.error("Comparison of session ID's was failed from the deferred results cache "
                            + "with the expected session ID: {}. \nMessage: {}",
                    sessionId, e.getMessage());
            return false;
        }
    }
}
