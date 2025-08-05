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

package org.qubership.atp.svp.repo.impl;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.svp.core.exceptions.InvalidBulkValidatorApiUsageException;
import org.qubership.atp.svp.model.bulkvalidator.ComparingProcessRequest;
import org.qubership.atp.svp.model.bulkvalidator.ComparingProcessResponse;
import org.qubership.atp.svp.model.bulkvalidator.GettingTestCaseIdsRequest;
import org.qubership.atp.svp.model.bulkvalidator.GettingTestCaseIdsResponse;
import org.qubership.atp.svp.model.bulkvalidator.JsonCompareRequest;
import org.qubership.atp.svp.model.bulkvalidator.JsonCompareResponse;
import org.qubership.atp.svp.model.bulkvalidator.TestRunCreationRequest;
import org.qubership.atp.svp.model.bulkvalidator.TestRunCreationResponse;
import org.qubership.atp.svp.model.bulkvalidator.ValidationObject;
import org.qubership.atp.svp.repo.feign.BulkValidatorApiFeignClient;
import org.qubership.atp.svp.repo.feign.BulkValidatorPublicFeignClient;
import org.qubership.atp.svp.repo.feign.BulkValidatorValidatorFeignClient;
import org.qubership.atp.svp.utils.DtoConvertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class BulkValidatorRepository {

    private final BulkValidatorPublicFeignClient bulkValidatorPublicFeignClient;
    private final DtoConvertService dtoConvertService;
    private final BulkValidatorApiFeignClient bulkValidatorApiFeignClient;
    private final BulkValidatorValidatorFeignClient bulkValidatorValidatorFeignClient;

    /**
     * Constructor for class.
     */
    @Autowired
    public BulkValidatorRepository(BulkValidatorPublicFeignClient bulkValidatorPublicFeignClient,
                                   DtoConvertService dtoConvertService,
                                   BulkValidatorApiFeignClient bulkValidatorApiFeignClient,
                                   BulkValidatorValidatorFeignClient bulkValidatorValidatorFeignClient) {
        this.bulkValidatorPublicFeignClient = bulkValidatorPublicFeignClient;
        this.dtoConvertService = dtoConvertService;
        this.bulkValidatorApiFeignClient = bulkValidatorApiFeignClient;
        this.bulkValidatorValidatorFeignClient = bulkValidatorValidatorFeignClient;
    }

    /**
     * Creates test run in Bulk Validator.
     */
    public TestRunCreationResponse createTestRun(UUID bvProjectId, UUID testCaseId,
                                                 List<ValidationObject> validationObjects) {
        try {
            TestRunCreationRequest request = new TestRunCreationRequest(testCaseId, validationObjects);
            String body = new GsonBuilder().create().toJson(request);
            return dtoConvertService.convert(bulkValidatorPublicFeignClient
                    .createTr(bvProjectId, body).getBody(), TestRunCreationResponse.class);
        } catch (Exception e) {
            String errorMessage = "Failed to create test run in Bulk Validator " + e.getMessage();
            log.error(errorMessage, e);
            throw new InvalidBulkValidatorApiUsageException(errorMessage, e);
        }
    }

    /**
     * Runs comparing process for test runs in Bulk Validator.
     */
    public List<ComparingProcessResponse> compare(UUID bvProjectId, List<UUID> testRunIds) {
        try {
            ComparingProcessRequest request = ComparingProcessRequest.createRequestForTestRuns(testRunIds);
            String body = new GsonBuilder().create().toJson(request);
            String response = bulkValidatorApiFeignClient.compare(bvProjectId, body).getBody();
            return Arrays.asList(dtoConvertService.convertFromString(response, ComparingProcessResponse[].class));
        } catch (Exception e) {
            String errorMessage = "Failed to perform comparing process in Bulk Validator " + e.getMessage();
            log.error(errorMessage, e);
            throw new InvalidBulkValidatorApiUsageException(errorMessage, e);
        }
    }

    /**
     * get result of compare Json in Bulk Validator.
     */
    public JsonCompareResponse getHighlightJson(UUID tcId, UUID trId, UUID objectIds, UUID bvProjectId) {
        try {
            JsonCompareRequest request = JsonCompareRequest.createRequestForGettingHighlight(tcId, trId, objectIds);
            String body = new GsonBuilder().create().toJson(request);
            String response = bulkValidatorValidatorFeignClient.highlightByIds(bvProjectId, body).getBody();
            return dtoConvertService.convertFromString(response, JsonCompareResponse.class);
        } catch (Exception e) {
            String errorMessage = "Failed to get highlight in Bulk Validator" + e.getMessage();
            log.error(errorMessage, e);
            throw new InvalidBulkValidatorApiUsageException(errorMessage, e);
        }
    }

    /**
     * get test case ids by names.
     */
    public GettingTestCaseIdsResponse getTestCaseIds(UUID bvProjectId, List<String> names) {
        try {
            String body = new GsonBuilder().create().toJson(new GettingTestCaseIdsRequest(names));
            String response = new GsonBuilder().create()
                    .toJson(bulkValidatorPublicFeignClient.getTcIdsByTcNames(bvProjectId, body).getBody());
            return new Gson().fromJson(response, GettingTestCaseIdsResponse.class);
        } catch (Exception e) {
            String errorMessage = "Failed get TestCaseIds, bv public controller";
            log.error(errorMessage, e);
            throw new InvalidBulkValidatorApiUsageException(errorMessage, e);
        }
    }

}
