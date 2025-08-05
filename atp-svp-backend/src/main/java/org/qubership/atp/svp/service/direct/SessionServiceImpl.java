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
import java.util.UUID;

import javax.transaction.Transactional;

import org.qubership.atp.svp.model.db.SessionEntity;
import org.qubership.atp.svp.repo.jpa.SessionRepository;
import org.qubership.atp.svp.service.SessionService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final EurekaDiscoveryServiceImpl eurekaDiscoveryServiceImpl;
    private final SessionRepository sessionRepository;

    public SessionServiceImpl(EurekaDiscoveryServiceImpl eurekaDiscoveryServiceImpl,
                              SessionRepository sessionRepository) {
        this.eurekaDiscoveryServiceImpl = eurekaDiscoveryServiceImpl;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public void addNewSession(UUID sessionId) {
        String podName = eurekaDiscoveryServiceImpl.getCurrentPodName();
        sessionRepository.saveAndFlush(new SessionEntity(sessionId, podName));
        log.info(String.format("Session %s was added successfully", sessionId));
    }

    @Override
    public boolean hasSession(UUID sessionId) {
        String podName = eurekaDiscoveryServiceImpl.getCurrentPodName();
        return sessionRepository.containsSession(sessionId, podName);
    }

    @Override
    public List<String> getSessions() {
        String podName = eurekaDiscoveryServiceImpl.getCurrentPodName();
        return sessionRepository.getSessionIds(podName);
    }

    @Override
    public void removeSession(UUID sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    @Override
    @Transactional
    public synchronized void removeLostSessions() {
        List<String> podNames = eurekaDiscoveryServiceImpl.getAllPods();
        sessionRepository.removeLostSessions(podNames);
    }
}
