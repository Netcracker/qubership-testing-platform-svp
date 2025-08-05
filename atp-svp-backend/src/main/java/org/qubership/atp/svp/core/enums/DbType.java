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

package org.qubership.atp.svp.core.enums;

import java.util.HashMap;
import java.util.Map;

public enum DbType {
    CASSANDRA,
    ORACLE,
    POSTGRES,
    UNDEFINED;

    private static Map<DbType, String> driverNames = new HashMap<DbType, String>() {
        {
            put(CASSANDRA, "org.apache.cassandra.cql.jdbc.CassandraDriver");
            put(ORACLE, "oracle.jdbc.driver.OracleDriver");
            put(POSTGRES, "org.postgresql.Driver");
            put(UNDEFINED, "com.mysql.jdbc.Driver");
        }
    };

    public static String getDriverName(DbType dbType) {
        return driverNames.get(dbType);
    }
}
