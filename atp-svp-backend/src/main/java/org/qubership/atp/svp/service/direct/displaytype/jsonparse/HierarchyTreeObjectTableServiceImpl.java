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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.model.impl.JsonParseSettings;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.JsonCell;
import org.qubership.atp.svp.model.table.JsonElementNestingDepth;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.model.table.JsonTableRow;
import org.qubership.atp.svp.model.table.SortedLevelJsonArray;
import org.qubership.atp.svp.service.JsonParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HierarchyTreeObjectTableServiceImpl implements JsonParseService {

    private CommonJsonParseTableService commonJsonParseTableService;

    @Autowired
    public HierarchyTreeObjectTableServiceImpl(CommonJsonParseTableService commonJsonParseTableService) {
        this.commonJsonParseTableService = commonJsonParseTableService;
    }

    @Override
    public AbstractValueObject parse(String jsonAsString, JsonParseSettings settings) throws GettingValueException {
        return new TableValueObject(getHierarchyTreeObjectTableByJsonParseSettings(jsonAsString, settings));
    }

    private JsonTable getHierarchyTreeObjectTableByJsonParseSettings(String json, JsonParseSettings settings)
            throws GettingValueException {
        if (settings.getHierarchyTreeObjectNodeNames().isEmpty()) {
            throw new GettingValueException("NodeNames values are not filled in json parse settings");
        } else {
            JsonTable jsonTable = commonJsonParseTableService.getPreparedJsonTable(settings);
            JsonArray tableDataAsJson = commonJsonParseTableService.getJsonArrayByJsonPath(json,
                    settings.getJsonPath());
            commonJsonParseTableService.checkJson(tableDataAsJson);
            SortedLevelJsonArray sortedLevelJsonArray = getHierarchyTreeObjectSortingJsonRow(tableDataAsJson, settings);
            sortedLevelJsonArray.forEach(jsonElement -> {
                List<JsonCell> rowCells = commonJsonParseTableService.getJsonTableRowCells(settings, jsonElement);
                jsonTable.addJsonTableRow(new JsonTableRow(rowCells, jsonElement.getNestingDepth()));
            });
            commonJsonParseTableService.checkTable(jsonTable);
            return jsonTable;
        }
    }

    private SortedLevelJsonArray getHierarchyTreeObjectSortingJsonRow(JsonArray sourceDataAsJson,
                                                                      JsonParseSettings settings)
            throws GettingValueException {
        List<String> objectArrayNodeNames = settings.getHierarchyTreeObjectNodeNames();
        SortedLevelJsonArray sortedLevelJsonArray = new SortedLevelJsonArray();
        Deque<String> depthStack = new ArrayDeque<>();
        depthStack.add("$");

        //Calling a recursive method to build a tree of objects
        recursiveSearchObjects(sourceDataAsJson, objectArrayNodeNames, depthStack, sortedLevelJsonArray, 0);

        return sortedLevelJsonArray;
    }

    private void recursiveSearchObjects(JsonArray sourceDataAsJson, List<String> objectArrayNodeNames,
                                        Deque<String> depthStack, SortedLevelJsonArray sortedLevelJsonArray,
                                        int recursiveCount) throws GettingValueException {
        if (!depthStack.isEmpty()) {
            String jsonPathToArray = depthStack.getLast();
            Optional<JsonElement> jsonArray = getJsonElementAsJsonPath(sourceDataAsJson, jsonPathToArray);

            //Iterating through the resulting json array
            iteratingJsonArray(sourceDataAsJson, jsonArray, objectArrayNodeNames, sortedLevelJsonArray, depthStack,
                    recursiveCount);
        }
    }

    private void iteratingJsonArray(JsonArray sourceDataAsJson, Optional<JsonElement> jsonArray,
                                    List<String> objectArrayNodeNames, SortedLevelJsonArray sortedLevelJsonArray,
                                    Deque<String> depthStack, int recursiveCount) throws GettingValueException {
        if (jsonArray.isPresent()) {
            int jsonArraySize = ((JsonArray) jsonArray.get()).size();

            for (int idxJsonArray = 0; idxJsonArray < jsonArraySize; idxJsonArray++) {
                String jsonPathToObject = depthStack.getLast() + "[" + idxJsonArray + "]";
                Optional<JsonElement> jsonObject = getJsonElementAsJsonPath(sourceDataAsJson, jsonPathToObject);

                //Adding a JSONObject with the nesting level to the sorted array of json elements
                addingJsonObjectWithNestingDepthToSortedArray(jsonObject, sortedLevelJsonArray, depthStack);

                //Iterating through the array of object parameter names received from the front
                iteratingObjectArrayNodeNames(sourceDataAsJson, objectArrayNodeNames, jsonPathToObject,
                        sortedLevelJsonArray, depthStack, recursiveCount, idxJsonArray, jsonArraySize);
            }
        }
    }

    private void addingJsonObjectWithNestingDepthToSortedArray(Optional<JsonElement> jsonObject,
                                                               SortedLevelJsonArray sortedLevelJsonArray,
                                                               Deque<String> depthStack) {
        jsonObject.ifPresent(jsonElement -> sortedLevelJsonArray
                .add(new JsonElementNestingDepth(depthStack.size() - 1,
                        (JsonObject) jsonElement)));
    }

    private void iteratingObjectArrayNodeNames(JsonArray sourceDataAsJson, List<String> objectArrayNodeNames,
                                               String jsonPathToObject, SortedLevelJsonArray sortedLevelJsonArray,
                                               Deque<String> depthStack, int recursiveCount, int idxJsonArray,
                                               int jsonArraySize) throws GettingValueException {
        for (int idxNodeNames = 0; idxNodeNames < objectArrayNodeNames.size(); idxNodeNames++) {
            String jsonPathToArray = jsonPathToObject + "." + objectArrayNodeNames.get(idxNodeNames);
            Optional<JsonElement> jsonObject = getJsonElementAsJsonPath(sourceDataAsJson, jsonPathToArray);

            //If the field exists, then add an entry to the stack
            goToDownLevel(jsonObject, jsonPathToArray, sourceDataAsJson, objectArrayNodeNames,
                    depthStack, sortedLevelJsonArray, recursiveCount);

            // If this is the last parameter in the list and the last object in the JSONArray array, then we delete
            // the entry from the stack and go up a level higher
            goToUpLevel(objectArrayNodeNames, depthStack, recursiveCount, idxNodeNames, idxJsonArray, jsonArraySize);
        }
    }

    private void goToDownLevel(Optional<JsonElement> jsonObject, String jsonPathToArray, JsonArray sourceDataAsJson,
                               List<String> objectArrayNodeNames, Deque<String> depthStack,
                               SortedLevelJsonArray sortedLevelJsonArray,
                               int recursiveCount) throws GettingValueException {
        if (jsonObject.isPresent()) {
            //Checking for the maximum number of recursions called from each other to protect against overflow
            commonJsonParseTableService.safeRecursionLimitProtect(++recursiveCount);
            depthStack.add(jsonPathToArray);
            //We call the recursive method and dive to the level below
            recursiveSearchObjects(sourceDataAsJson, objectArrayNodeNames, depthStack,
                    sortedLevelJsonArray, recursiveCount);
        }
    }

    private void goToUpLevel(List<String> objectArrayNodeNames, Deque<String> depthStack, int recursiveCount,
                             int idxNodeNames, int idxJsonArray, int jsonArraySize) throws GettingValueException {
        if (idxNodeNames == objectArrayNodeNames.size() - 1 && idxJsonArray == jsonArraySize - 1) {
            depthStack.pollLast();
            //Reducing the recursion limit counter
            commonJsonParseTableService.safeRecursionLimitProtect(--recursiveCount);
        }
    }

    private Optional<JsonElement> getJsonElementAsJsonPath(JsonElement sourceJsonElement, String nameField) {
        Configuration conf = Configuration.defaultConfiguration()
                .addOptions(Option.SUPPRESS_EXCEPTIONS)
                .jsonProvider(new GsonJsonProvider());
        Optional<JsonElement> jsonElement = Optional.ofNullable(JsonPath.using(conf).parse(sourceJsonElement.toString())
                .read(JsonPath.compile(nameField)));
        if (jsonElement.isPresent()) {
            JsonElement element = new GsonBuilder().create().toJsonTree(jsonElement.get());
            if (element.isJsonObject() && element.getAsJsonObject().size() != 0
                    || element.isJsonArray() && element.getAsJsonArray().size() != 0) {
                return Optional.of(element);
            }
        }
        return Optional.empty();
    }
}
