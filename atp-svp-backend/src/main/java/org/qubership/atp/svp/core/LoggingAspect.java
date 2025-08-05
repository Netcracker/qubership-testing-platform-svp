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

package org.qubership.atp.svp.core;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Aspect
@Slf4j
public class LoggingAspect {

    /**
     * Logs the beginning of each method.
     */
    @Before("execution(* org.qubership.atp.svp..*.*(..))")
    public void loggingStartMethod(JoinPoint joinPoint) {
        log.debug("[METHOD START] " + getMethodInfo(joinPoint));
    }

    /**
     * Logs the end of each method.
     */
    @After("execution(* org.qubership.atp.svp..*.*(..))")
    public void loggingEndMethod(JoinPoint joinPoint) {
        log.debug("[METHOD END] " + getMethodInfo(joinPoint));
    }

    private String getMethodInfo(JoinPoint joinPoint) {
        return joinPoint.getSourceLocation().getWithinType().getSimpleName() + " - "
                + joinPoint.getSignature().getName() + " - "
                + Arrays.toString(joinPoint.getArgs());
    }
}
