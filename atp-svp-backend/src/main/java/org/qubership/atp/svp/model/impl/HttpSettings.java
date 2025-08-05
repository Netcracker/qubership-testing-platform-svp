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

package org.qubership.atp.svp.model.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpMethod;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class HttpSettings implements SourceSettings {

    private String query;
    private Map<String, String> queryParams;
    private HttpMethod requestType;
    private String body;
    private Map<String, String> urlEncodedBody;
    private Map<String, String> headers;

    public HttpSettings() {
        this.queryParams = new HashMap<>();
        this.headers = new HashMap<>();
    }

    @Override
    public boolean equals(Object settings) {
        return settings instanceof HttpSettings;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass().getName());
    }
}
