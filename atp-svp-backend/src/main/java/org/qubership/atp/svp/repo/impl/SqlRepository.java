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

package org.qubership.atp.svp.repo.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.svp.core.exceptions.SqlScriptExecuteException;
import org.qubership.atp.svp.model.environments.DBServer;
import org.qubership.atp.svp.model.table.Table;
import org.qubership.atp.svp.repo.impl.pool.ConnectionPool;
import org.qubership.atp.svp.repo.impl.pool.DiffServerConnection;
import org.qubership.atp.svp.repo.impl.pool.DiffServersConnectionPool;
import org.qubership.atp.svp.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Repository;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import oracle.sql.CLOB;

/**
 * Taken from ATP MIA.
 */
@Repository
public class SqlRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlRepository.class);
    private static ConnectionPool serverPool;
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final String TIMEOUT_PATTERN = "Timeout during query execution [timeout = %s %s; query = %s]";

    public SqlRepository(ConnectionPool connectionPool) {
        serverPool = connectionPool;
    }

    /**
     * Execute query.
     *
     * @param server server
     * @param query query
     * @return DbTable
     */
    public static Table executeQuery(DBServer server, String query) {
        try (DiffServerConnection c = serverPool.createAndGetConnection(server)) {
            Map<String, String> mdcMap = MDC.getCopyOfContextMap();
            try (PreparedStatement statement = c.getConnection().prepareStatement(query)) {
                ResultSet rs = executorService.submit((Callable<ResultSet>) () -> {
                            MdcUtils.setContextMap(mdcMap);
                            return statement.executeQuery();
                        })
                        .get(DiffServersConnectionPool.getExecutionTimeout(), TimeUnit.SECONDS);
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnsCount = rsmd.getColumnCount();
                List<String> headers = Lists.newArrayListWithExpectedSize(columnsCount);
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    String columnName = rsmd.getColumnName(i).toUpperCase();
                    headers.add(columnName);
                }
                return new Table(headers, Utils.streamOf(new RsIter(rs, headers, columnsCount))
                        .collect(Collectors.toList()));
            } catch (TimeoutException e) {
                String errorMessage = String.format(TIMEOUT_PATTERN,
                        DiffServersConnectionPool.getExecutionTimeout(), "seconds", query);
                throw Utils.error(LOGGER, errorMessage, null, RuntimeException.class);
            } catch (Exception e) {
                String errorMessage = "Could not execute query [" + query + "]\n" + e.getMessage();
                throw Utils.error(LOGGER, errorMessage, e, SqlScriptExecuteException.class);
            }
        }
    }

    /**
     * Executes query.
     */
    public static String executeQueryAndGetFirstValue(DBServer server, String query) throws IndexOutOfBoundsException {
        try {
            Table table = executeQuery(server, query);
            return table.getRows().get(0).values().iterator().next();
        } catch (IndexOutOfBoundsException e) {
            String errorMessage = String.format("No rows found for query %s", query);
            throw Utils.error(LOGGER, errorMessage, null, RuntimeException.class);
        }
    }

    private static class RsIter extends AbstractIterator<Map<String, String>> {

        private final ResultSet rs;
        private final List<String> columnsNames;
        private final int columnsCount;

        private RsIter(ResultSet rs, List<String> columnsNames, int columnsCount) {
            this.rs = rs;
            this.columnsNames = columnsNames;
            this.columnsCount = columnsCount;
        }

        @Override
        protected Map<String, String> computeNext() {
            try {
                if (rs.next()) {
                    Map<String, String> row = Maps.newHashMapWithExpectedSize(columnsCount);
                    for (int i = 1; i <= columnsCount; i++) {
                        Object rsObject = rs.getObject(i);
                        if (rsObject instanceof CLOB) {
                            rsObject = ((CLOB) rsObject).getSubString(1, ((CLOB) rsObject).getBufferSize());
                        }
                        row.put(columnsNames.get(i - 1).toUpperCase(), Objects.toString(rsObject));
                    }
                    return row;
                }
                return endOfData();
            } catch (SQLException e) {
                String errorMessage = "Could not get row from result set.";
                throw Utils.error(LOGGER, errorMessage, e, RuntimeException.class);
            }
        }
    }
}
