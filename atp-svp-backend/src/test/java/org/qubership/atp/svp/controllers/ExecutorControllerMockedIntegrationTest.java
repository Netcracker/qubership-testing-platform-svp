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

package org.qubership.atp.svp.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
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
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import org.qubership.atp.svp.clients.api.logcollector.dto.public_api.ConfigurationDto;
import org.qubership.atp.svp.clients.api.logcollector.dto.public_api.SearchResultsDto;
import org.qubership.atp.svp.core.enums.DisplayType;
import org.qubership.atp.svp.kafka.LogCollectorEventKafkaListener;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.kafka.LogCollectorKafkaMessage;
import org.qubership.atp.svp.model.logcollector.SearchStatus;
import org.qubership.atp.svp.repo.feign.EnvironmentFeignClient;
import org.qubership.atp.svp.repo.feign.EnvironmentsProjectFeignClient;
import org.qubership.atp.svp.repo.feign.LogCollectorConfigurationFeignClient;
import org.qubership.atp.svp.repo.feign.LogCollectorFeignClient;
import org.qubership.atp.svp.repo.feign.LogCollectorQueueFeignClient;
import org.qubership.atp.svp.repo.impl.BulkValidatorRepository;
import org.qubership.atp.svp.repo.impl.EnvironmentRepository;
import org.qubership.atp.svp.repo.impl.LogCollectorRepository;
import org.qubership.atp.svp.service.DeferredSearchService;
import org.qubership.atp.svp.service.direct.PotSessionServiceImpl;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.qubership.atp.svp.service.direct.WebSocketMessagingService;
import org.qubership.atp.svp.service.direct.displaytype.LinkDisplayTypeServiceImpl;
import org.qubership.atp.svp.service.direct.displaytype.ParamDisplayTypeServiceImpl;
import org.qubership.atp.svp.tests.TestWithTestData;
import org.qubership.atp.svp.utils.DtoConvertService;

@AutoConfigureMockMvc
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles({"IntegrationTest"})
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties", properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
public class ExecutorControllerMockedIntegrationTest extends TestWithTestData {

    @ClassRule
    public static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));

    private final CountDownLatch latch = new CountDownLatch(1);
    private final int delayForReceiveNotification = 5;
    private final int delayForAsynchronousEventsProcessing = 2;

    @SpyBean
    public DeferredSearchService deferredSearchService;
    private final String commonParametersFilePath = "src/test/config/project/test/common_parameters.json";
    private final UUID projectId = UUID.fromString(fileProject);
    private final UUID envId = UUID.fromString("4167b70f-6e5d-4d7d-91a1-8eac81742934");

    @Autowired
    public KafkaTemplate<String, LogCollectorKafkaMessage> kafkaTemplate;
    @Autowired
    private MockMvc mockMvc;
    @Value("${kafka.topic.end.logcollector}")
    private String logCollectorTopic;
    @SpyBean
    private PotSessionServiceImpl potSessionService;
    @SpyBean
    private LogCollectorEventKafkaListener logCollectorEventKafkaListener;
    @SpyBean
    private ParamDisplayTypeServiceImpl paramDisplayTypeService;
    @SpyBean
    private LinkDisplayTypeServiceImpl linkDisplayTypeService;
    @SpyBean
    private LogCollectorRepository logCollectorRepository;

    @MockBean
    private LogCollectorFeignClient logCollectorFeignClient;
    @MockBean
    private LogCollectorQueueFeignClient logCollectorQueueFeignClient;
    @MockBean
    private LogCollectorConfigurationFeignClient logCollectorConfigurationFeignClient;

    @MockBean
    private EnvironmentFeignClient environmentFeignClient;
    @MockBean
    private BulkValidatorRepository bulkValidatorRepository;
    @MockBean
    private WebSocketMessagingService webSocketMessagingService;
    @SpyBean
    private DtoConvertService dtoConvertService;
    @MockBean
    private EnvironmentsProjectFeignClient environmentsProjectFeignClient;
    @MockBean
    private EnvironmentRepository environmentRepository;
    @MockBean
    private ProjectConfigService projectConfigService;

    @Before
    public void beforeEach() {
        // Mock getting environment by project id
        Environment environment = new Environment();
        environment.setProjectId(projectId);
        when(environmentRepository.getEnvironmentById(any())).thenReturn(environment);
    }

    @After
    public void afterEach() throws IOException {
        Files.deleteIfExists(Paths.get(commonParametersFilePath));
    }

    /* LogCollector's deferred search results related tests */

    // TODO add a test for BV if status 200 came from LC and status 500 came from BV ?

    /**
     * Tested method: getInfo()
     * Endpoint: /api/svp/project/{projectId}/executor/get-info
     * <p>
     * State:
     * One Common Parameter,
     * One Page
     * Two {@link DisplayType#INTEGRATION_LOG} typed parameters.
     * Log Collector returns 500 error during the register search.
     * <p>
     * Expected Behavior:
     * One time send request to download LogCollectorConfigurations to LC for projectId
     * All common parameters were added to session,
     * All results of common and page parameters were added to session,
     * Calculation of the validation statuses of tab, page and session was correct,
     * Validation statuses were added to session and sent to websocket topic,
     * The event listener method from LogCollector was not invoked,
     * Validation was not performed with BulkValidator.
     */
