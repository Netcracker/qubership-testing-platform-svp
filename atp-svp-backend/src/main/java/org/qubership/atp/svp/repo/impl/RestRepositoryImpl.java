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

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.qubership.atp.svp.core.exceptions.RestRequestException;
import org.qubership.atp.svp.model.impl.HttpSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class RestRepositoryImpl {

    public static final String BEARER_TOKEN_PREFIX = "Bearer ";
    private final RestTemplate restTemplateConfig;
    private final AuthTokenProvider tokenProvider;
    @Value("${atp.openshift.host:}")
    private String openshiftHost;
    @Value("${atp.openshift.project:}")
    private String openshiftProject;

    @Autowired
    public RestRepositoryImpl(@Qualifier("restTemplate") RestTemplate restTemplateConfig,
                              AuthTokenProvider tokenProvider) {
        this.restTemplateConfig = restTemplateConfig;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Executes HTTP request and returns result as String.
     *
     * @param baseUrl - HTTP server from env service
     * @param settings - HTTP request settings
     * @return REST json response as String.
     */
    public String executeRequest(String baseUrl, HttpSettings settings) {

        return getStringResponse(settings, restTemplateConfig, baseUrl);
    }

    private String getStringResponse(HttpSettings settings, RestTemplate restTemplate, String baseUrl) {
        HttpEntity<Object> request = setHttpEntity(settings.getBody(), settings.getUrlEncodedBody(),
                setHeaders(settings, baseUrl));
        try {
            URI uri = getDecodeUri(settings, baseUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    settings.getRequestType(),
                    request,
                    String.class
            );
            return response.getBody();
        } catch (Throwable e) {
            String message = "Request error. \n Status code: " + e.getMessage() + ".\n Response body: " + e;
            log.error(message);
            throw new RestRequestException(message);
        }
    }

    @NotNull
    private URI getDecodeUri(HttpSettings settings, String baseUrl) {
        String pathAndQuery = settings.getQuery();
        String httpUrl;
        MultiValueMap<String, String> multiParams = new LinkedMultiValueMap<>();
        int index = pathAndQuery.indexOf('?');
        if (index >= 0) {
            String path = pathAndQuery.substring(0, index);
            String query = pathAndQuery.substring(index + 1);
            if (!query.isEmpty()) {
                Arrays.stream(query.split("&"))
                        .map(param -> param.split("=", 2))
                        .forEach(entry -> multiParams.add(entry[0], entry.length > 1 ? entry[1] : ""));
            }
            httpUrl = baseUrl + path;
        } else {
            httpUrl = baseUrl + pathAndQuery;
        }
        settings.getQueryParams().forEach(multiParams::add);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(httpUrl).queryParams(multiParams);
        return builder.build().encode().toUri();
    }

    private HttpEntity<Object> setHttpEntity(String body, Map<String, String> urlEncodedBody, HttpHeaders headers) {
        if (Objects.nonNull(urlEncodedBody) && !urlEncodedBody.isEmpty()) {
            MultiValueMap<String, String> multiMapBody = new LinkedMultiValueMap<>();
            urlEncodedBody.forEach(multiMapBody::add);
            return new HttpEntity<>(multiMapBody, headers);
        } else if (Objects.nonNull(body) && !body.isEmpty()) {
            return new HttpEntity<>(body, headers);
        }
        return new HttpEntity<>(headers);
    }

    private HttpHeaders setHeaders(HttpSettings settings, String baseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (isAuthTokenAndBaseUrlBelongsOpenshiftHost(openshiftProject, openshiftHost, baseUrl)) {
            headers.add(HttpHeaders.AUTHORIZATION,
                    BEARER_TOKEN_PREFIX + tokenProvider.getAuthToken().get());
        }
        settings.getHeaders().forEach(headers::set);
        return headers;
    }

    private boolean isAuthTokenAndBaseUrlBelongsOpenshiftHost(String openshiftProject, String openshiftHost,
                                                              String baseUrl) {
        boolean result = false;
        if (tokenProvider.getAuthToken().isPresent() && Objects.nonNull(openshiftProject) && !openshiftProject.isEmpty()
                && Objects.nonNull(openshiftHost) && !openshiftHost.isEmpty()) {
            String openshiftProjectAndHost = openshiftProject + "." + openshiftHost;
            result = baseUrl.toLowerCase().contains(openshiftProjectAndHost.toLowerCase());
        }
        return result;
    }
}
