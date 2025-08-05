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

package org.qubership.atp.svp.service;

import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;

public interface DeferredSearchService {

    Optional<AbstractParameterExecutionContext> findContextByRequestSearchId(UUID requestSearchId);

    void storeContextByRequestSearchId(UUID searchId, AbstractParameterExecutionContext context);

    void evictContextByRequestSearchId(UUID requestSearchId);

    void killAllDeferredSearchResultsByExpiredSessionId(UUID sessionId);

    void killExpiredDeferredSearchResults();

    void killDeferredSearchResult(UUID requestSearchId, String messageAboutExpiring);
}
