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

package org.qubership.atp.svp.model.events;

import java.util.UUID;

import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ReloadSutParameterEvent extends ParameterEvent {

    private UUID requestId;

    @Builder
    public ReloadSutParameterEvent(@NonNull UUID requestId,
                                   @NonNull AbstractParameterExecutionContext parameterExecutionContext) {
        super(parameterExecutionContext);
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "ReloadSutParameterEvent{"
                + "RequestSearchId=" + requestId
                + "parameterExecutionContext=" + super.getParameterExecutionContext()
                + '}';
    }
}
