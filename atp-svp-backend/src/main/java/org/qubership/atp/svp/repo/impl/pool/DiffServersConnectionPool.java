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

package org.qubership.atp.svp.repo.impl.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.svp.core.enums.DbType;
import org.qubership.atp.svp.core.exceptions.ConnectionDbException;
import org.qubership.atp.svp.model.environments.DBServer;
import org.qubership.atp.svp.utils.Utils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DiffServersConnectionPool implements ConnectionPool {

    private static LoadingCache<DBServer, DiffServerConnection> connectionCache;
    private static long executionTimeout;
    private static long cleanCacheTimeout;

    /**
     * Creates connection pool for DB connections.
     */
    public DiffServersConnectionPool(@Value("${db.close.delay:180}") String cleanTimeout,
                                     @Value("${db.alive.length:200}") String aliveLengthStr) {
        parseDbDelayValues(cleanTimeout, aliveLengthStr);
        connectionCache = CacheBuilder.newBuilder()
                .expireAfterWrite(DiffServersConnectionPool.cleanCacheTimeout, TimeUnit.SECONDS)
                .softValues()
                .removalListener((RemovalListener<DBServer, DiffServerConnection>) notification ->
                        notification.getValue().isExpired(DiffServersConnectionPool.cleanCacheTimeout))
                .build(new CacheLoader<DBServer, DiffServerConnection>() {
                    @Override
                    public DiffServerConnection load(@NotNull DBServer server) throws Exception {
                        return new DiffServerConnection(DriverManager.getConnection(server.getConnectionStr(),
                                server.getUser(), server.getPassword()), -1);
                    }
                });
        // Schedule cache cleanup every cleanCacheTimeout
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        service.scheduleAtFixedRate(() -> {
                    MdcUtils.setContextMap(mdcMap);
                    connectionCache.cleanUp();
                }, 0,
                cleanCacheTimeout, TimeUnit.SECONDS);
    }

    private void parseDbDelayValues(String timeDiffStr, String aliveLengthStr) {
        cleanCacheTimeout = Utils.parseLongValueOrDefault(timeDiffStr, 180, "db.close.delay");
        executionTimeout = Utils.parseLongValueOrDefault(aliveLengthStr, 200, "db.alive.length");
    }

    public static long getExecutionTimeout() {
        return executionTimeout;
    }

    @Override
    public DiffServerConnection createAndGetConnection(DBServer server) {
        return createAndGetConnection(server, executionTimeout);
    }

    /**
     * Checks if there is available connection in Cache.
     * If it is then returns already opened connection
     * otherwise creates a new one and overrides old.
     *
     * @param server      - server
     * @param aliveLength - limit for connection live
     * @return opened connection
     */
    @Override
    public DiffServerConnection createAndGetConnection(DBServer server, long aliveLength) {
        try {
            if (connectionCache.asMap().containsKey(server) && connectionCache.getUnchecked(server).isFree()) {
                return connectionCache.get(server);
            } else {
                Connection connection = driverPreloadAndGetConnection(server);
                DiffServerConnection diffServerConnection = new DiffServerConnection(connection, -1);
                connectionCache.put(server, diffServerConnection);
                return diffServerConnection;
            }
        } catch (ExecutionException e) {
            String errorMessage = "Error while retrieving server [" + server.getConnectionStr() + "] from LoadingCache";
            throw Utils.error(log, errorMessage, e, RuntimeException.class);
        }
    }

    // loads suitable driver and creates connection via DriverManager
    private static Connection driverPreloadAndGetConnection(DBServer server) {
        try {
            for (DbType dbType : DbType.values()) {
                if (server.getConnectionStr().toLowerCase().contains(dbType.toString().toLowerCase())) {
                    Class.forName(DbType.getDriverName(dbType));
                }
            }
            return DriverManager.getConnection(server.getConnectionStr(), server.getUser(), server.getPassword());
        } catch (ClassNotFoundException e) {
            String errorMessage = "Could not create connection "
                    + "because can't load driver class for [" + server.getConnectionStr() + "]";
            throw Utils.error(log, errorMessage, e, ConnectionDbException.class);
        } catch (SQLException e) {
            String errorMessage = "Can't connect to sql server [" + server.getConnectionStr() + "]";
            throw Utils.error(log, errorMessage, e, ConnectionDbException.class);
        }
    }
}
