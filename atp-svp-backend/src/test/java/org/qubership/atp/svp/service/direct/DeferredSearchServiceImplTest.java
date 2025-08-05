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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionTabEntity;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.repo.impl.LogCollectorRepository;
import org.qubership.atp.svp.tests.DbMockEntity;
import org.qubership.atp.svp.tests.TestWithTestData;


@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = DeferredSearchServiceImplTest.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false",
                "svp.deferred-search-results.lifespan.sec=600"})
public class DeferredSearchServiceImplTest extends TestWithTestData {

    @SpyBean
    DeferredSearchServiceImpl deferredSearchService;

    @MockBean
    WebSocketMessagingService messagingService;
    @MockBean
    KafkaMessagingService kafkaMessagingService;
    @MockBean
    SessionServiceImpl sessionServiceImpl;
    @MockBean
    LogCollectorRepository logCollectorRepository;

    @Test
    public void killExpiredDeferredSearchResults_oneIsExpired_expiredIsDeleted() throws IOException {
        UUID sessionId = UUID.randomUUID();
        UUID expiredId = UUID.randomUUID();
        UUID searchId = UUID.randomUUID();
        OffsetDateTime currentTime = OffsetDateTime.now();
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterEntity();
        PotSessionParameterEntity expiredParameter = DbMockEntity.generatePotSessionParameterEntity();
        PotSessionTabEntity potSessionTabEntity = new PotSessionTabEntity();
        potSessionTabEntity.setId(UUID.randomUUID());
        expiredParameter.setPotSessionTabEntity(potSessionTabEntity);
        SutParameterExecutionContext expiredParameterExecutionContext = generateParameterExecutionContext(sessionId,
                currentTime.minusDays(1), expiredParameter, expiredId);
        SutParameterExecutionContext parameterExecutionContext = generateParameterExecutionContext(sessionId,
                currentTime, parameter, searchId);
        ConcurrentHashMap<UUID, AbstractParameterExecutionContext> currentLogCollectorSearches
                = new ConcurrentHashMap<>();
        currentLogCollectorSearches.put(expiredId, expiredParameterExecutionContext);
        currentLogCollectorSearches.put(searchId, parameterExecutionContext);
        Mockito.doReturn(true).when(sessionServiceImpl).hasSession(any());
        Mockito.doNothing().when(messagingService).sendSutParameterResult(any(), any());
        ReflectionTestUtils.setField(deferredSearchService, "deferredSearches",
                currentLogCollectorSearches);

        deferredSearchService.killExpiredDeferredSearchResults();

        Assert.assertEquals(ValidationStatus.WARNING, expiredParameter.getValidationInfo().getStatus());
        Assert.assertEquals(ValidationStatus.IN_PROGRESS, parameter.getValidationInfo().getStatus());
        verify(logCollectorRepository, times(1)).cancelSearches(any());
        Assert.assertEquals(1, currentLogCollectorSearches.size());
        verify(messagingService).sendSutParameterResult(sessionId, expiredParameter);
    }


    @Test
    public void killExpiredDeferredSearchResults_withoutDeferredSearchResultsLifespan() throws IOException {
        UUID sessionId = UUID.randomUUID();
        UUID searchId = UUID.randomUUID();
        UUID searchId2 = UUID.randomUUID();
        OffsetDateTime currentTime = OffsetDateTime.now();
        PotSessionParameterEntity parameter1 = DbMockEntity.generatePotSessionParameterEntity();
        PotSessionParameterEntity parameter2 = DbMockEntity.generatePotSessionParameterEntity();
        SutParameterExecutionContext parameterExecutionContext1 = generateParameterExecutionContext(sessionId,
                currentTime, parameter1, searchId2);
        SutParameterExecutionContext parameterExecutionContext2 = generateParameterExecutionContext(sessionId,
                currentTime, parameter2, searchId);
        ConcurrentHashMap<UUID, AbstractParameterExecutionContext> currentLogCollectorSearches
                = new ConcurrentHashMap<>();
        currentLogCollectorSearches.put(searchId, parameterExecutionContext1);
        currentLogCollectorSearches.put(searchId2, parameterExecutionContext2);
        Mockito.doReturn(true).when(sessionServiceImpl).hasSession(any());
        Mockito.doNothing().when(messagingService).sendSutParameterResult(any(), any());
        ReflectionTestUtils.setField(deferredSearchService, "deferredSearches",
                currentLogCollectorSearches);

        deferredSearchService.killExpiredDeferredSearchResults();

        Assert.assertEquals(ValidationStatus.IN_PROGRESS, parameter1.getValidationInfo().getStatus());
        Assert.assertEquals(ValidationStatus.IN_PROGRESS, parameter2.getValidationInfo().getStatus());
        verify(logCollectorRepository, never()).cancelSearches(any());
        Assert.assertEquals(2, currentLogCollectorSearches.size());
        Mockito.doReturn(true).when(sessionServiceImpl).hasSession(any());
        verify(messagingService, never()).sendSutParameterResult(any(), any());
    }
}
