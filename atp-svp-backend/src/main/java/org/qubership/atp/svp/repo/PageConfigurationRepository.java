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

package org.qubership.atp.svp.repo;

import java.util.List;

import org.qubership.atp.svp.core.exceptions.PageStorageException;
import org.qubership.atp.svp.model.configuration.KeyParameterConfiguration;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.model.impl.SutParameter;
import org.qubership.atp.svp.model.project.ProjectConfiguration;

public interface PageConfigurationRepository {

    List<PageConfiguration> getPageConfigurations(ProjectConfiguration config, String folder)
            throws PageStorageException;

    void createOrUpdatePageConfiguration(ProjectConfigsEntity repositoryConfig,
                                         PageConfiguration pageConfiguration,
                                         String folder);

    void bulkCreateOrUpdatePageConfiguration(ProjectConfigsEntity repositoryConfig,
                                             List<PageConfiguration> pageConfigurationList,
                                             String folder);

    void deletePageConfigurations(ProjectConfigsEntity config, String name, String folder);

    List<KeyParameterConfiguration> getKeyParameters(ProjectConfiguration config, String folder);

    void updateKeyParameters(ProjectConfigsEntity config, List<KeyParameterConfiguration> keyParameters, String folder);

    void updateCommonParameters(ProjectConfigsEntity config, List<SutParameter> commonParameters, String folder);

    List<SutParameter> getCommonParameters(ProjectConfiguration config, String folder);

    void copyPage(ProjectConfigsEntity config, String name, String newName,
                  String folder, String targetFolder, int newOrder);

    void movePage(ProjectConfigsEntity config, String name, String targetName,
                  String folder, String targetFolder, int newOrder);

    void createFolder(ProjectConfigsEntity config, FolderEntity folder);

    void editFolder(ProjectConfigsEntity config, String oldName, FolderEntity folder);

    List<String> getFolders(ProjectConfiguration config);

    void deleteFolder(ProjectConfigsEntity config, String folder);

    void importProject(ProjectConfigsEntity config, List<FolderEntity> folders);

    void initializationProjectConfigs(ProjectConfigsEntity config);
}
