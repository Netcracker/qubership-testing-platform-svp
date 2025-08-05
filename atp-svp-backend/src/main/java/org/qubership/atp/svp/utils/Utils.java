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

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.svp.core.exceptions.ConnectionDbException;
import org.qubership.atp.svp.core.exceptions.SqlScriptExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Cut json if it bigger.
     */
    public static String cutterJson(String json) {
        String jsonSource = json.length() > 10000 ? json.substring(0, 10000)
                + "json source is too big and was cut off" : json;
        return jsonSource;
    }

    /**
     * Throws an exception and adds an error to the log.
     */
    public static <T extends RuntimeException> RuntimeException error(Logger log, String errorMessage,
                                                                      @Nullable Exception exp, Class<T> type) {
        String exceptionText = "";
        if (!Objects.isNull(exp)) {
            exceptionText = " \n[Message:" + exp.getMessage() + "\nCause:" + exp.getCause();
            if (exp.getStackTrace() != null && exp.getStackTrace().length > 0) {
                exceptionText += "\nStackTrace:" + exp.getStackTrace()[0];
            }
            exceptionText += "]";
        }
        final String errorText = Objects.isNull(exp) ? errorMessage : errorMessage + exceptionText;
        log.error(errorText);

        if (type.equals(RuntimeException.class)) {
            return Objects.isNull(exp) ? new RuntimeException(errorText)
                    : new RuntimeException(errorText, exp);
        } else if (type.equals(ConnectionDbException.class)) {
            return Objects.isNull(exp) ? new ConnectionDbException(errorText)
                    : new ConnectionDbException(errorText, exp);
        } else {
            return Objects.isNull(exp) ? new SqlScriptExecuteException(errorText)
                    : new SqlScriptExecuteException(errorText, exp);
        }
    }

    @Nonnull
    public static <T> Stream<T> streamOf(@Nonnull Iterator<T> iterator) {
        Spliterator<T> split = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        return StreamSupport.stream(split, false);
    }

    /**
     * Parses long value from string.
     * If {@link NumberFormatException} occur, then default value is used.
     * Default use while parsing {@code @Value} annotation.
     *
     * @param valueToParse string value to parse.
     * @param defaultValue if {@code valueToParse} incorrect use this one.
     * @param valueName    field name to show in logs.
     * @return parsed value.
     */
    public static long parseLongValueOrDefault(String valueToParse, long defaultValue, String valueName) {
        long tempValue;
        try {
            tempValue = Long.parseLong(valueToParse);
        } catch (NumberFormatException e) {
            tempValue = defaultValue;
            LOGGER.error("Error can't parse {} value [{}], use the standard [{}]",
                    valueName, valueToParse, defaultValue);
        }
        return tempValue;
    }

    /**
     * Get BvTestRunUrlPrefix base on atp.integration.enable property.
     * If atp.integration.enable - returns caralogue UI url,
     * otherwise returns bv UI url from config.
     *
     * @param atpIntegrationEnabled atp.integration.enable.
     * @param bvUiUrl bv ui url.
     * @param catalogueUiUrl catalogue ui url.
     * @return BvTestRunUrlPrefix.
     */
    public static String getBvTestRunUrlPrefix(boolean atpIntegrationEnabled,
                                               String bvUiUrl, String catalogueUiUrl, UUID bvProjectId) {

        String projectPath = "/project/" + bvProjectId;
        String bvtPath = "/bvt/";
        String bvtUiPath = bvUiUrl + projectPath + bvtPath;
        String catalogueBvtUiPath = catalogueUiUrl + projectPath + "/bvtool" + bvtPath;
        return atpIntegrationEnabled ? catalogueBvtUiPath : bvtUiPath;
    }
}
