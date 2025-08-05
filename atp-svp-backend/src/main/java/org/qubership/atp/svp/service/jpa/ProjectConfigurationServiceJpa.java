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

package org.qubership.atp.svp.service.jpa;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.core.exceptions.project.ProjectNotFoundException;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.repo.jpa.ProjectConfigsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectConfigurationServiceJpa {

    private final ProjectConfigsRepository projectConfigsRepository;

    @Autowired
    public ProjectConfigurationServiceJpa(ProjectConfigsRepository projectConfigsRepository) {
        this.projectConfigsRepository = projectConfigsRepository;
    }

    public void saveProjectConfig(ProjectConfigsEntity projectConfigsEntity) {
        projectConfigsRepository.saveAndFlush(projectConfigsEntity);
    }

    public void saveAllProjectsConfigs(List<ProjectConfigsEntity> projectsConfigs) {
        projectConfigsRepository.saveAll(projectsConfigs);
    }

    public boolean isExistsProjectConfigById(UUID projectId) {
        return projectConfigsRepository.existsById(projectId);
    }

    public void removeProjectConfig(UUID projectConfigId) {
        projectConfigsRepository.removeProjectConfigsEntityByProjectId(projectConfigId);
        projectConfigsRepository.flush();
    }

    public ProjectConfigsEntity findProjectConfigById(UUID projectConfigId) {
        return projectConfigsRepository.findById(projectConfigId).orElseThrow(ProjectNotFoundException::new);
    }

    public List<ProjectConfigsEntity> getProjectConfigsByType(RepositoryType type) {
        return projectConfigsRepository.findProjectConfigsEntitiesByPagesSourceTypeAndGitUrlNotNull(type);
    }

    public List<ProjectConfigsEntity> getAllProjectConfigs() {
        return projectConfigsRepository.findAll();
    }

    public List<String> getProjectIds() {
        return projectConfigsRepository.getAllProjectIds();
    }
}
