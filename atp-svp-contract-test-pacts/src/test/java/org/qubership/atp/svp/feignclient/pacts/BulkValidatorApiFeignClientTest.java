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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.GsonBuilder;
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

import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.svp.repo.feign.BulkValidatorApiFeignClient;

@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {BulkValidatorApiFeignClient.class})
@ContextConfiguration(classes = {BulkValidatorApiFeignClientTest.TestApp.class})
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(
        properties = {"feign.atp.bulkValidator.name=atp-bulkValidator",
                "feign.atp.bulkValidator.route=",
                "feign.atp.bulkValidator.url=http://localhost:8889",
                "feign.httpclient.enabled=false"})
public class BulkValidatorApiFeignClientTest {

    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("atp-bulkValidator", "localhost", 8889, this);
    @Autowired
    BulkValidatorApiFeignClient bulkValidatorApiFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        UUID projectId = UUID.fromString("61abd5ac-efbe-49cc-b7f5-925a7f543481");

        ResponseEntity<String> compareResponseResult = bulkValidatorApiFeignClient
                .compare(projectId, new GsonBuilder().create().toJson(createContext()));
        Assert.assertEquals(compareResponseResult.getStatusCode().value(), 200);
    }

    @Pact(consumer = "atp-svp")
    public RequestResponsePact createPact(PactDslWithProvider builder) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("PUT /api/bvtool/project/{projectId}/api/v1/compare OK")
                .path("/api/bvtool/project/61abd5ac-efbe-49cc-b7f5-925a7f543481/api/v1/compare")
                .method("PUT")
                .body(new GsonBuilder().create().toJson(createContext()))
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(new GsonBuilder().create().toJson(createContext()));
        return response.toPact();
    }

    @Configuration
    public static class TestApp {
    }

    public String createContext() {
        String requestString = "str";
        return requestString;
    }
}
