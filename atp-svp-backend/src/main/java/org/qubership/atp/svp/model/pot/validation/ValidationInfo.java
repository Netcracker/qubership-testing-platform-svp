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

package org.qubership.atp.svp.model.pot.validation;

import org.qubership.atp.svp.core.enums.ValidationStatus;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "ActualTablesValidationInfo", value = ActualTablesValidationInfo.class),
        @JsonSubTypes.Type(name = "BulkValidatorJsonValidationInfo", value = BulkValidatorJsonValidationInfo.class),
        @JsonSubTypes.Type(name = "IntegrationLogsValidationInfo", value = IntegrationLogsValidationInfo.class),
        @JsonSubTypes.Type(name = "TableVsTableValidationInfo", value = TableVsTableValidationInfo.class),
})
public class ValidationInfo {
    private ValidationStatus status;
    private String errorDescription;

    public ValidationInfo(ValidationStatus status) {
        this.status = status;
    }

    public ValidationInfo(ValidationStatus status, String errorDescription) {
        this.status = status;
        this.errorDescription = errorDescription;
    }
}
