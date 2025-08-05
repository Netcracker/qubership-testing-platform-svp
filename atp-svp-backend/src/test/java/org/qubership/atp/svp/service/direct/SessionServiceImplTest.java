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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.repo.jpa.SessionRepository;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SessionServiceImpl.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class SessionServiceImplTest {

    @Autowired
    SessionServiceImpl sessionServiceImpl;
    @MockBean
    EurekaDiscoveryServiceImpl eurekaDiscoveryServiceImpl;
    @MockBean
    SessionRepository sessionRepository;


    @Test
    public void hasSession_SessionExist_returnTrue() {
        UUID newSessionId = UUID.randomUUID();
        String testPodName = "testName";
        when(sessionRepository.containsSession(any(), anyString())).thenReturn(true);
        when(eurekaDiscoveryServiceImpl.getCurrentPodName()).thenReturn(testPodName);

       boolean sessionExist = sessionServiceImpl.hasSession(newSessionId);

        Assert.assertTrue(sessionExist);
    }

    @Test
    public void hasSession_SessionNotExist_returnFalse() {
        UUID newSessionId = UUID.randomUUID();

        boolean sessionExist = sessionServiceImpl.hasSession(newSessionId);

        Assert.assertFalse(sessionExist);
    }
}
