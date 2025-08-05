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

package org.qubership.atp.svp.utils.converters;

import java.io.IOException;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Converter
@Component
@NoArgsConstructor
@Slf4j
public class ListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> requestParams) {
        String requestParamsJson = null;
        if (requestParams != null) {
            try {
                requestParamsJson = new ObjectMapper().writeValueAsString(requestParams);
            } catch (final JsonProcessingException e) {
                log.error("JSON writing error", e);
            }
        }
        return requestParamsJson;
    }

    @Override
    public List<String> convertToEntityAttribute(String requestParamsJson) {
        List<String> requestParams = null;
        if (Strings.isNotBlank(requestParamsJson)) {
            try {
                requestParams = new ObjectMapper().readValue(requestParamsJson, List.class);
            } catch (final IOException e) {
                log.error("JSON reading error", e);
            }
        }
        return requestParams;
    }

}
