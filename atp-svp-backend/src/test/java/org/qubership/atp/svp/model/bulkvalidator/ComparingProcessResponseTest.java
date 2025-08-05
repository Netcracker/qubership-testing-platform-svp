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

package org.qubership.atp.svp.model.bulkvalidator;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.qubership.automation.pc.compareresult.ResultType;

@RunWith(Parameterized.class)
public class ComparingProcessResponseTest {

    private final ResultType bulkValidatorValidationResultType;
    private final boolean expectedNonIdenticalCompareResult;

    public ComparingProcessResponseTest(ResultType bulkValidatorValidationResultType,
                                        boolean expectedNonIdenticalCompareResult) {
        this.bulkValidatorValidationResultType = bulkValidatorValidationResultType;
        this.expectedNonIdenticalCompareResult = expectedNonIdenticalCompareResult;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> nonIdenticalFlagByResultType() {
        return Arrays.asList(new Object[][]{
                {ResultType.IDENTICAL, false},
                {ResultType.HIDDEN, true},
                {ResultType.SUCCESS, true},
                {ResultType.SKIPPED, true},
                {ResultType.IGNORED, true},
                {ResultType.PASSED, true},
                {ResultType.SKIPPED, true},
                {ResultType.BROKEN_STEP_INDEX, true},
                {ResultType.CHANGED, true},
                {ResultType.MODIFIED, true},
                {ResultType.AR_MISSED, true},
                {ResultType.ER_MISSED, true},
                {ResultType.EXTRA, true},
                {ResultType.MISSED, true},
                {ResultType.FAILED, true},
                {ResultType.ERROR, true}
        });
    }

    @Test
    public void nonIdenticalCompareResult_differentBulkValidatorValidationResultTypes_returnsCorrectFlag() {
        ComparingProcessResponse response = new ComparingProcessResponse();
        response.setCompareResult(bulkValidatorValidationResultType);

        Assert.assertEquals(expectedNonIdenticalCompareResult, response.nonIdenticalCompareResult());
    }
}
