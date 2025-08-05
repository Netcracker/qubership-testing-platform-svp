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

import org.qubership.atp.svp.controllers.CommonParametersControllerTest;
import org.qubership.atp.svp.controllers.ExportCandidatesControllerTest;
import org.qubership.atp.svp.controllers.FolderControllerTest;
import org.qubership.atp.svp.controllers.IntegrationsControllerTest;
import org.qubership.atp.svp.controllers.KeyParametersControllerTest;
import org.qubership.atp.svp.controllers.PagesControllerTest;
import org.qubership.atp.svp.ei.ExportImportTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        PagesControllerTest.class,
        IntegrationsControllerTest.class,
        CommonParametersControllerTest.class,
        KeyParametersControllerTest.class,
        FolderControllerTest.class,
        ExportCandidatesControllerTest.class,
        ExportImportTest.class
})
public class IntegrationTestSuite {

}
