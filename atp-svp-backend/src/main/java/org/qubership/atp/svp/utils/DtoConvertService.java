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

package org.qubership.atp.svp.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DtoConvertService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor for class.
     */
    public DtoConvertService() {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.getConfiguration().getConverters().add(new String2UuidConverter());
        objectMapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Get modelMapper for additional configurations.
     *
     * @return modelMapper.
     */
    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    /**
     * Convert t.
     *
     * @param <T> the type parameter
     * @param from the from
     * @param to the to
     * @return the t
     */
    public <T> T convert(Object from, Class<T> to) {
        T finish = modelMapper.map(from, to);
        return finish;
    }

    /**
     * Convert list to list.
     *
     * @param <T> the type parameter
     * @param from the from
     * @param to the to
     * @return the set
     */
    public <T> List<T> convertList(List from, Class<T> to) {
        if (from == null) {
            return new ArrayList<>();
        }
        return (List<T>) from.stream().map(o -> convert(o, to)).collect(Collectors.toList());
    }

    /**
     * Convert set to list.
     *
     * @param <T> the type parameter
     * @param from the from
     * @param to the to
     * @return the set
     */
    public <T> List<T> convertSetToList(Set from, Class<T> to) {
        if (from == null) {
            return new ArrayList<>();
        }
        return (List<T>) from.stream().map(o -> convert(o, to)).collect(Collectors.toList());
    }

    /**
     * Convert String to object.
     *
     * @param <T> the type parameter
     * @param from the from
     * @param to the to
     * @return the object
     */
    public <T> T convertFromString(String from, Class<T> to) throws JsonProcessingException {
        return objectMapper.readValue(from, to);
    }
}
