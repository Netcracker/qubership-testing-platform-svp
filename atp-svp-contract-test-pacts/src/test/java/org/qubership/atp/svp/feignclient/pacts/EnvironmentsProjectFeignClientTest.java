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

import au.com.dius.pact.consumer.dsl.*;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.svp.clients.api.environments.dto.projects.EnvironmentResDto;
import org.qubership.atp.svp.clients.api.environments.dto.projects.ProjectFullVer2ViewDto;
import org.qubership.atp.svp.repo.feign.EnvironmentsProjectFeignClient;

@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {EnvironmentsProjectFeignClient.class})
@ContextConfiguration(classes = {EnvironmentsProjectFeignClientTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(
        properties = {"feign.atp.environments.name=atp-environments", "feign.atp.environments.route=",
                "feign.atp.environments.url=http://localhost:8888",
                "feign.httpclient.enabled=false"})
public class EnvironmentsProjectFeignClientTest {

    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("atp-environments", "localhost", 8888, this);
    @Autowired
    EnvironmentsProjectFeignClient environmentsProjectFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        UUID projectId = UUID.fromString("61abd5ac-efbe-49cc-b7f5-925a7f543481");

        ResponseEntity<List<ProjectFullVer2ViewDto>> result_EnvironmentFull =
                environmentsProjectFeignClient.getAllProjects(null, false);
        Assert.assertEquals(result_EnvironmentFull.getStatusCode().value(), 200);
        Assert.assertTrue(result_EnvironmentFull.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<EnvironmentResDto>> environmentsByProjectId =
                environmentsProjectFeignClient.getEnvironments(projectId, true);
        Assert.assertEquals(environmentsByProjectId.getStatusCode().value(), 200);
        Assert.assertTrue(environmentsByProjectId.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<String>> systemsNameResponse =
                environmentsProjectFeignClient.getSystemsName(projectId);
        Assert.assertEquals(systemsNameResponse.getStatusCode().value(), 200);
        Assert.assertTrue(systemsNameResponse.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<String>> connectionsNameResponse =
                environmentsProjectFeignClient.getConnectionsName(projectId);
        Assert.assertEquals(connectionsNameResponse.getStatusCode().value(), 200);
        Assert.assertTrue(connectionsNameResponse.getHeaders().get("Content-Type").contains("application/json"));
    }

    @Pact(consumer = "atp-svp")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        DslPart object = new PactDslJsonBody()
                .stringType("description")
                .stringType("name")
                .integerType("created")
                .uuid("createdBy")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("shortName")
                .eachLike("environments").closeArray();

        DslPart objects = new PactDslJsonArray().template(object);

        DslPart object1 = new PactDslJsonBody()
                .stringType("description")
                .stringType("name")
                .integerType("created")
                .uuid("createdBy")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("graylogName")
                .uuid("projectId")
                .eachLike("systems").closeArray();
        DslPart objects1 = new PactDslJsonArray().template(object1);

        DslPart systemsNameResponse = new PactDslJsonArray().template(PactDslJsonRootValue.stringType("example system"));

        DslPart connectionsNameResponse = new PactDslJsonArray().template(PactDslJsonRootValue.stringType("example connection"));

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("GET /api/projects OK")
                .path("/api/projects")
                .method("GET")
                .query("full=false")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(objects)

                .given("all ok")
                .uponReceiving("GET /api/projects/{projectId}/environments OK")
                .path("/api/projects/61abd5ac-efbe-49cc-b7f5-925a7f543481/environments")
                .method("GET")
                .query("full=true")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(objects1)

                .given("all ok")
                .uponReceiving("GET /api/projects/{projectId}/environments/systems/name OK")
                .path("/api/projects/61abd5ac-efbe-49cc-b7f5-925a7f543481/environments/systems/name")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(systemsNameResponse)

                .given("all ok")
                .uponReceiving("GET /api/projects/{projectId}/environments/connections/name OK")
                .path("/api/projects/61abd5ac-efbe-49cc-b7f5-925a7f543481/environments/connections/name")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(connectionsNameResponse);

        return response.toPact();
    }

    @Configuration
    public static class TestApp {

    }
}

