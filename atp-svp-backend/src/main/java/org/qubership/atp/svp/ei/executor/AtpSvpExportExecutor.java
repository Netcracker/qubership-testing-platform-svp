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

import org.qubership.atp.ei.node.ExportExecutor;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.exceptions.ExportException;
import org.qubership.atp.svp.ei.component.ExportImportStrategiesRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtpSvpExportExecutor implements ExportExecutor {

    private final ExportImportStrategiesRegistry exportStrategiesRegistry;

    @Value("${spring.application.name}")
    private String implementationName;

    @Override
    @Transactional
    public void exportToFolder(ExportImportData exportData, Path path) throws ExportException {
        log.info("Start export. Request {}", exportData);
        exportStrategiesRegistry.getExportStrategy(exportData.getFormat())
                .export(exportData, path);
        log.info("End export. Request {}", exportData);
    }

    @Override
    public String getExportImplementationName() {
        return implementationName;
    }
}
