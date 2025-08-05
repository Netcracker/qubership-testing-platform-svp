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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.enums.ValidationType;
import org.qubership.atp.svp.core.exceptions.ConnectionDbException;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.SqlScriptExecuteException;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.core.exceptions.VariableException;
import org.qubership.atp.svp.model.bulkvalidator.ComparingProcessResponse;
import org.qubership.atp.svp.model.db.SutParameterEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.DBServer;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.impl.BulkValidatorValidation;
import org.qubership.atp.svp.model.impl.ErConfig;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.impl.TableSettings;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.validation.ActualTablesValidationInfo;
import org.qubership.atp.svp.model.pot.validation.BulkValidatorTableValidationInfo;
import org.qubership.atp.svp.model.pot.validation.BulkValidatorTestRunInfo;
import org.qubership.atp.svp.model.pot.validation.TableValidationInfo;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.Table;
import org.qubership.atp.svp.repo.impl.CassandraRepository;
import org.qubership.atp.svp.repo.impl.SqlRepository;
import org.qubership.atp.svp.service.DefaultDisplayTypeService;
import org.qubership.atp.svp.service.direct.BulkValidatorValidationService;
import org.qubership.atp.svp.service.direct.CompareTablesService;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.qubership.atp.svp.utils.Utils;
import org.qubership.automation.pc.comparator.impl.table.FatTableComparator;
import org.qubership.automation.pc.compareresult.DiffMessage;
import org.qubership.automation.pc.configuration.parameters.Parameters;
import org.qubership.automation.pc.core.exceptions.ComparatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TableDisplayTypeServiceImpl extends DefaultDisplayTypeService {

    private final FatTableComparator tableComparator = new FatTableComparator();

    private final ExecutionVariablesServiceImpl executionVariablesService;
    private final CompareTablesService compareTablesService;
    private final CassandraRepository cassandraRepository;
    private final BulkValidatorValidationService bulkValidatorValidationService;

    @Value("${atp.catalogue.ui.url:}")
    private String catalogueUiUrl;

    @Value("${atp.integration.enabled:false}")
    private boolean atpIntegrationEnabled;

    @Value("${atp.bv.url}")
    private String bvUrl;

    /**
     * Constructor for TableDisplayTypeServiceImpl.
     */
    @Autowired
    public TableDisplayTypeServiceImpl(ExecutionVariablesServiceImpl executionVariablesService,
                                       CompareTablesService compareTablesService,
                                       CassandraRepository cassandraRepository,
                                       BulkValidatorValidationService bulkValidatorValidationService) {
        this.executionVariablesService = executionVariablesService;
        this.compareTablesService = compareTablesService;
        this.cassandraRepository = cassandraRepository;
        this.bulkValidatorValidationService = bulkValidatorValidationService;
    }

    @Override
    public AbstractValueObject getValueFromSource(Source source, AbstractParameterExecutionContext context)
            throws GettingValueException {
        try {
            Server server = context.getSessionConfiguration()
                    .getEnvironment()
                    .getSystem(source.getSystem())
                    .getServer(source.getConnection());
            String script = executionVariablesService.getSourceWithExecutionVariables(source.getScript(),
                    context.getExecutionVariables());
            Table queryResult;
            switch (source.getEngineType()) {
                case SQL:
                    queryResult = SqlRepository.executeQuery(new DBServer(server), script);
                    setNameTable(source, queryResult, context);
                    return new TableValueObject(queryResult);
                case CASSANDRA:
                    queryResult = cassandraRepository.executeQuery(new DBServer(server), script);
                    setNameTable(source, queryResult, context);
                    return new TableValueObject(queryResult);
                default:
                    throw new IllegalStateException("Unexpected EngineType value: "
                            + source.getEngineType() + " for DisplayType: TABLE");
            }
        } catch (ConnectionDbException e) {
            throw new GettingValueException(e.getMessage(), e.getSqlMessage());
        } catch (SqlScriptExecuteException e) {
            throw new GettingValueException(e.getMessage(), e.getSqlMessage());
        } catch (VariableException e) {
            throw new GettingValueException(e.getMessage(), e.getMessage());
        } catch (RuntimeException e) {
            throw new GettingValueException(e.getMessage());
        }
    }

    private void setNameTable(Source source, Table queryResult, AbstractParameterExecutionContext context) {
        String tableName = ((TableSettings) source.getSettingsByType(TableSettings.class)).getTableName();
        SutParameterEntity parameterConfig = context.getParameter().getParameterConfig();
        boolean isAdditionalSources = parameterConfig.getAdditionalSources().size() > 0;
        ErConfig erConfig = parameterConfig.getErConfig();
        boolean isErAndCustom = Objects.nonNull(erConfig) && erConfig.getType().equals(ValidationType.CUSTOM);
        if (isAdditionalSources || isErAndCustom) {
            queryResult.setName(tableName.isEmpty() ? source.getSystem() : tableName);
        } else {
            queryResult.setName(tableName);
        }
    }

    @Override
    public void validateParameter(AbstractParameterExecutionContext context) throws ValidationException {
        PotSessionParameterEntity parameter = context.getParameter();
        boolean shouldHighlightDiffs = context.getSessionConfiguration().shouldHighlightDiffs();
        switch (parameter.getParameterConfig().getErConfig().getType()) {
            case PLAIN:
                validateTableByRule(parameter, shouldHighlightDiffs);
                break;
            case BV_LINK:
                UUID projectId = context.getSessionConfiguration().getProjectId();
                ConcurrentHashMap<String, ExecutionVariable> variables = context.getExecutionVariables();
                validateTableWithBulkValidator(parameter, projectId, shouldHighlightDiffs, variables);
                break;
            case CUSTOM:
                compareTablesService.compareTables(parameter, shouldHighlightDiffs);
                break;
            default:
                log.warn("Unknown or NONE ER Type!");
                break;
        }
    }

    private void validateTableByRule(PotSessionParameterEntity parameter, boolean highlightDifferences)
            throws ValidationException {
        ActualTablesValidationInfo overallValidationInfo = new ActualTablesValidationInfo();
        boolean diffsFound = false;
        try {
            for (int idx = 0; idx < parameter.getArValues().size(); idx++) {
                Table arTable = (Table) ((TableValueObject) parameter.getArValues().get(idx)).getTable();
                overallValidationInfo.addTableHeaders(arTable);
                Parameters rules = new Parameters();
                parameter.getParameterConfig().getErConfig().getTableValidations()
                        .forEach(validation -> {
                            String value = validation.getValue();
                            if (null != value && value.contains(",")) {
                                value = Arrays.stream(value.split(","))
                                        .map(String::trim)
                                        .collect(Collectors.joining(","));
                                validation.setValue(value);
                            }
                            rules.put("checkColumn", validation.composeCheckColumnRule());
                        });
                List<DiffMessage> diffs = tableComparator.compare("", arTable.toString(), rules);
                TableValidationInfo tableValidationInfo = new TableValidationInfo();
                if (!diffs.isEmpty()) {
                    diffsFound = true;
                }
                if (highlightDifferences) {
                    tableValidationInfo.setDiffs(diffs);
                }
                overallValidationInfo.addTableValidation(tableValidationInfo);
            }
        } catch (ComparatorException ex) {
            throw new ValidationException("Error occurred on trying to validate '"
                    + parameter.getPath() + "' as TABLE by rule!", ex);
        }
        overallValidationInfo.setStatus(diffsFound ? ValidationStatus.FAILED : ValidationStatus.PASSED);
        parameter.setValidationInfo(overallValidationInfo);
    }

    private void validateTableWithBulkValidator(PotSessionParameterEntity parameter, UUID bvProjectId,
                                                boolean highlightDifferences, ConcurrentHashMap<String,
            ExecutionVariable> variables)
            throws ValidationException {
        BulkValidatorValidation bvValidation = parameter.getParameterConfig().getErConfig()
                .getBulkValidatorValidation();
        boolean isManualValidation = parameter.getParameterConfig().getErConfig()
                .getBulkValidatorValidation().getIsManualValidation();
        boolean diffsFound = false;
        List<String> notFindNames = bulkValidatorValidationService.getNotFoundTestCasesByName(bvProjectId,
                bvValidation, variables);
        bvValidation.checkErrors();
        ActualTablesValidationInfo overallValidationInfo = new ActualTablesValidationInfo();
        for (int idx = 0; idx < parameter.getArValues().size(); idx++) {
            Table arTable = (Table) ((TableValueObject) parameter.getArValues().get(idx)).getTable();
            BulkValidatorTableValidationInfo tableValidationInfo = new BulkValidatorTableValidationInfo();

            List<DiffMessage> diffs = new ArrayList<>();
            List<Map<String, String>> rows = arTable.getRows();
            for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                List<ComparingProcessResponse> comparingResponses =
                        bulkValidatorValidationService.validateDataWithBulkValidator(
                                bvProjectId, bvValidation, rows.get(rowIdx), diffs,
                                arTable.getName(), rowIdx);
                List<BulkValidatorTestRunInfo> testRunsInfo = bulkValidatorValidationService.getTestRunsInfo(
                        Utils.getBvTestRunUrlPrefix(atpIntegrationEnabled, bvUrl,
                                catalogueUiUrl,
                                bvProjectId), comparingResponses);
                tableValidationInfo.addBulkValidatorTestRunsInfoForRow(rowIdx, testRunsInfo);
            }

            if (!isManualValidation && !diffs.isEmpty()) {
                diffsFound = true;
            }
            if (!isManualValidation && highlightDifferences) {
                tableValidationInfo.setDiffs(diffs);
            }
            overallValidationInfo.addTableValidation(tableValidationInfo);
        }
        if (notFindNames != null) {
            overallValidationInfo.setStatus(ValidationStatus.FAILED);
            overallValidationInfo.setErrorDescription("BV Test Cases below weren't found: "
                    + notFindNames.stream()
                    .map(String::toString).collect(Collectors.joining(" , ")));
        } else if (diffsFound) {
            overallValidationInfo.setStatus(ValidationStatus.FAILED);
        } else {
            overallValidationInfo.setStatus(isManualValidation ? ValidationStatus.MANUAL : ValidationStatus.PASSED);
        }
        parameter.setValidationInfo(overallValidationInfo);
    }
}
