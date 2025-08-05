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

package org.qubership.atp.svp.model.ui;

import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ProjectConfigResponseModel {
    RepositoryType type;
    String gitUrl;
    String projectName;
    int defaultLogCollectorSearchTimeRange;
    Boolean isFullInfoNeededInPot;
    Boolean isMigrateProject;

    /**
     * Constructor converter from ProjectConfigsEntity to ProjectConfigResponseModel.
     * @param projectConfigsEntity model ProjectConfigsEntity.
     */
    public ProjectConfigResponseModel(ProjectConfigsEntity projectConfigsEntity) {
        this.type = projectConfigsEntity.getPagesSourceType();
        this.gitUrl = projectConfigsEntity.getGitUrl();
        this.projectName = projectConfigsEntity.getProjectName();
        this.defaultLogCollectorSearchTimeRange = projectConfigsEntity.getDefaultLogCollectorSearchTimeRange();
        this.isFullInfoNeededInPot = projectConfigsEntity.isFullInfoNeededInPot();
        this.isMigrateProject = false;
    }
}


