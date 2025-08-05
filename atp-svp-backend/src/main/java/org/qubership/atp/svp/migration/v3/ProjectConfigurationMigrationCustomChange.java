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

package org.qubership.atp.svp.migration.v3;

import org.qubership.atp.svp.config.BeanAwareSpringLiquibase;
import org.qubership.atp.svp.core.exceptions.ProjectConfigException;
import org.qubership.atp.svp.migration.ProjectMigrationToDataBaseService;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProjectConfigurationMigrationCustomChange implements CustomTaskChange {

    @SneakyThrows
    @Override
    public void execute(Database database) throws CustomChangeException {
        ProjectMigrationToDataBaseService projectMigrationToDataBaseService
                = BeanAwareSpringLiquibase.getBean(ProjectMigrationToDataBaseService.class);
        try {
            projectMigrationToDataBaseService.migrateAllProjectConfigsFromJsonFile();
        } catch (ProjectConfigException e) {
            log.error("Unexpected error in ProjectConfigurationMigrationCustomChange", e);
        }
    }

    @Override
    public String getConfirmationMessage() {
        return null;
    }

    @Override
    public void setUp() throws SetupException {

    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {

    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }
}
