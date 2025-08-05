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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.qubership.atp.svp.model.configuration.FolderConfiguration;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Entity
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "folders", uniqueConstraints = {@UniqueConstraint(name = "FOLDER_NAME_AND_PROJECT_ID_CONSTRAINT",
        columnNames = {"name", "project_id"})})
public class FolderEntity {

    public static final String DEFAULT_FOLDER_NAME = "Default";

    @Id
    @Column(name = "folder_id")
    private UUID folderId;

    private String name;

    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "project_id", foreignKey = @ForeignKey(name = "FK_project_id__project"), nullable = false)
    private ProjectConfigsEntity project;

    @ToString.Exclude
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime lastUpdateDateTime;

    @JsonManagedReference
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PageConfigurationEntity> pages;

    @OrderBy("keyOrder")
    @JsonManagedReference
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<KeyParameterEntity> keyParameterEntities;

    @JsonManagedReference
    @OneToMany(mappedBy = "folder", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<CommonParameterEntity> commonParameters;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Column(name = "source_id")
    private UUID sourceId;

    /**
     * Constructor for creating FolderEntity from String.
     *
     * @param project project
     * @param name folder name
     */
    public FolderEntity(ProjectConfigsEntity project, String name) {
        this.folderId = UUID.randomUUID();
        this.name = name;
        this.project = project;
    }

    /**
     * Constructor for converting ProjectConfigsEntity to FolderEntity.
     *
     * @param project project
     * @param folderConfiguration folder_config.json
     */
    public FolderEntity(ProjectConfigsEntity project, FolderConfiguration folderConfiguration) {
        if (Objects.nonNull(folderConfiguration.getId())) {
            this.folderId = folderConfiguration.getId();
        } else {
            this.folderId = UUID.randomUUID();
        }
        this.name = folderConfiguration.getName();
        this.project = project;
        this.sourceId = folderConfiguration.getSourceId();
    }

    /**
     * Add page.
     *
     * @param page page entity
     */
    public void addPage(PageConfigurationEntity page) {
        if (pages == null) {
            pages = new ArrayList<>();
        }
        pages.add(page);
    }

    /**
     * Remove page.
     *
     * @param page page entity
     */
    public void removePage(PageConfigurationEntity page) {
        pages.remove(page);
        page.setFolder(null);
    }

    /**
     * Add keyParameter.
     *
     * @param keyParameter keyParameter entity
     */
    public void addKeyParameter(KeyParameterEntity keyParameter) {
        if (keyParameterEntities == null) {
            keyParameterEntities = new ArrayList<KeyParameterEntity>();
        }
        keyParameterEntities.add(keyParameter);
    }

    /**
     * Add commonParameter.
     *
     * @param commonParameter commonParameter entity
     */
    public void addCommonParameter(CommonParameterEntity commonParameter) {
        if (commonParameters == null) {
            commonParameters = new ArrayList<CommonParameterEntity>();
        }
        commonParameters.add(commonParameter);
    }
}
