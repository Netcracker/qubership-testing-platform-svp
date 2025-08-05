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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.svp.config.BeanAwareSpringLiquibase;
import org.qubership.atp.svp.migration.ProjectMigrationToDataBaseService;
import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.service.direct.GitProjectServiceImpl;
import org.qubership.atp.svp.service.direct.ProjectConfigService;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitProjectsConfigMigrationCustomChange implements CustomTaskChange {

    private JdbcConnection connection;

    @SneakyThrows
    @Override
    public void execute(Database database) throws CustomChangeException {
        connection = (JdbcConnection) database.getConnection();
        removeLocalProjects();
        GitProjectServiceImpl gitProjectService = BeanAwareSpringLiquibase.getBean(GitProjectServiceImpl.class);
        ProjectConfigService projectConfigService = BeanAwareSpringLiquibase.getBean(ProjectConfigService.class);
        ProjectMigrationToDataBaseService projectMigrationToDataBaseService =
                BeanAwareSpringLiquibase.getBean(ProjectMigrationToDataBaseService.class);
        List<ProjectConfiguration> projectConfigs = projectConfigService.getAllTypesProjectsConfigs();
        gitProjectService.reloadAllGitProjects(projectConfigs, true);
        projectMigrationToDataBaseService.projectsJsonMigrateToBase(projectConfigs);
    }

    private void removeLocalProjects() throws DatabaseException, SQLException {
        List<UUID> projects = new ArrayList<>();
        ResultSet rs = getLocalProjects();
        while (rs.next()) {
            UUID projectId = UUID.fromString(rs.getString(1));
            String pathToFolder = rs.getString(2);
            Path path = Paths.get(pathToFolder);
            if (Files.exists(path)) {
                projects.add(projectId);
            }
        }
        if (!projects.isEmpty()) {
            deleteProjects(projects);
        }
    }

    private ResultSet getLocalProjects() throws SQLException, DatabaseException {
        String nativeQuery = "select project_id, path_folder_local_project "
                + "from project_configs pc "
                + "where pages_source_type = 'LOCAL' "
                + "and path_folder_local_project notnull";
        PreparedStatement stmt = connection.prepareStatement(nativeQuery);
        return stmt.executeQuery();
    }

    private void deleteProjects(List<UUID> projects) throws DatabaseException, SQLException {
        if (projects.isEmpty()) {
            return;
        }
        // Construct a parameterized query with placeholders
        String query = "DELETE FROM folders WHERE project_id IN ("
                + projects.stream().map(p -> "?").collect(Collectors.joining(", ")) + ")";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            // Ensures safe execution of the query with proper resource management
            for (int i = 0; i < projects.size(); i++) {
                stmt.setObject(i + 1, projects.get(i));
            }
            stmt.executeUpdate();
            connection.commit();
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
