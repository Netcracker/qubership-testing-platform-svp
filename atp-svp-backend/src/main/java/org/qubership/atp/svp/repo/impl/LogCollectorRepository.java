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

package org.qubership.atp.svp.repo.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.svp.clients.api.logcollector.dto.public_api.ConfigurationDto;
import org.qubership.atp.svp.clients.api.logcollector.dto.public_api.SearchRequestDto;
import org.qubership.atp.svp.clients.api.logcollector.dto.public_api.SearchResultsDto;
import org.qubership.atp.svp.core.exceptions.InvalidLogCollectorApiUsageException;
import org.qubership.atp.svp.model.impl.LogCollectorSettings;
import org.qubership.atp.svp.model.logcollector.LogCollectorConfiguration;
import org.qubership.atp.svp.model.logcollector.LogCollectorSearchRequest;
import org.qubership.atp.svp.model.logcollector.SearchResult;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.values.LogCollectorValueObject;
import org.qubership.atp.svp.repo.feign.LogCollectorConfigurationFeignClient;
import org.qubership.atp.svp.repo.feign.LogCollectorFeignClient;
import org.qubership.atp.svp.repo.feign.LogCollectorQueueFeignClient;
import org.qubership.atp.svp.utils.DtoConvertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class LogCollectorRepository {

    private final LogCollectorFeignClient logCollectorFeignClient;
    private final LogCollectorQueueFeignClient logCollectorQueueFeignClient;
    private final LogCollectorConfigurationFeignClient logCollectorConfigurationFeignClient;
    private final DtoConvertService dtoConvertService;

    @Value("${feign.atp.logcollector.url}")
    private String logCollectorUrl;

    /**
     * Constructor for class.
     */
    @Autowired
    public LogCollectorRepository(LogCollectorFeignClient logCollectorFeignClient,
                                  LogCollectorConfigurationFeignClient logCollectorConfigurationFeignClient,
                                  LogCollectorQueueFeignClient logCollectorQueueFeignClient,
                                  DtoConvertService dtoConvertService) {
        this.logCollectorFeignClient = logCollectorFeignClient;
        this.logCollectorConfigurationFeignClient = logCollectorConfigurationFeignClient;
        this.logCollectorQueueFeignClient = logCollectorQueueFeignClient;
        this.dtoConvertService = dtoConvertService;
    }

    /**
     * Start search from LogCollector via /api/logs/registerSearch endpoint.
     *
     * @param executionConfiguration - session execution configuration
     * @param settings - settings for LogCollector search
     * @return {@link LogCollectorValueObject} with searchResult response from LogCollector registerSearch response.
     */
    public LogCollectorValueObject startSearch(SessionExecutionConfiguration executionConfiguration,
                                               LogCollectorSettings settings, UUID requestId) {
        List<UUID> configurationsIds = getLogCollectorConfigurationIds(
                executionConfiguration.getLogCollectorConfigurations(), settings);
        LogCollectorSearchRequest request = settings.generateRequest(executionConfiguration.getProjectId(),
                executionConfiguration.getEnvironment().getId(), configurationsIds,
                executionConfiguration.getLogCollectorSearchPeriod(), requestId);
        SearchResult startSearchResult = registerSearchInLogCollector(request);
        return new LogCollectorValueObject(startSearchResult);
    }

    private SearchResult registerSearchInLogCollector(LogCollectorSearchRequest request) {
        log.info("Registering search in LogCollector with request body: {}", request);
        try {
            SearchRequestDto dtoRequest = dtoConvertService.convert(request, SearchRequestDto.class);
            SearchResultsDto dtoResult = logCollectorFeignClient.registerSearch(dtoRequest).getBody();
            SearchResult result = dtoConvertService.convert(dtoResult, SearchResult.class);
            log.info("Successfully received the register search id: {} "
                    + "for request body: {}", result.getSearchId(), request);
            return result;
        } catch (Throwable e) {
            String message = "Failed to register search in LogCollector with request body: " + request;
            log.error(message, e);
            throw new InvalidLogCollectorApiUsageException(message, e);
        }
    }

    /**
     * Get search results without waiting from LogCollector
     * via /api/logs/getSearchResults/{responseSearchId}/fastResponse endpoint.
     *
     * @param responseSearchId - search ID from LogCollector
     * @return {@link LogCollectorValueObject} with search results from LogCollector getSearchResults response.
     */
    public LogCollectorValueObject getLogCollectorValueObjectFromSearchResult(UUID responseSearchId) {
        SearchResult searchResult = getSearchResultsFromLogCollector(responseSearchId);
        LogCollectorValueObject lcValue = new LogCollectorValueObject(searchResult);
        lcValue.setHasResults(true);
        return lcValue;
    }

    private SearchResult getSearchResultsFromLogCollector(UUID responseSearchId) {
        log.info("Getting search results with id: {} from LogCollector", responseSearchId);
        try {
            SearchResultsDto dtoSearchResult = logCollectorFeignClient.getSearchResultsFast(responseSearchId).getBody();
            SearchResult searchResult = dtoConvertService.convert(dtoSearchResult, SearchResult.class);
            log.info("Successfully received search results with id: {} from LogCollector", responseSearchId);
            return searchResult;
        } catch (Exception e) {
            String message = "Failed to received search results with id: " + responseSearchId + " from LogCollector";
            log.error(message, e);
            throw new InvalidLogCollectorApiUsageException(message, e);
        }
    }

    private List<UUID> getLogCollectorConfigurationIds(List<UUID> logCollectorConfig,
                                                       LogCollectorSettings settings) {
        List<UUID> configurations = logCollectorConfig
                .stream().filter(configuration -> settings.getConfigurations().contains(configuration))
                .collect(Collectors.toList());
        if (configurations.isEmpty()) {
            throw new RuntimeException("Log Collector configurations for current LC settings are empty.");
        }
        return configurations;
    }

    /**
     * Download and returns List LogCollectorConfiguration  from Log Collector API.
     */
    public List<LogCollectorConfiguration> getConfigurations(UUID projectId) {
        log.info("Getting configurations in LogCollector for projectId: {}.", projectId);
        List<ConfigurationDto> dtoConfigurations =
                logCollectorConfigurationFeignClient.getConfigurationsByProjectId(projectId).getBody();
        List<LogCollectorConfiguration> configurations = dtoConvertService.convertList(dtoConfigurations,
                LogCollectorConfiguration.class);
        log.info("Successfully received the configurations from the LogCollector for projectId: {}.", projectId);
        return configurations;
    }

    /**
     * Request to cancel searches in Log Collector.
     *
     * @param killedSearches list search id which will send to cancel in Log Collector.
     */
    public void cancelSearches(List<UUID> killedSearches) {
        log.info("Canceling searches queue in LogCollector: {}.", killedSearches);
        try {
            List<UUID> canceledSearches = logCollectorQueueFeignClient.cancelSearches(killedSearches).getBody();
            log.info("Successfully canceled searches in LogCollector: {}.", canceledSearches);
        } catch (Throwable e) {
            String message = "Failed to cancel searches in LogCollector: " + killedSearches + ".";
            log.error(message, e);
        }
    }
}
