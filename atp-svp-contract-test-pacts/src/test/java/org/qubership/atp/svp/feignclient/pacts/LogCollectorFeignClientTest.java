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

package org.qubership.atp.svp.feignclient.pacts;

import static java.util.Arrays.asList;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.svp.clients.api.logcollector.dto.public_api.*;
import org.qubership.atp.svp.repo.feign.LogCollectorFeignClient;



@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {LogCollectorFeignClient.class})
@ContextConfiguration(classes = {LogCollectorFeignClientTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(
        properties = {"feign.atp.logcollector.name=atp-logcollector", "feign.atp.logcollector.route=",
                "feign.atp.logcollector.url=http://localhost:8888",
                "feign.httpclient.enabled=false"})
public class LogCollectorFeignClientTest {

    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("atp-logcollector", "localhost", 8888, this);
    @Autowired
    LogCollectorFeignClient logCollectorFeignClient;

    public static final String DATE_TIME_1 = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public static final String DATE_TIME_2 = "yyyy-MM-dd'T'HH:mm:ssXXX";


    @Test
    @PactVerification()
    public void allPass() {

        OffsetDateTime offsetDateTime =
                OffsetDateTime.parse("2000-01-31T17:00:00+04:00", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")) ;

        UUID searchId = UUID.fromString("61abd5ac-efbe-49cc-b7f5-925a7f543481");
        UUID configurationId = UUID.fromString("61abd5ac-efbe-49cc-b7f5-925a7f543482");

        ContextParameterDto contextParameterDtos = new ContextParameterDto();
        contextParameterDtos.setKey("key");
        contextParameterDtos.setType(ContextTypeDto.TEXT);
        contextParameterDtos.setValue("value");
        contextParameterDtos.setMiaType(MiaTypeDto.PROCESS);

        FilebasedParametersDto filebasedParametersDto = new FilebasedParametersDto();
        filebasedParametersDto.setSurroundingLinesQuantity(1);
        filebasedParametersDto.setSkipEndDate(false);
        filebasedParametersDto.setSearchStrategy("searchStrategy");
        filebasedParametersDto.setFileTrimmingStrategy("fileTrimmingStrategy");
        filebasedParametersDto.setCaseSensitive(false);
        filebasedParametersDto.isRegexp(false);
        filebasedParametersDto.setSearchPattern("searchPattern");
        filebasedParametersDto.setSurroundingMinutesQuantity(1);
        filebasedParametersDto.setSearchPerOneDay(false);
        filebasedParametersDto.setSurroundingMessagesBefore(1);
        filebasedParametersDto.setSurroundingMessagesAfter(1);
        filebasedParametersDto.setFilterOutNotMatched(false);

        GraylogParametersDto graylogParameters = new GraylogParametersDto();
        graylogParameters.isRegexp(false);
        graylogParameters.caseSensitive(false);
        graylogParameters.setSearchPatternQuery("searchPatternQuery");
        graylogParameters.setFilterText("filterText");
        graylogParameters.setFilterOutNotMatched(false);

        KuberParametersDto kuberParameters = new KuberParametersDto();
        kuberParameters.setSurroundingLinesQuantity(1);
        kuberParameters.skipEndDate(false);
        kuberParameters.setSearchStrategy("searchStrategy");
        kuberParameters.caseSensitive(false);
        kuberParameters.isRegexp(false);
        kuberParameters.setNamespace("namespace");
        kuberParameters.setSearchPattern("searchPattern");
        kuberParameters.setSurroundingMinutesQuantity(1);
        kuberParameters.setSearchPerOneDay(false);
        kuberParameters.setDeployment("deployment");
        kuberParameters.setSurroundingMessagesBefore(1);
        kuberParameters.setSurroundingMessagesAfter(1);
        kuberParameters.setFilterOutNotMatched(false);

        SearchParametersDto searchParametersDto = new SearchParametersDto();
        searchParametersDto.setStartDate(offsetDateTime);
        searchParametersDto.setEndDate(offsetDateTime);
        searchParametersDto.setSurroundingMessagesBefore(1);
        searchParametersDto.setSurroundingMessagesAfter(1);
        searchParametersDto.setFilebasedParameters(filebasedParametersDto);
        searchParametersDto.setGraylogParameters(graylogParameters);
        searchParametersDto.setTimeRange(1L);
        searchParametersDto.setKuberParameters(kuberParameters);
        searchParametersDto.setFileTrimmingStrategy("fileTrimmingStrategy");
        searchParametersDto.setSearchPatternType("searchPatternType");
        searchParametersDto.setSurroundingLinesQuantity(1);
        searchParametersDto.setSearchStrategy("searchStrategy");
        searchParametersDto.setFileTrimmingStrategy("fileTrimmingStrategy");
        searchParametersDto.isCaseSensitiveFilebased(false);
        searchParametersDto.isRegexpGraylog(false);
        searchParametersDto.setSearchPattern("searchPattern");
        searchParametersDto.setSurroundingMinutesQuantity(1);
        searchParametersDto.setSearchPerOneDay(false);
        searchParametersDto.setSearchPatternQuery("searchPatternQuery");
        searchParametersDto.setFilterText("filterText");
        searchParametersDto.setRangeType("ABSOLUTE");

        SearchRequestDto searchRequestDto = new SearchRequestDto();
        searchRequestDto.setConfigurations(asList(configurationId));
        searchRequestDto.setTemplates(asList(searchId));
        searchRequestDto.setParameters(searchParametersDto);
        searchRequestDto.setContext(asList(contextParameterDtos));
        searchRequestDto.setEnvironmentId(searchId);
        searchRequestDto.setRequestId(searchId);
        searchRequestDto.setProjectId(searchId);
        searchRequestDto.setRequestTool("requestTool");

        ResponseEntity<SearchResultsDto> registerSearch = logCollectorFeignClient.registerSearch(searchRequestDto);
        Assert.assertEquals(200, registerSearch.getStatusCode().value());
        Assert.assertTrue(registerSearch.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<SearchResultsDto> getSearchResults = logCollectorFeignClient.getSearchResultsFast(searchId);
        Assert.assertEquals(200, getSearchResults.getStatusCode().value());
        Assert.assertTrue(getSearchResults.getHeaders().get("Content-Type").contains("application/json"));

    }

    @Pact(consumer = "atp-svp")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        DslPart configurations = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.uuid("61abd5ac-efbe-49cc-b7f5-925a7f543483"));
        DslPart templates = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.uuid("61abd5ac-efbe-49cc-b7f5-925a7f543481"));

        DslPart graylogParameters = new PactDslJsonBody()
                .stringType("searchPatternQuery", "searchPatternQuery")
                .stringType("filterText", "filterText")
                .booleanType("isRegexp", false)
                .booleanType("caseSensitive", false)
                .booleanType("filterOutNotMatched", false)
                ;

        DslPart filebasedParameters = new PactDslJsonBody()
                .stringType("fileTrimmingStrategy", "fileTrimmingStrategy")
                .stringType("searchPattern", "searchPattern")
                .stringType("searchStrategy", "searchStrategy")
                .integerType("surroundingLinesQuantity", 1)
                .integerType("surroundingMinutesQuantity", 1)
                .integerType("surroundingMessagesBefore", 1)
                .integerType("surroundingMessagesAfter", 1)
                .booleanType("filterOutNotMatched", false)
                .booleanType("searchPerOneDay", false)
                .booleanType("caseSensitive", false)
                .booleanType("isRegexp", false)
                .booleanType("skipEndDate", false);

        DslPart kuberParameters = new PactDslJsonBody()
                .stringType("namespace", "namespace")
                .stringType("deployment", "deployment")
                .stringType("searchPattern", "searchPattern")
                .stringType("searchStrategy", "searchStrategy")
                .integerType("surroundingLinesQuantity", 1)
                .integerType("surroundingMinutesQuantity", 1)
                .integerType("surroundingMessagesBefore", 1)
                .integerType("surroundingMessagesAfter", 1)
                .booleanType("filterOutNotMatched", false)
                .booleanType("caseSensitive", false)
                .booleanType("isRegexp", false)
                .booleanType("skipEndDate", false)
                .booleanType("searchPerOneDay", false)
                ;

        DslPart searchParameters = new PactDslJsonBody()
                .stringType("fileTrimmingStrategy","fileTrimmingStrategy")
                .stringType("rangeType", "ABSOLUTE")
                .stringType("searchPattern", "searchPattern")
                .stringType("searchPatternType", "searchPatternType")
                .stringType("searchPatternQuery", "searchPatternQuery")
                .stringType("filterText", "filterText")
                .stringType("searchStrategy", "searchStrategy")
                .integerType("surroundingLinesQuantity", 1)
                .integerType("surroundingMinutesQuantity", 1)
                .integerType("timeRange", 1)
                .integerType("surroundingMessagesBefore", 1)
                .integerType("surroundingMessagesAfter", 1)
                .booleanType("searchPerOneDay", false)
                .booleanType("isRegexpGraylog", false)
                .booleanType("skipArchivation", false)
                .booleanType("isCaseSensitiveFilebased", false)
                .booleanType("isCaseSensitiveGraylog", false)
                .object("graylogParameters", graylogParameters)
                .object("filebasedParameters", filebasedParameters)
                .object("kuberParameters", kuberParameters)
                .datetime("startDate", DATE_TIME_1)
                .datetime("endDate", DATE_TIME_2)
                ;


        DslPart contextParameterDto = new PactDslJsonBody()
                .stringType("type", "TEXT")
                .stringType("key", "key")
                .stringType("value", "value")
                .stringType("miaType", "PROCESS")
                ;
        DslPart contextParametersDto = new PactDslJsonArray().template(contextParameterDto);



        DslPart logCollectorSearchRequest = new PactDslJsonBody()
        .uuid("environmentId", "61abd5ac-efbe-49cc-b7f5-925a7f543481")
        .uuid("projectId", "61abd5ac-efbe-49cc-b7f5-925a7f543481")
        .uuid("requestId", "61abd5ac-efbe-49cc-b7f5-925a7f543481")
        .stringType("requestTool", "requestTool")
        .object("context", contextParametersDto)
        .object("parameters", searchParameters)
        .object("templates", templates)
        .object("configurations", configurations);

        DslPart parameters1 = PactDslJsonArray.arrayEachLike()
                .eachKeyMappedToAnArrayLike("key")
                .stringType("value");

        DslPart stringList1 = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.stringType("61abd5ac-efbe-49cc-b7f5-925a7f543481"));

        DslPart parameters2 = PactDslJsonArray.arrayEachLike()
                .eachKeyMappedToAnArrayLike("key")
                .stringType("value");

        DslPart stringList2 = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.stringType("61abd5ac-efbe-49cc-b7f5-925a7f543481"));

        DslPart searchThreadFindResultDto1 = new PactDslJsonBody()
                .stringType("foundInDirectory")
                .stringType("logFileName")
                .stringType("logsLink")
                .stringType("timestamp")
                .eachLike("messages", stringList1)
                .object("parameters", parameters1);

        DslPart searchThreadFindResultDto2 = new PactDslJsonBody()
                .stringType("foundInDirectory")
                .stringType("logFileName")
                .stringType("logsLink")
                .stringType("timestamp")
                .eachLike("messages", stringList2)
                .object("parameters", parameters2);

        DslPart systemSearchResultsDto1 = new PactDslJsonBody()
                .stringType("errorCode")
                .stringType("errorDetails")
                .stringType("logsLink")
                .uuid("systemSearchId")
                .stringType("systemName")
                .stringType("startedAt")
                .stringType("finishedAt")
                .booleanType("searchPerOneDay")
                .integerType("findsFound")
                .stringType("status")
                .eachLike("searchThreadResult", searchThreadFindResultDto1);

        DslPart systemSearchResultsDto2 = new PactDslJsonBody()
                .stringType("errorCode")
                .stringType("errorDetails")
                .stringType("logsLink")
                .uuid("systemSearchId")
                .stringType("systemName")
                .stringType("startedAt")
                .stringType("finishedAt")
                .booleanType("searchPerOneDay")
                .integerType("findsFound")
                .stringType("status")
                .eachLike("searchThreadResult", searchThreadFindResultDto2);

        DslPart componentSearchResultsDto1 = new PactDslJsonBody()
                .stringType("errorCode")
                .stringType("errorDetails")
                .uuid("searchId")
                .stringType("logsLink")
                .uuid("componentSearchId")
                .uuid("componentId")
                .stringType("componentName")
                .stringType("componentType")
                .stringType("status")
                .stringType("startedAt")
                .stringType("finishedAt")
                .stringType("searchPatternType")
                .stringType("searchPattern")
                .booleanType("caseSensitive")
                .booleanType("skipEndDate")
                .stringType("filterText")
                .stringType("searchStrategy")
                .integerType("surroundingLinesQuantity")
                .integerType("surroundingMinutesQuantity")
                .stringType("fileTrimmingStrategy")
                .eachLike("systemSearchResults", systemSearchResultsDto1);

        DslPart componentSearchResultsDto2 = new PactDslJsonBody()
                .stringType("errorCode")
                .stringType("errorDetails")
                .uuid("searchId")
                .stringType("logsLink")
                .uuid("componentSearchId")
                .uuid("componentId")
                .stringType("componentName")
                .stringType("componentType")
                .stringType("status")
                .stringType("startedAt")
                .stringType("finishedAt")
                .stringType("searchPatternType")
                .stringType("searchPattern")
                .booleanType("caseSensitive")
                .booleanType("skipEndDate")
                .stringType("filterText")
                .stringType("searchStrategy")
                .integerType("surroundingLinesQuantity")
                .integerType("surroundingMinutesQuantity")
                .stringType("fileTrimmingStrategy")
                .eachLike("systemSearchResults", systemSearchResultsDto2);

        DslPart searchResponse1 = new PactDslJsonBody()
                .stringType("errorCode")
                .stringType("errorDetails")
                .stringType("logsLink")
                .stringType("startDate")
                .stringType("endDate")
                .stringType("startedAt")
                .stringType("finishedAt")
                .uuid("searchId")
                .uuid("projectId")
                .uuid("environmentId")
                .booleanType("skipArchivation")
                .stringType("status", "COMPLETED")
                .eachLike("componentSearchResults", componentSearchResultsDto1);

        DslPart searchResponse2 = new PactDslJsonBody()
                .stringType("errorCode")
                .stringType("errorDetails")
                .stringType("logsLink")
                .stringType("startDate")
                .stringType("endDate")
                .stringType("startedAt")
                .stringType("finishedAt")
                .uuid("searchId")
                .uuid("projectId")
                .uuid("environmentId")
                .booleanType("skipArchivation")
                .stringType("status", "COMPLETED")
                .eachLike("componentSearchResults", componentSearchResultsDto2);

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("POST /api/logs/registerSearch OK")
                .path("/api/logs/registerSearch")
                .method("POST")
                .headers(headers)
                .body(logCollectorSearchRequest)
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(searchResponse1)

                .given("all ok")
                .uponReceiving("GET /api/logs/getSearchResults/{searchId}/fastResponse OK")
                .path("/api/logs/getSearchResults/61abd5ac-efbe-49cc-b7f5-925a7f543481/fastResponse")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(searchResponse2)
                ;

        return response.toPact();
    }

    @Configuration
    public static class TestApp {

    }
}
