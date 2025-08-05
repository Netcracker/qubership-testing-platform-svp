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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import org.qubership.atp.svp.model.impl.HttpSettings;

@ExtendWith(MockitoExtension.class)
class RestRepositoryImplTest {
    @InjectMocks
    private RestRepositoryImpl restRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private AuthTokenProvider tokenProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void executeRequest_withQueryParamsContainingSpecialCharacter_shouldSuccessfullyExecuted() {
        String baseUrl = "http://test/service/api/v1/test";
        Map<String, String> mapParams = new HashMap<>();
        mapParams.put("param2", "abc");
        HttpSettings httpSettings = new HttpSettings();
        httpSettings.setBody("");
        httpSettings.setQuery("/?param1=[{\"&= |\\^`");
        httpSettings.setUrlEncodedBody(null);
        httpSettings.setQueryParams(mapParams);
        httpSettings.setHeaders(new HashMap<>());
        httpSettings.setRequestType(HttpMethod.GET);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(String.class))).thenReturn(ResponseEntity.ok().build());

        restRepository.executeRequest(baseUrl, httpSettings);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
        URI actualURI = uriCaptor.getValue();
        assertNotNull(actualURI, "Captured URL shouldn't be null");
        String actualQuery = actualURI.getRawQuery();
        assertNotNull(actualQuery, "Captured actualQuery shouldn't be null");
        assertEquals("param1=%5B%7B%22&=%20%7C%5C%5E%60&param2=abc", actualQuery);
    }

    @Test
    public void executeRequest_withQueryParamsContainingManyEqualChar_shouldSuccessfullyExecuted() {
        String baseUrl = "http://test/service/api/v1/test";
        Map<String, String> mapParams = new HashMap<>();
        mapParams.put("param3", "abc");
        HttpSettings httpSettings = new HttpSettings();
        httpSettings.setBody("");
        httpSettings.setQuery("/?param1=a1=a2=a3&param2=b1=b2=b3");
        httpSettings.setUrlEncodedBody(null);
        httpSettings.setQueryParams(mapParams);
        httpSettings.setHeaders(new HashMap<>());
        httpSettings.setRequestType(HttpMethod.GET);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(String.class))).thenReturn(ResponseEntity.ok().build());

        restRepository.executeRequest(baseUrl, httpSettings);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
        URI actualURI = uriCaptor.getValue();
        assertNotNull(actualURI, "Captured URL shouldn't be null");
        String actualQuery = actualURI.getRawQuery();
        assertNotNull(actualQuery, "Captured actualQuery shouldn't be null");
        assertEquals("param1=a1%3Da2%3Da3&param2=b1%3Db2%3Db3&param3=abc", actualQuery);
    }
    @Test
    public void executeRequest_withQueryNull_shouldSuccessfullyExecuted() {
        String baseUrl = "http://test/service/api/v1/test";
        HttpSettings httpSettings = new HttpSettings();
        httpSettings.setBody("");
        httpSettings.setQuery("/param1");
        httpSettings.setUrlEncodedBody(null);
        httpSettings.setHeaders(new HashMap<>());
        httpSettings.setRequestType(HttpMethod.GET);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(String.class))).thenReturn(ResponseEntity.ok().build());

        restRepository.executeRequest(baseUrl, httpSettings);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
        URI actualURI = uriCaptor.getValue();
        assertNotNull(actualURI, "Captured URL shouldn't be null");
        String actualQuery = actualURI.getRawQuery();
        assertNull(actualQuery, "Captured actualQuery shouldn't be not null");
    }
}
