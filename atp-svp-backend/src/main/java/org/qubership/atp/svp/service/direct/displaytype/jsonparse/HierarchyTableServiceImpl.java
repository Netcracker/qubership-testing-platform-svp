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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

@Service
public class HierarchyTableServiceImpl implements JsonParseService {

    private CommonJsonParseTableService commonJsonParseTableService;

    @Autowired
    public HierarchyTableServiceImpl(CommonJsonParseTableService commonJsonParseTableService) {
        this.commonJsonParseTableService = commonJsonParseTableService;
    }

    @Override
    public AbstractValueObject parse(String jsonAsString, JsonParseSettings settings)
            throws GettingValueException {
        return new TableValueObject(getHierarchyTableByJsonParseSettings(jsonAsString, settings));
    }

    private JsonTable getHierarchyTableByJsonParseSettings(String json, JsonParseSettings settings)
            throws GettingValueException {
        JsonTable jsonTable = commonJsonParseTableService.getPreparedJsonTable(settings);
        if (settings.getHierarchyNodeNames().hasEmptyNodeNames()) {
            throw new GettingValueException("One or more NodeNames values were blank");
        } else {
            JsonArray tableDataAsJson = commonJsonParseTableService.getJsonArrayByJsonPath(json,
                    settings.getJsonPath());
            commonJsonParseTableService.checkJson(tableDataAsJson);
            SortedLevelJsonArray sortedLevelJsonArray = getHierarchySortingJsonRow(tableDataAsJson, settings);
            for (JsonElementNestingDepth jsonElement : sortedLevelJsonArray) {
                List<JsonCell> rowCells = commonJsonParseTableService.getJsonTableRowCells(settings, jsonElement);
                jsonTable.addJsonTableRow(new JsonTableRow(rowCells, jsonElement.getNestingDepth()));
            }
        }
        commonJsonParseTableService.checkTable(jsonTable);
        return jsonTable;
    }

    private SortedLevelJsonArray getHierarchySortingJsonRow(JsonArray sourceDataAsJson, JsonParseSettings settings)
            throws GettingValueException {
        String idNodeName = settings.getHierarchyNodeNames().getId();
        String rootIdNodeName = settings.getHierarchyNodeNames().getRootId();
        String parentIdNodeName = settings.getHierarchyNodeNames().getParentId();
        SortedLevelJsonArray sortedLevelJsonArray = new SortedLevelJsonArray();
        Deque<String> depthStack = new ArrayDeque<>();
        Set<String> foundIds = new HashSet<>();

        mainLoopSearchingRootObjects(sourceDataAsJson, idNodeName, rootIdNodeName, parentIdNodeName, depthStack,
                sortedLevelJsonArray, foundIds);

        return sortedLevelJsonArray;
    }

    private void mainLoopSearchingRootObjects(JsonArray sourceDataAsJson, String idNodeName, String rootIdNodeName,
                                              String parentIdNodeName, Deque<String> depthStack,
                                              SortedLevelJsonArray sortedLevelJsonArray,
                                              Set<String> foundIds) throws GettingValueException {
        for (JsonElement sourceJsonElement : sourceDataAsJson) {
            Optional<String> rootIdValue = getIdNodeNameAsJsonPath(sourceJsonElement,
                    rootIdNodeName);
            Optional<String> idValue = getIdNodeNameAsJsonPath(sourceJsonElement, idNodeName);
            if (rootIdValue.isPresent() && idValue.isPresent() && rootIdValue.equals(idValue)) {
                sortedLevelJsonArray.add(new JsonElementNestingDepth(depthStack.size(),
                        sourceJsonElement.getAsJsonObject()));
                foundIds.add(idValue.get());

                recursiveSearchChildObjects(idValue, sourceDataAsJson, depthStack, sortedLevelJsonArray,
                        foundIds, idNodeName, parentIdNodeName, 0);
            }
        }
    }

    private void recursiveSearchChildObjects(Optional<String> desiredIdValue, JsonArray sourceDataAsJson,
                                             Deque<String> depthStack,
                                             SortedLevelJsonArray sortedLevelJsonArray, Set<String> foundIds,
                                             String idNodeName, String parentIdNodeName,
                                             int recursiveCount) throws GettingValueException {

        for (JsonElement sourceJsonElement : sourceDataAsJson) {
            Optional<String> parentIdValue = getIdNodeNameAsJsonPath(sourceJsonElement, parentIdNodeName);
            Optional<String> childIdValue = getIdNodeNameAsJsonPath(sourceJsonElement, idNodeName);

            //We are looking for a child object and go down to its level.
            goToDownLevel(desiredIdValue, sourceDataAsJson, depthStack, sortedLevelJsonArray, foundIds,
                    idNodeName, parentIdNodeName, recursiveCount, parentIdValue, childIdValue, sourceJsonElement);

            //If there are no more child objects then we go up a level.
            goToUpLevel(sourceDataAsJson, depthStack, sortedLevelJsonArray, foundIds, idNodeName, parentIdNodeName,
                    recursiveCount, sourceJsonElement);
        }
    }

    private void goToDownLevel(Optional<String> desiredIdValue, JsonArray sourceDataAsJson,
                               Deque<String> depthStack,
                               SortedLevelJsonArray sortedLevelJsonArray, Set<String> foundIds,
                               String idNodeName, String parentIdNodeName,
                               int recursiveCount, Optional<String> parentIdValue, Optional<String> childIdValue,
                               JsonElement sourceJsonElement) throws GettingValueException {
        if (desiredIdValue.isPresent() && parentIdValue.isPresent() && childIdValue.isPresent()
                && desiredIdValue.equals(parentIdValue) && !foundIds.contains(childIdValue.get())) {
            commonJsonParseTableService.safeRecursionLimitProtect(++recursiveCount);
            depthStack.addLast(parentIdValue.get());
            sortedLevelJsonArray.add(new JsonElementNestingDepth(depthStack.size(),
                    sourceJsonElement.getAsJsonObject()));
            foundIds.add(childIdValue.get());
            desiredIdValue = childIdValue;
            recursiveSearchChildObjects(desiredIdValue, sourceDataAsJson, depthStack, sortedLevelJsonArray,
                    foundIds, idNodeName, parentIdNodeName, recursiveCount);
        }
    }

    private void goToUpLevel(JsonArray sourceDataAsJson, Deque<String> depthStack,
                             SortedLevelJsonArray sortedLevelJsonArray, Set<String> foundIds, String idNodeName,
                             String parentIdNodeName, int recursiveCount, JsonElement sourceJsonElement)
            throws GettingValueException {
        JsonElement lastElement = sourceDataAsJson.get(sourceDataAsJson.size() - 1);
        if (sourceJsonElement.equals(lastElement) && depthStack.size() != 0) {
            commonJsonParseTableService.safeRecursionLimitProtect(--recursiveCount);
            Optional<String> desiredIdValue = Optional.ofNullable(depthStack.pollLast());
            recursiveSearchChildObjects(desiredIdValue, sourceDataAsJson, depthStack, sortedLevelJsonArray,
                    foundIds, idNodeName, parentIdNodeName, recursiveCount);
        }
    }

    private Optional<String> getIdNodeNameAsJsonPath(JsonElement sourceJsonElement, String nameField) {
        Configuration conf = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
        return Optional.ofNullable(JsonPath.using(conf).parse(sourceJsonElement.toString())
                .read(JsonPath.compile(nameField)));
    }
}
