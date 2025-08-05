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
import java.sql.SQLException;
import java.time.Duration;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiffServerConnection implements AutoCloseable {

    private final Connection connection;
    private Date time;
    private volatile boolean isFree;
    private final long aliveLength;

    /**
     * Creates box class to sql connection.
     *
     * @param connection - sql connection to DB.
     */
    public DiffServerConnection(Connection connection, long aliveLength) {
        this.connection = connection;
        isFree = true;
        this.aliveLength = aliveLength;
    }

    /**
     * Methods returns connection.
     * Also updates time that need to close connection
     *
     * @return connection.
     */
    public Connection getConnection() {
        isFree = false;
        time = new Date();
        return connection;
    }

    /**
     * Checks and closes if this connection should be expired.
     *
     * @param timeDiff - the boundary for time difference between connection start and now.
     * @return true if connection is free and time difference is more than {@code timeDiff}.
     */
    public boolean isExpired(long timeDiff) {
        Date timeNow = new Date();
        timeDiff = aliveLength == -1 ? timeDiff : aliveLength;
        if (isFree && Duration.ofMillis(timeNow.getTime() - time.getTime()).getSeconds() >= timeDiff) {
            try {
                if (connection.isClosed()) {
                    log.info("Connection is already closed [{}]. Don't need to do that", connection);
                } else {
                    connection.close();
                }
                return true;
            } catch (SQLException e) {
                log.error("Can't close sql connection because [{}]", e.getMessage());
            }
        }
        return false;
    }

    @Override
    public void close() {
        isFree = true;
    }

    public boolean isFree() {
        return isFree;
    }
}
