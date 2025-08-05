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

package org.qubership.atp.svp.service.direct.displaytype;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.qubership.atp.svp.core.enums.JsonParseViewType;
import org.qubership.atp.svp.core.exceptions.ConnectionDbException;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.JoinJsonTableException;
import org.qubership.atp.svp.core.exceptions.SqlScriptExecuteException;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.core.exceptions.VariableException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.DBServer;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.impl.HttpSettings;
import org.qubership.atp.svp.model.impl.JsonJoinConditionSettings;
import org.qubership.atp.svp.model.impl.JsonParseSettings;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.PotFile;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.JsonCell;
import org.qubership.atp.svp.model.table.JsonSimpleCell;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.model.table.JsonTableRow;
import org.qubership.atp.svp.repo.impl.CassandraRepository;
import org.qubership.atp.svp.repo.impl.RestRepositoryImpl;
import org.qubership.atp.svp.repo.impl.SqlRepository;
import org.qubership.atp.svp.repo.impl.SshRepository;
import org.qubership.atp.svp.service.DefaultDisplayTypeService;
import org.qubership.atp.svp.service.JsonParseTypeFactory;
import org.qubership.atp.svp.service.direct.BulkValidatorValidationService;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.qubership.atp.svp.service.direct.JsonDisplayTypeValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.JSchException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JsonDisplayTypeServiceImpl extends DefaultDisplayTypeService {

    private final JsonParseTypeFactory jsonParseTypeFactory;
    private final ExecutionVariablesServiceImpl executionVariablesService;
    private final RestRepositoryImpl restRepository;
    private final BulkValidatorValidationService bulkValidatorValidationService;
    private final JsonDisplayTypeValidationService jsonDisplayTypeValidationService;
    private final CassandraRepository cassandraRepository;
    private final SshRepository sshRepository;

    /**
     * Constructor for JsonDisplayTypeServiceImpl.
     */
    @Autowired
    public JsonDisplayTypeServiceImpl(ExecutionVariablesServiceImpl executionVariablesService,
                                      RestRepositoryImpl restRepository,
                                      JsonParseTypeFactory jsonParseTypeFactory,
                                      BulkValidatorValidationService bulkValidatorValidationService,
                                      CassandraRepository cassandraRepository,
                                      JsonDisplayTypeValidationService jsonDisplayTypeValidationService,
                                      SshRepository sshRepository) {
        this.executionVariablesService = executionVariablesService;
        this.restRepository = restRepository;
        this.jsonParseTypeFactory = jsonParseTypeFactory;
        this.bulkValidatorValidationService = bulkValidatorValidationService;
        this.cassandraRepository = cassandraRepository;
        this.jsonDisplayTypeValidationService = jsonDisplayTypeValidationService;
        this.sshRepository = sshRepository;
    }

    @Override
    public AbstractValueObject getValueFromSource(Source source, AbstractParameterExecutionContext context)
            throws GettingValueException {
        try {
            Server server = context.getSessionConfiguration().getEnvironment().getSystem(source.getSystem())
                    .getServer(source.getConnection());
            String script = executionVariablesService.getSourceWithExecutionVariables(source.getScript(),
                    context.getExecutionVariables());
            JsonParseSettings jsonSettings = (JsonParseSettings) source.getSettingsByType(JsonParseSettings.class);
            String resultAsString = "";

            if (jsonSettings.getIsMockJsonSwitcher()) {
                resultAsString = jsonSettings.getMockJsonScript();
            } else {
                switch (source.getEngineType()) {
                    case SQL:
                        resultAsString = SqlRepository.executeQueryAndGetFirstValue(new DBServer(server), script);
                        break;
                    case CASSANDRA:
                        resultAsString = cassandraRepository.executeQueryAndGetFirstValue(new DBServer(server), script);
                        break;
                    case REST:
                        String baseUrl = server.getConnection().getParameters().get("url");
                        HttpSettings httpSettings = (HttpSettings) source.getSettingsByType(HttpSettings.class);
                        HttpSettings httpSettingsWithVariables =
                                executionVariablesService.getHttpSettingsWithExecutionVariables(
                                        context.getExecutionVariables(), httpSettings);
                        resultAsString = restRepository.executeRequest(baseUrl, httpSettingsWithVariables);
                        break;
                    case SSH:
                        resultAsString = sshRepository.executeCommandSsh(server, script);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected EngineType value: "
                                + source.getEngineType() + " for DisplayType: JSON");
                }
            }
            AbstractValueObject result;
            if (jsonSettings.getJsonPath().isEmpty()) {
                result = new SimpleValueObject(resultAsString);
            } else {
                ConcurrentHashMap<String, ExecutionVariable> executionVariables = context.getExecutionVariables();
                JsonParseSettings jsonParseSettingsWithVariables =
                        executionVariablesService.getJsonParseSettingsWithExecutionVariables(jsonSettings,
                                executionVariables);
                result = jsonParseTypeFactory.getJsonParseForType(jsonSettings.getJsonViewType())
                        .parse(resultAsString, jsonParseSettingsWithVariables);
                if (jsonSettings.getIsJoinConditionSwitcher()) {
                    String primaryParamName = context.getParameter().getName();
                    joinTable(executionVariables, primaryParamName, jsonSettings, result);
                }
                result.setValueAsFile(new PotFile(resultAsString.getBytes(StandardCharsets.UTF_8)));
            }
            return result;
        } catch (ConnectionDbException e) {
            throw new GettingValueException(e.getMessage(), e.getSqlMessage());
        } catch (SqlScriptExecuteException e) {
            throw new GettingValueException(e.getMessage(), e.getSqlMessage());
        } catch (VariableException e) {
            throw new GettingValueException(e.getMessage(), e.getMessage());
        } catch (RuntimeException | JSchException e) {
            throw new GettingValueException(e.getMessage());
        } finally {
            //It is necessary not to send large Json mock to the websocket
            ((JsonParseSettings) source.getSettingsByType(JsonParseSettings.class)).setMockJsonScript("");
        }
    }

    private void joinTable(ConcurrentHashMap<String, ExecutionVariable> executionVariables, String primaryParamName,
                           JsonParseSettings jsonSettings,
                           AbstractValueObject resultTable) {

        JsonTable primaryTable = (JsonTable) ((TableValueObject) resultTable).getTable();

        if (primaryTable.containsRows()) {
            List<JsonJoinConditionSettings> joinConditionSettings = jsonSettings.getJsonJoinConditionSettings();
            JsonTable joinJsonTable = prepJoinTableFromPrimaryTable(primaryParamName, jsonSettings, primaryTable,
                    joinConditionSettings);

            prepJoinTableAndGetVariablesFromRefTable(executionVariables, joinConditionSettings, joinJsonTable);
            JsonTable jsonTableResult = createJoinTable(joinConditionSettings, primaryTable, joinJsonTable);
            ((TableValueObject) resultTable).setTable(jsonTableResult);
        }
    }

    @NotNull
    private JsonTable prepJoinTableFromPrimaryTable(String primaryParamName,
                                                    JsonParseSettings jsonSettings,
                                                    JsonTable primaryTable,
                                                    List<JsonJoinConditionSettings> joinConditionSettings) {
        List<Integer> idxPrimaryHeaders = new ArrayList<>();
        int size = primaryTable.getHeaders().size();
        joinConditionSettings.forEach(joinConditionSetting ->
                joinConditionSetting.getIdxPrimaryHeaderNames().forEach(idxPrimary -> {
                    if (primaryTable.getHeaders().size() - 1 < idxPrimary) {
                        String alertMassage = getStringExceptionIdxHeader(primaryParamName, size, idxPrimary);
                        log.error(alertMassage);
                        throw new JoinJsonTableException(alertMassage);
                    }
                }));

        joinConditionSettings.stream().findFirst().ifPresent(joinConditionSetting ->
                idxPrimaryHeaders.addAll(joinConditionSetting.getIdxPrimaryHeaderNames()));
        sortedPrimaryTable(primaryTable, idxPrimaryHeaders, primaryParamName, jsonSettings);

        JsonTable joinJsonTable = new JsonTable();
        List<String> primaryHeaders = primaryTable.getHeaders().stream().map(header ->
                header + " (" + primaryParamName + ")").collect(Collectors.toList());
        joinJsonTable.getHeaders().addAll(primaryHeaders);
        return joinJsonTable;
    }

    private void prepJoinTableAndGetVariablesFromRefTable(ConcurrentHashMap<String, ExecutionVariable> variables,
                                                          List<JsonJoinConditionSettings> joinConditionSettings,
                                                          JsonTable joinJsonTable) {
        joinConditionSettings.forEach(joinConditionSetting -> {
            if (!joinConditionSetting.hasEmptyJoinConditionSettings()) {
                String pathReferenceToSutParameterName
                        = joinConditionSetting.getPathReferenceSutParameterName();
                int primIdxSize = joinConditionSetting.getIdxPrimaryHeaderNames().size();
                int refIdxSize = joinConditionSetting.getIdxReferenceHeaderNames().size();
                String refPathTable = joinConditionSetting.getPathReferenceSutParameterName();
                if (primIdxSize != refIdxSize) {
                    String alertMessage = "In the join conditions setting, a different number of key columns for the "
                            + "join is selected: primary table size: [" + primIdxSize + "] and reference table  "
                            + "[" + refPathTable + "] size: [" + refIdxSize + "].";
                    log.error(alertMessage);
                    throw new JoinJsonTableException(alertMessage);
                }
                Optional<JsonTable> referenceJsonTable = executionVariablesService
                        .getSourceWithJsonTableVariables(pathReferenceToSutParameterName, variables);
                if (referenceJsonTable.isPresent()) {
                    JsonTable jsonTableRef = referenceJsonTable.get();
                    joinConditionSetting.setReferenceTable(SerializationUtils.clone(jsonTableRef));
                    List<String> headerNames = jsonTableRef.getHeaders().stream().map(header ->
                                    header + " (" + pathReferenceToSutParameterName + ")")
                            .collect(Collectors.toList());
                    joinJsonTable.getHeaders().addAll(headerNames);
                } else {
                    String alertMessage = "Referenced JsonTable not found on path: " + pathReferenceToSutParameterName;
                    log.error(alertMessage);
                    throw new JoinJsonTableException(alertMessage);
                }
            }
        });
    }

    private JsonTable createJoinTable(List<JsonJoinConditionSettings> joinConditionSettings, JsonTable primaryTable,
                                      JsonTable joinJsonTable) {
        String noDateValue = "—";
        int primaryHeaderSize = primaryTable.getHeaders().size();
        primaryTable.getRows().forEach(primRow -> {
            List<JsonTableRow> tempJoinRowsList = new ArrayList<>();
            AtomicInteger sumLengthRows = new AtomicInteger(primaryHeaderSize);

            joinConditionSettings.forEach(joinConditionSetting -> {
                boolean isStartIterPrimRows = true;
                int countFoundRef = 0;
                JsonTable referenceTable = joinConditionSetting.getReferenceTable();
                int referenceHeaderSize = referenceTable.getHeaders().size();
                String nameRefParamTable = joinConditionSetting.getPathReferenceSutParameterName();
                List<Integer> idxPrimaryHeader = joinConditionSetting.getIdxPrimaryHeaderNames();
                List<Integer> idxReferenceHeader = joinConditionSetting.getIdxReferenceHeaderNames();
                isJsonSimpleCell(idxPrimaryHeader, primRow, nameRefParamTable);
                List<String> primaryCellValues = getCellValues(idxPrimaryHeader, primRow);
                List<String> tempRefCellValues = idxReferenceHeader.stream().map(x -> "")
                        .collect(Collectors.toList());
                //Iterating over a reference table
                if (!referenceTable.getRows().isEmpty()) {
                    for (int idxRefRow = 0; idxRefRow < referenceTable.getRows().size(); ) {
                        JsonTableRow refRow = referenceTable.getRows().get(idxRefRow);
                        isJsonSimpleCell(idxReferenceHeader, refRow, nameRefParamTable);
                        List<String> refCellValues = getCellValues(idxReferenceHeader, refRow);
                        List<JsonCell> tempJoinRow = new ArrayList<>();
                        if (primaryCellValues.equals(refCellValues)) {
                            //Go here if the first key match is found primer tables
                            if (isStartIterPrimRows) {
                                if (tempJoinRowsList.isEmpty()) {
                                    tempJoinRow.addAll(primRow.getCells());
                                    tempJoinRow.addAll(refRow.getCells());
                                    tempJoinRowsList.add(new JsonTableRow(tempJoinRow, primRow.getNestingDepth()));
                                } else {
                                    tempJoinRowsList.get(countFoundRef).addAllCells(refRow.getCells());
                                }
                                countFoundRef++;
                                referenceTable.getRows().remove(idxRefRow);
                                isStartIterPrimRows = false;
                                tempRefCellValues = refCellValues;
                                continue;
                                //If the found key matches the previous one in the reference table
                            } else if (tempRefCellValues.equals(refCellValues)) {
                                if (tempJoinRowsList.size() > countFoundRef
                                        && sumLengthRows.get() > primaryHeaderSize) {
                                    tempJoinRowsList.get(countFoundRef).addAllCells(refRow.getCells());
                                } else {
                                    tempJoinRow.addAll(generateJoinRow(primaryHeaderSize, ""));
                                    tempJoinRow.addAll(generateJoinRow(
                                            sumLengthRows.get() - primaryHeaderSize, noDateValue));
                                    tempJoinRow.addAll(refRow.getCells());
                                    tempJoinRowsList.add(new JsonTableRow(tempJoinRow,
                                            primRow.getNestingDepth() + 1));
                                }
                                countFoundRef++;
                                referenceTable.getRows().remove(idxRefRow);
                                tempRefCellValues = refCellValues;
                                continue;
                            }
                            //If the reference table has ended and the primary key has not been found
                        } else if (idxRefRow + 1 == referenceTable.getRows().size()) {
                            if (tempJoinRowsList.isEmpty()) {
                                tempJoinRow.addAll(primRow.getCells());
                                tempJoinRow.addAll(generateJoinRow(referenceHeaderSize, noDateValue));
                                tempJoinRowsList.add(new JsonTableRow(tempJoinRow, primRow.getNestingDepth()));
                            } else {
                                tempJoinRow.addAll(generateJoinRow(referenceHeaderSize, noDateValue));
                                IntStream.range(countFoundRef, tempJoinRowsList.size())
                                        .forEachOrdered(i -> tempJoinRowsList.get(i).addAllCells(tempJoinRow));
                            }
                            countFoundRef++;
                        }
                        idxRefRow++;
                    }
                    //If you started looking for the primary key in an empty reference table
                } else {
                    List<JsonCell> tempJoinRow = new ArrayList<>();
                    tempJoinRow.addAll(primRow.getCells());
                    tempJoinRow.addAll(generateJoinRow(referenceHeaderSize, noDateValue));
                    tempJoinRowsList.add(new JsonTableRow(tempJoinRow, primRow.getNestingDepth()));
                }
                sumLengthRows.addAndGet(referenceTable.getHeaders().size());
            });
            joinJsonTable.addAllJsonTableRows(tempJoinRowsList);
        });
        fillNewHeadersToCellsJoinTable(joinJsonTable);
        return joinJsonTable;
    }

    private void fillNewHeadersToCellsJoinTable(JsonTable joinJsonTable) {
        int joinHeaderSize = joinJsonTable.getHeaders().size();
        joinJsonTable.getRows().forEach(row -> IntStream.range(0, joinHeaderSize)
                .forEach(i -> row.getCells().get(i).setColumnHeader(joinJsonTable.getHeaders().get(i)))
        );
    }

    private void sortedPrimaryTable(JsonTable primaryTable, List<Integer> idxPrimaryHeaders,
                                    String primaryParamName, JsonParseSettings jsonSettings) {
        if (jsonSettings.getJsonViewType().equals(JsonParseViewType.TABLE)) {
            primaryTable.getRows().sort((objRow1, objRow2) -> {
                isJsonSimpleCell(idxPrimaryHeaders, objRow1, primaryParamName);
                AtomicReference<String> valueObject1 = new AtomicReference<>("");
                AtomicReference<String> valueObject2 = new AtomicReference<>("");
                AtomicInteger resultCompare = new AtomicInteger();
                idxPrimaryHeaders.stream().anyMatch(idxPrim -> {
                    valueObject1.set(((JsonSimpleCell) objRow1.getCells().get(idxPrim)).getSimpleValue());
                    valueObject2.set(((JsonSimpleCell) objRow2.getCells().get(idxPrim)).getSimpleValue());
                    resultCompare.set(valueObject1.get().compareTo(valueObject2.get()));
                    return resultCompare.get() != 0;
                });
                return resultCompare.get();
            });
        }
    }

    private List<JsonCell> generateJoinRow(int sumLengthRows, String value) {
        return IntStream.range(0, sumLengthRows).mapToObj(x -> new JsonSimpleCell(value)).collect(Collectors.toList());
    }

    private void isJsonSimpleCell(List<Integer> idxHeaderNames, JsonTableRow row, String nameParamTable) {
        AtomicReference<String> nameCell = new AtomicReference<>("");
        int size = row.getCells().size();
        boolean isJsonSimpleCell = idxHeaderNames.stream()
                .allMatch(idxHeader -> {
                    try {
                        nameCell.set(row.getCells().get(idxHeader).getColumnHeader());
                        return row.getCells().get(idxHeader) instanceof JsonSimpleCell;
                    } catch (IndexOutOfBoundsException e) {
                        String alertMassage = getStringExceptionIdxHeader(nameParamTable, size, idxHeader);
                        log.error(alertMassage, e);
                        throw new JoinJsonTableException(alertMassage, e);
                    }
                });
        if (!isJsonSimpleCell) {
            String messageError = "The cell type does not match the simple type in the table: "
                    + nameParamTable + ", column: " + nameCell;
            log.warn(messageError);
            throw new JoinJsonTableException(messageError);
        }
    }

    private String getStringExceptionIdxHeader(String nameParamTable, int size, Integer idxHeader) {
        return "In JsonTable named [" + nameParamTable + "] - size table [" + size + "] column "
                + "with index [" + idxHeader + "] does not exist. Change the column settings for "
                + "this table or the condition in the Join Condition settings where this table "
                + "participates.";
    }

    private List<String> getCellValues(List<Integer> idxPrimaryHeaderNames, JsonTableRow row) {
        return idxPrimaryHeaderNames.stream().map(idx -> ((JsonSimpleCell) row.getCells()
                .get(idx)).getSimpleValue()).collect(Collectors.toList());
    }

    @Override
    public void validateParameter(AbstractParameterExecutionContext context)
            throws ValidationException {
        PotSessionParameterEntity parameter = context.getParameter();
        boolean shouldHighlightDiffs = context.getSessionConfiguration().shouldHighlightDiffs();
        switch (parameter.getParameterConfig().getErConfig().getType()) {
            case BV_LINK:
                ConcurrentHashMap<String, ExecutionVariable> variables = context.getExecutionVariables();
                UUID projectId = context.getSessionConfiguration().getProjectId();
                bulkValidatorValidationService.validateJsonWithBulkValidator(parameter, projectId, variables);
                break;
            case CUSTOM:
                jsonDisplayTypeValidationService.customValidation(parameter, shouldHighlightDiffs);
                break;
            case PLAIN:
                jsonDisplayTypeValidationService.plainValidation(parameter, shouldHighlightDiffs);
                break;
            default:
                log.warn("Unknown or NONE ER Type!");
                break;
        }
    }
}
