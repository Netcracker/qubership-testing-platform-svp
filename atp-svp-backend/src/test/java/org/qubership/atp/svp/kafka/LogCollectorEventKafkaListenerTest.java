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

package org.qubership.atp.svp.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonSerializer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ser.std.StringSerializer;
import org.testcontainers.utility.DockerImageName;

import org.qubership.atp.svp.config.kafka.DefaultKafkaValidationErrorHandler;
import org.qubership.atp.svp.core.enums.DisplayType;
import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.core.enums.ValidationType;
import org.qubership.atp.svp.model.db.SutParameterEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.events.ReloadSutParameterEvent;
import org.qubership.atp.svp.model.impl.ErConfig;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.impl.SutParameter;
import org.qubership.atp.svp.model.kafka.LogCollectorKafkaMessage;
import org.qubership.atp.svp.model.logcollector.SearchStatus;
import org.qubership.atp.svp.model.pot.PotSessionParameter;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.SimpleExecutionVariable;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.model.pot.validation.ValidationInfo;
import org.qubership.atp.svp.repo.feign.EnvironmentFeignClient;
import org.qubership.atp.svp.repo.feign.EnvironmentsProjectFeignClient;
import org.qubership.atp.svp.repo.impl.EnvironmentRepository;
import org.qubership.atp.svp.service.DeferredSearchService;
import org.qubership.atp.svp.service.listeners.ParameterEventListener;
import org.qubership.atp.svp.tests.TestWithTestData;
import org.qubership.atp.svp.utils.DtoConvertService;
import org.qubership.atp.svp.model.environments.Connection;
import org.qubership.atp.svp.model.environments.System;

@AutoConfigureMockMvc
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles({"IntegrationTest"})
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties", properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
public class LogCollectorEventKafkaListenerTest extends TestWithTestData {

    private final CountDownLatch latch = new CountDownLatch(1);
    private final int delayForReceiveNotification = 5;

    @Value("${kafka.topic.end.logcollector}")
    private String logCollectorSearchEndTopic;

    @SpyBean
    private LogCollectorEventKafkaListener logCollectorEventKafkaListener;

    private final UUID projectId = UUID.fromString("323eda51-47b5-414a-951a-27221fa374a2");

    @SpyBean
    private DefaultKafkaValidationErrorHandler defaultKafkaValidationErrorHandler;

    @Autowired
    public KafkaTemplate<String, LogCollectorKafkaMessage> kafkaTemplate;

    @SpyBean
    public DeferredSearchService deferredSearchService;

