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

import static org.powermock.api.mockito.PowerMockito.spy;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.model.environments.Connection;
import org.qubership.atp.svp.model.environments.DBServer;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.utils.CryptoUtils;
import org.qubership.atp.svp.utils.CryptoUtilsTest;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@PowerMockIgnore(value = {"javax.management.*"})
@SpringBootTest(classes = {DiffServersConnectionPool.class},
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
@PrepareForTest({DiffServerConnection.class})
public class DiffServersConnectionPoolTest extends CryptoUtilsTest {

    private static DBServer dbServer;
    // it's SpyBean
    private DiffServersConnectionPool pool;

    @Before
    public void init() throws SQLException {
        Connection ncConn = new Connection();
        Map<String, String> parameters = new HashMap<String, String>() {{
            put("jdbc_url", "jdbc:h2:~/test");
            put("db_login", "sa");
            put("db_password", "");
        }};
        ncConn.setParameters(parameters);
        dbServer = new DBServer(new Server(ncConn, "db"));
            // avoid bug when we can't open the first connection
            DriverManager.getConnection(
                    dbServer.getConnectionStr(), dbServer.getUser(), CryptoUtils.decryptValue(dbServer.getPassword()));

    }

    @Test
    public void createAndGetConnectionNew() {
        pool = spy(new DiffServersConnectionPool("100", "-1"));
        HashSet<java.sql.Connection> h = new HashSet<>();
        DiffServerConnection conn = pool.createAndGetConnection(dbServer);
        java.sql.Connection consql = conn.getConnection();
        h.add(consql);
        conn = pool.createAndGetConnection(dbServer);
        h.add(conn.getConnection());
        Assert.assertEquals(2, h.size());
        Assert.assertNotEquals(consql, conn.getConnection());
    }

    @Test
    public void createAndGetConnectionReuse() {
        pool = spy(new DiffServersConnectionPool("100", "-1"));
        HashSet<java.sql.Connection> h = new HashSet<>();
        DiffServerConnection conn = pool.createAndGetConnection(dbServer);
        java.sql.Connection consql = conn.getConnection();
        h.add(consql);
        conn.close();
        conn = pool.createAndGetConnection(dbServer);
        h.add(conn.getConnection());
        Assert.assertEquals(1, h.size());
        Assert.assertEquals(consql, conn.getConnection());
    }

    @Test
    public void createAndGetConnectionNewBecauseTimeout() throws InterruptedException {
        pool = spy(new DiffServersConnectionPool("3", "-1"));
        HashSet<java.sql.Connection> h = new HashSet<>();
        DiffServerConnection conn = pool.createAndGetConnection(dbServer);
        java.sql.Connection consql = conn.getConnection();
        h.add(consql);
        conn.close();
        Thread.sleep(7000);
        conn = pool.createAndGetConnection(dbServer);
        h.add(conn.getConnection());
        Assert.assertEquals(2, h.size());
        Assert.assertNotEquals(consql, conn.getConnection());
    }
}

