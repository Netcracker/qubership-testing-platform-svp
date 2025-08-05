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

package org.qubership.atp.svp.ei.executor;

import java.nio.file.Path;

import org.qubership.atp.ei.node.ImportExecutor;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.svp.ei.service.AtpImportStrategy;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtpSvpImportExecutor implements ImportExecutor {

    private final AtpImportStrategy atpImportStrategy;


    @Override
    public void importData(ExportImportData exportImportData, Path path) throws Exception {
        log.info("Request for import with data: {}", exportImportData);
        atpImportStrategy.svpImport(exportImportData, path);
    }

    @Override
    public ValidationResult preValidateData(ExportImportData importData, Path workDir) throws Exception {
        return null;
    }

    @Override
    public ValidationResult validateData(ExportImportData importData, Path workDir) throws Exception {
        return atpImportStrategy.validateData(importData, workDir);
    }
}
