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

package org.qubership.atp.svp.service.direct.displaytype.jsonparse;

import org.qubership.atp.svp.utils.Utils;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommonJsonParseService {

    /**
     * Getting parsed json string by json path.
     *
     * @param json String json
     * @param jsonPath Path to the node
     * @return String json
     * @throws PathNotFoundException an exception.
     */
    public String getJsonAsStringByJsonPath(String json, String jsonPath) throws PathNotFoundException {
        try {
            Object resultObj = JsonPath.compile(jsonPath).read(json);
            return resultObj instanceof String
                    ? resultObj.toString()
                    : getGsonWithPrettyPrinting().toJson(resultObj);
        } catch (PathNotFoundException e) {
            throw new PathNotFoundException(e.getMessage() + " Json source: " + Utils.cutterJson(json));
        }
    }

    /**
     * Getting first value.
     */
    public String getFirstValueByJsonAsStringByJsonPath(String json, String jsonPath) throws PathNotFoundException {
        try {
            String value = jsonPath.replaceFirst("\\$\\.", "");
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            if (jsonObject.get(value).isJsonArray()) {
                JsonArray jsonArray = jsonObject.getAsJsonArray(value);
                return jsonArray.get(0).getAsString();
            } else {
                log.error("Json path: {} is not correctly and is not array in json: \n {}",
                        jsonPath, Utils.cutterJson(json));
                throw new RuntimeException("Json path: " + jsonPath + " is not correctly and is not array in json:"
                        + Utils.cutterJson(json));
            }
        } catch (PathNotFoundException e) {
            throw new PathNotFoundException(e.getMessage() + " Json source: " + Utils.cutterJson(json));
        }
    }

    public Gson getGsonWithPrettyPrinting() {
        return new GsonBuilder().setPrettyPrinting().create();
    }
}
