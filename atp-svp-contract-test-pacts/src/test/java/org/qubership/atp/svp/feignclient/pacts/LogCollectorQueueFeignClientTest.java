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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.svp.repo.feign.LogCollectorQueueFeignClient;

@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {LogCollectorQueueFeignClient.class})
@ContextConfiguration(classes = {LogCollectorQueueFeignClientTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(
        properties = {"feign.atp.logcollector.name=atp-logcollector", "feign.atp.logcollector.route=",
                "feign.atp.logcollector.url=http://localhost:8888",
                "feign.httpclient.enabled=false"})
public class LogCollectorQueueFeignClientTest {

    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("atp-logcollector", "localhost", 8888, this);
    @Autowired
    LogCollectorQueueFeignClient logCollectorQueueFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        UUID searchId = UUID.fromString("61abd5ac-efbe-49cc-b7f5-925a7f543481");

        ResponseEntity<List<UUID>> cancelSearches = logCollectorQueueFeignClient.cancelSearches(asList(searchId));
        Assert.assertEquals(200, cancelSearches.getStatusCode().value());
        Assert.assertTrue(cancelSearches.getHeaders().get("Content-Type").contains("application/json"));

    }

    @Pact(consumer = "atp-svp")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String cancelSearchRequest = "[\"61abd5ac-efbe-49cc-b7f5-925a7f543481\"]";


        DslPart cancelSearchesResponse = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.stringType("61abd5ac-efbe-49cc-b7f5-925a7f543481"));

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("POST /api/cancelSearches OK")
                .path("/api/cancelSearches")
                .method("POST")
                .body(cancelSearchRequest)
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(cancelSearchesResponse);
        return response.toPact();
    }

    @Configuration
    public static class TestApp {

    }
}
