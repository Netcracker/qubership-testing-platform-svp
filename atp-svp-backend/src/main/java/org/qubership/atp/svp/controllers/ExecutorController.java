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

import java.io.ByteArrayInputStream;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.svp.annotations.ControllerWithCatalogPrefix;
import org.qubership.atp.svp.mdc.MdcField;
import org.qubership.atp.svp.model.api.GetInfoRequest;
import org.qubership.atp.svp.model.api.GetParameterResultRequest;
import org.qubership.atp.svp.model.api.ram.SessionDto;
import org.qubership.atp.svp.model.api.tsg.PreconfiguredValidation;
import org.qubership.atp.svp.model.messages.SutParameterResultMessage;
import org.qubership.atp.svp.model.pot.PotFile;
import org.qubership.atp.svp.service.ExecutorService;
import org.qubership.atp.svp.service.PotSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RestController
@ControllerWithCatalogPrefix
@RequestMapping("/executor")
@Slf4j
public class ExecutorController /*implements ExecutorControllerApi*/ {

    private PotSessionService potSessionService;
    private final ExecutorService executorService;

    @Autowired
    public ExecutorController(PotSessionService potSessionService, ExecutorService executorService) {
        this.potSessionService = potSessionService;
        this.executorService = executorService;
    }

    /**
     * Prepares and starts session which is stored in runtime. Returns object of new session.
     */
    @PostMapping("/start-session")
    @AuditAction(auditAction = "Get or create session for project: {{#projectId}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    public UUID getOrCreateSession(@PathVariable(value = "projectId") UUID projectId,
                                   @RequestBody GetInfoRequest request) {
        MdcUtils.put(MdcField.ENVIRONMENT_ID.toString(), request.getEnvironmentId());
        MdcUtils.put(MdcField.SESSION_ID.toString(), request.getSessionId());
        return executorService.getOrCreateSession(projectId, request, false);
    }

    /**
     * Generates zip or doc with POT and returns stream to download it.
     */
    @PostMapping(value = "/get-pot")
    @AuditAction(auditAction = "Generate POT document for session: {{#request.sessionId}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    public ResponseEntity<Resource> getPotReportForSession(@PathVariable(value = "projectId") UUID projectId,
                                                           @RequestBody GetInfoRequest request) {
        MdcUtils.put(MdcField.ENVIRONMENT_ID.toString(), request.getEnvironmentId());
        MdcUtils.put(MdcField.SESSION_ID.toString(), request.getSessionId());
        PotFile potFile = executorService.getPotReportForSession(request.getSessionId());
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(potFile.getBytes()));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + potFile.getName())
                .body(resource);
    }

    /**
     * Gets info for selected pages and common parameters.
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "getInfo for session : {{#request.sessionId}}")
    @PostMapping("/get-info")
    public ResponseEntity<Void> getInfo(@PathVariable(value = "projectId") UUID projectId,
                                        @RequestBody GetInfoRequest request) {
        MdcUtils.put(MdcField.ENVIRONMENT_ID.toString(), request.getEnvironmentId());
        MdcUtils.put(MdcField.SESSION_ID.toString(), request.getSessionId());
        executorService.getInfo(projectId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Synchronize getting info and generation POT for selected pages
     * Generates zip or doc with POT and returns stream to download it.
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "Synchronize get POT for project: {{#projectId}}")
    @PostMapping("/get-pot-sync")
    public ResponseEntity<Resource> getPotSync(@PathVariable(value = "projectId") UUID projectId,
                                               @RequestBody GetInfoRequest request) {
        MdcUtils.put(MdcField.ENVIRONMENT_ID.toString(), request.getEnvironmentId());
        MdcUtils.put(MdcField.SESSION_ID.toString(), request.getSessionId());
        PotFile potFile = executorService.getPotSync(projectId, request);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(potFile.getBytes()));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + potFile.getName())
                .body(resource);
    }

    /**
     * Synchronize getting info and generation session result for selected pages.
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "Ram get info for project: {{#projectId}}")
    @PostMapping("/get-info-ram")
    public SessionDto getInfoSync(@PathVariable(value = "projectId") UUID projectId,
                                  @RequestBody GetInfoRequest request) {
        MdcUtils.put(MdcField.ENVIRONMENT_ID.toString(), request.getEnvironmentId());
        MdcUtils.put(MdcField.SESSION_ID.toString(), request.getSessionId());
        return executorService.getInfoSessionDtoRam(projectId, request);
    }

    /**
     * Gets common parameters.
     */
    @PostMapping("/get-common-parameters")
    @AuditAction(auditAction = "Common parameters get info for project: {{#projectId}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    public ResponseEntity<Void> getCommonParameters(@PathVariable(value = "projectId") UUID projectId,
                                                    @RequestBody GetInfoRequest request) {
        MdcUtils.put(MdcField.ENVIRONMENT_ID.toString(), request.getEnvironmentId());
        MdcUtils.put(MdcField.SESSION_ID.toString(), request.getSessionId());
        executorService.getInfoForCommonParameters(projectId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/get-parameter-result")
    @AuditAction(auditAction = "Get parameter result for session:  {{#request.sessionId}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    public SutParameterResultMessage getParameterResult(@PathVariable(value = "projectId") UUID projectId,
                                                        @RequestBody GetParameterResultRequest request) {
        MdcUtils.put(MdcField.SESSION_ID.toString(), request.getSessionId());
        return new SutParameterResultMessage(request.getSessionId(), potSessionService.getParameterResult(request));
    }

    /**
     * get info for preconfigured validation pages.
     * @return a {@link PreconfiguredValidation}.
     */
    @PostMapping("/get-validations")
    @AuditAction(auditAction = "Get Validations for project: {{#projectId}}")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    public PreconfiguredValidation getValidations(@PathVariable(value = "projectId") UUID projectId,
                                                  @RequestBody GetInfoRequest request) {
        MdcUtils.put(MdcField.ENVIRONMENT_ID.toString(), request.getEnvironmentId());
        MdcUtils.put(MdcField.SESSION_ID.toString(), request.getSessionId());
        return executorService.getPreconfiguredValidationResults(projectId, request);
    }
}
