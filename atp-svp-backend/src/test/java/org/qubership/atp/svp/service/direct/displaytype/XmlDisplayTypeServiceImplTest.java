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
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.xml.sax.InputSource;

import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.environments.System;
import org.qubership.atp.svp.model.impl.HttpSettings;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.repo.impl.CassandraRepository;
import org.qubership.atp.svp.repo.impl.SoapRepositoryImpl;
import org.qubership.atp.svp.repo.impl.SqlRepository;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.qubership.atp.svp.tests.DbMockEntity;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = XmlDisplayTypeServiceImpl.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
@PrepareForTest(SqlRepository.class)
@PowerMockIgnore({"com.sun.org.apache.xalan.*", "javax.xml.*", "com.sun.org.apache.xerces.*", "org.xml.*", "org.w3c.*"})
public class XmlDisplayTypeServiceImplTest extends TestWithTestData {

    @SpyBean
    XmlDisplayTypeServiceImpl xmlDisplayTypeService;
    @SpyBean
    ExecutionVariablesServiceImpl executionVariablesService;

    @Mock
    Environment environment;
    @Mock
    System system;
    @Mock
    Server server;
    @Mock
    SutParameterExecutionContext parameterExecutionContext;
    @Mock
    SessionExecutionConfiguration sessionExecutionConfiguration;
    @MockBean
    CassandraRepository cassandraRepository;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(SqlRepository.class);
        Mockito.when(environment.getSystem(Mockito.anyString())).thenReturn(system);
        Mockito.when(system.getServer(Mockito.anyString())).thenReturn(server);
        Mockito.when(parameterExecutionContext.getSessionConfiguration()).thenReturn(sessionExecutionConfiguration);
        Mockito.when(sessionExecutionConfiguration.getEnvironment()).thenReturn(environment);
        Mockito.when(parameterExecutionContext.getExecutionVariables()).thenReturn(new ConcurrentHashMap<>());
    }

    /* GETTING VALUE PROCESS */

    // Negative test cases
    @Test(expected = GettingValueException.class)
    public void getValueFromSource_errorWhileGettingDataEngineSQL_throwsGettingValueException() throws GettingValueException {
        Source source = new Source("", "", EngineType.SQL, "", new HashSet<>());
        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), Mockito.anyString()))
                .thenThrow(new RuntimeException("Error while getting data!"));

        xmlDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_errorWhileGettingDataEngineCassandra_throwsGettingValueException() throws GettingValueException {
        Source source = new Source("", "", EngineType.CASSANDRA, "", new HashSet<>());
        Mockito.when(cassandraRepository.executeQueryAndGetFirstValue(Mockito.any(), Mockito.anyString()))
                .thenThrow(new RuntimeException("Error while getting data!"));

        xmlDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
    }

    @Test()
    public void getValueFromSource_tableDataEngineNotSpecified_throwsGettingValueException() {
        Source source = new Source("", "", EngineType.LOG_COLLECTOR,
                "", new HashSet<>());

        try {
            xmlDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
            Assert.fail("Switch not go in default case");
        } catch (GettingValueException ex) {
            Assert.assertEquals("Unexpected EngineType value: LOG_COLLECTOR for DisplayType: XML",
                    ex.getMessage());
        }
    }

    // Positive test cases
    @Test()
    public void getValueFromSource_tableDataEngineTypeSQL_returnsSimpleValueObjectWithFirstCellData() throws GettingValueException {
        Source source = new Source("", "", EngineType.SQL,
                "", new HashSet<>());

        Mockito.when(SqlRepository.executeQueryAndGetFirstValue(Mockito.any(), Mockito.anyString()))
                .thenReturn(DisplayTypeTestConstants.XML_CORRECT_VALUE);

        AbstractValueObject actualResult = xmlDisplayTypeService.getValueFromSource(source, parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(prettyPrintXml(DisplayTypeTestConstants.XML_CORRECT_VALUE), ((SimpleValueObject) actualResult).getValue());
    }

    @Test()
    public void getValueFromSource_tableDataEngineCassandra_returnsSimpleValueObjectWithFirstCellData() throws GettingValueException {
        Source source = new Source("", "", EngineType.CASSANDRA,
                "", new HashSet<>());

        Mockito.when(cassandraRepository.executeQueryAndGetFirstValue(Mockito.any(), Mockito.anyString()))
                .thenReturn(DisplayTypeTestConstants.XML_CORRECT_VALUE);

        AbstractValueObject actualResult = xmlDisplayTypeService.getValueFromSource(source, parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(prettyPrintXml(DisplayTypeTestConstants.XML_CORRECT_VALUE), ((SimpleValueObject) actualResult).getValue());
    }

    //    TODO Fix this test
    @Test
    @Ignore
    public void getValueFromSource_tableDataEngineTypeSOAP_returnsSimpleValueObjectWithFirstCellData() throws GettingValueException {
        Source source = new Source("", "", EngineType.SOAP, "", Collections.singleton(new HttpSettings()));

        //        HttpSettings httpSettings = new HttpSettings();
        Mockito.when(SoapRepositoryImpl.soapRequest(Mockito.any(), Mockito.any()))
                .thenReturn(null);

        AbstractValueObject actualResult = xmlDisplayTypeService.getValueFromSource(source, parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(DisplayTypeTestConstants.XML_CORRECT_VALUE, ((SimpleValueObject) actualResult).getValue());
    }

    /* VALIDATION PROCESS */

    // Negative test cases
    @Test(expected = ValidationException.class)
    public void validateParameter_correctValueNotSpecified_throwValidationException() throws ValidationException,
            IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForXml("NotXML",
                DisplayTypeTestConstants.XML_INCORRECT_VALUE, 1);

//        PotSessionParameter parameter =
//                generateMockedParameterWithSimpleValues("NotXML",
//                        DisplayTypeTestConstants.XML_INCORRECT_VALUE, 1);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        xmlDisplayTypeService.validateParameter(parameterExecutionContext);
    }

    // Positive test cases
    @Test()
    public void validateParameter_passedValidation_parameterValidationStatusIsPassed()
            throws ValidationException, IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForXml(
                DisplayTypeTestConstants.XML_CORRECT_VALUE,
                DisplayTypeTestConstants.XML_INCORRECT_VALUE, 1);

//        PotSessionParameter parameter =
//                generateMockedParameterWithSimpleValues(DisplayTypeTestConstants.XML_CORRECT_VALUE,
//                        DisplayTypeTestConstants.XML_INCORRECT_VALUE, 1);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);

        xmlDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void validateParameter_oneFailedValidationAndNoHighlightDifference_parameterValidationStatusIsFailedWithoutDiffMessages()
            throws ValidationException, IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForXml(
                DisplayTypeTestConstants.XML_CORRECT_VALUE,
                DisplayTypeTestConstants.XML_INCORRECT_VALUE,
                3,1);

//        PotSessionParameter parameter =
//                generateMockedParameterWithSimpleValues(DisplayTypeTestConstants.XML_CORRECT_VALUE,
//                        DisplayTypeTestConstants.XML_INCORRECT_VALUE, 3, 1);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        xmlDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertTrue(parameter.getArValues()
                .stream().allMatch(ar -> checkSimpleParameterValueForDiffMessages(ar, null, null)));
        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void validateParameter_allFailedValidationsAndNoHighlightDifference_parameterValidationStatusIsFailedWithoutDiffMessages()
            throws ValidationException, IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForXml(
                DisplayTypeTestConstants.XML_CORRECT_VALUE,
                DisplayTypeTestConstants.XML_INCORRECT_VALUE,
                3, 0, 1, 2);
//        PotSessionParameter parameter =
//                generateMockedParameterWithSimpleValues(DisplayTypeTestConstants.XML_CORRECT_VALUE,
//                        DisplayTypeTestConstants.XML_INCORRECT_VALUE, 3, 0, 1, 2);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(false);

        xmlDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertTrue(parameter.getArValues()
                .stream().allMatch(ar -> checkSimpleParameterValueForDiffMessages(ar, null, null)));
        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void validateParameter_oneFailedValidationAndHighlightDifference_parameterValidationStatusIsFailedWithDiffMessages()
            throws ValidationException, IOException {
        int incorrectResultIdx = 1;
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForXml(
                DisplayTypeTestConstants.XML_CORRECT_VALUE,
                DisplayTypeTestConstants.XML_INCORRECT_VALUE,
                3, incorrectResultIdx);
//        PotSessionParameter parameter =
//                generateMockedParameterWithSimpleValues(DisplayTypeTestConstants.XML_CORRECT_VALUE,
//                        DisplayTypeTestConstants.XML_INCORRECT_VALUE, 3, incorrectResultIdx);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);

        xmlDisplayTypeService.validateParameter(parameterExecutionContext);

        List<AbstractValueObject> parameterValues = parameter.getArValues();
        for (int i = 0; i < parameterValues.size(); i++) {
            String arDiffs;
            String erDiffs;
            if (i == incorrectResultIdx) {
                arDiffs = DisplayTypeTestConstants.HIGHLIGHTED_XML_AR_WITH_DIFF_MESSAGES;
                erDiffs = DisplayTypeTestConstants.HIGHLIGHTED_XML_ER_WITH_DIFF_MESSAGES;
            } else {
                arDiffs = DisplayTypeTestConstants.HIGHLIGHTED_XML_AR_WITH_NORMAL_DIFF_MESSAGES;
                erDiffs = DisplayTypeTestConstants.HIGHLIGHTED_XML_ER_WITH_NORMAL_DIFF_MESSAGES;
            }
            Assert.assertTrue(checkSimpleParameterValueForDiffMessages(parameterValues.get(i), arDiffs, erDiffs));
        }
        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void validateParameter_allFailedValidationsAndHighlightDifference_parameterValidationStatusIsFailedWithDiffMessages()
            throws ValidationException, IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForXml(
                DisplayTypeTestConstants.XML_CORRECT_VALUE,
                DisplayTypeTestConstants.XML_INCORRECT_VALUE,
                3, 0, 1, 2);
//        PotSessionParameter parameter =
//                generateMockedParameterWithSimpleValues(DisplayTypeTestConstants.XML_CORRECT_VALUE,
//                        DisplayTypeTestConstants.XML_INCORRECT_VALUE, 3, 0, 1, 2);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(sessionExecutionConfiguration.shouldHighlightDiffs()).thenReturn(true);

        xmlDisplayTypeService.validateParameter(parameterExecutionContext);

        Assert.assertTrue(parameter.getArValues().stream().allMatch(ar -> checkSimpleParameterValueForDiffMessages(ar,
                DisplayTypeTestConstants.HIGHLIGHTED_XML_AR_WITH_DIFF_MESSAGES,
                DisplayTypeTestConstants.HIGHLIGHTED_XML_ER_WITH_DIFF_MESSAGES)));
        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
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
