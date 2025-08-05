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

package org.qubership.atp.svp.service.direct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.enums.JsonParseViewType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.impl.JsonParseSettings;
import org.qubership.atp.svp.model.impl.SourceSettings;
import org.qubership.atp.svp.model.impl.TableValidation;
import org.qubership.atp.svp.model.pot.validation.ActualTablesValidationInfo;
import org.qubership.atp.svp.model.pot.validation.TableValidationInfo;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.JsonCell;
import org.qubership.atp.svp.model.table.JsonGroupedCell;
import org.qubership.atp.svp.model.table.JsonSimpleCell;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.model.table.Table;
import org.qubership.atp.svp.service.DefaultValidationDisplayTypeService;
import org.qubership.automation.pc.comparator.impl.JsonComparator;
import org.qubership.automation.pc.comparator.impl.PlainTextComparator;
import org.qubership.automation.pc.comparator.impl.table.FatTableComparator;
import org.qubership.automation.pc.compareresult.DiffMessage;
import org.qubership.automation.pc.configuration.parameters.Parameters;
import org.qubership.automation.pc.core.exceptions.ComparatorException;
import org.qubership.automation.pc.core.helpers.BuildColoredJson;
import org.qubership.automation.pc.core.helpers.BuildColoredText;
import org.qubership.automation.pc.models.HighlighterResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JsonDisplayTypeValidationService extends DefaultValidationDisplayTypeService {

    private final JsonComparator jsonComparator = new JsonComparator();
    private final PlainTextComparator plainTextComparator = new PlainTextComparator();
    private final CompareTablesService compareTablesService;
    private static final String ERROR_MASSAGE_GROUPING_CELL = "*Grouping cell no validate*";
    private static final String JSON_TABLE_NAME = " JsonTable";
    private static final String CHECK_COLUMN = "checkColumn";
    private static final String EMPTY_STRING_FOR_COMPARE_TABLE = "";

    private final FatTableComparator tableComparator = new FatTableComparator();

    /**
     * Constructor for JsonDisplayTypeValidationService.
     */
    @Autowired
    public JsonDisplayTypeValidationService(CompareTablesService validationTablesService) {
        this.compareTablesService = validationTablesService;
    }

    /**
     * Custom validation Json.
     */
    public void customValidation(PotSessionParameterEntity parameter, boolean shouldHighlightDiffs)
            throws ValidationException {
        if (isRawJsonParse(parameter)) {
            customValidateJsonRaw(parameter, shouldHighlightDiffs);
        } else {
            customValidateVerticalJsonTable(parameter, shouldHighlightDiffs);
        }
    }

    /**
     * Plain validation Json.
     */
    public void plainValidation(PotSessionParameterEntity parameter, boolean shouldHighlightDiffs)
            throws ValidationException {
        if (isRawJsonParse(parameter)) {
            plainValidateRawJsonParse(parameter, shouldHighlightDiffs);
        } else {
            validateTableByRule(parameter, shouldHighlightDiffs);
        }
    }

    private boolean isRawJsonParse(PotSessionParameterEntity parameter) {
        JsonParseSettings jsonSettings = null;
        Set<SourceSettings> settings = parameter.getParameterConfig().getSource().getSettings();
        for (SourceSettings setting : settings) {
            if (setting instanceof JsonParseSettings) {
                jsonSettings = (JsonParseSettings) setting;
            }
        }
        if (Objects.nonNull(jsonSettings) && jsonSettings.getJsonViewType().equals(JsonParseViewType.RAW)) {
            return true;
        } else {
            return false;
        }
    }

    private void plainValidateRawJsonParse(PotSessionParameterEntity parameter, boolean shouldHighlightDiffs)
            throws ValidationException {
        List<DiffMessage> overallDiffs = new ArrayList<>();
        try {
            for (AbstractValueObject arValue : parameter.getArValues()) {
                SimpleValueObject er = (SimpleValueObject) parameter.getEr();
                if (Strings.isNullOrEmpty(er.getValue())) {
                    throw new ValidationException("Expected values is empty, please check expected values in "
                            + "configuration");
                }
                SimpleValueObject ar = (SimpleValueObject) arValue;
                List<DiffMessage> diffs = plainTextComparator.compare(er.getValue(), ar.getValue(), new Parameters());
                overallDiffs.addAll(diffs);
                if (shouldHighlightDiffs) {
                    HighlighterResult highlighterResult = BuildColoredText.highlight(diffs,
                            er.getValue(), ar.getValue());
                    er.setHighlightedEr(highlighterResult.getEr().getComposedValue(false));
                    ar.setHighlightedAr(highlighterResult.getAr().getComposedValue(false));
                }
                parameter.setValidationStatus(calculateStatusByDiffs(overallDiffs));
            }
        } catch (ComparatorException ex) {
            throw new ValidationException("Error occurred on trying to validate '"
                    + parameter.getPath(), ex);
        }
    }

    private void validateTableByRule(PotSessionParameterEntity parameter, boolean highlightDifferences)
            throws ValidationException {
        ActualTablesValidationInfo overallValidationInfo = new ActualTablesValidationInfo();
        boolean diffsFound = false;
        try {
            for (int idx = 0; idx < parameter.getArValues().size(); idx++) {
                JsonTable arJsonTable = (JsonTable) ((TableValueObject) parameter.getArValues().get(idx)).getTable();
                String jsonTableName = parameter.getName() + JSON_TABLE_NAME;
                arJsonTable.setName(jsonTableName);
                Table arTable = getConvertTable(arJsonTable, jsonTableName);
                if (!arTable.getRows().isEmpty()) {
                    overallValidationInfo.addTableHeaders(arTable);
                    Parameters rules = new Parameters();
                    List<TableValidation> tableValidationList = parameter.getParameterConfig().getErConfig()
                            .getTableValidations();
                    checkOnError(arJsonTable, tableValidationList);
                    parameter.getParameterConfig().getErConfig().getTableValidations()
                            .forEach(validation -> rules.put(CHECK_COLUMN, validation.composeCheckColumnRule()));
                    List<DiffMessage> diffs =
                            tableComparator.compare(EMPTY_STRING_FOR_COMPARE_TABLE, arTable.toString(), rules);
                    TableValidationInfo tableValidationInfo = new TableValidationInfo();
                    if (!diffs.isEmpty()) {
                        diffsFound = true;
                    }
                    if (highlightDifferences) {
                        tableValidationInfo.setDiffs(diffs);
                    }
                    overallValidationInfo.addTableValidation(tableValidationInfo);
                }
            }
        } catch (ComparatorException ex) {
            throw new ValidationException("Error occurred on trying to validate '"
                    + parameter.getPath() + "' as json TABLE by rule!", ex);
        }
        overallValidationInfo.setStatus(diffsFound ? ValidationStatus.FAILED : ValidationStatus.PASSED);
        parameter.setValidationInfo(overallValidationInfo);
    }

    private void checkOnError(JsonTable jsonTable, List<TableValidation> tableValidationList)
            throws ValidationException {
        List<String> groupedJsonCells = jsonTable.getRows().stream().findFirst().get().getCells()
                .stream().filter(jsonCell -> jsonCell instanceof JsonGroupedCell)
                .map(JsonCell::getColumnHeader).collect(Collectors.toList());

        boolean hasError = tableValidationList.stream()
                .anyMatch(tableValidation -> groupedJsonCells.contains(tableValidation.getColumnName()));
        if (hasError) {
            throw new ValidationException("Couldn't validate grouped json cell");
        }
    }

    private void customValidateJsonRaw(PotSessionParameterEntity parameter, boolean shouldHighlightDiffs)
            throws ValidationException {
        try {
            List<DiffMessage> overallDiffs = new ArrayList<>();
            for (AbstractValueObject arValue : parameter.getArValues()) {
                SimpleValueObject er = (SimpleValueObject) parameter.getEr();
                SimpleValueObject ar = (SimpleValueObject) arValue;
                List<DiffMessage> diffs = jsonComparator.compare(er.getValue(), ar.getValue(), new Parameters());
                overallDiffs.addAll(diffs);
                if (shouldHighlightDiffs) {
                    HighlighterResult highlighterResult = BuildColoredJson.highlight(diffs,
                            er.getValue(), ar.getValue());
                    er.setHighlightedEr(highlighterResult.getEr().getComposedValue(false));
                    ar.setHighlightedAr(highlighterResult.getAr().getComposedValue(false));
                }
            }
            parameter.setValidationStatus(calculateStatusByDiffs(overallDiffs));
        } catch (ComparatorException ex) {
            throw new ValidationException("Error occurred on trying to validate '"
                    + parameter.getPath() + "' as JSON!", ex);
        }
    }

    private void customValidateVerticalJsonTable(PotSessionParameterEntity parameter, boolean shouldHighlightDiffs)
            throws ValidationException {
        convertJsonTableToTable(parameter);
        compareTablesService.compareTables(parameter, shouldHighlightDiffs);
    }

    private void convertJsonTableToTable(PotSessionParameterEntity parameter) {
        for (int i = 0; i < parameter.getArValues().size(); i++) {
            JsonTable arJsonTable = (JsonTable) ((TableValueObject) parameter.getArValues().get(i)).getTable();
            ((TableValueObject) parameter.getArValues().get(i)).setTable(getConvertTable(arJsonTable, "Table actual "
                    + "result"));
        }
        JsonTable erJsonTable = (JsonTable) ((TableValueObject) parameter.getEr()).getTable();
        ((TableValueObject) parameter.getEr()).setTable(getConvertTable(erJsonTable, "Table expected result"));
    }

    private Table getConvertTable(JsonTable jsonTable, String name) {
        Table table = new Table();
        table.setName(name);
        table.setHeaders(jsonTable.getHeaders());
        List<Map<String, String>> rowsTable = new ArrayList<>();
        jsonTable.getRows().forEach(jsonTableRow -> {
            Map<String, String> cellsTable = new HashMap<>();
            jsonTableRow.getCells().forEach(jsonCell -> {
                String columnHeader = jsonCell.getColumnHeader();
                String cellValue;
                if (jsonCell instanceof JsonSimpleCell) {
                    cellValue = ((JsonSimpleCell) jsonCell).getSimpleValue();
                } else {
                    cellValue = ERROR_MASSAGE_GROUPING_CELL;
                }
                cellsTable.put(columnHeader, cellValue);
            });
            rowsTable.add(cellsTable);
        });
        table.setRows(rowsTable);
        return table;
    }
}
