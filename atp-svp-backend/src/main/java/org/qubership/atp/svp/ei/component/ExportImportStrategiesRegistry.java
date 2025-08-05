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

package org.qubership.atp.svp.ei.component;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.svp.core.exceptions.ei.SvpExportTypeNotSupportException;
import org.qubership.atp.svp.core.exceptions.ei.SvpImportTypeNotSupportException;
import org.qubership.atp.svp.ei.service.ExportStrategy;
import org.qubership.atp.svp.ei.service.ImportStrategy;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExportImportStrategiesRegistry {

    private final List<ExportStrategy> exportStrategies;
    private final List<ImportStrategy> importStrategies;

    /**
     * Lookup export strategy by export tool type and request transport type parameters.
     *
     * @param format export format
     * @return export strategy implementation
     */
    public ExportStrategy getExportStrategy(@NotNull ExportFormat format) throws SvpExportTypeNotSupportException {
        return exportStrategies.stream()
                .filter(exportStrategy -> format.equals(exportStrategy.getFormat()))
                .findFirst()
                .orElseThrow(() -> new SvpExportTypeNotSupportException(format.name()));
    }

    /**
     * Lookup export strategy by import tool type and request transport type parameters.
     *
     * @param format export format
     * @return import strategy implementation
     */
    public ImportStrategy getImportStrategy(@NotNull ExportFormat format) throws SvpImportTypeNotSupportException {
        return importStrategies.stream()
                .filter(exportStrategy -> format.equals(exportStrategy.getFormat()))
                .findFirst()
                .orElseThrow(() -> new SvpImportTypeNotSupportException(format.name()));
    }
}
