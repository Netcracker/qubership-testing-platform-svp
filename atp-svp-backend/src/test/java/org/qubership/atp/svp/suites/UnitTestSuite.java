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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.qubership.atp.svp.kafka.ProjectEventKafkaListenerUnitTest;
import org.qubership.atp.svp.kafka.SvpKafkaListenerTest;
import org.qubership.atp.svp.migration.ProjectMigrationToDataBaseServiceTest;
import org.qubership.atp.svp.model.api.tsg.PreconfiguredValidationTest;
import org.qubership.atp.svp.model.bulkvalidator.ComparingProcessRequestTest;
import org.qubership.atp.svp.model.bulkvalidator.ComparingProcessResponseTest;
import org.qubership.atp.svp.model.impl.SourceTest;
import org.qubership.atp.svp.model.impl.TableValidationTest;
import org.qubership.atp.svp.repo.impl.FilePageConfigurationRepositoryTest;
import org.qubership.atp.svp.repo.impl.SqlRepositoryTest;
import org.qubership.atp.svp.repo.impl.pool.DiffServersConnectionPoolTest;
import org.qubership.atp.svp.service.DefaultDisplayTypeServiceTest;
import org.qubership.atp.svp.service.ProjectConfigServiceTest;
import org.qubership.atp.svp.service.direct.DeferredSearchServiceImplTest;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImplTest;
import org.qubership.atp.svp.service.direct.ExecutorServiceImplTest;
import org.qubership.atp.svp.service.direct.GitProjectServiceImplTest;
import org.qubership.atp.svp.service.direct.PotSessionServiceImplTest;
import org.qubership.atp.svp.service.direct.SessionDtoProcessorServiceTest;
import org.qubership.atp.svp.service.direct.SessionServiceImplTest;
import org.qubership.atp.svp.service.direct.WebSocketMessagingServiceTest;
import org.qubership.atp.svp.service.direct.displaytype.IntegrationLogDisplayTypeServiceImplTest;
import org.qubership.atp.svp.service.direct.displaytype.JsonDisplayTypeServiceImplTest;
import org.qubership.atp.svp.service.direct.displaytype.LinkDisplayTypeServiceImplTest;
import org.qubership.atp.svp.service.direct.displaytype.ParamDisplayTypeServiceImplTest;
import org.qubership.atp.svp.service.direct.displaytype.TableDisplayTypeServiceImplTest;
import org.qubership.atp.svp.service.direct.displaytype.XmlDisplayTypeServiceImplTest;
import org.qubership.atp.svp.service.direct.validation.ValidationServiceImplPageValidationParametrizedTest;
import org.qubership.atp.svp.service.direct.validation.ValidationServiceImplSessionValidationParametrizedTest;
import org.qubership.atp.svp.service.direct.validation.ValidationServiceImplTabValidationParametrizedTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        PreconfiguredValidationTest.class,
        ComparingProcessRequestTest.class,
        ComparingProcessResponseTest.class,
        SourceTest.class,
        TableValidationTest.class,
        DiffServersConnectionPoolTest.class,
        FilePageConfigurationRepositoryTest.class,
        SqlRepositoryTest.class,
        IntegrationLogDisplayTypeServiceImplTest.class,
        JsonDisplayTypeServiceImplTest.class,
        LinkDisplayTypeServiceImplTest.class,
        ParamDisplayTypeServiceImplTest.class,
        TableDisplayTypeServiceImplTest.class,
        XmlDisplayTypeServiceImplTest.class,
        ValidationServiceImplTabValidationParametrizedTest.class,
        ValidationServiceImplPageValidationParametrizedTest.class,
        ValidationServiceImplSessionValidationParametrizedTest.class,
        ExecutorServiceImplTest.class,
        DeferredSearchServiceImplTest.class,
        GitProjectServiceImplTest.class,
        PotSessionServiceImplTest.class,
        DefaultDisplayTypeServiceTest.class,
        ProjectConfigServiceTest.class,
        SessionDtoProcessorServiceTest.class,
        ProjectMigrationToDataBaseServiceTest.class,
        SessionServiceImplTest.class,
        WebSocketMessagingServiceTest.class,
        SvpKafkaListenerTest.class,
        ProjectEventKafkaListenerUnitTest.class,
        ExecutionVariablesServiceImplTest.class
})
public class UnitTestSuite {

}
