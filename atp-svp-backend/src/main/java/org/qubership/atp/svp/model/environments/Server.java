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

import static org.apache.commons.lang.StringUtils.EMPTY;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.assertj.core.util.Strings;
import org.qubership.atp.svp.utils.CryptoUtils;

public class Server {

    private static final int MILLISECONDS = 1000;
    private static final int TIMEOUT_CONNECTION_SEC = 60;
    private static final int SERVER_ALIVE_INTERVAL_SEC = 30;
    private static final int TIMEOUT_EXECUTE_SEC = 180;
    private static final int TIMEOUT_SESSION_SEC = 300;

    private final Connection connection;
    private final String name;

    public Server(Connection connection, String name) {
        this.name = name;
        this.connection = connection;
    }

    public String getName() {
        return name.toLowerCase();
    }

    public String getUser() {
        return getProperty(getName() + "_login");
    }

    public String getPass() {
        return getProperty(getName() + "_password");
    }

    public String getKey() {
        String key = getProperty(getName() + "_key");
        return key == null ? EMPTY : key.replaceAll("\n", "\r\n");
    }

    public String getPassPhrase() {
        return Objects.isNull(getProperty("passphrase"))
                ? getProperty("passphrase")
                : getProperty(getName() + "_passphrase");
    }

    /**
     * Get pty from environment. If not defined or incorrect value then 'false'.
     *
     * @return pty from environment. {@code true} if not defined or incorrect value
     */
    public boolean isPty() {
        final String pty = getProperty(getName() + "_pty");
        if (pty != null) {
            try {
                return Boolean.parseBoolean(pty);
            } catch (Exception e) {
                //nothing
            }
        }
        return false;
    }

    /**
     * Get shell from environment. If not defined or incorrect value then 'false'.
     *
     * @return shell from environment. {@code false} if not defined or incorrect value
     */
    public boolean isShell() {
        final String shell = getProperty(getName() + "_shell");
        if (shell != null) {
            try {
                return Boolean.parseBoolean(shell);
            } catch (Exception e) {
                //nothing
            }
        }
        return false;
    }

    /**
     * Get PtyTypeDumb from environment. If not defined or incorrect value then 'true'.
     *
     * @return shell from environment. {@code false} if not defined or incorrect value
     */
    public boolean isPtyTypeDumb() {
        final String ptyTypeDumb = getProperty(getName() + "_ptyTypeDumb");
        if (ptyTypeDumb != null) {
            try {
                return Boolean.parseBoolean(ptyTypeDumb);
            } catch (Exception e) {
                //nothing
            }
        }
        return true;
    }

    /**
     * Get timeout connect from environment. If not defined or incorrect value then 1 minute.
     *
     * @return timeout connect from environment. 1 minute if not defined or incorrect value
     */
    public int getTimeoutConnect() {
        return getTimeout("connect",
                TIMEOUT_CONNECTION_SEC * MILLISECONDS);
    }

    /**
     * Get timeout connect from environment. If not defined or incorrect value then 1 minute.
     *
     * @return timeout connect from environment. 1 minute if not defined or incorrect value
     */
    public int getTimeoutServAliveInterval() {
        return getTimeout("ServAliveInterval", SERVER_ALIVE_INTERVAL_SEC * MILLISECONDS);
    }

    /**
     * Get timeout execute from environment. If not defined or incorrect value then 1 minute.
     *
     * @return timeout execute from environment. 3 minutes if not defined or incorrect value
     */
    public int getTimeoutExecute() {
        return getTimeout("execute", TIMEOUT_EXECUTE_SEC * MILLISECONDS);
    }

    /**
     * Get timeout session from environment. If not defined or incorrect value then 1 minute.
     *
     * @return timeout session from environment. 5 minute if not defined or incorrect value
     */
    public int getTimeoutSession() {
        return getTimeout("session", TIMEOUT_SESSION_SEC);
    }

    /**
     * Get property from connection.
     *
     * @param key parameter key
     * @return property value, NULL in case connection or parameter doesn't exist
     */
    public String getProperty(String key) {
        if (connection != null && connection.getParameters() != null) {
            return CryptoUtils.decryptValue(connection.getParameters().get(key));
        } else {
            return null;
        }
    }

    public Map<String, String> getProperties() {
        return connection.getParameters();
    }

    public String getHostFull() {
        return getProperty(getName() + "_host");
    }

    /**
     * Gets host from property.
     *
     * @return host, NullPointerException otherwise
     */
    public String getHost() {
        Pattern pattern = Pattern.compile("([^:^]*)(:\\d*)?(.*)?");
        Matcher matcher = pattern.matcher(getHostFull());
        matcher.find();
        String host = matcher.group(1);
        return host;
    }

    /**
     * Gets port from host property,
     * for e.g. 127.0.0.1:24, then port will be 24.
     * If port not present in host property then default 22 will be returned.
     *
     * @return int port value.
     */
    public int getPort() {
        String host = getHostFull();
        int port = 22;
        if (host.contains(":")) {
            String[] arr = host.split(":");
            try {
                port = Integer.parseInt(arr[arr.length - 1]);
            } catch (NumberFormatException e) {
                String messageTemplate = "Can't parse port from host string. Host: [%s].";
                throw new IllegalArgumentException(String.format(messageTemplate, host));
            }
        }
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Server server = (Server) o;
        return Objects.equals(connection, server.connection)
                && Objects.equals(name, server.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connection, name);
    }

    /**
     * Get timeout from environment. If not defined or incorrect value then {@code defaultValue}.
     *
     * @param timeoutPostfix postfix of timeout
     * @param defaultValue default value to return
     * @return timeout from environment. {@code defaultValue} if not defined or incorrect value
     */
    private int getTimeout(String timeoutPostfix, int defaultValue) {
        final String timeout = getProperty(getName() + "_timeout_" + timeoutPostfix);
        if (!Strings.isNullOrEmpty(timeout)) {
            try {
                return Integer.parseInt(timeout);
            } catch (Exception e) {
                //nothing
            }
        }
        return defaultValue;
    }

    public Connection getConnection() {
        return connection;
    }
}
