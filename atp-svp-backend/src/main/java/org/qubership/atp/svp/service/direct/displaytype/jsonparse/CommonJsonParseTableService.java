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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.model.impl.JsonDataColumnSettings;
import org.qubership.atp.svp.model.impl.JsonParseSettings;
import org.qubership.atp.svp.model.table.JsonCell;
import org.qubership.atp.svp.model.table.JsonElementNestingDepth;
import org.qubership.atp.svp.model.table.JsonGroupedCell;
import org.qubership.atp.svp.model.table.JsonSimpleCell;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.model.table.JsonTableRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommonJsonParseTableService {

    private final String noParsingResultMessage = "—";
    private final String rootJsonPath = "$";
    private final int limitRecursiveCount = 1000;
    private CommonJsonParseService commonJsonParseService;

    @Autowired
    public CommonJsonParseTableService(CommonJsonParseService commonJsonParseService) {
        this.commonJsonParseService = commonJsonParseService;
    }

    /**
     * We get the Json row of the table.
     *
     * @param settings - configuration of json parsing, show how to parse source json
     * @param jsonElement - source json for parsing, representing the data of the row
     * @return list of {@link JsonCell} for row data.
     */
    public List<JsonCell> getJsonTableRowCells(JsonParseSettings settings, JsonElement jsonElement) {
        return settings.getColumnsData()
                .stream().map(column -> getTableCellDataAsJsonCell(jsonElement, column, settings))
                .collect(Collectors.toList());
    }

    /**
     * We get a prepared model of the table for transmission to the front.
     *
     * @param settings JsonParseSettings
     * @return JsonTable
     * @throws GettingValueException an exception
     */
    public JsonTable getPreparedJsonTable(JsonParseSettings settings)
            throws GettingValueException {
        interruptTablePreparationIfColumnsDataSettingsNotExist(settings);
        List<String> headers = settings.getColumnsData().stream().map(JsonDataColumnSettings::getHeader)
                .map(String::toUpperCase).collect(Collectors.toList());
        List<JsonTableRow> rows = new ArrayList<>();
        return new JsonTable(headers, rows);
    }

    private void interruptTablePreparationIfColumnsDataSettingsNotExist(JsonParseSettings settings)
            throws GettingValueException {
        if (settings.getColumnsData().isEmpty()) {
            throw new GettingValueException("No columns specified for generating table data from json.");
        }
    }

    /**
     * Returns a json array by the desired node name of the node.
     *
     * @param json String json
     * @param jsonPath Path to the json node
     * @return JsonArray
     * @throws PathNotFoundException an exception.
     */
    public JsonArray getJsonArrayByJsonPath(String json, String jsonPath) throws PathNotFoundException {
        try {
            JsonElement element = commonJsonParseService.getGsonWithPrettyPrinting()
                    .toJsonTree(JsonPath.compile(jsonPath).read(json));
            return getJsonElementAsJsonArray(element);
        } catch (PathNotFoundException e) {
            throw new PathNotFoundException(e.getMessage() + " Json source: " + json);
        }
    }

    /**
     * Returns an exception if the element is not a json object and json array.
     */
    public void checkJson(JsonArray element) {
        if (element.size() != 0 && !element.get(0).isJsonObject() && !element.get(0).isJsonArray()) {
            String message = String.format("The received data is not a JSON. Received data: %s", element);
            throw new RuntimeException(message);
        }
    }

    /**
     * The method accepts any JsonElement, if it is not a JsonArray, it wraps the data and returns a JsonArray.
     *
     * @param element Accepts any JsonElement
     * @return Returns a JsonArray
     */
    private JsonArray getJsonElementAsJsonArray(JsonElement element) {
        JsonArray jsonArray;
        if (element.isJsonArray()) {
            jsonArray = element.getAsJsonArray();
        } else {
            jsonArray = new JsonArray();
            jsonArray.add(element);
        }
        return jsonArray;
    }

    private JsonCell getTableCellDataAsJsonCell(JsonElement cellValueAsJson, JsonDataColumnSettings columnSettings,
                                                JsonParseSettings settings) {
        String cellValue = "";
        if (cellValueAsJson instanceof JsonObject) {
            cellValue = getCellData(cellValueAsJson.toString(), columnSettings.getJsonPath());
        } else if (cellValueAsJson instanceof JsonElementNestingDepth) {
            cellValue = getCellData(((JsonElementNestingDepth) cellValueAsJson).getJsonObject().toString(),
                    columnSettings.getJsonPath());
        }
        JsonCell jsonCell;
        if (Objects.nonNull(columnSettings.getGroupingJsonPaths()) && columnSettings.getGroupingJsonPaths().isEmpty()) {
            jsonCell = new JsonSimpleCell(columnSettings.getHeader().toUpperCase(), cellValue);
        } else {
            JsonArray childArrayAsJson = getJsonArrayByJsonPath(cellValue, rootJsonPath);
            Map<String, String> groupedCellData = getGroupedCellData(childArrayAsJson,
                    columnSettings.getGroupingJsonPaths(), settings);
            jsonCell = new JsonGroupedCell(columnSettings.getHeader().toUpperCase(), groupedCellData);
        }
        return jsonCell;
    }

    private String getCellData(String cellValue, String jsonPath) {
        try {
            return commonJsonParseService.getJsonAsStringByJsonPath(cellValue, jsonPath);
        } catch (PathNotFoundException e) {
            log.warn("Couldn't parse JsonElement by jsonPath {} with cellValue {}", jsonPath, cellValue);
            return noParsingResultMessage;
        }
    }

    private Map<String, String> getGroupedCellData(JsonArray cellValues, List<String> groupingJsonPaths,
                                                   JsonParseSettings settings) {
        Map<String, String> groupedCell = new LinkedHashMap<>();
        cellValues.forEach(value -> {
            List<String> groupNameParsingResults = groupingJsonPaths.stream()
                    .map(jsonPath -> getCellData(value.toString(), jsonPath))
                    .collect(Collectors.toList());
            groupedCell.put(concatStringsWithDivider(groupNameParsingResults, settings),
                    commonJsonParseService.getGsonWithPrettyPrinting().toJson(value));
        });
        return groupedCell;
    }

    private String concatStringsWithDivider(List<String> strings, JsonParseSettings settings) {
        StringBuilder concatResult = new StringBuilder();
        for (int i = 0; i < strings.size(); i++) {
            String currentString = strings.get(i).replaceAll("\n", "");
            concatResult.append(currentString);
            if (i != strings.size() - 1) {
                concatResult.append(settings.getGroupNameDivider());
            }
        }
        return concatResult.toString();
    }

    /**
     * Checking for the maximum number of recursions called from each other to protect against overflow.
     *
     * @param recursiveCount Number of iterations.
     * @throws GettingValueException an exception.
     */
    public void safeRecursionLimitProtect(int recursiveCount) throws GettingValueException {
        if (recursiveCount > limitRecursiveCount) {
            String messageError = "The \"recursiveSearchObject\" method has exceeded the recursion limit: "
                    + limitRecursiveCount;
            log.warn(messageError);
            throw new GettingValueException(messageError);
        }
    }

    /**
     * Set row on empty list if Table has 1 row with '—' value in all cell.
     */
    public void checkTable(JsonTable jsonTable) {
        if (jsonTable.getRows().size() == 1) {
            boolean isEmpty = jsonTable.getRows().get(0).getCells().stream().allMatch(jsonCell -> {
                if (jsonCell instanceof JsonSimpleCell) {
                    return ((JsonSimpleCell) jsonCell).getSimpleValue().equals(noParsingResultMessage);
                } else {
                    return ((JsonGroupedCell) jsonCell).getGroupedValue().values().stream()
                            .allMatch(cell -> cell.equals(noParsingResultMessage));
                }
            });
            if (isEmpty) {
                jsonTable.setRows(Collections.emptyList());
            }
        }
    }
}
