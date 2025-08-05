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

package org.qubership.atp.svp.service.direct;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class MetricsService {

    private Counter.Builder potRequestsCounter;
    private MeterRegistry meterRegistry;

    private static final String POT_REQUESTS_COUNTER_NAME = "atp.svp.pot.requests.total";
    private static final String VALIDATION_REQUESTS_TIMER = "atp.svp.page.validation.requests.duration";

    private static final String PROJECT_ID_TAG_NAME = "project_id";

    /**
     * The MetricsService constructor.
     */
    @Autowired
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.potRequestsCounter = Counter.builder(POT_REQUESTS_COUNTER_NAME)
                .description("total number of POT requests");
    }

    /**
     * increment pot request counter.
     * @param projectId project id
     */
    public void incrementPotRequestCounter(UUID projectId) {
        potRequestsCounter
                .tags(PROJECT_ID_TAG_NAME, projectId.toString())
                .register(meterRegistry)
                .increment();
    }

    /**
     * record validation request duration.
     * @param projectId project id
     * @param duration duration
     */
    public void recordValidationRequestDuration(Duration duration, UUID projectId) {
        String projectIdStr = "Unknown";
        if (Objects.nonNull(projectId)) {
            projectIdStr = projectId.toString();
        }
        meterRegistry.timer(VALIDATION_REQUESTS_TIMER, PROJECT_ID_TAG_NAME, projectIdStr).record(duration);
    }

}
