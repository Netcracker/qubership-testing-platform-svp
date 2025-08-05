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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.model.impl.GenerateLinkSettings;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.pot.ExecutionVariable;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.SimpleExecutionVariable;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = GenerateLinkDisplayTypeServiceImpl.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
public class GenerateLinkDisplayTypeServiceImplTest {

    @Autowired
    GenerateLinkDisplayTypeServiceImpl generateLinkDisplayTypeService;
    @SpyBean
    ExecutionVariablesServiceImpl executionVariablesService;
    @Mock
    SutParameterExecutionContext parameterExecutionContext;
    @Mock
    SessionExecutionConfiguration sessionExecutionConfiguration;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(parameterExecutionContext.getSessionConfiguration()).thenReturn(sessionExecutionConfiguration);
        Mockito.when(parameterExecutionContext.getExecutionVariables()).thenReturn(new ConcurrentHashMap<>());
    }

    // Positive test cases
    @Test()
    public void getValueFromSource_NameAndUrlLinkFilled_returnsSimpleValueObjectLink()
            throws GettingValueException {
        GenerateLinkSettings generateLinkSettings = new GenerateLinkSettings("nameLink","http://url.url");
        Source source = new Source(null, null, null,
                "", Collections.singleton(generateLinkSettings));
        String erLinkValue = "<a href=\"http://url.url\">nameLink</a>";

        AbstractValueObject actualResult = generateLinkDisplayTypeService.getValueFromSource(source, parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(erLinkValue, ((SimpleValueObject) actualResult).getValue());
    }

    @Test()
    public void getValueFromSource_BlankNameAndFilledUrlLink_returnsSimpleValueObjectLink()
            throws GettingValueException {
        GenerateLinkSettings generateLinkSettings = new GenerateLinkSettings(null,"http://url.url");
        Source source = new Source(null, null, null,
                "", Collections.singleton(generateLinkSettings));
        String erLinkValue = "<a href=\"http://url.url\">http://url.url</a>";

        AbstractValueObject actualResult = generateLinkDisplayTypeService.getValueFromSource(source, parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(erLinkValue, ((SimpleValueObject) actualResult).getValue());
    }

    @Test()
    public void getValueFromSource_NameAndUrlLinkIncludeVariables_returnsSimpleValueObjectLink()
            throws GettingValueException {
        ConcurrentHashMap variables = new ConcurrentHashMap();
        variables.put("var1", new SimpleExecutionVariable("var1", "Link"));
        variables.put("var1", new SimpleExecutionVariable("var2", "url"));
        Mockito.when(parameterExecutionContext.getExecutionVariables()).thenReturn(variables);
        GenerateLinkSettings generateLinkSettings = new GenerateLinkSettings("name${var1}","http://${var2}.url");
        Source source = new Source(null, null, null,
                null, Collections.singleton(generateLinkSettings));
        String expectedLinkValue = "<a href=\"http://url.url\">nameLink</a>";

        AbstractValueObject actualResult = generateLinkDisplayTypeService.getValueFromSource(source, parameterExecutionContext);

        Assert.assertTrue(actualResult instanceof SimpleValueObject);
        Assert.assertEquals(expectedLinkValue, ((SimpleValueObject) actualResult).getValue());
    }

    // Negative test cases
    @Test
    public void getValueFromSource_UrlLinkIsBlank_throwGettingValueException() {

        GenerateLinkSettings generateLinkSettings = new GenerateLinkSettings(null,null);
        Source source = new Source(null, null, null,
                "", Collections.singleton(generateLinkSettings));
        try {
            generateLinkDisplayTypeService.getValueFromSource(source, parameterExecutionContext);
        } catch (GettingValueException ex) {
            Assert.assertEquals("GetLink - URL is blank for DisplayType: GENERATE_LINK",
                    ex.getMessage());
        }
    }

}
