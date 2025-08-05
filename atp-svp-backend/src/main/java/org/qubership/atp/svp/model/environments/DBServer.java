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

package org.qubership.atp.svp.model.environments;

import java.util.Objects;

public class DBServer {

    private final Server server;

    public DBServer(Server server) {
        this.server = server;
    }

    /**
     *  Get jdbc_url property from Environment Service.
     * @return jdbc_url property from Environment Service if exist.
     *         Else returned connection string will be built from connection parameters.
     */
    public String getConnectionStr() {
        String jdbcUrl = server.getProperty("jdbc_url");
        return Objects.isNull(jdbcUrl) || jdbcUrl.isEmpty()
                ? buildConnectionStr()
                : server.getProperty("jdbc_url");
    }

    private String buildConnectionStr() {
        return "jdbc:" + server.getProperty("db_type") + ":thin:@" + server.getProperty("db_host")
                + ":" + server.getProperty("db_port") + "/" + server.getProperty("db_name");
    }

    /**
     * Get db_host property from Environment Service.
     * @return host from Server.
     */
    public String getHost() {
        return server.getProperty("db_host");
    }

    /**
     * Get db_port property from Environment Service.
     * @return port from Server.
     */
    public int getPort() {
        int port = Integer.parseInt(server.getProperty("db_port"));
        return port;
    }

    /**
     * Get db_name property from Environment Service.
     * @return password from Server.
     */
    public String getScheme() {
        return server.getProperty("db_name");
    }

    /**
     * Get db_login property from Environment Service.
     * @return login from Server.
     */
    public String getUser() {
        return server.getProperty("db_login");
    }

    /**
     * Get db_password property from Environment Service.
     * @return password from Server.
     */
    public String getPassword() {
        return server.getProperty("db_password");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DBServer)) {
            return false;
        }
        DBServer dbServer = (DBServer) o;
        return Objects.equals(getConnectionStr(), dbServer.getConnectionStr())
                && Objects.equals(getUser(), dbServer.getUser())
                && Objects.equals(getUser(), dbServer.getUser())
                && Objects.equals(server, dbServer.server);
    }

    @Override
    public int hashCode() {
        return Objects.hash(server, getConnectionStr(), getUser());
    }

    /**
     * Check servers field.
     */
    public boolean isServerEmpty() {
        if (server.getProperty("db_host").isEmpty()
                || server.getProperty("db_port").isEmpty()
                || server.getProperty("db_name").isEmpty()
                || server.getProperty("db_login").isEmpty()
                || server.getProperty("db_password").isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
}
