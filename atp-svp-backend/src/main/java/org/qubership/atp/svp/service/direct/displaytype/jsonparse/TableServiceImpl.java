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

import java.util.List;

import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.model.impl.JsonParseSettings;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.JsonCell;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.model.table.JsonTableRow;
import org.qubership.atp.svp.service.JsonParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;

@Service
public class TableServiceImpl implements JsonParseService {

    private CommonJsonParseTableService commonJsonParseTableService;

    @Autowired
    public TableServiceImpl(CommonJsonParseTableService commonJsonParseTableService) {
        this.commonJsonParseTableService = commonJsonParseTableService;
    }

    @Override
    public AbstractValueObject parse(String jsonAsString, JsonParseSettings settings) throws GettingValueException {
        return new TableValueObject(getTableByJsonParseSettings(jsonAsString, settings));
    }

    private JsonTable getTableByJsonParseSettings(String json, JsonParseSettings settings)
            throws GettingValueException {
        JsonTable jsonTable = commonJsonParseTableService.getPreparedJsonTable(settings);
        JsonArray tableDataAsJson = commonJsonParseTableService.getJsonArrayByJsonPath(json, settings.getJsonPath());
        commonJsonParseTableService.checkJson(tableDataAsJson);
        tableDataAsJson.forEach(jsonElement -> {
            List<JsonCell> rowCells = commonJsonParseTableService.getJsonTableRowCells(settings, jsonElement);
            jsonTable.addJsonTableRow(new JsonTableRow(rowCells));
        });
        commonJsonParseTableService.checkTable(jsonTable);
        return jsonTable;
    }
}
