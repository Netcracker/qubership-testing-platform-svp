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

package org.qubership.atp.svp.repo.impl;

import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.impl.HttpSettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

@Repository
public class SoapRepositoryImpl {

    /**
     * Executes SOAP request and returns result as String.
     *
     * @param server   - HTTP server from env service
     * @param settings - query HTTP settings
     * @return SOAP xml response as String.
     */
    public static String soapRequest(Server server, HttpSettings settings) {
        String baseUrl = server.getConnection().getParameters().get("url");
        WebClient.Builder clientBuilder = WebClient
                .builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE)
                .defaultHeader("SOAPAction", "");
        settings.getHeaders().forEach(clientBuilder::defaultHeader);
        ClientResponse block = clientBuilder.build()
                .post()
                .uri(settings.getQuery())
                .body(BodyInserters.fromObject(settings.getBody()))
                .exchange()
                .block();
        return block.bodyToMono(String.class).block();
    }
}
