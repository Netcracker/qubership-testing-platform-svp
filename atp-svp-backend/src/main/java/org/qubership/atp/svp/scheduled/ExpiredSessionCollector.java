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

import org.qubership.atp.svp.core.exceptions.StoringSessionException;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionSessionNotFoundException;
import org.qubership.atp.svp.service.PotSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExpiredSessionCollector {

    private final PotSessionService sessionService;

    @Autowired
    public ExpiredSessionCollector(PotSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Scheduled task which kills all expired sessions.
     */
    @Scheduled(fixedRate = 10000)
    public void killExpiredSessions() {
        log.debug("ExpiredSessionCollector - running...");
        try {
            sessionService.killExpiredSessions();
            log.debug("ExpiredSessionCollector - done.");
        } catch (ExecutionSessionNotFoundException e) {
            log.error("Could not find a session while looking for expired sessions. \nMessage: {}", e.getMessage());
        } catch (StoringSessionException e) {
            log.error("Could not remove a session while looking for expired sessions. \nMessage: {}", e.getMessage());
        } catch (Throwable e) {
            log.error("An error occurred while looking for expired sessions. \nMessage: {}", e.getMessage());
        }
    }
}
