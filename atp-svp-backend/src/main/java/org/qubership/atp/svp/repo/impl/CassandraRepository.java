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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.svp.core.exceptions.ConnectionDbException;
import org.qubership.atp.svp.core.exceptions.SqlScriptExecuteException;
import org.qubership.atp.svp.model.environments.DBServer;
import org.qubership.atp.svp.model.table.Table;
import org.qubership.atp.svp.repo.impl.pool.DiffServersConnectionPool;
import org.qubership.atp.svp.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.WhiteListPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class CassandraRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlRepository.class);
    private static final String TIMEOUT_PATTERN = "Timeout during query execution [timeout = %s %s; query = %s]";
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Execute query.
     *
     * @param server server
     * @param query query
     * @return DbTable
     */
    public Table executeQuery(DBServer server, String query) {
        if (server.isServerEmpty()) {
            throw new RuntimeException("Please, check connection field in Environments");
        }
        try (Cluster cluster = createCluster(server)) {
            try (Session session = connectToDb(server, cluster)) {
                try {
                    ResultSet resultSet = session.executeAsync(query)
                            .get(DiffServersConnectionPool.getExecutionTimeout(), TimeUnit.SECONDS);
                    return resultAsTable(resultSet);
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
    }

    /**
     * Executes query.
     *
     * @param server server
     * @param query query
     * @return Object from first row and first column in String format
     */
    public String executeQueryAndGetFirstValue(DBServer server, String query) {
        if (server.isServerEmpty()) {
            throw new RuntimeException("Please, check connection field in Environments");
        }
        try (Cluster cluster = createCluster(server)) {
            try (Session session = connectToDb(server, cluster)) {
                try {
                    ResultSet resultSet = session.executeAsync(query)
                            .get(DiffServersConnectionPool.getExecutionTimeout(), TimeUnit.SECONDS);
                    Row row = resultSet.one();
                    return row.getString(0);
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
    }

    private Session connectToDb(DBServer server, Cluster cluster) {
        try {
            return cluster.connect(server.getScheme());
        } catch (Exception e) {
            String errorMessage = "Can't connect to Cassandra server";
            throw Utils.error(LOGGER, errorMessage, e, ConnectionDbException.class);
        }
    }

    private Table resultAsTable(ResultSet rs) {
        ColumnDefinitions md = rs.getColumnDefinitions();
        int columns = rs.getColumnDefinitions().size();
        List<String> headers = new ArrayList<>(columns);
        md.forEach(column -> headers.add(column.getName()));
        List<Map<String, String>> rows = new ArrayList<>();
        rs.all().forEach(excelRow -> {
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < columns; ++i) {
                String columnName = md.getName(i);
                Object columnValueObject = excelRow.getObject(i);
                String columnValue = writeColumnValueAsString(columnValueObject);
                row.put(columnName, columnValue);
            }
            rows.add(row);
        });
        return new Table(headers, rows);
    }

    private String writeColumnValueAsString(Object columnValueObject) {
        String columnValue;
        try {
            if (columnValueObject instanceof UDTValue) {
                columnValue = mapper.writeValueAsString(convertUdtValueToMap(columnValueObject));
            } else if (columnValueObject instanceof List) {
                columnValue = mapper.writeValueAsString(convertListWithUdtValue(columnValueObject));
            } else {
                columnValue = columnValueObject == null ? StringUtils.EMPTY : columnValueObject.toString();
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
        return columnValue;
    }

    private Map<String, Object> convertUdtValueToMap(Object columnValueObject) {
        Map<String, Object> map = new HashMap<>();
        UDTValue udtValue = (UDTValue) columnValueObject;
        Collection<String> collection = udtValue.getType().getFieldNames();
        collection.forEach(name -> {
            Object object = udtValue.getObject(name);
            if (object instanceof UDTValue) {
                map.put(name, convertUdtValueToMap(object));
            } else if (object instanceof Collection) {
                map.put(name, convertListWithUdtValue(object));
            } else {
                map.put(name, object);
            }
        });
        return map;
    }

    private List<Object> convertListWithUdtValue(Object columnValueObject) {
        List<Object> listSubCell = new ArrayList<>();
        Collection listColumnValueObject = (Collection) columnValueObject;
        listColumnValueObject.forEach(object -> {
            if (object instanceof UDTValue) {
                listSubCell.add(convertUdtValueToMap(object));
            } else if (object instanceof List) {
                listSubCell.add(convertListWithUdtValue(object));
            } else {
                listSubCell.add(object);
            }
        });
        return listSubCell;
    }

    private Cluster createCluster(DBServer server) {
        LoadBalancingPolicy loadBalancingPolicy = new WhiteListPolicy(DCAwareRoundRobinPolicy.builder().build(),
                Collections.singleton(new InetSocketAddress(server.getHost(), server.getPort())));
        Cluster.Builder builder = Cluster.builder()
                .addContactPoints(server.getHost())
                .withLoadBalancingPolicy(loadBalancingPolicy)
                .withPort(server.getPort())
                .withoutJMXReporting()
                .withCredentials(server.getUser(), server.getPassword());
        int timeout = Math.toIntExact(TimeUnit.SECONDS.toMillis(30L));
        SocketOptions opts = new SocketOptions().setConnectTimeoutMillis(timeout)
                .setReadTimeoutMillis(timeout);
        builder.withSocketOptions(opts);
        return builder.build();
    }
}
