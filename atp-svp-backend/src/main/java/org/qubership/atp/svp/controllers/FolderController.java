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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.svp.annotations.ControllerWithCatalogPrefix;
import org.qubership.atp.svp.model.folder.FolderRequest;
import org.qubership.atp.svp.service.direct.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/folders")
@Slf4j
public class FolderController {

    private final FolderService folderService;

    @Autowired
    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    /**
     * Create new folder.
     */
    @PostMapping("/create")
    @AuditAction(auditAction = "Create folder with name {{#body.name}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"UPDATE\")")
    public ResponseEntity<Void> createFolder(@PathVariable(value = "projectId") UUID projectId,
                                             @RequestBody FolderRequest body) {
        folderService.createFolder(projectId, body.getName());
        return ResponseEntity.ok().build();
    }

    /**
     * Rename exist folder name.
     */
    @PostMapping()
    @AuditAction(auditAction = "Rename folder {{#body.oldName}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"UPDATE\")")
    public ResponseEntity<Void> editFolder(@PathVariable(value = "projectId") UUID projectId,
                                           @RequestBody FolderRequest body) {
        folderService.editFolder(projectId, body);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all folders for the project.
     */
    @GetMapping()
    @AuditAction(auditAction = "Get folders name")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"READ\")")
    public List<String> getFolder(@PathVariable(value = "projectId") UUID projectId) {
        return folderService.getFolders(projectId);
    }

    /**
     * Delete exist folder by name.
     */
    @DeleteMapping()
    @AuditAction(auditAction = "Delete folder {{#body.name}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"DELETE\")")
    public ResponseEntity<Void> deleteFolder(@PathVariable(value = "projectId") UUID projectId,
                                             @RequestBody FolderRequest body) {
        folderService.deleteFolder(projectId, body);
        return ResponseEntity.ok().build();
    }
}
