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

package org.qubership.atp.svp.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.svp.model.api.kafka.EventType;
import org.qubership.atp.svp.model.api.kafka.ProjectEvent;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.repo.impl.AuthTokenProvider;
import org.qubership.atp.svp.repo.impl.FilePageConfigurationRepository;
import org.qubership.atp.svp.repo.impl.GitPageConfigurationRepository;
import org.qubership.atp.svp.service.direct.IntegrationServiceImpl;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.qubership.atp.svp.tests.TestWithTestData;

@RunWith(SpringRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest(classes = ProjectEventKafkaListener.class,
        properties = {"spring.cloud.vault.enabled=false", "spring.cloud.consul.config.enabled=false",
                "kafka.project.event.enable= true", "svp.projects.config.path=./config/project"})
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class ProjectEventKafkaListenerUnitTest extends TestWithTestData {

    @Autowired
    ProjectEventKafkaListener projectEventKafkaListener;

    @SpyBean
    ProjectConfigService projectService;
    @MockBean
    FolderServiceJpa folderServiceJpa;
    @MockBean
    private Provider<UserInfo> userProvider;
    @MockBean
    private AuthTokenProvider authTokenProvider;
    @MockBean
    private ProjectConfigurationServiceJpa projectConfigurationServiceJpa;
    @MockBean
    private IntegrationServiceImpl integrationService;
    @MockBean
    FilePageConfigurationRepository filePageConfigurationRepository;
    @MockBean
    GitPageConfigurationRepository gitPageConfigurationRepository;

    @Test
    public void listen_projectEventCreateType_successAddedProject() throws JsonProcessingException {
        ProjectEvent projectEvent = new ProjectEvent();
        projectEvent.setProjectId(UUID.fromString("86ad1b70-79e0-4eca-93d2-cec7d8225f19"));
        projectEvent.setProjectName("testProject");
        projectEvent.setType(EventType.CREATE);
        ProjectConfigsEntity expectedProjectConfigsEntity = getProjectConfigsEntity();
        ArgumentCaptor<ProjectConfigsEntity> captorProjectConfigsEntity =
                ArgumentCaptor.forClass(ProjectConfigsEntity.class);

        projectEventKafkaListener.listen(projectEvent);

        Mockito.verify(folderServiceJpa, Mockito.times(1)).create(captorProjectConfigsEntity.capture(), anyString());
        ProjectConfigsEntity projectConfigs = captorProjectConfigsEntity.getValue();
        Assert.assertEquals(objectMapper.writeValueAsString(expectedProjectConfigsEntity),
                objectMapper.writeValueAsString(projectConfigs));
    }

    @Test(expected = IllegalStateException.class)
    public void listen_projectEventUpdateType_successAddedProject() throws JsonProcessingException {
        ProjectEvent projectEvent = new ProjectEvent();
        projectEvent.setProjectId(UUID.fromString("86ad1b70-79e0-4eca-93d2-cec7d8225f19"));
        projectEvent.setProjectName("testProject");
        projectEvent.setType(EventType.UPDATE);
        Mockito.when(projectService.isProjectExist(any())).thenReturn(true);

        projectEventKafkaListener.listen(projectEvent);


    }

    @Test
    public void listen_projectEventDeleteType_successDeleteProject() {
        ProjectEvent projectEvent = new ProjectEvent();
        projectEvent.setProjectId(UUID.fromString("86ad1b70-79e0-4eca-93d2-cec7d8225f19"));
        projectEvent.setProjectName("testProject");
        projectEvent.setType(EventType.DELETE);

        projectEventKafkaListener.listen(projectEvent);

        Mockito.verify(projectService, Mockito.times(1)).deleteProjectConfigEntity(any());
    }
}
