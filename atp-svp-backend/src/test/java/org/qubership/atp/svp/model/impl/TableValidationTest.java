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

package org.qubership.atp.svp.model.impl;

import org.junit.Assert;
import org.junit.Test;

import org.qubership.automation.pc.models.table.CheckColumnOperations;

public class TableValidationTest {

    private final String tableValidationValueMock = "value";
    private final String tableValidationColumnMock = "column";

    @Test
    public void composeCheckColumnRule_conditionIsNotNull_returnsRuleNameWithCondition() {
        TableValidation tableValidation = new TableValidation(tableValidationValueMock,
                tableValidationColumnMock, CheckColumnOperations.EQUALS);
        String erRuleName = tableValidationColumnMock + "="
                + CheckColumnOperations.EQUALS.toString() + "=" + tableValidationValueMock;

        String arRuleName = tableValidation.composeCheckColumnRule();

        Assert.assertEquals(erRuleName, arRuleName);
    }

    @Test
    public void composeCheckColumnRule_whenLineBreak() {
        TableValidation tableValidation = new TableValidation("[\\n \"true\" \\n]",
                tableValidationColumnMock, CheckColumnOperations.EQUALS);
        String erRuleName = tableValidationColumnMock + "="
                + CheckColumnOperations.EQUALS.toString() + "=" + "[\n \"true\" \n]";

        String arRuleName = tableValidation.composeCheckColumnRule();

        Assert.assertEquals(erRuleName, arRuleName);
    }

    @Test
    public void composeCheckColumnRule_conditionIsNull_returnsRuleNameWithEmptyCondition() {
        TableValidation tableValidation = new TableValidation(tableValidationValueMock,
                tableValidationColumnMock, null);
        String erRuleName = tableValidationColumnMock + "==" + tableValidationValueMock;

        String arRuleName = tableValidation.composeCheckColumnRule();

        Assert.assertEquals(erRuleName, arRuleName);
    }
}
