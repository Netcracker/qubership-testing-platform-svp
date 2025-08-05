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

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Value("${svp.getting.info.thread.pool.core.size}")
    private int gettingInfoThreadPoolCoreSize;
    @Value("${svp.getting.info.thread.pool.max.size}")
    private int gettingInfoThreadPoolMaxSize;
    @Value("${svp.getting.info.thread.pool.queue.capacity}")
    private int gettingInfoThreadPoolQueueCapacity;
    @Value("${svp.validation.thread.pool.core.size}")
    private int validationThreadPoolCoreSize;
    @Value("${svp.validation.thread.pool.max.size}")
    private int validationThreadPoolMaxSize;
    @Value("${svp.validation.thread.pool.queue.capacity}")
    private int validationThreadPoolQueueCapacity;

    /**
     * Custom async task executor for getting info process.
     *
     * @return TaskExecutor instance.
     */
    @Bean("GettingInfoProcessExecutor")
    public TaskExecutor getAsyncExecutorForGettingInfoProcess() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(gettingInfoThreadPoolCoreSize);
        executor.setMaxPoolSize(gettingInfoThreadPoolMaxSize);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setQueueCapacity(gettingInfoThreadPoolQueueCapacity);
        executor.setThreadNamePrefix("Async-GettingInfoProcessExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * Custom async task executor for validation process.
     *
     * @return TaskExecutor instance.
     */
    @Bean("ValidationProcessExecutor")
    public TaskExecutor getAsyncExecutorForValidationProcess() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(validationThreadPoolCoreSize);
        executor.setMaxPoolSize(validationThreadPoolMaxSize);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setQueueCapacity(validationThreadPoolQueueCapacity);
        executor.setThreadNamePrefix("Async-ValidationProcessExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
