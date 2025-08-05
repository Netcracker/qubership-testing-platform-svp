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

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.model.impl.LogCollectorSettings;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.values.LogCollectorValueObject;
import org.qubership.atp.svp.repo.impl.LogCollectorRepository;
import org.qubership.atp.svp.service.direct.DeferredSearchServiceImpl;

public interface LogCollectorBasedDisplayTypeService extends DisplayTypeService {

    /**
     * Returns result for Log Collector search response.
     */
    default LogCollectorValueObject getLogCollectorValueObject(LogCollectorRepository logCollectorRepository,
                                                               DeferredSearchServiceImpl deferredSearchService,
                                                               ExecutionVariablesService executionVariablesService,
                                                               AbstractParameterExecutionContext context,
                                                               Source source)
            throws GettingValueException {
        LogCollectorSettings settings = prepareLogCollectorSettings(executionVariablesService,
                source, context.getExecutionVariables());
        LogCollectorValueObject lcValue;
        if (context.isDeferredSearchResult()) {
            //Getting register search
            lcValue = getLogCollectorValueObjectRegisterSearch(logCollectorRepository, deferredSearchService,
                    context, settings);
        } else {
            // Getting search results
            lcValue = getLogCollectorValueObjectForSearch(logCollectorRepository, context);
        }
        return lcValue;
    }

    /**
     * Wrapper method, which gets the register search from requestSearchId,
     * then generates and returns {@link LogCollectorValueObject} with registerSearch.
     * <br>
     * In case of any Exception cached requestSearchId will be evict from cache
     * and method will throw the {@link GettingValueException}.
     */
    default LogCollectorValueObject getLogCollectorValueObjectRegisterSearch(LogCollectorRepository lcRepository,
                                                                             DeferredSearchServiceImpl deferredSearch,
                                                                             AbstractParameterExecutionContext context,
                                                                             LogCollectorSettings settings)
            throws GettingValueException {
        //Generate the requestId of the search query in LC
        UUID requestSearchId = UUID.randomUUID();
        //Put it together with the current context
        deferredSearch.storeContextByRequestSearchId(requestSearchId, context);
        LogCollectorValueObject lcValue;
        try {
            lcValue = lcRepository.startSearch(context.getSessionConfiguration(),
                    settings, requestSearchId);
            if (Objects.isNull(context.getResponseSearchId())) {
                UUID responseSearchId = lcValue.getSearchResult().getSearchId();
                context.setResponseSearchId(responseSearchId);
            }
        } catch (RuntimeException e) {
            context.setDeferredSearchResult(false);
            deferredSearch.evictContextByRequestSearchId(requestSearchId);
            throw new GettingValueException(e.getMessage());
        }
        checkErrorRegisterSearchResponse(deferredSearch, context, requestSearchId, lcValue);
        return lcValue;
    }

    /**
     * The method checks if there is an error in the response register search logCollector.
     */
    default void checkErrorRegisterSearchResponse(DeferredSearchServiceImpl deferredSearch,
                                                  AbstractParameterExecutionContext context, UUID requestSearchId,
                                                  LogCollectorValueObject lcValue) {
        if (lcValue.hasError()) {
            context.setDeferredSearchResult(false);
            deferredSearch.evictContextByRequestSearchId(requestSearchId);
        }
    }

    /**
     * Wrapper method, which gets the search results from responseSearchId,
     * then generates and returns {@link LogCollectorValueObject} with searchResults.
     */
    default LogCollectorValueObject getLogCollectorValueObjectForSearch(LogCollectorRepository logCollectorRepository,
                                                                        AbstractParameterExecutionContext context)
            throws GettingValueException {

        try {
            // Get responseSearchId from context
            UUID responseSearchId = context.getResponseSearchId();
            return logCollectorRepository.getLogCollectorValueObjectFromSearchResult(responseSearchId);
        } catch (Exception e) {
            throw new GettingValueException("Could not convert search result "
                    + "from LogCollector to LogCollectorValueObject. Error message: " + e.getMessage(), e);
        }
    }

    /**
     * Fill Log Collector search query with key and common parameters.
     */
    default LogCollectorSettings prepareLogCollectorSettings(ExecutionVariablesService executionVariablesService,
                                                             Source source,
                                                             ConcurrentHashMap<String, ExecutionVariable> variables) {
        LogCollectorSettings settings = (LogCollectorSettings) source.getSettingsByType(LogCollectorSettings.class);
        return executionVariablesService.getLogCollectorSettingsWithExecutionVariables(variables, settings);
    }
}
