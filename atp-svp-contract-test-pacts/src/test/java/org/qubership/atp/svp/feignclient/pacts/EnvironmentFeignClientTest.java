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
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.svp.clients.api.environments.dto.projects.EnvironmentFullVer1ViewDto;
import org.qubership.atp.svp.clients.api.environments.dto.projects.SystemFullVer2ViewDto;
import org.qubership.atp.svp.repo.feign.EnvironmentFeignClient;

@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {EnvironmentFeignClient.class})
@ContextConfiguration(classes = {EnvironmentFeignClientTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(
        properties = {"feign.atp.environments.name=atp-environments", "feign.atp.environments.route=",
                "feign.atp.environments.url=http://localhost:8888",
                "feign.httpclient.enabled=false"})
public class EnvironmentFeignClientTest {

    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("atp-environments", "localhost", 8888, this);
    @Autowired
    EnvironmentFeignClient environmentFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        UUID environmentId = UUID.fromString("61abd5ac-efbe-49cc-b7f5-925a7f543481");

        ResponseEntity<EnvironmentFullVer1ViewDto> environmentResponse =
                environmentFeignClient.getEnvironment(environmentId, true);
        Assert.assertEquals(environmentResponse.getStatusCode().value(), 200);
        Assert.assertTrue(environmentResponse.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<SystemFullVer2ViewDto>> systemsV2Response =
                environmentFeignClient.getSystemV2(environmentId, "system_type", true);

        Assert.assertEquals(systemsV2Response.getStatusCode().value(), 200);
        Assert.assertTrue(systemsV2Response.getHeaders().get("Content-Type").contains("application/json"));
    }

    @Pact(consumer = "atp-svp")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        DslPart environmentsByEnvironmentIdResponse = new PactDslJsonBody()
                .integerType("created")
                .uuid("createdBy")
                .stringType("description")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("name")
                .uuid("projectId")
                .eachLike("systems");

        DslPart object = new PactDslJsonBody()
                .integerType("created")
                .uuid("createdBy")
                .integerType("dateOfCheckVersion")
                .integerType("dateOfLastCheck")
                .stringType("description")
                .uuid("externalId")
                .uuid("id")
                .uuid("linkToSystemId")
                .booleanType("mergeByName")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("name")
                .stringType("status", "NOTHING")
                .stringType("version")
                .uuid("parentSystemId")
                .eachLike("connections").closeArray()
                .eachLike("environments").closeArray()
                .object("serverITF").closeObject()
                .object("parametersGettingVersion").closeObject()
                .object("systemCategory").closeObject();

        DslPart objects = new PactDslJsonArray().template(object);







        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("GET /api/environments/{environmentId} OK")
                .path("/api/environments/61abd5ac-efbe-49cc-b7f5-925a7f543481")
                .method("GET")
                .query("full=true")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(environmentsByEnvironmentIdResponse)

                .given("all ok")
                .uponReceiving("GET /api/v2/environments/{environmentId}/systems OK")
                .path("/api/v2/environments/61abd5ac-efbe-49cc-b7f5-925a7f543481/systems")
                .matchQuery("system_type", "\\w*", "type")
                .matchQuery("full", "true|false", "true")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(objects);

        return response.toPact();
    }

    @Configuration
    public static class TestApp {

    }
}
