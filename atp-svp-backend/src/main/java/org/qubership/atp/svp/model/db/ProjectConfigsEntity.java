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

package org.qubership.atp.svp.model.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.core.exceptions.project.ProjectFileAccessException;
import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.model.ui.ProjectConfigResponseModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "project_configs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectConfigsEntity {

    @Id
    private UUID projectId;

    @Column(nullable = false)
    private String projectName;

    @Column(name = "default_lc_range", nullable = false)
    private int defaultLogCollectorSearchTimeRange;

    @Column(name = "full_info_pot", nullable = false)
    private boolean isFullInfoNeededInPot;

    @Enumerated(EnumType.STRING)
    @Column(name = "pages_source_type", nullable = false, length = 20)
    private RepositoryType pagesSourceType;

    @Column(length = 500)
    private String gitUrl;

    @Column(length = 1000)
    private String pathFolderLocalProject;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String lastUpdateUserName;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime lastUpdateDateTime;

    @JsonManagedReference
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<FolderEntity> folders;

    /**
     * Constructor converter from ProjectConfiguration to ProjectConfigsEntity.
     *
     * @param prConfig model ProjectConfiguration
     * @param lastUpdateUserName String last update username
     */
    public ProjectConfigsEntity(ProjectConfiguration prConfig, String lastUpdateUserName) {
        this.projectId = prConfig.getId();
        this.projectName = prConfig.getName();
        this.defaultLogCollectorSearchTimeRange = prConfig.getDefaultLogCollectorSearchTimeRange();
        this.gitUrl = prConfig.getRepoConfig().getGitUrl();
        this.isFullInfoNeededInPot = prConfig.getIsFullInfoNeededInPot();
        this.pagesSourceType = prConfig.getRepoConfig().getType();
        this.pathFolderLocalProject = prConfig.getRepoConfig().getPath();
        this.lastUpdateUserName = lastUpdateUserName;
    }

    /**
     * Constructor converter from ProjectConfigResponseModel to ProjectConfigsEntity.
     */
    public ProjectConfigsEntity(ProjectConfigResponseModel projectConfigResponseModel, String lastUpdateUserName,
                                UUID projectId, String fullPathToProjectFolder) {
        this.projectId = projectId;
        this.projectName = projectConfigResponseModel.getProjectName();
        this.defaultLogCollectorSearchTimeRange = projectConfigResponseModel.getDefaultLogCollectorSearchTimeRange();
        this.gitUrl = projectConfigResponseModel.getGitUrl();
        this.isFullInfoNeededInPot = projectConfigResponseModel.getIsFullInfoNeededInPot();
        this.pagesSourceType = projectConfigResponseModel.getType();
        this.lastUpdateUserName = lastUpdateUserName;
        this.pathFolderLocalProject = fullPathToProjectFolder;
    }

    /**
     * Get path to folder if it exists.
     */
    public String getConfigPath(String folder) {
        String configPath = this.pathFolderLocalProject;
        checkAccess(configPath);
        if (!FolderEntity.DEFAULT_FOLDER_NAME.equals(folder)) {
            configPath += "/" + folder;
        }
        return configPath;
    }

    private void checkAccess(String configPath) {
        Path path = Paths.get(configPath);
        if (!Files.isWritable(path.getParent()) || !Files.isWritable(path)) {
            throw new ProjectFileAccessException();
        }
    }
}
