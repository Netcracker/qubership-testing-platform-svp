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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.GsonBuilder;
import org.qubership.atp.svp.model.bulkvalidator.GettingTestCaseIdsRequest;
import org.qubership.atp.svp.model.bulkvalidator.TestRunCreationRequest;
import org.qubership.atp.svp.repo.feign.BulkValidatorPublicFeignClient;
import io.pactfoundation.consumer.dsl.LambdaDsl;
import io.pactfoundation.consumer.dsl.LambdaDslObject;
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
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;

@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {BulkValidatorPublicFeignClient.class})
@ContextConfiguration(classes = {BulkValidatorPublicFeignClientTest.TestApp.class})
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(
        properties = {"feign.atp.bulkValidator.name=atp-bulkValidator",
                "feign.atp.bulkValidator.route=",
                "feign.atp.bulkValidator.url=http://localhost:8890",
                "feign.httpclient.enabled=false"})
public class BulkValidatorPublicFeignClientTest {

    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("atp-bulkValidator", "localhost", 8890, this);
    @Autowired
    BulkValidatorPublicFeignClient bulkValidatorPublicFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        UUID projectId = UUID.fromString("61abd5ac-efbe-49cc-b7f5-925a7f543481");

        ResponseEntity<Object> compareResponseResult = bulkValidatorPublicFeignClient
                .getTcIdsByTcNames(projectId, new GsonBuilder().create().toJson(createGettingTestCaseIdsRequest()));
        Assert.assertEquals(compareResponseResult.getStatusCode().value(), 200);

        ResponseEntity<Object> createTestRunResponseResult = bulkValidatorPublicFeignClient
                .createTr(projectId, new GsonBuilder().create().toJson(createRQ()));
        Assert.assertEquals(createTestRunResponseResult.getStatusCode().value(), 200);
    }

    @Pact(consumer = "atp-svp")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        Map<String, String> headers2 = new HashMap<>();
        headers2.put("Content-Type", "application/json; charset=UTF-8");

        DslPart lambdaStr = LambdaDsl.newJsonBody(ls -> {
            ls.minArrayLike("notFoundTcNames", 0, LambdaDslObject::stringType);
        }).build();

        DslPart lambdaTestRunResponse = LambdaDsl.newJsonBody(testRun -> {
            testRun.stringType("trId");
            testRun.object("context", contexts -> {
                contexts.minArrayLike("inputParameters", 0, testCaseParameter -> {
                    testCaseParameter.stringType("parameterId");
                    testCaseParameter.stringType("tcId");
                    testCaseParameter.stringType("name");
                    testCaseParameter.stringType("value");
                    testCaseParameter.numberType("orderNum");
                });

            });
            testRun.stringType("tcId");
            testRun.stringType("trStatus");
            testRun.stringType("run");
            testRun.minArrayLike("validationContext", 0, validationResult -> {
                validationResult.stringType("objectId");
                validationResult.minArrayLike("diffs", 0, diffs -> {
                    diffs.numberType("orderId");
                    diffs.stringType("expected");
                    diffs.stringType("expectedValue");
                    diffs.stringType("actual");
                    diffs.stringType("actualValue");
                    diffs.stringType("description");
                    diffs.stringType("result", "SUCCESS");
                });
                validationResult.stringType("summaryResult", "SUCCESS");
                validationResult.object("summaryMessage", summaryMessage -> {
                    summaryMessage.numberType("orderId");
                    summaryMessage.stringType("expected");
                    summaryMessage.stringType("expectedValue");
                    summaryMessage.stringType("actual");
                    summaryMessage.stringType("actualValue");
                    summaryMessage.stringType("description");
                    summaryMessage.stringType("result", "SUCCESS");
                });
                validationResult.booleanType("isIgnored");

            });
            testRun.stringType("summaryValidationResult", "SUCCESS");
        }).build();

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("PUT /api/bvtool/project/{projectId}/public/v1/getTcIdsByTcNames OK")
                .path("/api/bvtool/project/61abd5ac-efbe-49cc-b7f5-925a7f543481/public/v1/getTcIdsByTcNames")
                .method("PUT")
                .headers(headers2)
                .body(new GsonBuilder().create().toJson(createGettingTestCaseIdsRequest()))
                .willRespondWith()
                .body(lambdaStr)
                .headers(headers2)
                .status(200)
                .headers(headers)
                .body(lambdaStr)

                .uponReceiving("PUT /api/bvtool/project/{projectId}/public/v1/createTr OK")
                .path("/api/bvtool/project/61abd5ac-efbe-49cc-b7f5-925a7f543481/public/v1/createTr")
                .method("PUT")
                .headers(headers)
                .body(new GsonBuilder().create().toJson(createRQ()))
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(lambdaTestRunResponse);

        return response.toPact();
    }

    @Configuration
    public static class TestApp {
    }

    public TestRunCreationRequest createRQ() {
        TestRunCreationRequest req = new TestRunCreationRequest();
        req.setTcId(UUID.fromString("61abd5ac-efbe-49cc-b7f5-925a7f543481"));
        return req;
    }

    public GettingTestCaseIdsRequest createGettingTestCaseIdsRequest() {
        GettingTestCaseIdsRequest gettingTestCaseIdsRequest =
                new GettingTestCaseIdsRequest(Collections.singletonList("testCaseNames"));
        return gettingTestCaseIdsRequest;
    }
}
