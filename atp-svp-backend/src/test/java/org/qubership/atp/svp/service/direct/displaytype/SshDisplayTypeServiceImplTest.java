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

import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.jcraft.jsch.JSchException;
import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.environments.System;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.repo.impl.SshRepository;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.qubership.atp.svp.tests.DbMockEntity;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
public class SshDisplayTypeServiceImplTest extends TestWithTestData {
    @SpyBean
    SshResponseDisplayTypeServiceImpl sshResponseDisplayTypeServicesImpl;
    @SpyBean
    ExecutionVariablesServiceImpl executionVariablesService;
    @MockBean
    SshRepository sshRepository;
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

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(environment.getSystem(Mockito.anyString())).thenReturn(system);
        Mockito.when(system.getServer(Mockito.anyString())).thenReturn(server);
        Mockito.when(parameterExecutionContext.getSessionConfiguration()).thenReturn(sessionExecutionConfiguration);
        Mockito.when(sessionExecutionConfiguration.getEnvironment()).thenReturn(environment);
        Mockito.when(parameterExecutionContext.getExecutionVariables()).thenReturn(new ConcurrentHashMap<>());
    }

    /* GETTING VALUE PROCESS */

    @Test()
    public void getValueFromSource_() throws JSchException, GettingValueException {
        Source source = new Source("", "", EngineType.SSH,
                "", new HashSet<>());
        Mockito.when(sshRepository.executeCommandSsh(any(), any())).thenReturn(getSshResponse("row_1","row_2"));
        SimpleValueObject expectedResult = new SimpleValueObject(getSshResponse("row_1","row_2"));

            AbstractValueObject actualResponse = sshResponseDisplayTypeServicesImpl.getValueFromSource(source,
                    parameterExecutionContext);
           Assert.assertEquals(expectedResult, actualResponse);
    }

    @Test(expected = GettingValueException.class)
    public void getValueFromSource_exception() throws JSchException, GettingValueException {
        Source source = new Source("", "", EngineType.LOG_COLLECTOR,
                "", new HashSet<>());

        sshResponseDisplayTypeServicesImpl.getValueFromSource(source, parameterExecutionContext);
    }

    /* VALIDATION VALUE PROCESS */

    @Test()
    public void getValueFromSource_validate_PASSED() throws ValidationException, IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForSsh(
                getSshResponse("row_1", "row_2"), getSshResponse("row_1","row_2"));

//        PotSessionParameter parameter = new PotSessionParameter();
//        ValidationInfo validationInfo = new ValidationInfo();
//        parameter.setValidationInfo(validationInfo);
//        SimpleValueObject arValue = new SimpleValueObject(getSshResponse("row_1","row_2"));
//        parameter.getArValues().add(arValue);
//        SimpleValueObject erValue = new SimpleValueObject(getSshResponse("row_1","row_2"));
//        parameter.setEr(erValue);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(parameterExecutionContext.getSessionConfiguration().shouldHighlightDiffs()).thenReturn(true);

        sshResponseDisplayTypeServicesImpl.validateParameter(parameterExecutionContext);

        Assert.assertTrue(parameter.getArValues().stream().allMatch(ar -> checkSshSimpleParameterValueForDiffMessages(ar,
                parameter.getEr(),
                DisplayTypeTestConstants.HIGHLIGHTED_SSH_AR_WITH_NORMAL_DIFF_MESSAGES,
                DisplayTypeTestConstants.HIGHLIGHTED_SSH_ER_WITH_NORMAL_DIFF_MESSAGES)));
        Assert.assertEquals(ValidationStatus.PASSED, parameter.getValidationInfo().getStatus());
    }

    @Test()
    public void getValueFromSource_validate_FAILED() throws ValidationException, IOException {
        PotSessionParameterEntity parameter = DbMockEntity.generatePotSessionParameterForSsh(
                getSshResponse("row_2","row_1"), getSshResponse("row_1","row_2"));

//        PotSessionParameter parameter = new PotSessionParameter();
//        ValidationInfo validationInfo = new ValidationInfo();
//        parameter.setValidationInfo(validationInfo);
//        SimpleValueObject arValue = new SimpleValueObject(getSshResponse("row_2","row_1"));
//        parameter.getArValues().add(arValue);
//        SimpleValueObject erValue = new SimpleValueObject(getSshResponse("row_1","row_2"));
//        parameter.setEr(erValue);
        Mockito.when(parameterExecutionContext.getParameter()).thenReturn(parameter);
        Mockito.when(parameterExecutionContext.getSessionConfiguration().shouldHighlightDiffs()).thenReturn(true);

        sshResponseDisplayTypeServicesImpl.validateParameter(parameterExecutionContext);

        Assert.assertTrue(parameter.getArValues().stream().allMatch(ar -> checkSshSimpleParameterValueForDiffMessages(ar,
                parameter.getEr(),
                DisplayTypeTestConstants.HIGHLIGHTED_SSH_AR_WITH_DIFF_MESSAGES,
                DisplayTypeTestConstants.HIGHLIGHTED_SSH_ER_WITH_DIFF_MESSAGES)));
        Assert.assertEquals(ValidationStatus.FAILED, parameter.getValidationInfo().getStatus());
    }
}
