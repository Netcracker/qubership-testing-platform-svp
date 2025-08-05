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
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.InvalidBulkValidatorApiUsageException;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.core.exceptions.VariableException;
import org.qubership.atp.svp.model.bulkvalidator.ComparingProcessResponse;
import org.qubership.atp.svp.model.bulkvalidator.GettingTestCaseIdsResponse;
import org.qubership.atp.svp.model.bulkvalidator.JsonCompareResponse;
import org.qubership.atp.svp.model.bulkvalidator.TestRunCreationResponse;
import org.qubership.atp.svp.model.bulkvalidator.ValidationObject;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.impl.BulkValidatorJsonValidation;
import org.qubership.atp.svp.model.impl.BulkValidatorValidation;
import org.qubership.atp.svp.model.impl.BulkValidatorValidationItem;
import org.qubership.atp.svp.model.logcollector.SearchThreadFindResult;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.validation.BulkValidatorJsonValidationInfo;
import org.qubership.atp.svp.model.pot.validation.BulkValidatorTestRunInfo;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.repo.impl.BulkValidatorRepository;
import org.qubership.atp.svp.service.ExecutionVariablesService;
import org.qubership.atp.svp.utils.Utils;
import org.qubership.automation.pc.compareresult.DiffMessage;
import org.qubership.automation.pc.compareresult.ResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class BulkValidatorValidationService {

    private final BulkValidatorRepository bulkValidatorRepository;
    private final ExecutionVariablesService executionVariablesService;
    private static final String UUID_REGEXP_MATCHER =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @Value("${atp.catalogue.ui.url:}")
    private String catalogueUiUrl;

    @Value("${atp.integration.enabled:false}")
    private boolean atpIntegrationEnabled;

    @Value("${atp.bv.url}")
    private String bvUrl;

    @Autowired
    public BulkValidatorValidationService(BulkValidatorRepository bulkValidatorRepository,
                                          ExecutionVariablesService executionVariablesService) {
        this.bulkValidatorRepository = bulkValidatorRepository;
        this.executionVariablesService = executionVariablesService;
    }

    /**
     * Validate Json with Bulk Validator by next steps:
     * 1) Creation test runs for current AR with Bulk Validator
     * 2) Run comparing process for the test run
     * 3) Get Highlight Json for take result of compare and set in AR and ER
     * 4) Set validation Status.
     */
    public void validateJsonWithBulkValidator(PotSessionParameterEntity parameter,
                                              UUID bvProjectId,
                                              ConcurrentHashMap<String, ExecutionVariable> variables)
            throws ValidationException {
        try {
            BulkValidatorJsonValidation bvValidation = parameter.getParameterConfig().getErConfig()
                    .getBulkValidatorJsonValidation();

            bvValidation.checkError();
            UUID tcId = getTestCaseId(bvValidation, bvProjectId, variables);
            BulkValidatorJsonValidationInfo validationInfo = new BulkValidatorJsonValidationInfo();

            for (int idx = 0; idx < parameter.getArValues().size(); idx++) {
                SimpleValueObject arValue;
                try {
                    arValue = ((SimpleValueObject) parameter.getArValues().get(idx));
                } catch (ClassCastException e) {
                    throw new ValidationException("Could not validate parameter " + parameter.getName()
                            + " with Bulk Validator. Reason: Json type must be RAW only!");
                }
                List<ValidationObject> validationObjects = getListValidationObjectsForJson(arValue.getValue(),
                        bvValidation);
                TestRunCreationResponse testRunCreationResponse = bulkValidatorRepository.createTestRun(bvProjectId,
                        tcId, validationObjects);

                if (Objects.isNull(testRunCreationResponse.getTcId())
                        || testRunCreationResponse.getContext().getValues().isEmpty()) {
                    throw new ValidationException("Could not validate parameter " + parameter.getName()
                            + " with Bulk Validator. BV test case id or validation object name are not correct!");
                }

                UUID trId = testRunCreationResponse.getTrId();
                ComparingProcessResponse compareResponse = startTestRun(trId, bvProjectId);
                JsonCompareResponse jsonResponse = getHighlights(bvValidation, tcId, trId, bvProjectId,
                        testRunCreationResponse);
                setAr(arValue, jsonResponse);
                setEr(parameter, jsonResponse);
                validationInfo.getBvTestRunsInfo()
                        .add(createTestRunInfo(compareResponse, bvUrl, bvProjectId));
            }
            setValidationInfo(parameter, validationInfo);
        } catch (InvalidBulkValidatorApiUsageException ex) {
            throw new ValidationException(ex);
        }
    }

    private <T> List<ValidationObject> getListValidationObjectsForJson(T data,
                                                                       BulkValidatorJsonValidation bvValidation) {
        ValidationObject validationObject = new ValidationObject(bvValidation.getValidationObjectName(), (String) data);
        return Collections.singletonList(validationObject);
    }

    private ComparingProcessResponse startTestRun(UUID trId, UUID bvProjectId) {
        List<UUID> trIds = Collections.singletonList(trId);
        List<ComparingProcessResponse> compareResponse = bulkValidatorRepository.compare(bvProjectId, trIds);
        return compareResponse.stream().findFirst().get();
    }

    private JsonCompareResponse getHighlights(BulkValidatorJsonValidation bvValidation, UUID tcId, UUID trId,
                                              UUID bvProjectId,
                                              TestRunCreationResponse testRunCreationResponse) {
        String validationObjectName = bvValidation.getValidationObjectName();
        UUID objectIds = testRunCreationResponse.getContext().getValues().stream()
                .filter(values -> values.getName().equals(validationObjectName)).findFirst().get().getExternalId();

        return bulkValidatorRepository.getHighlightJson(tcId, trId, objectIds,
                bvProjectId);
    }

    private void setAr(SimpleValueObject arValue,
                       JsonCompareResponse jsonResponse) {
        String arEncode = jsonResponse.getAr().getValue();
        String decodedAr = decodeValue(arEncode);
        arValue.setValue(decodedAr);
    }

    private void setEr(PotSessionParameterEntity parameter,
                       JsonCompareResponse jsonResponse) {
        if (jsonResponse.getRules().stream().noneMatch(ruleResponse ->
                ruleResponse.getName().equals("validateSchema"))
                && Objects.nonNull(jsonResponse.getEr())) {
            String erEncode = jsonResponse.getEr().getValue();
            String decodedEr = decodeValue(erEncode);
            parameter.setEr(new SimpleValueObject(decodedEr));
        }
    }

    private String decodeValue(String value) {
        byte[] decodedArBytes = Base64.getDecoder().decode(value);
        return new String(decodedArBytes);
    }

    private BulkValidatorTestRunInfo createTestRunInfo(ComparingProcessResponse compareResponse,
                                                       String bvUrl,
                                                       UUID bvProjectId) {
        BulkValidatorTestRunInfo testRunInfo = new BulkValidatorTestRunInfo();
        ValidationStatus status = compareResponse.nonIdenticalCompareResult()
                ? ValidationStatus.FAILED : ValidationStatus.PASSED;
        testRunInfo.setValidationStatus(status);

        String bvTestRunUrlPrefix = Utils.getBvTestRunUrlPrefix(atpIntegrationEnabled, bvUrl,
                catalogueUiUrl, bvProjectId);
        String bvLink = bvTestRunUrlPrefix + compareResponse.getResultLink();
        testRunInfo.setTestRunLink(bvLink);

        String tcName = compareResponse.getTcName();
        testRunInfo.setTestCaseName(tcName);
        return testRunInfo;
    }

    private void setValidationInfo(PotSessionParameterEntity parameter,
                                   BulkValidatorJsonValidationInfo validationInfo) {
        if (validationInfo.getBvTestRunsInfo().stream()
                .noneMatch(testRun -> testRun.getValidationStatus().equals(ValidationStatus.FAILED))) {
            validationInfo.setStatus(ValidationStatus.PASSED);
        } else {
            validationInfo.setStatus(ValidationStatus.FAILED);
        }
        parameter.setValidationInfo(validationInfo);
    }

    private UUID getTestCaseId(BulkValidatorJsonValidation bvValidation, UUID bvProjectId,
                               ConcurrentHashMap<String, ExecutionVariable> variables) throws ValidationException {
        try {
            String testCase = bvValidation.getTestCaseId();
            testCase = executionVariablesService
                    .getTestCaseNamesFromVariable(new ArrayList<>(Collections.singletonList(testCase)), variables)
                    .stream().findFirst().get();
            if (testCase.matches(UUID_REGEXP_MATCHER)) {
                return UUID.fromString(bvValidation.getTestCaseId());
            } else {
                GettingTestCaseIdsResponse gettingTestCaseIdsResponse =
                        bulkValidatorRepository.getTestCaseIds(bvProjectId, Collections.singletonList(testCase));
                checkResponse(gettingTestCaseIdsResponse);
                String testCaseId = gettingTestCaseIdsResponse.getTestCases().values()
                        .stream().findFirst().get().stream().findFirst().get();
                return UUID.fromString(testCaseId);
            }
        } catch (VariableException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    private void checkResponse(GettingTestCaseIdsResponse gettingTestCaseIdsResponse) throws ValidationException {
        if (gettingTestCaseIdsResponse.getTestCases().isEmpty()) {
            String names = gettingTestCaseIdsResponse.getNotFoundTcNames().stream().collect(Collectors.joining(", "));
            throw new ValidationException("Could not validate parameter with Bulk Validator. BV test case: \""
                    + names + "\" didn't find in BV");
        }
    }

    /**
     * Check BvValidations testCases on variables, get testCaseIds by name and set found id in BvValidations
     * testCasesId.
     *
     * @return notFoundNames.
     */
    public List<String> getNotFoundTestCasesByName(UUID bvProjectId, BulkValidatorValidation bvValidation,
                                                   ConcurrentHashMap<String, ExecutionVariable> variables)
            throws ValidationException {
        try {
            List<String> testCases =
                    executionVariablesService.getTestCaseNamesFromVariable(bvValidation.getTestCaseIds(),
                            variables);
            List<String> testCasesNames = testCases.stream().filter(testCase -> !testCase
                    .matches(UUID_REGEXP_MATCHER)).collect(Collectors.toList());
            if (!testCasesNames.isEmpty()) {
                GettingTestCaseIdsResponse gettingTestCaseIdsResponse =
                        bulkValidatorRepository.getTestCaseIds(bvProjectId, testCasesNames);
                gettingTestCaseIdsResponse.getTestCases().values().forEach(testCases::addAll);
                testCases.removeAll(testCasesNames);
                bvValidation.setTestCaseIds(testCases);
                if (!gettingTestCaseIdsResponse.getNotFoundTcNames().isEmpty()) {
                    return gettingTestCaseIdsResponse.getNotFoundTcNames();
                }
            }
            bvValidation.setTestCaseIds(testCases);
            return null;
        } catch (VariableException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    /**
     * Validate data with Bulk Validator by next steps:
     * 1) Creation test runs for current row with Bulk Validator
     * 2) Run comparing process for all test runs
     * 3) Fill differences from comparing process responses.
     *
     * @param bvProjectId - project ID in Bulk Validator
     * @param bvValidation - validation settings
     * @param validationData - data for validation with Bulk Validator
     * @param diffs - differences between actual and expected result
     *         in the format given by actualDiffPath param
     * @param <T> - type of validation data
     * @return Bulk Validator comparing process responses.
     */
    public <T> List<ComparingProcessResponse> validateDataWithBulkValidator(UUID bvProjectId,
                                                                            BulkValidatorValidation bvValidation,
                                                                            T validationData, List<DiffMessage> diffs,
                                                                            String arName, int rowIdx)
            throws ValidationException {

        List<ValidationObject> validationObjects = getListValidationObjects(validationData, bvValidation);
        try {
            List<UUID> testRunIds = createTestRunsForRow(bvProjectId, bvValidation.getTestCaseIds(), validationObjects);
            List<ComparingProcessResponse> comparingResponses = bulkValidatorRepository.compare(bvProjectId,
                    testRunIds);
            fillDiffsFromComparingResponses(validationData, bvValidation, validationObjects,
                    comparingResponses, diffs, arName, rowIdx);
            return comparingResponses;
        } catch (RuntimeException e) {
            throw new ValidationException("Could not validate data: " + validationData
                    + " with Bulk Validator with settings: " + bvValidation + "\n message: " + e.getMessage(), e);
        }
    }

    /**
     * Creates Test Runs in Bulk Validator for row with validation data.
     *
     * @return ids of created test runs.
     */
    private List<UUID> createTestRunsForRow(UUID bvProjectId, List<String> testCaseIds,
                                            List<ValidationObject> validationObjects) {
        return testCaseIds.stream()
                .map(testCaseId ->
                        bulkValidatorRepository.createTestRun(bvProjectId, UUID.fromString(testCaseId),
                                        validationObjects)
                                .getTrId())
                .collect(Collectors.toList());
    }

    private <T> void fillDiffsFromComparingResponses(T validationData, BulkValidatorValidation bvValidation,
                                                     List<ValidationObject> listValidationObjects,
                                                     List<ComparingProcessResponse> comparingResponses,
                                                     List<DiffMessage> diffs,
                                                     String arName, int rowIdx) {

        List<BulkValidatorValidationItem> validationItem = bvValidation.getValidationItem();
        for (int i = 0, validationItemSize = validationItem.size(); i < validationItemSize; i++) {
            BulkValidatorValidationItem bvItem = validationItem.get(i);
            String actualDiffPath = arName + "|" + rowIdx + "|" + getValidationColumn(validationData, bvItem);
            String arValue = listValidationObjects.get(i).getAr();
            fillDiffsFromComparing(diffs, comparingResponses, actualDiffPath, arValue);
        }
    }

    private void fillDiffsFromComparing(List<DiffMessage> diffs,
                                        List<ComparingProcessResponse> comparingProcessResponses,
                                        String actualDiffPath, String actualValue) {
        if (comparingProcessResponses.stream()
                .anyMatch(ComparingProcessResponse::nonIdenticalCompareResult)) {
            DiffMessage diff = new DiffMessage();
            diff.setActual(actualDiffPath);
            diff.setActualValue(actualValue);
            diff.setResult(ResultType.MODIFIED);
            diffs.add(diff);
        }
    }

    /**
     * Generates links to Test Run with validation status from Bulk Validator.
     *
     * @return list of {@link BulkValidatorTestRunInfo} with Bulk Validator Test Case
     *         name as link alias(name), link to Test Run and Validation Status.
     */
    public List<BulkValidatorTestRunInfo> getTestRunsInfo(String bvTestRunUrlPrefix,
                                                          List<ComparingProcessResponse> comparingResponses) {
        return comparingResponses.stream()
                .map(response -> {
                    String bvTestRunLink = bvTestRunUrlPrefix + response.getResultLink();
                    ValidationStatus status = response.nonIdenticalCompareResult()
                            ? ValidationStatus.FAILED : ValidationStatus.PASSED;
                    return new BulkValidatorTestRunInfo(response.getTcName(), bvTestRunLink, status);
                }).collect(Collectors.toList());
    }

    private <T> List<ValidationObject> getListValidationObjects(T data, BulkValidatorValidation bvValidation)
            throws ValidationException {
        if (data instanceof SearchThreadFindResult) {
            return getListValidationObjectsForIntegrationLog(data, bvValidation);
        } else {
            return getListValidationObjectsForTable(data, bvValidation);
        }
    }

    private <T> List<ValidationObject> getListValidationObjectsForIntegrationLog(T data,
                                                                                 BulkValidatorValidation bvValidation) {
        SearchThreadFindResult searchThreadResult = (SearchThreadFindResult) data;
        return bvValidation.getValidationItem().stream()
                .map(bvItem -> {
                    String arValue;
                    if (searchThreadResult.hasLogParameter(bvItem.getValidationColumn())) {
                        arValue = searchThreadResult.getParameters()
                                .get(bvItem.getValidationColumn())
                                .replaceAll("(?:\\\\\\\\r\\\\\\\\n|\\\\\\\\r|\\\\\\\\n)", "\n");
                    } else {
                        arValue = searchThreadResult.getMessageAsSingleString();
                    }
                    return new ValidationObject(bvItem.getValidationObjectName(), arValue);
                }).collect(Collectors.toList());
    }

    private <T> List<ValidationObject> getListValidationObjectsForTable(T data, BulkValidatorValidation bvValidation)
            throws ValidationException {
        if (!(data instanceof HashMap)) {
            throw new ValidationException("Could not get AR values for table row during bulk validator validation");
        }
        return bvValidation.getValidationItem().stream()
                .map(bvItem -> {
                    String arValue = ((HashMap<String, String>) data).get(bvItem.getValidationColumn()
                            .toUpperCase());
                    return new ValidationObject(bvItem.getValidationObjectName(), arValue);
                }).collect(Collectors.toList());
    }

    /**
     * Return the column name according to the implementation.
     */
    private <T> String getValidationColumn(T validationData, BulkValidatorValidationItem bvItem) {
        if (validationData instanceof SearchThreadFindResult) {
            // "LOG_MESSAGE" is alias for Log Message validation
            return ((SearchThreadFindResult) validationData).hasLogParameter(bvItem.getValidationColumn())
                    ? bvItem.getValidationColumn()
                    : "LOG_MESSAGE";
        } else {
            return bvItem.getValidationColumn().toUpperCase();
        }
    }
}