//    @Test
//    public void
//    getInfo_integrationLogParamsWithLogCollectorServerErrorDuringRegisterSearch_postProcessingCommonAndSutParametersWasCorrect() throws Exception {
//        UUID configurationId1 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b8");
//        UUID configurationId2 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b9");
//        String logCollectorSearchQueryType1 = "Request";
//        String logCollectorSearchQueryType2 = "Response";
//        String pageName = "Integration";
//        String tabName = "OCS";
//        AtomicReference<UUID> sessionId = new AtomicReference<>();
//        // Set sessionId from startSession() method of PotSessionService
//        doAnswer(invocation -> {
//            UUID actualSessionId = (UUID) invocation.callRealMethod();
//            // Get real sessionId
//            sessionId.set(actualSessionId);
//            return actualSessionId;
//        }).when(potSessionService).startSession(any(), anyMap());
//        // Generate file with common parameters
//        String testFile = loadFileToString("src/test/resources/test_data/common-parameters/one_common_parameter.json");
//        writeToFile(commonParametersFilePath, testFile);
//        //Mock getting common parameters by Param display type
//        SimpleValueObject mockedCommonParamValue = new SimpleValueObject("Mocked_CommonParam_Value");
//        doReturn(mockedCommonParamValue).when(paramDisplayTypeService)
//                .getValueFromSource(any(), any());
//        //Mock getting LC config by project id
//        List<LogCollectorConfiguration> logCollectorConfiguration =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.LOG_COLLECTOR_CONFIGURATION_DEFAULT,
//                                LogCollectorConfiguration[].class));
//        ResponseEntity<List<ConfigurationDto>> logCollectorConfigurationDto =
//                new ResponseEntity<>(dtoConvertService.convertList(logCollectorConfiguration, ConfigurationDto.class)
//                        , HttpStatus.OK);
//        when(logCollectorConfigurationFeignClient.getConfigurationsByProjectId(eq(projectId))).thenReturn(logCollectorConfigurationDto);
//
//        // Extract real requestId when calling a method storeContextByLogCollectorSearchId() and set method
//        // kafkaTemplate.send()
//        AtomicReference<PotSessionParameter> expectedIntegrationLogsParameterWithValidation = new AtomicReference<>();
//        doAnswer(invocation -> {
//            // Get real requestId
//            UUID actualRequestId = invocation.getArgument(0);
//
//            String withValidationBeforeProcessing = ExecutorControllerTestConstants
//                    .getIntegrationLogPotSessionParameterWithValidationBeforeProcessing(pageName,
//                            configurationId1.toString(), false);
//            setPotSessionParameter(configurationId1, logCollectorSearchQueryType1,
//                    expectedIntegrationLogsParameterWithValidation, actualRequestId, withValidationBeforeProcessing);
//
//            invocation.callRealMethod();
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Request)")));
//
//        AtomicReference<PotSessionParameter> expectedIntegrationLogsParameterWithoutValidation =
//                new AtomicReference<>();
//        doAnswer(invocation -> {
//            // Get real requestId
//            UUID actualRequestId2 = invocation.getArgument(0);
//
//            String withoutValidationBeforeProcessing = ExecutorControllerTestConstants
//                    .getIntegrationLogPotSessionParameterWithoutValidationBeforeProcessing(pageName,
//                            configurationId2.toString(), false);
//            setPotSessionParameter(configurationId2, logCollectorSearchQueryType2,
//                    expectedIntegrationLogsParameterWithoutValidation, actualRequestId2,
//                    withoutValidationBeforeProcessing);
//            invocation.callRealMethod();
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Response)")));
//
//        //Mock starting LC search logs
//        byte[] errorResponseBody =
//                ExecutorControllerTestConstants.getLogCollectorErrorResponseBody("status 500 reading "
//                        + "client#registerSearch(request)", "/api/logs/registerSearch").getBytes();
//        Request request = Request.create(Request.HttpMethod.GET, "/api/logs/registerSearch", Collections.emptyMap(),
//                errorResponseBody, Charset.defaultCharset(), new RequestTemplate());
//        doThrow(new FeignException.InternalServerError("Can't register search", request, errorResponseBody))
//                .when(logCollectorFeignClient)
//                .registerSearch(argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getConfigurations().contains(configurationId1)
//                                || logCollectorSearchRequest.getConfigurations().contains(configurationId2)));
//
//        // Mock for expected Common PotSessionParameters after execution and validation processes
//        List<PotSessionParameter> expectedCommonParametersAfterProcessing =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.POT_SESSION_COMMON_PARAMETERS_DEFAULT,
//                                PotSessionParameter[].class));
//        expectedCommonParametersAfterProcessing.forEach(param -> {
//            param.setArValues(Collections.singletonList(mockedCommonParamValue));
//            param.setValidationInfo(new ValidationInfo(ValidationStatus.NONE));
//        });
//
//        // Mock GetInfoRequest
//        Map<String, String> keyParameters = new HashMap<>();
//        keyParameters.put("Sales_Order_ID", "Test_Sales_Order_ID");
//        keyParameters.put("Customer_ID", "Test_Customer_ID");
//        GetInfoRequest getInfoRequest = GetInfoRequest.builder()
//                .environmentId(envId)
//                .keyParameters(keyParameters)
//                .shouldHighlightDiffs(true)
//                .shouldSendSessionResults(true)
//                .pagesName(Collections.singletonList(pageName))
//                .build();
//
//        // Mock Get ProjectConfig From Bd
//        getMockProjectConfigFromBd(fileProject, projectConfigService);
//
//        mockMvc.perform(post("/api/svp/project/{projectId}/executor/get-info", projectId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(getInfoRequest)))
//                .andExpect(status().isOk());
//
//        PotSession actualPotSession = potSessionService.findSessionById(sessionId.get());
//
//        verify(logCollectorEventKafkaListener, never()).listen(any(), any());
//        verify(logCollectorEventKafkaListener, never()).listen(any(), any());
//        verify(bulkValidatorRepository, never()).compare(any(), anyList());
//        verify(bulkValidatorRepository, never()).createTestRun(any(), any(), anyList());
//
//        verify(logCollectorRepository).getConfigurations(projectId);
//
//        latch.await(delayForAsynchronousEventsProcessing, TimeUnit.SECONDS);
//
//        Assert.assertEquals(expectedCommonParametersAfterProcessing, actualPotSession.getCommonParameters());
//        expectedCommonParametersAfterProcessing.forEach(expectedCommonParameter ->
//                verify(webSocketMessagingService, timeout(25000))
//                        .sendSutParameterResult(sessionId.get(), expectedCommonParameter));
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithValidation.get());
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithoutValidation.get());
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForTab(sessionId.get(), pageName, tabName, ValidationStatus.WARNING);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForPage(sessionId.get(), pageName, ValidationStatus.WARNING);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForSession(sessionId.get(), ValidationStatus.FAILED);
//    }
//
//    private void setPotSessionParameter(UUID configurationId, String logCollectorSearchQueryType,
//                                        AtomicReference<PotSessionParameter> expectedIntegrationLogsParameterWithValidation,
//                                        UUID actualRequestId, String withValidationBeforeProcessing) throws IOException {
//        expectedIntegrationLogsParameterWithValidation.set(objectMapper.readValue(withValidationBeforeProcessing,
//                PotSessionParameter.class));
//        expectedIntegrationLogsParameterWithValidation.get().setArValues(Collections.singletonList(
//                new ErrorValueObject(ExecutorControllerTestConstants
//                        .getIntegrationLogParameterErrorValueObjectForRegisterSearch(
//                                configurationId.toString(), logCollectorSearchQueryType,
//                                actualRequestId.toString()))));
//        expectedIntegrationLogsParameterWithValidation.get().setValidationInfo(new ValidationInfo(ValidationStatus
//                .WARNING));
//    }
//
//    /**
//     *
//     */
//    @Test
//    public void
//    getInfo_integrationLogParamsWithLogCollectorServerErrorDuringGettingSearchResults_postProcessingCommonAndSutParametersWasCorrect() throws Exception {
//        UUID responseSearchId1 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f77");
//        UUID responseSearchId2 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f78");
//        UUID configurationId1 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b8");
//        UUID configurationId2 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b9");
//        String expectedKafkaResponseKey1 = responseSearchId1.toString();
//        String expectedKafkaResponseKey2 = responseSearchId2.toString();
//        String pageName = "Integration";
//        String tabName = "OCS";
//        AtomicReference<UUID> sessionId = new AtomicReference<>();
//        // Set sessionId from startSession() method of PotSessionService
//        doAnswer(invocation -> {
//            UUID actualSessionId = (UUID) invocation.callRealMethod();
//            // Get real sessionId
//            sessionId.set(actualSessionId);
//            return actualSessionId;
//        }).when(potSessionService).startSession(any(), anyMap());
//
//        // Generate file with common parameters
//        String testFile = loadFileToString("src/test/resources/test_data/common-parameters/one_common_parameter.json");
//        writeToFile(commonParametersFilePath, testFile);
//        //Mock getting common parameters by Param display type
//        SimpleValueObject mockedCommonParamValue = new SimpleValueObject("Mocked_CommonParam_Value");
//        doReturn(mockedCommonParamValue).when(paramDisplayTypeService).getValueFromSource(any(), any());
//        //Mock getting LC config by project id
//        List<LogCollectorConfiguration> logCollectorConfiguration =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.LOG_COLLECTOR_CONFIGURATION_DEFAULT,
//                                LogCollectorConfiguration[].class));
//        ResponseEntity<List<ConfigurationDto>> logCollectorConfigurationDto =
//                new ResponseEntity<>(dtoConvertService.convertList(logCollectorConfiguration, ConfigurationDto.class)
//                        , HttpStatus.OK);
//        when(logCollectorConfigurationFeignClient.getConfigurationsByProjectId(eq(projectId))).thenReturn(logCollectorConfigurationDto);
//
//        // Extract real requestId when calling a method storeContextByLogCollectorSearchId() and set method
//        // kafkaTemplate.send()
//        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage1 = new LogCollectorKafkaMessage();
//        doAnswer(invocation -> {
//            setExpectedLogCollectorKafkaMessage(expectedLogCollectorKafkaMessage1, invocation);
//            invocation.callRealMethod();
//            kafkaTemplate.send(logCollectorTopic, responseSearchId1.toString(), expectedLogCollectorKafkaMessage1);
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Request)")));
//
//        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage2 = new LogCollectorKafkaMessage();
//        doAnswer(invocation -> {
//            setExpectedLogCollectorKafkaMessage(expectedLogCollectorKafkaMessage2, invocation);
//            invocation.callRealMethod();
//            kafkaTemplate.send(logCollectorTopic, responseSearchId2.toString(), expectedLogCollectorKafkaMessage2);
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Response)")));
//
//        //Mock starting LC search logs
//        SearchResult searchResult1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId1.toString(),
//                                configurationId1.toString(), null), SearchResult.class);
//        SearchResult searchResult2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId2.toString(),
//                                configurationId2.toString(), null), SearchResult.class);
//        ResponseEntity<SearchResultsDto> mockGetSearchResultsResponseEntity1 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult1, SearchResultsDto.class),
//                        HttpStatus.OK);
//        ResponseEntity<SearchResultsDto> mockGetSearchResultsResponseEntity2 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult2, SearchResultsDto.class),
//                        HttpStatus.OK);
//        doReturn(mockGetSearchResultsResponseEntity1).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getConfigurations().contains(configurationId1)));
//        doReturn(mockGetSearchResultsResponseEntity2).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getConfigurations().contains(configurationId2)));
//
//        //Mock getting LC search logs result
//        byte[] errorResponseBody =
//                ExecutorControllerTestConstants.getLogCollectorErrorResponseBody("status 500 reading "
//                                + "client#getSearchResults(searchId)",
//                        "/api/logs/getSearchResults/{searchId}/fastResponse").getBytes();
//        Request request = Request.create(Request.HttpMethod.GET, "/api/logs/getSearchResults/{searchId}/fastResponse",
//                Collections.emptyMap(), errorResponseBody, Charset.defaultCharset(), new RequestTemplate());
//        doThrow(new FeignException.InternalServerError("Can't get search result", request, errorResponseBody))
//                .when(logCollectorFeignClient)
//                .getSearchResultsFast(argThat(searchId -> Arrays.asList(responseSearchId1, responseSearchId2)
//                        .contains(searchId)));
//
//        // Mock for expected Common PotSessionParameters after execution and validation processes
//        List<PotSessionParameter> expectedCommonParametersAfterProcessing =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.POT_SESSION_COMMON_PARAMETERS_DEFAULT,
//                                PotSessionParameter[].class));
//        expectedCommonParametersAfterProcessing.forEach(param -> {
//            param.setArValues(Collections.singletonList(mockedCommonParamValue));
//            param.setValidationInfo(new ValidationInfo(ValidationStatus.NONE));
//        });
//
//        // Mock for expected Integration Log parameter with validation before processing
//        PotSessionParameter expectedIntegrationLogsParameterWithValidation =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getIntegrationLogPotSessionParameterWithValidationBeforeProcessing(pageName,
//                                configurationId1.toString(), false), PotSessionParameter.class);
//        expectedIntegrationLogsParameterWithValidation.setArValues(Collections.singletonList(
//                new ErrorValueObject(ExecutorControllerTestConstants
//                        .getIntegrationLogParameterErrorValueObjectForGetSearchResult(
//                                responseSearchId1.toString()))));
//        expectedIntegrationLogsParameterWithValidation.setValidationInfo(new ValidationInfo(ValidationStatus
//                .WARNING));
//
//        // Mock for expected Integration Log parameter without validation before processing
//        PotSessionParameter expectedIntegrationLogsParameterWithoutValidation =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getIntegrationLogPotSessionParameterWithoutValidationBeforeProcessing(pageName,
//                                        configurationId2.toString(), false),
//                        PotSessionParameter.class);
//        expectedIntegrationLogsParameterWithoutValidation.setArValues(Collections.singletonList(
//                new ErrorValueObject(ExecutorControllerTestConstants
//                        .getIntegrationLogParameterErrorValueObjectForGetSearchResult(
//                                responseSearchId2.toString()))));
//        expectedIntegrationLogsParameterWithoutValidation
//                .setValidationInfo(new ValidationInfo(ValidationStatus.WARNING));
//
//        // Mock GetInfoRequest
//        Map<String, String> keyParameters = new HashMap<>();
//        keyParameters.put("Sales_Order_ID", "Test_Sales_Order_ID");
//        keyParameters.put("Customer_ID", "Test_Customer_ID");
//        GetInfoRequest getInfoRequest = GetInfoRequest.builder()
//                .environmentId(envId)
//                .keyParameters(keyParameters)
//                .shouldHighlightDiffs(true)
//                .shouldSendSessionResults(true)
//                .pagesName(Collections.singletonList(pageName))
//                .build();
//
//        // Mock Get ProjectConfig From Bd
//        getMockProjectConfigFromBd(fileProject, projectConfigService);
//
//        mockMvc.perform(post("/api/svp/project/{projectId}/executor/get-info", projectId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(getInfoRequest)))
//                .andExpect(status().isOk());
//
//        PotSession actualPotSession = potSessionService.findSessionById(sessionId.get());
//
//        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
//
//        verify(logCollectorEventKafkaListener, timeout(25000))
//                .listen(expectedKafkaResponseKey1, expectedLogCollectorKafkaMessage1);
//        verify(logCollectorEventKafkaListener, timeout(25000))
//                .listen(expectedKafkaResponseKey2, expectedLogCollectorKafkaMessage2);
//        verify(bulkValidatorRepository, never()).compare(any(), anyList());
//        verify(bulkValidatorRepository, never()).createTestRun(any(), any(), anyList());
//        // Checks for evict unnecessary deferred results from cache
//        Assert.assertFalse(deferredSearchService.findContextByRequestSearchId(responseSearchId1).isPresent());
//        Assert.assertFalse(deferredSearchService.findContextByRequestSearchId(responseSearchId2).isPresent());
//
//        verify(logCollectorRepository).getConfigurations(projectId);
//
//        Assert.assertEquals(expectedCommonParametersAfterProcessing, actualPotSession.getCommonParameters());
//        expectedCommonParametersAfterProcessing.forEach(expectedCommonParameter ->
//                verify(webSocketMessagingService, timeout(25000))
//                        .sendSutParameterResult(sessionId.get(), expectedCommonParameter));
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithValidation);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithoutValidation);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForTab(sessionId.get(), pageName, tabName, ValidationStatus.WARNING);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForPage(sessionId.get(), pageName, ValidationStatus.WARNING);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForSession(sessionId.get(), ValidationStatus.FAILED);
//    }
//
//    /**
//     * Tested method: getInfo()
//     * Endpoint: /api/svp/project/{projectId}/executor/get-info
//     * <p>
//     * State:
//     * One Common Parameter,
//     * One Page
//     * Two {@link DisplayType#INTEGRATION_LOG} typed parameters with PASSED validation
//     * <p>
//     * Expected Behavior:
//     * One time send request to download LogCollectorConfigurations to LC for projectId
//     * All common parameters were added to session,
//     * All results of common and page parameters were added to session,
//     * Calculation of the validation statuses of tab, page and session was correct,
//     * Validation statuses were added to session and sent to websocket topic.
//     */
//    @Test
//    public void getInfo_integrationLogParams_postProcessingCommonAndSutParametersWasCorrect() throws Exception {
//        UUID responseSearchId1 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f77");
//        UUID responseSearchId2 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f78");
//        UUID configurationId1 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b8");
//        UUID configurationId2 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b9");
//        String pageName = "Integration";
//        String tabName = "OCS";
//        AtomicReference<UUID> sessionId = new AtomicReference<>();
//        // Set sessionId from startSession() method of PotSessionService
//        doAnswer(invocation -> {
//            UUID actualSessionId = (UUID) invocation.callRealMethod();
//            // Get real sessionId
//            sessionId.set(actualSessionId);
//            return actualSessionId;
//        }).when(potSessionService).startSession(any(), anyMap());
//        // Generate file with common parameters
//        String testFile = loadFileToString("src/test/resources/test_data/common-parameters/one_common_parameter.json");
//        writeToFile(commonParametersFilePath, testFile);
//        //Mock getting common parameters by Param display type
//        SimpleValueObject mockedCommonParamValue = new SimpleValueObject("Mocked_CommonParam_Value");
//        doReturn(mockedCommonParamValue).when(paramDisplayTypeService).getValueFromSource(any(), any());
//        //Mock getting LC config by project id
//        List<LogCollectorConfiguration> logCollectorConfiguration =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.LOG_COLLECTOR_CONFIGURATION_DEFAULT,
//                                LogCollectorConfiguration[].class));
//        ResponseEntity<List<ConfigurationDto>> logCollectorConfigurationDto =
//                new ResponseEntity<>(dtoConvertService.convertList(logCollectorConfiguration, ConfigurationDto.class)
//                        , HttpStatus.OK);
//        when(logCollectorConfigurationFeignClient.getConfigurationsByProjectId(eq(projectId))).thenReturn(logCollectorConfigurationDto);
//
//        // Extract real requestId when calling a method storeContextByLogCollectorSearchId() and set method
//        // kafkaTemplate.send()
//        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage1 = new LogCollectorKafkaMessage();
//        doAnswer(invocation -> {
//            setExpectedLogCollectorKafkaMessage(expectedLogCollectorKafkaMessage1, invocation);
//            invocation.callRealMethod();
//            kafkaTemplate.send(logCollectorTopic, responseSearchId1.toString(), expectedLogCollectorKafkaMessage1);
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Request)")));
//
//        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage2 = new LogCollectorKafkaMessage();
//        doAnswer(invocation -> {
//            setExpectedLogCollectorKafkaMessage(expectedLogCollectorKafkaMessage2, invocation);
//            invocation.callRealMethod();
//            kafkaTemplate.send(logCollectorTopic, responseSearchId2.toString(), expectedLogCollectorKafkaMessage2);
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Response)")));
//
//        //Mock starting LC search logs
//        SearchResult searchResult1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId1.toString(),
//                                configurationId1.toString(), null), SearchResult.class);
//        SearchResult searchResult2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId2.toString(),
//                                configurationId2.toString(), null), SearchResult.class);
//        ResponseEntity<SearchResultsDto> mockRegisterSearchResponseEntity1 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult1, SearchResultsDto.class),
//                        HttpStatus.OK);
//        ResponseEntity<SearchResultsDto> mockRegisterSearchResponseEntity2 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult2, SearchResultsDto.class),
//                        HttpStatus.OK);
//        doReturn(mockRegisterSearchResponseEntity1).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getRequestId()
//                                .equals(expectedLogCollectorKafkaMessage1.getRequestId())));
//        doReturn(mockRegisterSearchResponseEntity2).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getRequestId()
//                                .equals(expectedLogCollectorKafkaMessage2.getRequestId())));
//
//        //Mock getting LC search logs result
//        SearchResult searchResults1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorResultSearchResponseDefault(responseSearchId1.toString(),
//                                        configurationId1.toString(), null, SearchStatus.COMPLETED.name()),
//                        SearchResult.class);
//        SearchResult searchResults2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorResultSearchResponseDefault(responseSearchId2.toString(),
//                                        configurationId2.toString(), null, SearchStatus.COMPLETED.name()),
//                        SearchResult.class);
//        ResponseEntity<SearchResultsDto> mockGetSearchResultsResponseEntity1 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResults1, SearchResultsDto.class),
//                        HttpStatus.OK);
//        ResponseEntity<SearchResultsDto> mockGetSearchResultsResponseEntity2 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResults2, SearchResultsDto.class),
//                        HttpStatus.OK);
//        when(logCollectorFeignClient.getSearchResultsFast(eq(responseSearchId1)))
//                .thenReturn(mockGetSearchResultsResponseEntity1);
//        when(logCollectorFeignClient.getSearchResultsFast(eq(responseSearchId2)))
//                .thenReturn(mockGetSearchResultsResponseEntity2);
//        // Mock BV test run creation
//        TestRunCreationResponse bvTestRunCreationResponse =
//                objectMapper.readValue(ExecutorControllerTestConstants.BV_TEST_RUN_CREATION_RESPONSE_DEFAULT,
//                        TestRunCreationResponse.class);
//        when(bulkValidatorRepository.createTestRun(any(), any(), anyList()))
//                .thenReturn(bvTestRunCreationResponse);
//        // Mock BV compare response
//        List<ComparingProcessResponse> bvComparingProcessResponse =
//                Arrays.asList(objectMapper.readValue(ExecutorControllerTestConstants.BV_COMPARING_RESPONSE_DEFAULT,
//                        ComparingProcessResponse[].class));
//        when(bulkValidatorRepository.compare(any(), anyList()))
//                .thenReturn(bvComparingProcessResponse);
//        // Mock for expected Common PotSessionParameters after execution and validation processes
//        List<PotSessionParameter> expectedCommonParametersAfterProcessing =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.POT_SESSION_COMMON_PARAMETERS_DEFAULT,
//                                PotSessionParameter[].class));
//        expectedCommonParametersAfterProcessing.forEach(param -> {
//            param.setArValues(Collections.singletonList(mockedCommonParamValue));
//            param.setValidationInfo(new ValidationInfo(ValidationStatus.NONE));
//        });
//        // Mock for expected Integration Log parameter with validation before processing
//        PotSessionParameter expectedIntegrationLogsParameterWithValidation =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getIntegrationLogPotSessionParameterWithValidationBeforeProcessing(pageName,
//                                configurationId1.toString(), false), PotSessionParameter.class);
//        // Mock for expected Integration Log parameter without validation before processing
//        PotSessionParameter expectedIntegrationLogsParameterWithoutValidation =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getIntegrationLogPotSessionParameterWithoutValidationBeforeProcessing(pageName,
//                                        configurationId2.toString(), false),
//                        PotSessionParameter.class);
//        // Mock LogCollector response as AR value for Integration Log parameter
//        LogCollectorValueObject lcValueObject1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorValueObjectDefault(responseSearchId1.toString(),
//                                        configurationId1.toString(),
//                                        null, SearchStatus.COMPLETED.name()),
//                        LogCollectorValueObject.class);
//        expectedIntegrationLogsParameterWithValidation.setArValues(Collections.singletonList(lcValueObject1));
//        LogCollectorValueObject lcValueObject2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorValueObjectDefault(responseSearchId2.toString(),
//                                        configurationId2.toString(),
//                                        null, SearchStatus.COMPLETED.name()),
//                        LogCollectorValueObject.class);
//        expectedIntegrationLogsParameterWithoutValidation.setArValues(Collections.singletonList(lcValueObject2));
//        // Mock validation info for Integration Log parameter
//        IntegrationLogsValidationInfo lcParamValidationInfo =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .LOG_COLLECTOR_PARAMETER_VALIDATION_INFO_DEFAULT,
//                        IntegrationLogsValidationInfo.class);
//        expectedIntegrationLogsParameterWithValidation.setValidationInfo(lcParamValidationInfo);
//        expectedIntegrationLogsParameterWithoutValidation.setValidationInfo(new ValidationInfo(ValidationStatus
//                .NONE));
//
//        // Mock GetInfoRequest
//        Map<String, String> keyParameters = new HashMap<>();
//        keyParameters.put("Sales_Order_ID", "Test_Sales_Order_ID");
//        keyParameters.put("Customer_ID", "Test_Customer_ID");
//        GetInfoRequest getInfoRequest = GetInfoRequest.builder()
//                .environmentId(envId)
//                .keyParameters(keyParameters)
//                .shouldHighlightDiffs(true)
//                .shouldSendSessionResults(true)
//                .pagesName(Collections.singletonList(pageName))
//                .build();
//
//        // Mock Get ProjectConfig From Bd
//        getMockProjectConfigFromBd(fileProject, projectConfigService);
//
//        mockMvc.perform(post("/api/svp/project/{projectId}/executor/get-info", projectId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(getInfoRequest)))
//                .andExpect(status().isOk());
//
//        PotSession actualPotSession = potSessionService.findSessionById(sessionId.get());
//
//        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
//
//        verify(logCollectorEventKafkaListener, timeout(25000))
//                .listen(responseSearchId1.toString(), expectedLogCollectorKafkaMessage1);
//        verify(logCollectorEventKafkaListener, timeout(25000))
//                .listen(responseSearchId2.toString(), expectedLogCollectorKafkaMessage2);
//
//        verify(logCollectorRepository).getConfigurations(projectId);
//
//        Assert.assertEquals(expectedCommonParametersAfterProcessing, actualPotSession.getCommonParameters());
//        expectedCommonParametersAfterProcessing.forEach(expectedCommonParameter ->
//                verify(webSocketMessagingService, timeout(25000))
//                        .sendSutParameterResult(sessionId.get(), expectedCommonParameter));
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithValidation);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithoutValidation);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForTab(sessionId.get(), pageName, tabName, ValidationStatus.PASSED);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForPage(sessionId.get(), pageName, ValidationStatus.PASSED);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForSession(sessionId.get(), ValidationStatus.PASSED);
//    }
//
//    /**
//     * Tested method: getInfoF()
//     * Endpoint: /api/svp/project/{projectId}/executor/get-info
//     * <p>
//     * State:
//     * One Common Parameter,
//     * One Page
//     * Two {@link DisplayType#INTEGRATION_LOG} typed parameters:
//     * 1) validation status lC_WARNING cause response from LogCollector has error on SearchResult level
//     * 2) validation status NONE
//     * <p>
//     * Expected Behavior:
//     * One time send request to download LogCollectorConfigurations to LC for projectId
//     * All common parameters were added to session,
//     * All results of common and page parameters were added to session,
//     * Calculation of the validation statuses of tab, page and session was correct,
//     * Validation statuses were added to session and sent to websocket topic,
//     * Validation was not performed with BulkValidator.
//     */
//    @Test
//    public void getInfo_integrationLogParams_postProcessingCommonAndSutParametersWasWarning() throws Exception {
//        UUID responseSearchId1 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f77");
//        UUID responseSearchId2 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f78");
//        UUID configurationId1 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b8");
//        UUID configurationId2 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b9");
//        String lcErrorCode = "\"LC-1051\"";
//        String expectedKafkaResponseKey1 = responseSearchId1.toString();
//        String expectedKafkaResponseKey2 = responseSearchId2.toString();
//        String pageName = "Integration";
//        String tabName = "OCS";
//        AtomicReference<UUID> sessionId = new AtomicReference<>();
//        // Set sessionId from startSession() method of PotSessionService
//        doAnswer(invocation -> {
//            UUID actualSessionId = (UUID) invocation.callRealMethod();
//            // Get real sessionId
//            sessionId.set(actualSessionId);
//            return actualSessionId;
//        }).when(potSessionService).startSession(any(), anyMap());
//        // Generate file with common parameters
//        String testFile = loadFileToString("src/test/resources/test_data/common-parameters/one_common_parameter.json");
//        writeToFile(commonParametersFilePath, testFile);
//        //Mock getting common parameters by Param display type
//        SimpleValueObject mockedCommonParamValue = new SimpleValueObject("Mocked_CommonParam_Value");
//        doReturn(mockedCommonParamValue).when(paramDisplayTypeService)
//                .getValueFromSource(any(), any());
//        //Mock getting LC config by project id
//        List<LogCollectorConfiguration> logCollectorConfiguration =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.LOG_COLLECTOR_CONFIGURATION_DEFAULT,
//                                LogCollectorConfiguration[].class));
//        ResponseEntity<List<ConfigurationDto>> logCollectorConfigurationDto =
//                new ResponseEntity<>(dtoConvertService.convertList(logCollectorConfiguration, ConfigurationDto.class)
//                        , HttpStatus.OK);
//        when(logCollectorConfigurationFeignClient.getConfigurationsByProjectId(eq(projectId))).thenReturn(logCollectorConfigurationDto);
//
//        // Extract real requestId when calling a method storeContextByLogCollectorSearchId() and set method
//        // kafkaTemplate.send()
//        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage1 = new LogCollectorKafkaMessage();
//        doAnswer(invocation -> {
//            setExpectedLogCollectorKafkaMessage(expectedLogCollectorKafkaMessage1, invocation);
//            invocation.callRealMethod();
//            kafkaTemplate.send(logCollectorTopic, responseSearchId1.toString(), expectedLogCollectorKafkaMessage1);
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Request)")));
//
//        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage2 = new LogCollectorKafkaMessage();
//        doAnswer(invocation -> {
//            setExpectedLogCollectorKafkaMessage(expectedLogCollectorKafkaMessage2, invocation);
//            invocation.callRealMethod();
//            kafkaTemplate.send(logCollectorTopic, responseSearchId2.toString(), expectedLogCollectorKafkaMessage2);
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Response)")));
//
//        //Mock starting LC search logs
//        SearchResult searchResult1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId1.toString(),
//                                configurationId1.toString(), null), SearchResult.class);
//        SearchResult searchResult2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId2.toString(),
//                                configurationId2.toString(), null), SearchResult.class);
//        ResponseEntity<SearchResultsDto> mockRegisterSearchResponseEntity1 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult1, SearchResultsDto.class),
//                        HttpStatus.OK);
//        ResponseEntity<SearchResultsDto> mockRegisterSearchResponseEntity2 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult2, SearchResultsDto.class),
//                        HttpStatus.OK);
//        doReturn(mockRegisterSearchResponseEntity1).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getConfigurations().contains(configurationId1)));
//        doReturn(mockRegisterSearchResponseEntity2).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getConfigurations().contains(configurationId2)));
//        //Mock getting LC search logs result
//        SearchResult searchResults1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorResultSearchResponseDefault(responseSearchId1.toString(),
//                                        configurationId1.toString(), lcErrorCode, SearchStatus.FAILED.name()),
//                        SearchResult.class);
//        SearchResult searchResults2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorResultSearchResponseDefault(responseSearchId2.toString(),
//                                        configurationId2.toString(), null, SearchStatus.COMPLETED.name()),
//                        SearchResult.class);
//        ResponseEntity<SearchResultsDto> mockGetSearchResultsResponseEntity1 =
//                new ResponseEntity<SearchResultsDto>(dtoConvertService.convert(searchResults1, SearchResultsDto.class),
//                        HttpStatus.OK);
//        ResponseEntity<SearchResultsDto> mockGetSearchResultsResponseEntity2 =
//                new ResponseEntity<SearchResultsDto>(dtoConvertService.convert(searchResults2, SearchResultsDto.class),
//                        HttpStatus.OK);
//        when(logCollectorFeignClient.getSearchResultsFast(eq(responseSearchId1)))
//                .thenReturn(mockGetSearchResultsResponseEntity1);
//        when(logCollectorFeignClient.getSearchResultsFast(eq(responseSearchId2)))
//                .thenReturn(mockGetSearchResultsResponseEntity2);
//        // Mock for expected Common PotSessionParameters after execution and validation processes
//        List<PotSessionParameter> expectedCommonParametersAfterProcessing =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.POT_SESSION_COMMON_PARAMETERS_DEFAULT,
//                                PotSessionParameter[].class));
//        expectedCommonParametersAfterProcessing.forEach(param -> {
//            param.setArValues(Collections.singletonList(mockedCommonParamValue));
//            param.setValidationInfo(new ValidationInfo(ValidationStatus.NONE));
//        });
//        // Mock for expected Integration Log parameter with validation before processing
//        PotSessionParameter expectedIntegrationLogsParameterWithValidation =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getIntegrationLogPotSessionParameterWithValidationBeforeProcessing(pageName,
//                                configurationId1.toString(), false), PotSessionParameter.class);
//        // Mock for expected Integration Log parameter without validation before processing
//        PotSessionParameter expectedIntegrationLogsParameterWithoutValidation =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getIntegrationLogPotSessionParameterWithoutValidationBeforeProcessing(pageName,
//                                        configurationId2.toString(), false),
//                        PotSessionParameter.class);
//        // Mock LogCollector response as AR value for Integration Log parameter
//        LogCollectorValueObject lcValueObject1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorValueObjectDefault(responseSearchId1.toString(),
//                                        configurationId1.toString(),
//                                        lcErrorCode, SearchStatus.FAILED.name()),
//                        LogCollectorValueObject.class);
//        expectedIntegrationLogsParameterWithValidation.setArValues(Collections.singletonList(lcValueObject1));
//        LogCollectorValueObject lcValueObject2 = objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorValueObjectDefault(responseSearchId2.toString(), configurationId2.toString(),
//                                null, SearchStatus.COMPLETED.name()),
//                LogCollectorValueObject.class);
//        expectedIntegrationLogsParameterWithoutValidation.setArValues(Collections.singletonList(lcValueObject2));
//        // Mock validation info for Integration Log parameter
//        expectedIntegrationLogsParameterWithValidation
//                .setValidationInfo(new ValidationInfo(ValidationStatus.LC_WARNING));
//        expectedIntegrationLogsParameterWithoutValidation
//                .setValidationInfo(new ValidationInfo(ValidationStatus.NONE));
//
//        // Mock GetInfoRequest
//        Map<String, String> keyParameters = new HashMap<>();
//        keyParameters.put("Sales_Order_ID", "Test_Sales_Order_ID");
//        keyParameters.put("Customer_ID", "Test_Customer_ID");
//        GetInfoRequest getInfoRequest = GetInfoRequest.builder()
//                .environmentId(envId)
//                .keyParameters(keyParameters)
//                .shouldHighlightDiffs(true)
//                .shouldSendSessionResults(true)
//                .pagesName(Collections.singletonList(pageName))
//                .build();
//        getMockProjectConfigFromBd(fileProject, projectConfigService);
//
//        mockMvc.perform(post("/api/svp/project/{projectId}/executor/get-info", projectId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(getInfoRequest)))
//                .andExpect(status().isOk());
//
//        PotSession actualPotSession = potSessionService.findSessionById(sessionId.get());
//
//        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
//
//        verify(logCollectorEventKafkaListener, timeout(25000))
//                .listen(expectedKafkaResponseKey1, expectedLogCollectorKafkaMessage1);
//        verify(logCollectorEventKafkaListener, timeout(25000))
//                .listen(expectedKafkaResponseKey2, expectedLogCollectorKafkaMessage2);
//        verify(bulkValidatorRepository, never()).compare(any(), anyList());
//        verify(bulkValidatorRepository, never()).createTestRun(any(), any(), anyList());
//
//        verify(logCollectorRepository).getConfigurations(projectId);
//
//        Assert.assertEquals(expectedCommonParametersAfterProcessing, actualPotSession.getCommonParameters());
//        expectedCommonParametersAfterProcessing.forEach(expectedCommonParameter ->
//                verify(webSocketMessagingService, timeout(25000))
//                        .sendSutParameterResult(sessionId.get(), expectedCommonParameter));
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithValidation);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithoutValidation);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForTab(sessionId.get(), pageName, tabName, ValidationStatus.WARNING);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForPage(sessionId.get(), pageName, ValidationStatus.WARNING);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForSession(sessionId.get(), ValidationStatus.FAILED);
//    }
//
//    /**
//     * Tested method: getInfoF()
//     * Endpoint: /api/svp/project/{projectId}/executor/get-info
//     * <p>
//     * State:
//     * One Common Parameter,
//     * One Page
//     * Two {@link DisplayType#INTEGRATION_LOG} typed parameters:
//     * 1) validation status lC_WARNING cause response from LogCollector has error on SearchResult level and LC not
//     * send massage to Kafka
//     * 2) validation status NONE
//     * <p>
//     * Expected Behavior:
//     * One time send request to download LogCollectorConfigurations to LC for projectId
//     * All common parameters were added to session,
//     * All results of common and page parameters were added to session,
//     * Calculation of the validation statuses of tab, page and session was correct,
//     * Validation statuses were added to session and sent to websocket topic,
//     * Validation was not performed with BulkValidator.
//     */
//    @Test
//    public void getInfo_integrationLogParams_postProcessingCommonAndSutParametersOneParamHasErrorInRegisterSearchWasWarning() throws Exception {
//        UUID responseSearchId1 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f77");
//        UUID responseSearchId2 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f78");
//        UUID configurationId1 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b8");
//        UUID configurationId2 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b9");
//        String lcErrorCode = "\"LC-1051\"";
//        String expectedKafkaResponseKey2 = responseSearchId2.toString();
//        String pageName = "Integration";
//        String tabName = "OCS";
//        AtomicReference<UUID> sessionId = new AtomicReference<>();
//        // Set sessionId from startSession() method of PotSessionService
//        doAnswer(invocation -> {
//            UUID actualSessionId = (UUID) invocation.callRealMethod();
//            // Get real sessionId
//            sessionId.set(actualSessionId);
//            return actualSessionId;
//        }).when(potSessionService).startSession(any(), anyMap());
//        // Generate file with common parameters
//        String testFile = loadFileToString("src/test/resources/test_data/common-parameters/one_common_parameter.json");
//        writeToFile(commonParametersFilePath, testFile);
//        //Mock getting common parameters by Param display type
//        SimpleValueObject mockedCommonParamValue = new SimpleValueObject("Mocked_CommonParam_Value");
//        doReturn(mockedCommonParamValue).when(paramDisplayTypeService)
//                .getValueFromSource(any(), any());
//        //Mock getting LC config by project id
//        List<LogCollectorConfiguration> logCollectorConfiguration =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.LOG_COLLECTOR_CONFIGURATION_DEFAULT,
//                                LogCollectorConfiguration[].class));
//        ResponseEntity<List<ConfigurationDto>> logCollectorConfigurationDto =
//                new ResponseEntity<>(dtoConvertService.convertList(logCollectorConfiguration, ConfigurationDto.class)
//                        , HttpStatus.OK);
//        when(logCollectorConfigurationFeignClient.getConfigurationsByProjectId(eq(projectId))).thenReturn(logCollectorConfigurationDto);
//
//        // Extract real requestId when calling a method storeContextByLogCollectorSearchId() and set method
//        // kafkaTemplate.send()
//        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage2 = new LogCollectorKafkaMessage();
//        doAnswer(invocation -> {
//            setExpectedLogCollectorKafkaMessage(expectedLogCollectorKafkaMessage2, invocation);
//            invocation.callRealMethod();
//            kafkaTemplate.send(logCollectorTopic, responseSearchId2.toString(), expectedLogCollectorKafkaMessage2);
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Response)")));
//
//        //Mock starting LC search logs
//        SearchResult searchResult1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId1.toString(),
//                                configurationId1.toString(), lcErrorCode), SearchResult.class);
//        searchResult1.setStatus(SearchStatus.FAILED);
//        SearchResult searchResult2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId2.toString(),
//                                configurationId2.toString(), null), SearchResult.class);
//
//        ResponseEntity<SearchResultsDto> mockRegisterSearchResponseEntity1 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult1, SearchResultsDto.class),
//                        HttpStatus.OK);
//        ResponseEntity<SearchResultsDto> mockRegisterSearchResponseEntity2 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult2, SearchResultsDto.class),
//                        HttpStatus.OK);
//
//        doReturn(mockRegisterSearchResponseEntity1).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getConfigurations().contains(configurationId1)));
//        doReturn(mockRegisterSearchResponseEntity2).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getConfigurations().contains(configurationId2)));
//        //Mock getting LC search logs result
//        SearchResult searchResults2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorResultSearchResponseDefault(responseSearchId2.toString(),
//                                        configurationId2.toString(), null, SearchStatus.COMPLETED.name()),
//                        SearchResult.class);
//        ResponseEntity<SearchResultsDto> mockGetSearchResultsResponseEntity2 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResults2, SearchResultsDto.class),
//                        HttpStatus.OK);
//        when(logCollectorFeignClient.getSearchResultsFast(eq(responseSearchId2)))
//                .thenReturn(mockGetSearchResultsResponseEntity2);
//        // Mock for expected Common PotSessionParameters after execution and validation processes
//        List<PotSessionParameter> expectedCommonParametersAfterProcessing =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.POT_SESSION_COMMON_PARAMETERS_DEFAULT,
//                                PotSessionParameter[].class));
//        expectedCommonParametersAfterProcessing.forEach(param -> {
//            param.setArValues(Collections.singletonList(mockedCommonParamValue));
//            param.setValidationInfo(new ValidationInfo(ValidationStatus.NONE));
//        });
//        // Mock for expected Integration Log parameter with validation before processing
//
//        PotSessionParameter expectedIntegrationLogsParameterWithError =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getIntegrationLogPotSessionParameterWithValidationBeforeProcessing(pageName,
//                                configurationId1.toString(), false), PotSessionParameter.class);
//        // Mock for expected Integration Log parameter without validation before processing
//        PotSessionParameter expectedIntegrationLogsParameterWithoutError =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getIntegrationLogPotSessionParameterWithoutValidationBeforeProcessing(pageName,
//                                        configurationId2.toString(), false),
//                        PotSessionParameter.class);
//        // Mock LogCollector response as AR value for Integration Log parameter
//        LogCollectorValueObject lcValueObject1 = new LogCollectorValueObject();
//        lcValueObject1.setSearchResult(searchResult1);
//        lcValueObject1.setHasResults(false);
//        lcValueObject1.getSearchResult().setStatus(SearchStatus.FAILED);
//        expectedIntegrationLogsParameterWithError.setArValues(Collections.singletonList(lcValueObject1));
//        LogCollectorValueObject lcValueObject2 = objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorValueObjectDefault(responseSearchId2.toString(), configurationId2.toString(),
//                                null, SearchStatus.COMPLETED.name()),
//                LogCollectorValueObject.class);
//        expectedIntegrationLogsParameterWithoutError.setArValues(Collections.singletonList(lcValueObject2));
//        // Mock validation info for Integration Log parameter
//        expectedIntegrationLogsParameterWithError
//                .setValidationInfo(new ValidationInfo(ValidationStatus.LC_WARNING));
//        expectedIntegrationLogsParameterWithoutError
//                .setValidationInfo(new ValidationInfo(ValidationStatus.NONE));
//
//        // Mock GetInfoRequest
//        Map<String, String> keyParameters = new HashMap<>();
//        keyParameters.put("Sales_Order_ID", "Test_Sales_Order_ID");
//        keyParameters.put("Customer_ID", "Test_Customer_ID");
//        GetInfoRequest getInfoRequest = GetInfoRequest.builder()
//                .environmentId(envId)
//                .keyParameters(keyParameters)
//                .shouldHighlightDiffs(true)
//                .shouldSendSessionResults(true)
//                .pagesName(Collections.singletonList(pageName))
//                .build();
//
//        // Mock Get ProjectConfig From Bd
//        getMockProjectConfigFromBd(fileProject, projectConfigService);
//
//        mockMvc.perform(post("/api/svp/project/{projectId}/executor/get-info", projectId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(getInfoRequest)))
//                .andExpect(status().isOk());
//
//        PotSession actualPotSession = potSessionService.findSessionById(sessionId.get());
//
//        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
//
//        verify(logCollectorEventKafkaListener, timeout(25000))
//                .listen(expectedKafkaResponseKey2, expectedLogCollectorKafkaMessage2);
//        verify(bulkValidatorRepository, never()).compare(any(), anyList());
//        verify(bulkValidatorRepository, never()).createTestRun(any(), any(), anyList());
//
//        verify(logCollectorRepository).getConfigurations(projectId);
//
//        Assert.assertEquals(expectedCommonParametersAfterProcessing, actualPotSession.getCommonParameters());
//        expectedCommonParametersAfterProcessing.forEach(expectedCommonParameter ->
//                verify(webSocketMessagingService, timeout(25000))
//                        .sendSutParameterResult(sessionId.get(), expectedCommonParameter));
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithError);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithoutError);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForTab(sessionId.get(), pageName, tabName, ValidationStatus.WARNING);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForPage(sessionId.get(), pageName, ValidationStatus.WARNING);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForSession(sessionId.get(), ValidationStatus.FAILED);
//    }
//
//    /**
//     * Tested method: getInfo()
//     * Endpoint: /api/svp/project/{projectId}/executor/get-info
//     * <p>
//     * State:
//     * One Common Parameter,
//     * One Page
//     * Two {@link DisplayType#INTEGRATION_LOG} typed parameters:
//     * 1) validation status PASSED
//     * 2) validation status NONE cause response from LogCollector has no logs
//     * <p>
//     * Expected Behavior:
//     * One time send request to download LogCollectorConfigurations to LC for projectId
//     * All common parameters were added to session,
//     * All results of common and page parameters were added to session,
//     * Calculation of the validation statuses of tab, page and session was correct,
//     * Validation statuses were added to session and sent to websocket topic,
//     * Validation was not performed with BulkValidator.
//     */
//    @Test
//    public void getInfo_integrationLogParams_postProcessingCommonAndSutParametersWithoutLogs() throws Exception {
//        UUID responseSearchId1 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f77");
//        UUID responseSearchId2 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f78");
//        UUID configurationId1 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b8");
//        UUID configurationId2 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b9");
//        String lcErrorCode = "\"LC-1031\"";
//        String expectedKafkaResponseKey1 = responseSearchId1.toString();
//        String expectedKafkaResponseKey2 = responseSearchId2.toString();
//        String pageName = "Integration";
//        String tabName = "OCS";
//        AtomicReference<UUID> sessionId = new AtomicReference<>();
//        // Set sessionId from startSession() method of PotSessionService
//        doAnswer(invocation -> {
//            UUID actualSessionId = (UUID) invocation.callRealMethod();
//            // Get real sessionId
//            sessionId.set(actualSessionId);
//            return actualSessionId;
//        }).when(potSessionService).startSession(any(), anyMap());
//        // Generate file with common parameters
//        String testFile = loadFileToString("src/test/resources/test_data/common-parameters/one_common_parameter.json");
//        writeToFile(commonParametersFilePath, testFile);
//        //Mock getting common parameters by Param display type
//        SimpleValueObject mockedCommonParamValue = new SimpleValueObject("Mocked_CommonParam_Value");
//        doReturn(mockedCommonParamValue).when(paramDisplayTypeService)
//                .getValueFromSource(any(), any());
//        //Mock getting LC config by project id
//        List<LogCollectorConfiguration> logCollectorConfiguration =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.LOG_COLLECTOR_CONFIGURATION_DEFAULT,
//                                LogCollectorConfiguration[].class));
//        ResponseEntity<List<ConfigurationDto>> logCollectorConfigurationDto =
//                new ResponseEntity<>(dtoConvertService.convertList(logCollectorConfiguration, ConfigurationDto.class)
//                        , HttpStatus.OK);
//        when(logCollectorConfigurationFeignClient.getConfigurationsByProjectId(eq(projectId))).thenReturn(logCollectorConfigurationDto);
//
//        // Extract real requestId when calling a method storeContextByLogCollectorSearchId() and set method
//        // kafkaTemplate.send()
//        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage1 = new LogCollectorKafkaMessage();
//        doAnswer(invocation -> {
//            setExpectedLogCollectorKafkaMessage(expectedLogCollectorKafkaMessage1, invocation);
//            invocation.callRealMethod();
//            kafkaTemplate.send(logCollectorTopic, responseSearchId1.toString(), expectedLogCollectorKafkaMessage1);
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Request)")));
//
//        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage2 = new LogCollectorKafkaMessage();
//        doAnswer(invocation -> {
//            setExpectedLogCollectorKafkaMessage(expectedLogCollectorKafkaMessage2, invocation);
//            invocation.callRealMethod();
//            kafkaTemplate.send(logCollectorTopic, responseSearchId2.toString(), expectedLogCollectorKafkaMessage2);
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Response)")));
//
//        //Mock starting LC search logs
//        SearchResult searchResult1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId1.toString(),
//                                configurationId1.toString(), null), SearchResult.class);
//        SearchResult searchResult2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId2.toString(),
//                                configurationId2.toString(), null), SearchResult.class);
//        ResponseEntity<SearchResultsDto> mockRegisterSearchResponseEntity1 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult1, SearchResultsDto.class),
//                        HttpStatus.OK);
//        ResponseEntity<SearchResultsDto> mockRegisterSearchResponseEntity2 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult2, SearchResultsDto.class),
//                        HttpStatus.OK);
//        doReturn(mockRegisterSearchResponseEntity1).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getConfigurations().contains(configurationId1)));
//        doReturn(mockRegisterSearchResponseEntity2).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getConfigurations().contains(configurationId2)));
//        //Mock getting LC search logs result
//        SearchResult searchResults1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorResultSearchResponseWithoutLogs(responseSearchId1.toString(),
//                                        configurationId1.toString(), lcErrorCode, SearchStatus.FAILED.name()),
//                        SearchResult.class);
//        SearchResult searchResults2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorResultSearchResponseDefault(responseSearchId2.toString(),
//                                        configurationId2.toString(), null, SearchStatus.COMPLETED.name()),
//                        SearchResult.class);
//        ResponseEntity<SearchResultsDto> mockGetSearchResultsResponseEntity1 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResults1, SearchResultsDto.class),
//                        HttpStatus.OK);
//        ResponseEntity<SearchResultsDto> mockGetSearchResultsResponseEntity2 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResults2, SearchResultsDto.class),
//                        HttpStatus.OK);
//        when(logCollectorFeignClient.getSearchResultsFast(eq(responseSearchId1)))
//                .thenReturn(mockGetSearchResultsResponseEntity1);
//        when(logCollectorFeignClient.getSearchResultsFast(eq(responseSearchId2)))
//                .thenReturn(mockGetSearchResultsResponseEntity2);
//        // Mock for expected Common PotSessionParameters after execution and validation processes
//        List<PotSessionParameter> expectedCommonParametersAfterProcessing =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.POT_SESSION_COMMON_PARAMETERS_DEFAULT,
//                                PotSessionParameter[].class));
//        expectedCommonParametersAfterProcessing.forEach(param -> {
//            param.setArValues(Collections.singletonList(mockedCommonParamValue));
//            param.setValidationInfo(new ValidationInfo(ValidationStatus.NONE));
//        });
//        // Mock for expected Integration Log parameter with validation before processing
//        PotSessionParameter expectedIntegrationLogsParameterWithValidation =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getIntegrationLogPotSessionParameterWithValidationBeforeProcessing(pageName,
//                                configurationId1.toString(), false), PotSessionParameter.class);
//        // Mock for expected Integration Log parameter without validation before processing
//        PotSessionParameter expectedIntegrationLogsParameterWithoutValidation =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getIntegrationLogPotSessionParameterWithoutValidationBeforeProcessing(pageName,
//                                        configurationId2.toString(), false),
//                        PotSessionParameter.class);
//        // Mock LogCollector response as AR value for Integration Log parameter
//        LogCollectorValueObject lcValueObject1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorValueObjectWithoutLogs(responseSearchId1.toString(),
//                                        configurationId1.toString(),
//                                        lcErrorCode, SearchStatus.FAILED.name()),
//                        LogCollectorValueObject.class);
//        expectedIntegrationLogsParameterWithValidation.setArValues(Collections.singletonList(lcValueObject1));
//        LogCollectorValueObject lcValueObject2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorValueObjectDefault(responseSearchId2.toString(),
//                                        configurationId2.toString(),
//                                        null, SearchStatus.COMPLETED.name()),
//                        LogCollectorValueObject.class);
//        expectedIntegrationLogsParameterWithoutValidation.setArValues(Collections.singletonList(lcValueObject2));
//        // Mock validation info for Integration Log parameter
//        expectedIntegrationLogsParameterWithValidation
//                .setValidationInfo(new ValidationInfo(ValidationStatus.NONE));
//        expectedIntegrationLogsParameterWithoutValidation
//                .setValidationInfo(new ValidationInfo(ValidationStatus.NONE));
//        // Mock GetInfoRequest
//        Map<String, String> keyParameters = new HashMap<>();
//        keyParameters.put("Sales_Order_ID", "Test_Sales_Order_ID");
//        keyParameters.put("Customer_ID", "Test_Customer_ID");
//        GetInfoRequest getInfoRequest = GetInfoRequest.builder()
//                .environmentId(envId)
//                .keyParameters(keyParameters)
//                .shouldHighlightDiffs(true)
//                .shouldSendSessionResults(true)
//                .pagesName(Collections.singletonList(pageName))
//                .build();
//
//        // Mock Get ProjectConfig From Bd
//        getMockProjectConfigFromBd(fileProject, projectConfigService);
//
//        mockMvc.perform(post("/api/svp/project/{projectId}/executor/get-info", projectId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(getInfoRequest)))
//                .andExpect(status().isOk());
//
//        PotSession actualPotSession = potSessionService.findSessionById(sessionId.get());
//
//        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
//
//        verify(logCollectorEventKafkaListener, timeout(25000))
//                .listen(expectedKafkaResponseKey1, expectedLogCollectorKafkaMessage1);
//        verify(logCollectorEventKafkaListener, timeout(25000))
//                .listen(expectedKafkaResponseKey2, expectedLogCollectorKafkaMessage2);
//        verify(bulkValidatorRepository, never()).compare(any(), anyList());
//        verify(bulkValidatorRepository, never()).createTestRun(any(), any(), anyList());
//
//        verify(logCollectorRepository).getConfigurations(projectId);
//
//        Assert.assertEquals(expectedCommonParametersAfterProcessing, actualPotSession.getCommonParameters());
//        expectedCommonParametersAfterProcessing.forEach(expectedCommonParameter ->
//                verify(webSocketMessagingService, timeout(25000))
//                        .sendSutParameterResult(sessionId.get(), expectedCommonParameter));
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithValidation);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendSutParameterResult(sessionId.get(), expectedIntegrationLogsParameterWithoutValidation);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForTab(sessionId.get(), pageName, tabName, ValidationStatus.NONE);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForPage(sessionId.get(), pageName, ValidationStatus.NONE);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForSession(sessionId.get(), ValidationStatus.PASSED);
//    }
//
//    /**
//     * Tested method: getInfo()
//     * Endpoint: /api/svp/project/{projectId}/executor/get-info
//     * <p>
//     * State:
//     * Lot Common Parameters (>1),
//     * Two Pages with different parameters
//     * <p>
//     * Expected Behavior:
//     * All common parameters were added to session,
//     * All results of page parameters were added to session,
//     * Calculation of the validation statuses of tabs, pages and session was correct,
//     * Validation statuses were added to session and sent to websocket topic.
//     */
//    @Test
//    public void getInfo_twoPagesSelected_postProcessingCommonAndSutParametersWasCorrect() throws Exception {
//        UUID responseSearchId1 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f77");
//        UUID responseSearchId2 = UUID.fromString("ef7e2569-d226-411f-b91f-d510721d2f78");
//        UUID configurationId1 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b8");
//        UUID configurationId2 = UUID.fromString("24d1ea9f-45b3-4256-9a2d-26372ad134b9");
//        List<String> pagesName = Arrays.asList("Integration", "CustomerInfo");
//        AtomicReference<UUID> sessionId = new AtomicReference<>();
//
//        // Set sessionId from startSession() method of PotSessionService
//        doAnswer(invocation -> {
//            UUID actualSessionId = (UUID) invocation.callRealMethod();
//            // Get real sessionId
//            sessionId.set(actualSessionId);
//            return actualSessionId;
//        }).when(potSessionService).startSession(any(), anyMap());
//
//        // Generate file with common parameters
//        String testFile = loadFileToString("src/test/resources/test_data/common-parameters/common_parameters.json");
//        writeToFile(commonParametersFilePath, testFile);
//
//        //Mock getting parameters by Param display type
//        SimpleValueObject mockedParamValue = new SimpleValueObject("Mocked_Param_Value");
//        doReturn(mockedParamValue).when(paramDisplayTypeService)
//                .getValueFromSource(any(), any());
//        //Mock getting parameters by Link display type
//        SimpleValueObject mockedLinkValue = new SimpleValueObject("Mocked_Link_Value");
//        doReturn(mockedLinkValue).when(linkDisplayTypeService)
//                .getValueFromSource(any(), any());
//
//        //Mock getting LC config by project id
//        List<LogCollectorConfiguration> logCollectorConfiguration =
//                Arrays.asList(objectMapper
//                        .readValue(ExecutorControllerTestConstants.LOG_COLLECTOR_CONFIGURATION_DEFAULT,
//                                LogCollectorConfiguration[].class));
//        ResponseEntity<List<ConfigurationDto>> logCollectorConfigurationDto =
//                new ResponseEntity<>(dtoConvertService.convertList(logCollectorConfiguration, ConfigurationDto.class)
//                        , HttpStatus.OK);
//        when(logCollectorConfigurationFeignClient.getConfigurationsByProjectId(eq(projectId))).thenReturn(logCollectorConfigurationDto);
//
//        // Extract real requestId when calling a method storeContextByLogCollectorSearchId() and set method
//        // kafkaTemplate.send()
//        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage1 = new LogCollectorKafkaMessage();
//        doAnswer(invocation -> {
//            setExpectedLogCollectorKafkaMessage(expectedLogCollectorKafkaMessage1, invocation);
//            invocation.callRealMethod();
//            kafkaTemplate.send(logCollectorTopic, responseSearchId1.toString(), expectedLogCollectorKafkaMessage1);
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Request)")));
//
//        LogCollectorKafkaMessage expectedLogCollectorKafkaMessage2 = new LogCollectorKafkaMessage();
//        doAnswer(invocation -> {
//            setExpectedLogCollectorKafkaMessage(expectedLogCollectorKafkaMessage2, invocation);
//            invocation.callRealMethod();
//            kafkaTemplate.send(logCollectorTopic, responseSearchId2.toString(), expectedLogCollectorKafkaMessage2);
//            return null;
//        }).when(deferredSearchService).storeContextByRequestSearchId(any(),
//                Mockito.argThat(context -> context.getParameter().getParameterConfig().getName().
//                        equals("[OCS] Modify Add-On Subscription (Response)")));
//
//        //Mock starting LC search logs
//        SearchResult searchResult1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId1.toString(),
//                                configurationId1.toString(), null), SearchResult.class);
//        SearchResult searchResult2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                        .getLogCollectorRegisterSearchResponseDefault(responseSearchId2.toString(),
//                                configurationId2.toString(), null), SearchResult.class);
//        ResponseEntity<SearchResultsDto> mockRegisterSearchResponseEntity1 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult1, SearchResultsDto.class),
//                        HttpStatus.OK);
//        ResponseEntity<SearchResultsDto> mockRegisterSearchResponseEntity2 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResult2, SearchResultsDto.class),
//                        HttpStatus.OK);
//        doReturn(mockRegisterSearchResponseEntity1).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getConfigurations().contains(configurationId1)));
//        doReturn(mockRegisterSearchResponseEntity2).when(logCollectorFeignClient).registerSearch(
//                argThat(logCollectorSearchRequest ->
//                        logCollectorSearchRequest.getConfigurations().contains(configurationId2)));
//
//        //Mock getting LC search logs result
//        SearchResult searchResults1 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorResultSearchResponseDefault(responseSearchId1.toString(),
//                                        configurationId1.toString(), null, SearchStatus.COMPLETED.name()),
//                        SearchResult.class);
//        SearchResult searchResults2 =
//                objectMapper.readValue(ExecutorControllerTestConstants
//                                .getLogCollectorResultSearchResponseDefault(responseSearchId2.toString(),
//                                        configurationId2.toString(), null, SearchStatus.COMPLETED.name()),
//                        SearchResult.class);
//        ResponseEntity<SearchResultsDto> mockGetSearchResultsResponseEntity1 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResults1, SearchResultsDto.class),
//                        HttpStatus.OK);
//        ResponseEntity<SearchResultsDto> mockGetSearchResultsResponseEntity2 =
//                new ResponseEntity<>(dtoConvertService.convert(searchResults2, SearchResultsDto.class),
//                        HttpStatus.OK);
//        when(logCollectorFeignClient.getSearchResultsFast(eq(responseSearchId1)))
//                .thenReturn(mockGetSearchResultsResponseEntity1);
//        when(logCollectorFeignClient.getSearchResultsFast(eq(responseSearchId2)))
//                .thenReturn(mockGetSearchResultsResponseEntity2);
//
//        // Mock GetInfoRequest
//        Map<String, String> keyParameters = new HashMap<>();
//        keyParameters.put("Sales_Order_ID", "Test_Sales_Order_ID");
//        keyParameters.put("Customer_ID", "Test_Customer_ID");
//        GetInfoRequest getInfoRequest = GetInfoRequest.builder()
//                .environmentId(envId)
//                .keyParameters(keyParameters)
//                .shouldHighlightDiffs(true)
//                .shouldSendSessionResults(true)
//                .pagesName(pagesName)
//                .build();
//
//        // Mock BV test run creation
//        TestRunCreationResponse bvTestRunCreationResponse =
//                objectMapper.readValue(ExecutorControllerTestConstants.BV_TEST_RUN_CREATION_RESPONSE_DEFAULT,
//                        TestRunCreationResponse.class);
//        when(bulkValidatorRepository.createTestRun(any(), any(), anyList()))
//                .thenReturn(bvTestRunCreationResponse);
//
//        // Mock BV compare response
//        List<ComparingProcessResponse> bvComparingProcessResponse =
//                Arrays.asList(objectMapper.readValue(ExecutorControllerTestConstants.BV_COMPARING_RESPONSE_DEFAULT,
//                        ComparingProcessResponse[].class));
//        when(bulkValidatorRepository.compare(any(), anyList()))
//                .thenReturn(bvComparingProcessResponse);
//
//        // Mock Get ProjectConfig From Bd
//        getMockProjectConfigFromBd(fileProject, projectConfigService);
//
//        // Start get info process
//        mockMvc.perform(post("/api/svp/project/{projectId}/executor/get-info", projectId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(getInfoRequest)))
//                .andExpect(status().isOk());
//
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForTab(sessionId.get(), "Integration",
//                        "OCS", ValidationStatus.PASSED);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForTab(sessionId.get(), "Customer Info",
//                        "Sales Order", ValidationStatus.FAILED);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForTab(sessionId.get(), "Customer Info",
//                        "JSON/XML", ValidationStatus.WARNING);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForTab(sessionId.get(), "Customer Info",
//                        "Real Data", ValidationStatus.FAILED);
//
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForPage(sessionId.get(), "Integration", ValidationStatus.PASSED);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForPage(sessionId.get(), "Customer Info", ValidationStatus.FAILED);
//        verify(webSocketMessagingService, timeout(25000))
//                .sendValidationStatusForSession(sessionId.get(), ValidationStatus.FAILED);
//    }

    private void setExpectedLogCollectorKafkaMessage(LogCollectorKafkaMessage expectedLogCollectorKafkaMessage2,
                                                     InvocationOnMock invocation) {
        // Get real requestId
        UUID actualRequestId = invocation.getArgument(0);
        // Set payload for send to the kafka
        expectedLogCollectorKafkaMessage2.setRequestId(actualRequestId);
        expectedLogCollectorKafkaMessage2.setStatus(SearchStatus.COMPLETED.toString());
        expectedLogCollectorKafkaMessage2.setRequestTool("atp-svp");
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
