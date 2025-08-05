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

package org.qubership.atp.svp.suites;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.qubership.atp.svp.controllers.ExecutorControllerMockedIntegrationTest;
import org.qubership.atp.svp.kafka.LogCollectorEventKafkaListenerTest;
import org.qubership.atp.svp.kafka.ProjectEventKafkaListenerTest;

// TODO It is necessary to implement the performance of tests on CI/CD.
@Ignore("Ignored because tests do not work on CI/CD. Work locally with Docker installed.")
@RunWith(Suite.class)
@Suite.SuiteClasses({
        LogCollectorEventKafkaListenerTest.class,
        ProjectEventKafkaListenerTest.class,
        ExecutorControllerMockedIntegrationTest.class
})
public class KafkaTestSuite {

}
