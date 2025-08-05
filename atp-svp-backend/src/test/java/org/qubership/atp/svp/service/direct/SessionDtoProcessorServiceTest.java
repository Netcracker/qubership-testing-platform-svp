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

package org.qubership.atp.svp.service.direct;

import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.util.Collections;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.model.api.ram.SessionDto;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.repo.impl.EnvironmentRepository;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionPageRepository;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionParameterRepository;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = SessionDtoProcessorService.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false"})
public class SessionDtoProcessorServiceTest extends TestWithTestData {

    @SpyBean
    SessionDtoProcessorService sessionDtoProcessorService;

    @MockBean
    private EnvironmentRepository environmentRepository;

    @MockBean
    PotSessionPageRepository potSessionPageRepository;

    @MockBean
    PotSessionParameterRepository potSessionParameterRepository;

    @Test
    public void potSessionParameterConverterToDto_WithLogCollector2ParamNoneAndPassed_ProcessingToSessionDto() throws IOException,
            JSONException {
        String filePath = "src/test/resources/test_data/sessionDTO/potSessionMock/WithLogCollector2ParamNoneAndPassed"
                + ".json";
        PotSessionEntity session = objectMapper.readValue(readFileToString(filePath), PotSessionEntity.class);
        Mockito.when(potSessionPageRepository.findByPotSessionEntitySessionId(any()))
                .thenReturn(session.getPotSessionPageEntities());
        Mockito.when(potSessionParameterRepository.findByPotSessionEntitySessionId(any()))
                .thenReturn(session.getCommonParameters());

        SessionDto sessionDto = sessionDtoProcessorService.potSessionParameterConverterToDto(session);

        String actualResultSessionDtoStr = objectMapper.writeValueAsString(sessionDto);
        String pathToExpectedSessionDTO = "src/test/resources/test_data/sessionDTO"
                + "/ExpectedSessionDtoLogcollectorResult.json";
        JSONAssert.assertEquals(readFileToString(pathToExpectedSessionDTO), actualResultSessionDtoStr, true);
    }

    @Test
    public void potSessionParameterConverterToDto_WithJsonTableWithLinks_ProcessingToSessionDtoAndReplaceLinks()
            throws IOException,
            JSONException {
        String filePath = "src/test/resources/test_data/sessionDTO/potSessionMock/WithJsonTableWithLinks.json";
        PotSessionEntity session = objectMapper.readValue(readFileToString(filePath), PotSessionEntity.class);
        Mockito.when(potSessionPageRepository.findByPotSessionEntitySessionId(any()))
                .thenReturn(session.getPotSessionPageEntities());
        Mockito.when(potSessionParameterRepository.findByPotSessionEntitySessionId(any()))
                .thenReturn(session.getCommonParameters());

        SessionDto sessionDto = sessionDtoProcessorService.potSessionParameterConverterToDto(session);

        String actualResultSessionDtoStr = objectMapper.writeValueAsString(sessionDto);
        String pathToExpectedSessionDTO = "src/test/resources/test_data/sessionDTO/sessionDTOJsonTableLink.json";
        JSONAssert.assertEquals(readFileToString(pathToExpectedSessionDTO), actualResultSessionDtoStr, true);
    }

    @Test
    public void potSessionParameterConverterToDto_WithTableWithLinks_ProcessingToSessionDtoAndReplaceLinks() throws
            IOException,
            JSONException {
        String filePath = "src/test/resources/test_data/sessionDTO/potSessionMock/WithTableWithLinks.json";
        PotSessionEntity session = objectMapper.readValue(readFileToString(filePath), PotSessionEntity.class);
        Mockito.when(potSessionPageRepository.findByPotSessionEntitySessionId(any()))
                .thenReturn(session.getPotSessionPageEntities());
        Mockito.when(potSessionParameterRepository.findByPotSessionEntitySessionId(any()))
                .thenReturn(session.getCommonParameters());

        SessionDto sessionDto = sessionDtoProcessorService.potSessionParameterConverterToDto(session);

        String actualResultSessionDtoStr = objectMapper.writeValueAsString(sessionDto);
        String pathToExpectedSessionDTO = "src/test/resources/test_data/sessionDTO/sessionDTOTableLink.json";
        JSONAssert.assertEquals(readFileToString(pathToExpectedSessionDTO), actualResultSessionDtoStr, true);
    }

    @Test
    public void
    potSessionParameterConverterToDto_ConfigPageWithHideGroup_ProcessingToSessionDtoBuildTreeWithoutHideGroup()
            throws IOException,
            JSONException {
        String filePath = "src/test/resources/test_data/sessionDTO/potSessionMock/ConfigPageWithHideGroup.json";
        PotSessionEntity session = objectMapper.readValue(readFileToString(filePath), PotSessionEntity.class);
        Mockito.when(potSessionPageRepository.findByPotSessionEntitySessionId(any()))
                .thenReturn(session.getPotSessionPageEntities());
        Mockito.when(potSessionParameterRepository.findByPotSessionEntitySessionId(any()))
                .thenReturn(session.getCommonParameters());

        SessionDto sessionDto = sessionDtoProcessorService.potSessionParameterConverterToDto(session);

        String actualResultSessionDtoStr = objectMapper.writeValueAsString(sessionDto);
        String pathToExpectedSessionDTO = "src/test/resources/test_data/sessionDTO/sessionDTOHideGroup.json";
        JSONAssert.assertEquals(readFileToString(pathToExpectedSessionDTO), actualResultSessionDtoStr, true);
    }

    @Test
    public void
    potSessionParameterConverterToDto_ConfigWithCommonParameters_SuccessfullyConvertWithoutPage()
            throws IOException,
            JSONException {
        String filePath = "src/test/resources/test_data/sessionDTO/potSessionMock/configWithCommons.json";
        PotSessionEntity session = objectMapper.readValue(readFileToString(filePath), PotSessionEntity.class);
        Mockito.when(potSessionPageRepository.findByPotSessionEntitySessionId(any()))
                .thenReturn(Collections.emptyList());
        Mockito.when(potSessionParameterRepository.findByPotSessionEntitySessionId(any()))
                .thenReturn(session.getCommonParameters());

        SessionDto sessionDto = sessionDtoProcessorService.potSessionParameterConverterToDto(session);

        String actualResultSessionDtoStr = objectMapper.writeValueAsString(sessionDto);
        String pathToExpectedSessionDTO = "src/test/resources/test_data/sessionDTO/expectedCommonParametersDto.json";
        JSONAssert.assertEquals(readFileToString(pathToExpectedSessionDTO), actualResultSessionDtoStr, true);
    }
}
