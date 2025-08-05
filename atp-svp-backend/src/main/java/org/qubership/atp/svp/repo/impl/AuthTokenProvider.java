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

import java.util.Optional;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import brave.Span;
import brave.Tracer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthTokenProvider {

    public static final String DEFAULT_PROFILE = "default";
    private final AccessTokenProvider accessTokenProvider;
    private final OAuth2ProtectedResourceDetails protectedResourceDetails;
    private final Tracer tracer;
    private final AccessTokenRequest accessTokenRequest = new DefaultAccessTokenRequest();
    @Value("${spring.profiles.active:disable-security}")
    private String activeProfile;

    /**
     * RamAdapterConfiguration constructor.
     *
     * @param accessTokenProvider access token provider
     * @param protectedResourceDetails protected resource details
     * @param tracer tracer
     */
    public AuthTokenProvider(AccessTokenProvider accessTokenProvider,
                             OAuth2ProtectedResourceDetails protectedResourceDetails,
                             @Autowired(required = false) Tracer tracer) {
        this.accessTokenProvider = accessTokenProvider;
        this.protectedResourceDetails = protectedResourceDetails;
        this.tracer = tracer;
    }

    /**
     * Get Authorization token.
     *
     * @return Bearer token
     */
    public Optional<String> getAuthToken() {
        if (isAuthentication()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            //noinspection unchecked
            Optional<String> relayToken = Optional.ofNullable(authentication)
                    .map(Authentication::getPrincipal)
                    .filter(principal -> principal instanceof KeycloakPrincipal)
                    .map(principal -> (KeycloakPrincipal<KeycloakSecurityContext>) principal)
                    .map(KeycloakPrincipal::getKeycloakSecurityContext)
                    .map(KeycloakSecurityContext::getTokenString);
            if (relayToken.isPresent()) {
                log.info("user token found");
                return relayToken;
            }
            Optional<Span> nextSpan = Optional.ofNullable(tracer).map(Tracer::nextSpan);
            try {
                log.info("get m2m token");
                nextSpan.ifPresent(span -> {
                    span.name("get m2m token");
                    span.start();
                });
                OAuth2AccessToken accessToken = accessTokenProvider.obtainAccessToken(
                        protectedResourceDetails,
                        accessTokenRequest
                );
                log.info("m2m token received");
                return Optional.ofNullable(accessToken.getValue());
            } finally {
                nextSpan.ifPresent(Span::finish);
            }
        } else {
            return Optional.empty();
        }
    }

    public boolean isAuthentication() {
        return DEFAULT_PROFILE.equalsIgnoreCase(activeProfile);
    }
}

