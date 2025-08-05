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

import org.assertj.core.util.Strings;
import org.qubership.atp.svp.core.exceptions.ValidationException;

import lombok.Data;

@Data
public class BulkValidatorJsonValidation {

    private String validationObjectName;
    private String testCaseId;

    /**
     * check validationObjectName and testCaseId on null and empty.
     */
    public void checkError() throws ValidationException {
        if (Strings.isNullOrEmpty(validationObjectName)
                || Strings.isNullOrEmpty(testCaseId)) {
            throw new ValidationException("Could not validate parameter with Bulk Validator. BV test case id or "
                    + "validation object name are not defined!");
        }
    }
}
