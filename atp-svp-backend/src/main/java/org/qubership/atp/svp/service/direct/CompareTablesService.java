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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.pot.validation.TableValidationInfo;
import org.qubership.atp.svp.model.pot.validation.TableVsTableValidationInfo;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.Table;
import org.qubership.automation.pc.comparator.impl.table.FatTableComparator;
import org.qubership.automation.pc.compareresult.DiffMessage;
import org.qubership.automation.pc.configuration.parameters.Parameters;
import org.qubership.automation.pc.core.exceptions.ComparatorException;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CompareTablesService {

    private final FatTableComparator tableComparator = new FatTableComparator();

    /**
     * The method compare tables and set Validation info for PotSessionParameter.
     */
    public void compareTables(PotSessionParameterEntity parameter, boolean highlightDifferences)
            throws ValidationException {
        Table leadTable = (Table) ((TableValueObject) parameter.getEr()).getTable();
        TableVsTableValidationInfo info = new TableVsTableValidationInfo();
        info.setGroupingValues(leadTable.getGroupingValues(parameter.getParameterConfig().getErConfig()
                .getTableValidationSettings().getGroupingColumns()));
        info.addTableHeaders(leadTable);
        // Compare AR tables with ER table
        List<String> keyColumns = parameter.getParameterConfig().getErConfig()
                .getTableValidationSettings().getKeyColumns()
                .stream().map(String::toUpperCase).collect(Collectors.toList());
        Parameters parameters = new Parameters();
        keyColumns.forEach(key -> parameters.put("primaryKey", key));
        boolean diffsFound = false;
        for (int arTableIdx = 0; arTableIdx < parameter.getArValues().size(); arTableIdx++) {
            if (parameter.getArValues().get(arTableIdx) instanceof TableValueObject) {
                Table arTable = (Table) ((TableValueObject) parameter.getArValues().get(arTableIdx)).getTable();
                TableValidationInfo tableValidationInfo = new TableValidationInfo();
                info.addTableHeaders(arTable);
                // Sort ARs (for UI usage)
                if (!keyColumns.isEmpty()) {
                    arTable = sortArTable(leadTable, arTable, keyColumns);
                }
                // Compare tables
                try {
                    List<DiffMessage> diffs = tableComparator.compare(leadTable.toString(),
                            arTable.toString(), parameters);
                    if (!diffs.isEmpty()) {
                        diffsFound = true;
                    }
                    if (highlightDifferences) {
                        tableValidationInfo.setDiffs(diffs);
                    }
                } catch (ComparatorException ex) {
                    throw new ValidationException("Error occurred on trying to validate '"
                            + parameter.getPath() + "' as TABLE vs TABLE!", ex);
                }

                info.addTableValidation(tableValidationInfo);
            } else {
                log.warn("Table validation skipped!");
            }
        }
        // Finally set validation result
        info.setStatus(diffsFound ? ValidationStatus.FAILED : ValidationStatus.PASSED);
        parameter.setValidationInfo(info);
    }

    private Table sortArTable(Table er, Table ar, List<String> keyColumns) {
        List<Map<String, String>> unprocessedArRows = ar.getRows();
        List<Map<String, String>> orderedArRows = new ArrayList<>();
        for (int erIdx = 0; erIdx < er.getRows().size(); erIdx++) {
            Map<String, String> erRow = er.getRow(erIdx);
            boolean pairFound = false;
            List<String> erRowValues = new ArrayList<>(keyColumns.size());
            keyColumns.forEach(key -> erRowValues.add(erRow.get(key)));
            Iterator<Map<String, String>> iterator = unprocessedArRows.iterator();
            while (iterator.hasNext()) {
                Map<String, String> arRow = iterator.next();
                List<String> arRowValues = new ArrayList<>(keyColumns.size());
                keyColumns.forEach(key -> arRowValues.add(arRow.get(key)));
                if (erRowValues.equals(arRowValues)) {
                    orderedArRows.add(arRow);
                    iterator.remove();
                    pairFound = true;
                    break;
                }
            }
            if (!pairFound) {
                orderedArRows.add(Collections.emptyMap());
            }
        }
        orderedArRows.addAll(unprocessedArRows);
        ar.setRows(orderedArRows);
        return ar;
    }
}
