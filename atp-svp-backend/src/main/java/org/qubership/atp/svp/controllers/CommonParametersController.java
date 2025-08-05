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
import org.qubership.atp.svp.model.impl.SutParameter;
import org.qubership.atp.svp.model.ui.CommonParameterRequest;
import org.qubership.atp.svp.service.CommonParametersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RestController
@ControllerWithCatalogPrefix
@RequestMapping("/common-parameters")
@Slf4j
public class CommonParametersController /*implements CommonParametersControllerApi*/ {

    CommonParametersService commonParametersService;

    /**
     * Constructor for CommonParametersController.
     */
    @Autowired
    public CommonParametersController(CommonParametersService commonParametersService) {
        this.commonParametersService = commonParametersService;
    }

    /**
     * Get list {@link SutParameter}.
     * @param request body which have folder name
     */
    @PostMapping()
    @AuditAction(auditAction = "Get common parameters in folder {{#request.folder}}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.svp.utils.UserManagementEntities).PARAMETER.getName(), #projectId, 'READ')")
    public List<SutParameter> getCommonParameters(@PathVariable(value = "projectId") UUID projectId,
                                                  @RequestBody() CommonParameterRequest request) {
        return commonParametersService.getCommonParameters(projectId, request.getFolder());
    }

    /**
     * Update common parameters file.
     *
     * @param request body which have {@link SutParameter} and folder name
     */
    @PostMapping("update")
    @AuditAction(auditAction = "Update common parameters in folder {{#request.folder}}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.svp.utils.UserManagementEntities).PARAMETER.getName(), #projectId, 'UPDATE')")
    public ResponseEntity<Void> updateCommonParameters(@PathVariable(value = "projectId") UUID projectId,
                                                       @RequestBody() CommonParameterRequest request) {
        commonParametersService.createOrUpdateCommonParameters(projectId, request.getCommonParameters(),
                request.getFolder());
        return ResponseEntity.ok().build();
    }
}
