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

import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.qubership.atp.svp.model.environments.Connection;
import org.qubership.atp.svp.model.environments.DBServer;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.table.Table;
import org.qubership.atp.svp.repo.impl.pool.DiffServersConnectionPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@PowerMockIgnore(value = {"javax.management.*"})
@SpringBootTest(classes = {SqlRepository.class, DiffServersConnectionPool.class},
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
@PrepareForTest(SqlRepository.class)
public class SqlRepositoryTest {

    private static final DBServer dbServer = new DBServer(new Server(new Connection(), "db"));
    @Autowired
    private DiffServersConnectionPool pool;

    @Test
    public void executeQueryAndGetFirstValue_whenValuePresent() {
        final String query = "select account_num from dbTable";
        final String firstValue = "123456";
        final Table dbTable = new Table(
                ImmutableList.of("account_num", "bill_seq"),
                ImmutableList.of(ImmutableMap.of("account_num", "123456", "bill_seq", "1"))
        );
        PowerMockito.mockStatic(SqlRepository.class);
        when(SqlRepository.executeQuery(eq(dbServer), eq(query))).thenReturn(dbTable);
        when(SqlRepository.executeQueryAndGetFirstValue(eq(dbServer), eq(query))).thenCallRealMethod();
        String actualValue = SqlRepository.executeQueryAndGetFirstValue(dbServer, query);
        Assert.assertEquals(firstValue, actualValue);
    }
}
