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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.enums.ValidationType;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.core.exceptions.VariableException;
import org.qubership.atp.svp.model.bulkvalidator.ComparingProcessResponse;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.impl.BulkValidatorValidation;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.logcollector.ComponentSearchResults;
import org.qubership.atp.svp.model.logcollector.SearchResult;
import org.qubership.atp.svp.model.logcollector.SearchThreadFindResult;
import org.qubership.atp.svp.model.logcollector.SystemSearchResults;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.validation.BulkValidatorTableValidationInfo;
import org.qubership.atp.svp.model.pot.validation.BulkValidatorTestRunInfo;
import org.qubership.atp.svp.model.pot.validation.IntegrationLogsValidationInfo;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.LogCollectorValueObject;
import org.qubership.atp.svp.repo.impl.LogCollectorRepository;
import org.qubership.atp.svp.service.DefaultDisplayTypeService;
import org.qubership.atp.svp.service.LogCollectorBasedDisplayTypeService;
import org.qubership.atp.svp.service.direct.BulkValidatorValidationService;
import org.qubership.atp.svp.service.direct.DeferredSearchServiceImpl;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.qubership.atp.svp.utils.Utils;
import org.qubership.automation.pc.compareresult.DiffMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IntegrationLogDisplayTypeServiceImpl extends DefaultDisplayTypeService
        implements LogCollectorBasedDisplayTypeService {

    private final LogCollectorRepository logCollectorRepository;
    private final DeferredSearchServiceImpl deferredSearchService;
    private final ExecutionVariablesServiceImpl executionVariablesService;
    private final BulkValidatorValidationService bulkValidatorValidationService;

    /**
     * Constructor for IntegrationLogDisplayTypeServiceImpl.
     */
    @Autowired
    public IntegrationLogDisplayTypeServiceImpl(LogCollectorRepository logCollectorRepository,
                                                DeferredSearchServiceImpl deferredSearchService,
                                                ExecutionVariablesServiceImpl executionVariablesService,
                                                BulkValidatorValidationService bulkValidatorValidationService) {
        this.logCollectorRepository = logCollectorRepository;
        this.deferredSearchService = deferredSearchService;
        this.executionVariablesService = executionVariablesService;
        this.bulkValidatorValidationService = bulkValidatorValidationService;
    }

    @Value("${atp.catalogue.ui.url:}")
    private String catalogueUiUrl;

    @Value("${atp.integration.enabled:false}")
    private boolean atpIntegrationEnabled;

    @Value("${atp.bv.url}")
    private String bvUrl;

    @Override
    // TODO Unit tests for the logic of processing results from LC
    public AbstractValueObject getValueFromSource(Source source, AbstractParameterExecutionContext context)
            throws GettingValueException {
        try {
            return getLogCollectorValueObject(logCollectorRepository, deferredSearchService,
                    executionVariablesService, context, source);
        } catch (VariableException e) {
            throw new GettingValueException(e.getMessage(), e.getMessage());
        }
    }

    @Override
    public void validateParameter(AbstractParameterExecutionContext context) throws ValidationException {
        PotSessionParameterEntity parameter = context.getParameter();
        boolean shouldHighlightDiffs = context.getSessionConfiguration().shouldHighlightDiffs();
        for (AbstractValueObject arValue : parameter.getArValues()) {
            if (arValue.hasData()) {
                if (checkOnBulkValidationType(parameter)) {
                    UUID projectId = context.getSessionConfiguration().getProjectId();
                    ConcurrentHashMap<String, ExecutionVariable> variables = context.getExecutionVariables();
                    validateTableWithBulkValidator(parameter, projectId, shouldHighlightDiffs, variables);
                } else {
                    super.validateParameter(context);
                }
            }
        }
    }

    private boolean checkOnBulkValidationType(PotSessionParameterEntity parameter) {
        return parameter.getParameterConfig().getErConfig().getType().equals(ValidationType.BV_LINK);
    }

    private void validateTableWithBulkValidator(PotSessionParameterEntity parameter,
                                                UUID projectId,
                                                boolean highlightDifferences,
                                                ConcurrentHashMap<String, ExecutionVariable> variables)
            throws ValidationException {
        BulkValidatorValidation bvValidation = parameter.getParameterConfig()
                .getErConfig().getBulkValidatorValidation();
        // Validation only for first actual result
        // (LogCollector additional sources are not implemented yet)
        LogCollectorValueObject lcValue = (LogCollectorValueObject) parameter.getArValues().get(0);
        // Validation status set to PASSED by default
        IntegrationLogsValidationInfo overallValidationInfo =
                new IntegrationLogsValidationInfo(ValidationStatus.PASSED);
        boolean isManualValidation = parameter.getParameterConfig().getErConfig()
                .getBulkValidatorValidation().getIsManualValidation();
        fillOverallValidationWithComponentsValidation(lcValue.getSearchResult(), bvValidation, projectId,
                highlightDifferences, overallValidationInfo, isManualValidation, variables);
        if (isManualValidation) {
            overallValidationInfo.setStatus(ValidationStatus.MANUAL);
        }
        parameter.setValidationInfo(overallValidationInfo);
    }

    private void fillOverallValidationWithComponentsValidation(SearchResult searchResult,
                                                               BulkValidatorValidation bvValidation,
                                                               UUID bvProjectId,
                                                               boolean highlightDifferences,
                                                               IntegrationLogsValidationInfo overallValidationInfo,
                                                               boolean isManualValidation,
                                                               ConcurrentHashMap<String, ExecutionVariable> variables)
            throws ValidationException {
        BulkValidatorTableValidationInfo componentValidationInfo = new BulkValidatorTableValidationInfo();
        // Analogue of table  processing
        int rowIdx = 0;
        List<DiffMessage> diffs = new ArrayList<>();
        List<String> notFindNames = bulkValidatorValidationService.getNotFoundTestCasesByName(bvProjectId,
                bvValidation, variables);
        bvValidation.checkErrors();
        for (ComponentSearchResults component : searchResult.getComponentSearchResults()) {
            List<SystemSearchResults> systemSearchResults = component.getSystemSearchResults();
            for (SystemSearchResults system : systemSearchResults) {
                List<SearchThreadFindResult> searchThreadResult = system.getSearchThreadResult();
                for (SearchThreadFindResult thread : searchThreadResult) {
                    // Analogue of row processing
                    List<ComparingProcessResponse> comparingResponses =
                            bulkValidatorValidationService.validateDataWithBulkValidator(bvProjectId, bvValidation,
                                    thread, diffs, component.getComponentName(), rowIdx);
                    List<BulkValidatorTestRunInfo> testRunsInfo = bulkValidatorValidationService.getTestRunsInfo(
                            Utils.getBvTestRunUrlPrefix(atpIntegrationEnabled, bvUrl, catalogueUiUrl, bvProjectId),
                            comparingResponses);
                    componentValidationInfo.addBulkValidatorTestRunsInfoForRow(rowIdx, testRunsInfo);
                    rowIdx++;
                }
            }
            overallValidationInfo.setTableValidations(componentValidationInfo, component.getComponentType());
        }
        if (notFindNames != null) {
            overallValidationInfo.setStatus(ValidationStatus.FAILED);
            overallValidationInfo.setErrorDescription("BV Test Cases below weren't found:\n"
                    + notFindNames.stream()
                    .map(String::toString).collect(Collectors.joining(" , ")));
        } else if (!isManualValidation && !diffs.isEmpty()) {
            overallValidationInfo.setStatus(ValidationStatus.FAILED);
        }
        if (!isManualValidation && highlightDifferences) {
            componentValidationInfo.setDiffs(diffs);
        }
    }
}