    @ClassRule
    public static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));

    /**
     * FOR COOL TEST
     */
    private final String commonParametersFilePath = "src/test/config/project/test/common_parameters.json";
    private final String testPageFilePath = "src/test/config/project/test/pages/%s";
    @SpyBean
    private ParameterEventListener sutParameterListener;

    private SessionExecutionConfiguration sessionConfiguration;

    @MockBean
    private EnvironmentFeignClient environmentFeignClient;

    @MockBean
    private DtoConvertService dtoConvertService;

    @MockBean
    private EnvironmentsProjectFeignClient environmentsProjectFeignClient;

    @MockBean
    private EnvironmentRepository environmentRepository;

    @Before
    public void beforeEach() {
        // Mock getting environment by project id
        Environment environment = new Environment();
        environment.setProjectId(projectId);
        when(environmentRepository.getEnvironmentById(any())).thenReturn(environment);
        //Mock session configuration
        sessionConfiguration = SessionExecutionConfiguration.builder()
                .environment(getEnvironment())
                .pagesName(Collections.emptyList())
                .logCollectorConfigurations(Collections.emptyList())
                .shouldHighlightDiffs(false)
                .shouldSendSessionResults(false)
                .isFullInfoNeededInPot(false)
                .onlyForPreconfiguredParams(false)
                .isPotGenerationMode(false)
                .onlyCommonParametersExecuted(false)
                .forcedLoadingCommonParameters(false)
                .build();
    }

    @After
    public void afterEach() throws IOException {
        Files.deleteIfExists(Paths.get(commonParametersFilePath));
        Files.deleteIfExists(Paths.get(String.format(testPageFilePath, "SalesOrderInformation")));
    }

    @Test
    public void
    whenGotLogCollectorKafkaEvent_thenCalledMethodListenOnLogCollectorEventKafkaListenerWithCorrectPayload() throws
            Exception {
        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage = new LogCollectorKafkaMessage();
        UUID responseSearchId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        String expectedKafkaResponseKey = responseSearchId.toString();
        expectedLogCollectorKafkaMessage.setRequestId(requestId);
        expectedLogCollectorKafkaMessage.setStatus(SearchStatus.COMPLETED.toString());
        expectedLogCollectorKafkaMessage.setRequestTool("atp-svp");

        PotSessionParameterEntity potSessionParameter = getPotSessionParameterEntity();
        UUID sessionId = UUID.randomUUID();
        ConcurrentHashMap executionVariables = new ConcurrentHashMap<>();
        executionVariables.put("name ExecutionVariable",
                new SimpleExecutionVariable("name ExecutionVariable", "value ExecutionVariable"));

        AtomicInteger zeroCounter = new AtomicInteger();
        SutParameterExecutionContext expectedContext = SutParameterExecutionContext.builder()
                .sessionId(sessionId)
                .parameterStarted(OffsetDateTime.now())
                .sessionConfiguration(sessionConfiguration)
                .executionVariables(executionVariables)
                .parameter(potSessionParameter)
                .isDeferredSearchResult(false)
                .countOfUnprocessedParametersUnderTab(new AtomicInteger(2))
                .countOfUnprocessedSynchronousParametersUnderPage(zeroCounter)
                .countOfUnprocessedTabsUnderPage(zeroCounter)
                .countOfUnprocessedPagesUnderSession(zeroCounter)
                .responseSearchId(responseSearchId)
                .build();
        ReloadSutParameterEvent expectedReloadSutParameterEvent = ReloadSutParameterEvent.builder()
                .requestId(requestId)
                .parameterExecutionContext(expectedContext)
                .build();

        deferredSearchService.storeContextByRequestSearchId(requestId, expectedContext);

        //Action
        kafkaTemplate.send(logCollectorSearchEndTopic, expectedKafkaResponseKey, expectedLogCollectorKafkaMessage);

        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);

        Mockito.verify(logCollectorEventKafkaListener, Mockito.timeout(5000))
                .listen(expectedKafkaResponseKey, expectedLogCollectorKafkaMessage);
        Mockito.verify(sutParameterListener, Mockito.timeout(5000))
                .handleReloadSutParameterEvent(eq(expectedReloadSutParameterEvent));
        Mockito.verify(defaultKafkaValidationErrorHandler, never()).handleError(any(), any(), any());
    }

    @Test
    public void whenGotLogCollectorKafkaEventWithNotEndedStatus_thenNotInvokeMethodHandleReloadSutParameterEvent()
            throws Exception {
        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage = new LogCollectorKafkaMessage();
        UUID responseSearchId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        String expectedKafkaResponseKey = responseSearchId.toString();
        expectedLogCollectorKafkaMessage.setRequestId(requestId);
        expectedLogCollectorKafkaMessage.setStatus(SearchStatus.IN_PROGRESS.toString());
        expectedLogCollectorKafkaMessage.setRequestTool("atp-svp");

        PotSessionParameterEntity potSessionParameter = getPotSessionParameterEntity();
        UUID sessionId = UUID.randomUUID();
        SutParameterExecutionContext expectedContext = SutParameterExecutionContext.builder()
                .sessionId(sessionId)
                .parameterStarted(OffsetDateTime.now())
                .sessionConfiguration(sessionConfiguration)
                .executionVariables(new ConcurrentHashMap<>())
                .parameter(potSessionParameter)
                .isDeferredSearchResult(false)
                .countOfUnprocessedParametersUnderTab(new AtomicInteger(2))
                .countOfUnprocessedSynchronousParametersUnderPage(new AtomicInteger())
                .countOfUnprocessedTabsUnderPage(new AtomicInteger())
                .countOfUnprocessedPagesUnderSession(new AtomicInteger())
                .responseSearchId(responseSearchId)
                .build();
        ReloadSutParameterEvent expectedReloadSutParameterEvent = ReloadSutParameterEvent.builder()
                .requestId(requestId)
                .parameterExecutionContext(expectedContext)
                .build();
        deferredSearchService.storeContextByRequestSearchId(requestId, expectedContext);

        //Action
        kafkaTemplate.send(logCollectorSearchEndTopic, expectedKafkaResponseKey, expectedLogCollectorKafkaMessage);

        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
        Mockito.verify(logCollectorEventKafkaListener, Mockito.timeout(5000))
                .listen(expectedKafkaResponseKey, expectedLogCollectorKafkaMessage);
        Mockito.verify(sutParameterListener, never())
                .handleReloadSutParameterEvent(eq(expectedReloadSutParameterEvent));
        Mockito.verify(defaultKafkaValidationErrorHandler, never()).handleError(any(), any(), any());
    }

    @Test
    public void
    whenGotLogCollectorKafkaEventWithNotInDeferredSearchResultsCacheSearchId_thenNotInvokeMethodHandleReloadSutParameterEvent() throws Exception {
        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage = new LogCollectorKafkaMessage();
        UUID responseSearchId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        String expectedKafkaResponseKey = responseSearchId.toString();
        expectedLogCollectorKafkaMessage.setRequestId(requestId);
        expectedLogCollectorKafkaMessage.setStatus(SearchStatus.COMPLETED.toString());
        expectedLogCollectorKafkaMessage.setRequestTool("atp-svp");

        PotSessionParameterEntity potSessionParameter = getPotSessionParameterEntity();
        UUID sessionId = UUID.randomUUID();
        SutParameterExecutionContext expectedContext = SutParameterExecutionContext.builder()
                .sessionId(sessionId)
                .parameterStarted(OffsetDateTime.now())
                .sessionConfiguration(sessionConfiguration)
                .executionVariables(new ConcurrentHashMap<>())
                .parameter(potSessionParameter)
                .isDeferredSearchResult(false)
                .countOfUnprocessedParametersUnderTab(new AtomicInteger(2))
                .countOfUnprocessedSynchronousParametersUnderPage(new AtomicInteger())
                .countOfUnprocessedTabsUnderPage(new AtomicInteger())
                .countOfUnprocessedPagesUnderSession(new AtomicInteger())
                .responseSearchId(responseSearchId)
                .build();
        ReloadSutParameterEvent expectedReloadSutParameterEvent = ReloadSutParameterEvent.builder()
                .requestId(requestId)
                .parameterExecutionContext(expectedContext)
                .build();

        //Action
        kafkaTemplate.send(logCollectorSearchEndTopic, expectedKafkaResponseKey, expectedLogCollectorKafkaMessage);

        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
        Mockito.verify(logCollectorEventKafkaListener, Mockito.timeout(5000))
                .listen(expectedKafkaResponseKey, expectedLogCollectorKafkaMessage);
        Mockito.verify(sutParameterListener, never())
                .handleReloadSutParameterEvent(eq(expectedReloadSutParameterEvent));
        Mockito.verify(defaultKafkaValidationErrorHandler, never()).handleError(any(), any(), any());
    }

    @Test
    public void
    givenGotKafkaLogCollectorEventWithNullStatus_validatedMessageObjectIsNull_thenInvokeRecordFilterStrategyAndErrorHandler() throws Exception {
        UUID searchId = UUID.randomUUID();
        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage = null;
        //Action
        kafkaTemplate.send(logCollectorSearchEndTopic, searchId.toString(), expectedLogCollectorKafkaMessage);
        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
        Mockito.verify(logCollectorEventKafkaListener, never()).listen(any(), any());
        Mockito.verify(defaultKafkaValidationErrorHandler, never()).handleError(any(), any(), any());
    }

    @Test
    public void
    givenGotKafkaLogCollectorEventWithNullStatus_validatedMessageObjectRequestToolNotNameAtpSvp_thenInvokeRecordFilterStrategyAndErrorHandler() throws Exception {
        UUID searchId = UUID.randomUUID();
        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage = new LogCollectorKafkaMessage();
        expectedLogCollectorKafkaMessage.setStatus("Complete");
        expectedLogCollectorKafkaMessage.setRequestId(searchId);
        expectedLogCollectorKafkaMessage.setLogsLink(null);
        expectedLogCollectorKafkaMessage.setRequestTool("not-svp");
        //Action
        kafkaTemplate.send(logCollectorSearchEndTopic, searchId.toString(), expectedLogCollectorKafkaMessage);
        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
        Mockito.verify(logCollectorEventKafkaListener, never()).listen(any(), any());
        Mockito.verify(defaultKafkaValidationErrorHandler, never()).handleError(any(), any(), any());
    }

    @Test
    public void
    givenGotKafkaLogCollectorEventWithNullStatus_validatedMessageObjectRequestToolIsNull_thenInvokeRecordFilterStrategyAndErrorHandler() throws Exception {
        UUID searchId = UUID.randomUUID();
        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage = new LogCollectorKafkaMessage();
        expectedLogCollectorKafkaMessage.setStatus("Complete");
        expectedLogCollectorKafkaMessage.setRequestId(searchId);
        expectedLogCollectorKafkaMessage.setLogsLink(null);
        expectedLogCollectorKafkaMessage.setRequestTool(null);
        //Action
        kafkaTemplate.send(logCollectorSearchEndTopic, searchId.toString(), expectedLogCollectorKafkaMessage);
        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
        Mockito.verify(logCollectorEventKafkaListener, never()).listen(any(), any());
        Mockito.verify(defaultKafkaValidationErrorHandler, never()).handleError(any(), any(), any());
    }

    @Test
    public void
    givenGotKafkaLogCollectorEventWithNullStatus_validatedMessageObject_thenInvokeErrorHandlerAndNotInvokeKafkaListener2() throws Exception {
        UUID searchId = UUID.randomUUID();
        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage = new LogCollectorKafkaMessage();
        expectedLogCollectorKafkaMessage.setStatus(null);
        expectedLogCollectorKafkaMessage.setRequestId(null);
        expectedLogCollectorKafkaMessage.setLogsLink(null);
        expectedLogCollectorKafkaMessage.setRequestTool("atp-svp");
        //Action
        kafkaTemplate.send(logCollectorSearchEndTopic, searchId.toString(), expectedLogCollectorKafkaMessage);
        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
        Mockito.verify(logCollectorEventKafkaListener, never()).listen(any(), any());
        Mockito.verify(defaultKafkaValidationErrorHandler, Mockito.timeout(5000)).handleError(any(), any(), any());
    }

    private Environment getEnvironment() {
        Connection connection = new Connection();
        connection.setSystemId(UUID.randomUUID().toString());
        connection.setParameters(Collections.singletonMap("parameter key", "parameter value"));
        connection.setSourceTemplateId(UUID.randomUUID().toString());
        connection.setConnectionType("SSH");
        System system = new System();
        system.setEnvironmentId(UUID.randomUUID().toString());
        system.setConnections(Collections.singletonList(connection));
        Environment environment = new Environment();
        environment.setProjectId(UUID.randomUUID());
        environment.setSystems(Collections.singletonList(system));
        return environment;
    }

    private PotSessionParameter getPotSessionParameter() {
        PotSessionParameter potSessionParameter = new PotSessionParameter();
        potSessionParameter.setPage("TestPage");
        potSessionParameter.setTab("TestTab");
        potSessionParameter.setGroup("TestGroup");
        potSessionParameter.setParameterConfig(getSutParameter());
        potSessionParameter.setIsSynchronousLoading(false);
        potSessionParameter.setValidationInfo(new ValidationInfo());
        return potSessionParameter;
    }

    private PotSessionParameterEntity getPotSessionParameterEntity() {
        PotSessionParameterEntity potSessionParameter = new PotSessionParameterEntity();
        potSessionParameter.setPage("TestPage");
        potSessionParameter.setTab("TestTab");
        potSessionParameter.setGroup("TestGroup");
        potSessionParameter.setParameterConfig(getSutParameterEntity());
        potSessionParameter.setIsSynchronousLoading(false);
        potSessionParameter.setValidationInfo(new ValidationInfo());
        return potSessionParameter;
    }

    private SutParameterEntity getSutParameterEntity() {
        Source source = new Source();
        source.setSystem("SOME SYSTEM");
        source.setConnection("SOME CONNECTION");
        source.setEngineType(EngineType.REST);
        source.setScript("SOME SCRIPT");
        source.setSettings(Collections.EMPTY_SET);
        ErConfig erConfig = new ErConfig();
        erConfig.setType(ValidationType.NONE);
        SutParameterEntity sutParameter = new SutParameterEntity();
        sutParameter.setName("SOME SUT PARAM");
        sutParameter.setDisplayType(DisplayType.SSH_RESPONSE);
        sutParameter.setSource(source);
        sutParameter.setErConfig(erConfig);
        sutParameter.setComponent("component");
        return sutParameter;
    }

    private SutParameter getSutParameter() {
        Source source = new Source();
        source.setSystem("SOME SYSTEM");
        source.setConnection("SOME CONNECTION");
        source.setEngineType(EngineType.REST);
        source.setScript("SOME SCRIPT");
        source.setSettings(Collections.EMPTY_SET);
        ErConfig erConfig = new ErConfig();
        erConfig.setType(ValidationType.NONE);
        SutParameter sutParameter = new SutParameter();
        sutParameter.setName("SOME SUT PARAM");
        sutParameter.setDisplayType(DisplayType.SSH_RESPONSE);
        sutParameter.setDataSource(source);
        sutParameter.setEr(erConfig);
        sutParameter.setComponent("component");
        return sutParameter;
    }

    @TestConfiguration
    static class KafkaTestContainersConfiguration {

        @Bean
        public String getKafkaServerAddress() {
            return kafka.getBootstrapServers();
        }

        @Bean
        public KafkaTemplate<String, LogCollectorKafkaMessage> kafkaTemplate(ProducerFactory<String,
                LogCollectorKafkaMessage> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        public ProducerFactory<String, LogCollectorKafkaMessage> producerFactory() {
            Map<String, Object> configProps = new HashMap<>();
            configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
            configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            return new DefaultKafkaProducerFactory<>(configProps);
        }
    }
}
