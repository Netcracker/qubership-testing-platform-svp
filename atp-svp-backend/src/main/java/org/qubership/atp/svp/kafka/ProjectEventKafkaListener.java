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

import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.svp.model.api.kafka.EventType;
import org.qubership.atp.svp.model.api.kafka.ProjectEvent;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.service.direct.ProjectConfigService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "kafka.project.event.enable", havingValue = "true")
@Slf4j
public class ProjectEventKafkaListener implements ProjectEventListener<ProjectEvent> {

    private final ProjectConfigService projectService;
    private final FolderServiceJpa folderServiceJpa;

    @Autowired
    public ProjectEventKafkaListener(ProjectConfigService projectService, FolderServiceJpa folderServiceJpa) {
        this.projectService = projectService;
        this.folderServiceJpa = folderServiceJpa;
    }

    @Override
    @KafkaListener(topics = "${kafka.project.event.consumer.topic.name:catalog_notification_topic}",
            containerFactory = "stringProjectEventConcurrentKafkaListenerContainerFactory",
            errorHandler = "defaultKafkaValidationErrorHandler")
    @Transactional
    public void listen(@Payload @Valid @NonNull ProjectEvent projectEvent) {
        String projectName = projectEvent.getProjectName();
        UUID projectId = projectEvent.getProjectId();
        EventType type = projectEvent.getType();
        String message = "{} the Project: '{}', Id: '{}', by event Kafka";
        switch (type) {
            case CREATE: {
                log.info(message, type, projectName, projectId);
                if (!projectService.isProjectExist(projectId)) {
                    ProjectConfigsEntity projectConfig =
                            projectService.createProjectConfigDb(projectName, projectId, true);
                    if (!folderServiceJpa.isExistFolderByProjectId(projectId)) {
                        folderServiceJpa.create(projectConfig, "Default");
                    }
                }
                break;
            }
            case DELETE: {
                log.info(message, type, projectName, projectId);
                projectService.deleteProjectConfigEntity(projectId);
                break;
            }
            default: {
                throw new IllegalStateException("Unknown type of events " + type);
            }
        }
    }
}

