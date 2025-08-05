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

package org.qubership.atp.svp.migration.v4;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.svp.config.BeanAwareSpringLiquibase;
import org.qubership.atp.svp.service.jpa.PageConfigurationServiceJpa;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigPageEntityMigrationCustomChange implements CustomTaskChange {

    private PageConfigurationServiceJpa pageConfigurationServiceJpa;
    private JdbcConnection connection;

    @SneakyThrows
    @Override
    public void execute(Database database) {
        pageConfigurationServiceJpa = BeanAwareSpringLiquibase.getBean(PageConfigurationServiceJpa.class);
        connection = (JdbcConnection) database.getConnection();

        ResultSet rs = getPageConfig();
        processParameters(rs);

        connection.commit();
    }

    private void processParameters(ResultSet rs) throws SQLException {
        Map<UUID, Set<UUID>> listValues = new HashMap<>();

        while (rs.next()) {
            UUID folderId = UUID.fromString(rs.getString(2));
            UUID pageId = UUID.fromString(rs.getString(1));

            if (listValues.containsKey(folderId)) {
                Set<UUID> set = listValues.get(folderId);
                set.add(pageId);
                listValues.replace(folderId, set);
            } else {
                listValues.put(folderId, new HashSet<>(Collections.singleton(pageId)));
            }
        }

        listValues.forEach((k, v) -> {
            int order = 0;
            for (UUID pageId : v) {
                try {
                    updateParameter(order, pageId);
                } catch (DatabaseException e) {
                    throw new RuntimeException(e);
                } catch (SQLException e) {
                    log.error("Can not migrate page with pageId: {} and order {}", pageId, order, e);
                    throw new RuntimeException(e);
                }
                order++;
            }
        });
    }


    private void updateParameter(int order, UUID pageId)
            throws DatabaseException, SQLException {
        String updateParameter = String.format("UPDATE public.page_configs SET \"order\"='%s' "
                + "WHERE page_id='%s'::uuid;", order, pageId);
        PreparedStatement stmt2 = connection.prepareStatement(updateParameter);
        stmt2.execute();
    }


    private ResultSet getPageConfig() throws DatabaseException, SQLException {
        String nativeQuery = "SELECT page_id, folder_id FROM public.page_configs;";
        PreparedStatement stmt = connection.prepareStatement(nativeQuery);
        return stmt.executeQuery();
    }

    @Override
    public String getConfirmationMessage() {
        return null;
    }

    @Override
    public void setUp() {

    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {

    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }
}
