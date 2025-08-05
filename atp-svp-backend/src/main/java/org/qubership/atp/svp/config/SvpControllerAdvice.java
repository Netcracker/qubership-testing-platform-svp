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

package org.qubership.atp.svp.config;

import org.qubership.atp.svp.model.api.DefaultExceptionMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class SvpControllerAdvice {

    /**
     * Handle AccessDeniedException exception with saved headers.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    protected ResponseEntity<Object> handleException(AccessDeniedException ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Access Denied: " + ex.getMessage());
    }

    private ResponseEntity<DefaultExceptionMessage> defaultExceptionResponse(Throwable ex, int status) {
        log.error("Exception appeared on controller!", ex);
        return new ResponseEntity<>(new DefaultExceptionMessage(ex.getMessage()), HttpStatus.valueOf(status));
    }
}
