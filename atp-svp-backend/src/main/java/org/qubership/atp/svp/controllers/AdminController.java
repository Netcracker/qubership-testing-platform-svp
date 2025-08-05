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

package org.qubership.atp.svp.controllers;

import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.svp.annotations.ControllerWithCatalogPrefix;
import org.qubership.atp.svp.migration.ProjectMigrationToDataBaseService;
import org.qubership.atp.svp.model.ui.ProjectConfigResponseModel;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RestController
@ControllerWithCatalogPrefix
@RequestMapping("/admin")
@Slf4j
public class AdminController /*implements AdminControllerApi*/ {

    private final ProjectConfigService projectConfigService;
    private final ProjectMigrationToDataBaseService projectMigrationToDataBaseService;

    @Autowired
    public AdminController(ProjectConfigService projectConfigService,
                           ProjectMigrationToDataBaseService projectMigrationToDataBaseService) {
        this.projectConfigService = projectConfigService;
        this.projectMigrationToDataBaseService = projectMigrationToDataBaseService;
    }

    @GetMapping("/project-configuration")
    @AuditAction(auditAction = "Get project config")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"READ\")")
    public ResponseEntity<ProjectConfigResponseModel> getProjectConfig(@PathVariable(value = "projectId")
                                                                       UUID projectId) {
        ProjectConfigResponseModel projectConfig = projectConfigService.getProjectConfigByIdForResponse(projectId);
        return ResponseEntity.ok(projectConfig);
    }

    /**
     * Add or update ProjectConfig and reload Git Project.
     */
    @PostMapping("/project-configuration")
    @AuditAction(auditAction = "Update project config")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"UPDATE\")")
    public ResponseEntity<Void> updateProjectConfig(@PathVariable(value = "projectId") UUID projectId,
                                                    @RequestBody ProjectConfigResponseModel projectConfigResponse) {
        projectMigrationToDataBaseService.updateProjectConfigAndMigrate(projectConfigResponse, projectId);
        return ResponseEntity.ok().build();
    }
}
