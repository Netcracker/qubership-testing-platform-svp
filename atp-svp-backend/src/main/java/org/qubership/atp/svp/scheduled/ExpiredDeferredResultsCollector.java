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

package org.qubership.atp.svp.scheduled;

import org.qubership.atp.svp.service.DeferredSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExpiredDeferredResultsCollector {

    private final DeferredSearchService deferredSearchService;

    @Autowired
    public ExpiredDeferredResultsCollector(DeferredSearchService deferredSearchService) {
        this.deferredSearchService = deferredSearchService;
    }

    /**
     * Scheduled task which kills all expired deferred results (LogCollector related parameters).
     * <br>
     * Executes every 10 seconds.
     */
    //  TODO think about how to count fixedRate, relative to svp.deferred-results.lifespan
    @Scheduled(fixedRate = 10000)
    public void killExpiredDeferredResults() {
        log.debug("ExpiredDeferredResultsCollector - running...");
        try {
            deferredSearchService.killExpiredDeferredSearchResults();
            log.debug("ExpiredDeferredResultsCollector - done.");
        } catch (Throwable e) {
            log.error("An error occurred while looking for expired deferred results. \nMessage: {}", e.getMessage());
        }
    }
}
