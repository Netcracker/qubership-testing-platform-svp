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

package org.qubership.atp.svp.service.direct.displaytype;

import org.qubership.atp.svp.core.exceptions.ConnectionDbException;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.SqlScriptExecuteException;
import org.qubership.atp.svp.core.exceptions.VariableException;
import org.qubership.atp.svp.model.environments.DBServer;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.LogCollectorValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.repo.impl.CassandraRepository;
import org.qubership.atp.svp.repo.impl.LogCollectorRepository;
import org.qubership.atp.svp.repo.impl.SqlRepository;
import org.qubership.atp.svp.service.DefaultDisplayTypeService;
import org.qubership.atp.svp.service.LogCollectorBasedDisplayTypeService;
import org.qubership.atp.svp.service.direct.DeferredSearchServiceImpl;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParamDisplayTypeServiceImpl extends DefaultDisplayTypeService
        implements LogCollectorBasedDisplayTypeService {

    private final LogCollectorRepository logCollectorRepository;
    private final DeferredSearchServiceImpl deferredSearchService;
    private final ExecutionVariablesServiceImpl executionVariablesService;
    private final CassandraRepository cassandraRepository;

    /**
     * Constructor for ParamDisplayTypeServiceImpl.
     */
    @Autowired
    public ParamDisplayTypeServiceImpl(LogCollectorRepository logCollectorRepository,
                                       DeferredSearchServiceImpl deferredSearchService,
                                       ExecutionVariablesServiceImpl executionVariablesService,
                                       CassandraRepository cassandraRepository) {
        this.logCollectorRepository = logCollectorRepository;
        this.deferredSearchService = deferredSearchService;
        this.executionVariablesService = executionVariablesService;
        this.cassandraRepository = cassandraRepository;
    }

    @Override
    // TODO Unit tests for the logic of processing results from LC
    public AbstractValueObject getValueFromSource(Source source, AbstractParameterExecutionContext context)
            throws GettingValueException {
        try {
            Server server = context.getSessionConfiguration().getEnvironment().getSystem(source.getSystem())
                    .getServer(source.getConnection());
            String script = executionVariablesService.getSourceWithExecutionVariables(source.getScript(),
                    context.getExecutionVariables());
            String resultAsString = "";
            switch (source.getEngineType()) {
                case CASSANDRA:
                    resultAsString = cassandraRepository.executeQueryAndGetFirstValue(new DBServer(server), script);
                    break;
                case SQL:
                    resultAsString = SqlRepository.executeQueryAndGetFirstValue(new DBServer(server), script);
                    break;
                case LOG_COLLECTOR:
                    LogCollectorValueObject lcValue = getLogCollectorValueObject(logCollectorRepository,
                            deferredSearchService, executionVariablesService, context, source);
                    if (context.isDeferredSearchResult()) {
                        return lcValue;
                    } else {
                        resultAsString = lcValue.findFirstLogResult();
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected EngineType value: "
                            + source.getEngineType() + " for DisplayType: PARAM");
            }
            return new SimpleValueObject(resultAsString);
        } catch (ConnectionDbException e) {
            throw new GettingValueException(e.getMessage(), e.getSqlMessage());
        } catch (SqlScriptExecuteException e) {
            throw new GettingValueException(e.getMessage(), e.getSqlMessage());
        } catch (VariableException e) {
            throw new GettingValueException(e.getMessage(), e.getMessage());
        } catch (RuntimeException e) {
            throw new GettingValueException(e.getMessage());
        }
    }
}
