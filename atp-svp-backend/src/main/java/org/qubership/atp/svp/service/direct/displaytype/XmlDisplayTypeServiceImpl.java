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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.qubership.atp.svp.core.exceptions.ConnectionDbException;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.SqlScriptExecuteException;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.core.exceptions.VariableException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.DBServer;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.impl.HttpSettings;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.repo.impl.CassandraRepository;
import org.qubership.atp.svp.repo.impl.SoapRepositoryImpl;
import org.qubership.atp.svp.repo.impl.SqlRepository;
import org.qubership.atp.svp.service.DefaultDisplayTypeService;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.qubership.automation.pc.comparator.impl.XmlComparator;
import org.qubership.automation.pc.compareresult.DiffMessage;
import org.qubership.automation.pc.configuration.parameters.Parameters;
import org.qubership.automation.pc.core.exceptions.ComparatorException;
import org.qubership.automation.pc.core.helpers.BuildColoredXML;
import org.qubership.automation.pc.models.HighlighterResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;


@Service
public class XmlDisplayTypeServiceImpl extends DefaultDisplayTypeService {

    private final XmlComparator xmlComparator = new XmlComparator();

    private final ExecutionVariablesServiceImpl executionVariablesService;
    private final CassandraRepository cassandraRepository;

    @Autowired
    public XmlDisplayTypeServiceImpl(ExecutionVariablesServiceImpl executionVariablesService,
                                     CassandraRepository cassandraRepository) {
        this.executionVariablesService = executionVariablesService;
        this.cassandraRepository = cassandraRepository;
    }

    @Override
    public AbstractValueObject getValueFromSource(Source source, AbstractParameterExecutionContext context)
            throws GettingValueException {
        try {
            String resultAsString = "";
            Server server = context.getSessionConfiguration()
                    .getEnvironment()
                    .getSystem(source.getSystem())
                    .getServer(source.getConnection());
            String script = executionVariablesService.getSourceWithExecutionVariables(source.getScript(),
                    context.getExecutionVariables());
            switch (source.getEngineType()) {
                case SQL:
                    resultAsString = SqlRepository.executeQueryAndGetFirstValue(new DBServer(server), script);
                    break;
                case CASSANDRA:
                    resultAsString = cassandraRepository.executeQueryAndGetFirstValue(new DBServer(server), script);
                    break;
                case SOAP:
                    HttpSettings settings = (HttpSettings) source.getSettingsByType(HttpSettings.class);
                    HttpSettings httpSettingsWithVariables =
                            executionVariablesService.getHttpSettingsWithExecutionVariables(
                                    context.getExecutionVariables(), settings);
                    resultAsString = SoapRepositoryImpl.soapRequest(server, httpSettingsWithVariables);
                    break;
                default:
                    throw new IllegalStateException("Unexpected EngineType value: "
                            + source.getEngineType() + " for DisplayType: XML");
            }
            return new SimpleValueObject(prettyPrintXml(resultAsString));
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

    @Override
    public void validateParameter(AbstractParameterExecutionContext context)
            throws ValidationException {
        PotSessionParameterEntity parameter = context.getParameter();
        try {
            List<DiffMessage> overallDiffs = new ArrayList<>();
            for (AbstractValueObject arValue : parameter.getArValues()) {
                SimpleValueObject er = (SimpleValueObject) parameter.getEr();
                SimpleValueObject ar = (SimpleValueObject) arValue;
                List<DiffMessage> diffs = xmlComparator.compare(er.getValue(), ar.getValue(), new Parameters());
                overallDiffs.addAll(diffs);
                if (context.getSessionConfiguration().shouldHighlightDiffs()) {
                    HighlighterResult highlighterResult = BuildColoredXML.highlight(diffs,
                            er.getValue(), ar.getValue());
                    ar.setHighlightedEr(highlighterResult.getEr().getComposedValue(false));
                    ar.setHighlightedAr(highlighterResult.getAr().getComposedValue(false));
                }
            }
            parameter.setValidationStatus(calculateStatusByDiffs(overallDiffs));
        } catch (ComparatorException ex) {
            throw new ValidationException("Error occurred on trying to validate '"
                    + parameter.getPath() + "' as XML!", ex);
        }
    }

    private String prettyPrintXml(String sourceXml) throws GettingValueException {
        try {
            Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "2");
            javax.xml.transform.Source xmlSource =
                    new SAXSource(new InputSource(new ByteArrayInputStream(sourceXml.getBytes())));
            StreamResult res = new StreamResult(new ByteArrayOutputStream());
            serializer.transform(xmlSource, res);
            return res.getOutputStream().toString();
        } catch (TransformerException e) {
            throw new GettingValueException("An error occurred during transformation XML: " + e.getMessage());
        }
    }
}
