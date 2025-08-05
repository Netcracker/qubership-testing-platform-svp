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
import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.model.ui.PageRequest;
import org.qubership.atp.svp.model.ui.UpdatePageOrderRequest;
import org.qubership.atp.svp.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RequestMapping("/pages")
@RestController()
@ControllerWithCatalogPrefix
public class PagesController /*implements PagesControllerApi*/ {

    private final PageService pageService;

    @Autowired
    public PagesController(PageService pageService) {
        this.pageService = pageService;
    }

    /**
     * Returns a list of all pages.
     */
    @PostMapping()
    @AuditAction(auditAction = "get pages name in folder {{#request.folder}}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.svp.utils.UserManagementEntities).VALIDATION_PAGE.getName(),"
            + "#projectId, 'READ')")
    public List<String> getPages(@PathVariable(value = "projectId") UUID projectId,
                                 @RequestBody() PageRequest request) {
        return pageService.getPagesList(projectId, request.getFolder());
    }

    /**
     * Returns the page by name.
     */
    @PostMapping("page")
    @AuditAction(auditAction = "get page {{#request.name}} in folder {{#request.name}}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.svp.utils.UserManagementEntities).VALIDATION_PAGE.getName(),"
            + "#projectId, 'READ')")
    public PageConfiguration getPage(@PathVariable(value = "projectId") UUID projectId,
                                     @RequestBody() PageRequest request) {
        return pageService.getPage(projectId, request.getName(), request.getFolder());
    }

    /**
     * Creates page configuration file. Returns name of the file.
     */
    @PostMapping("create-or-update")
    @AuditAction(auditAction = "Create or update page {{#request.name}} in folder {{#request.folder}}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.svp.utils.UserManagementEntities).VALIDATION_PAGE.getName(),"
            + "#projectId, 'UPDATE')")
    public ResponseEntity<Void> createOrUpdatePage(@PathVariable(value = "projectId") UUID projectId,
                                                   @RequestBody() PageRequest request) {
        pageService.createOrUpdatePage(projectId, request.getPage(), request.getOldPageName(), request.getName(),
                request.getFolder());
        return ResponseEntity.ok().build();
    }

    /**
     * Delete the page by name and folder.
     */
    @DeleteMapping("")
    @AuditAction(auditAction = "Delete page {{#request.name}} in folder {{#request.folder}}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.svp.utils.UserManagementEntities).VALIDATION_PAGE.getName(),"
            + "#projectId, 'DELETE')")
    public ResponseEntity<Void> deletePage(@PathVariable(value = "projectId") UUID projectId,
                                           @RequestBody() PageRequest request) {
        pageService.deletePage(projectId, request.getName(), request.getFolder());
        return ResponseEntity.ok().build();
    }

    /**
     * Move the page to the selected folder.
     */
    @PostMapping("move")
    @AuditAction(auditAction = "Move page {{#request.name}} from folder {{#request.folder}} "
            + "to {{#request.targetFolder}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"UPDATE\")")
    public ResponseEntity<Void> movePage(@PathVariable(value = "projectId") UUID projectId,
                                         @RequestBody() PageRequest request) {
        pageService.movePage(projectId, request.getName(), request.getFolder(), request.getTargetFolder());
        return ResponseEntity.ok().build();
    }

    /**
     * Copy the page to the selected folder.
     */
    @PostMapping("copy")
    @AuditAction(auditAction = "Copy page {{#request.name}} from folder {{#request.folder}} "
            + "to {{#request.targetFolder}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"UPDATE\")")
    public ResponseEntity<Void> copyPage(@PathVariable(value = "projectId") UUID projectId,
                                         @RequestBody() PageRequest request) {
        pageService.copyPage(projectId, request.getName(), request.getFolder(), request.getTargetFolder());
        return ResponseEntity.ok().build();
    }

    /**
     * Creates page configuration file. Returns name of the file.
     */
    @PostMapping("order")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.svp.utils.UserManagementEntities).VALIDATION_PAGE.getName(),"
            + "#projectId, 'UPDATE')")
    public ResponseEntity<Void> updatePageOrder(@PathVariable(value = "projectId") UUID projectId,
                                                @RequestBody() UpdatePageOrderRequest request) {
        pageService.updatePageOrder(projectId, request);
        return ResponseEntity.ok().build();
    }
}
