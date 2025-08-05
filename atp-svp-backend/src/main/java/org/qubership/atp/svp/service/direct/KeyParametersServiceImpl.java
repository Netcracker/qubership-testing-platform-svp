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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.svp.core.exceptions.key.parameters.KeyParameterDbSaveException;
import org.qubership.atp.svp.core.exceptions.key.parameters.KeyParametersGetException;
import org.qubership.atp.svp.model.configuration.KeyParameterConfiguration;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.KeyParameterEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.ui.KeyParameterRequest;
import org.qubership.atp.svp.service.AbstractRepositoryConfigService;
import org.qubership.atp.svp.service.KeyParametersService;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.KeyParameterServiceJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KeyParametersServiceImpl extends AbstractRepositoryConfigService implements KeyParametersService {

    private final FolderServiceJpa folderServiceJpa;
    private final KeyParameterServiceJpa keyParameterServiceJpa;

    @Autowired
    public KeyParametersServiceImpl(FolderServiceJpa folderServiceJpa, KeyParameterServiceJpa keyParameterServiceJpa) {
        this.folderServiceJpa = folderServiceJpa;
        this.keyParameterServiceJpa = keyParameterServiceJpa;
    }

    @Override
    public List<KeyParameterConfiguration> getKeyParameters(UUID projectId, String folderName) {
        log.info("Getting the key parameters for project {} in folder {}", projectId, folderName);
        try {
            FolderEntity folderEntity = folderServiceJpa.getFolderByProjectIdAndName(projectId, folderName);
            return keyParameterServiceJpa.getKeyParameters(folderEntity.getFolderId());
        } catch (DataAccessException e) {
            log.error("KeyParametersServiceImpl - getKeyParameters(), projectId: {}, folder: {} ", projectId,
                    folderName, e);
            throw new KeyParametersGetException();
        }
    }

    @Override
    @Transactional
    public void createOrUpdateKeyParameters(UUID projectId, KeyParameterRequest request) {
        String folderName = request.getFolder();
        List<KeyParameterConfiguration> keyParameters = request.getKeyParameters();
        log.info("Updating Key Parameters: {}, projectId: {}, folder Name: {}", keyParameters, projectId, folderName);
        try {
            FolderEntity folderEntity = folderServiceJpa.getFolderByProjectIdAndName(projectId, folderName);
            folderEntity.getKeyParameterEntities().removeAll(folderEntity.getKeyParameterEntities());
            for (int order = 0; order < keyParameters.size(); order++) {
                folderEntity.getKeyParameterEntities()
                        .add(new KeyParameterEntity(folderEntity, keyParameters.get(order), order));
            }
            folderServiceJpa.update(folderEntity);
            updateLocalAndGit(folderName, folderEntity);
        } catch (DataAccessException e) {
            log.error("KeyParametersServiceImpl - createOrUpdateKeyParameters(), projectId: {}, folder: {} ", projectId,
                    folderName);
            throw new KeyParameterDbSaveException();
        }
        log.info("Key Parameters updated successfully");
    }

    private void updateLocalAndGit(String folderName, FolderEntity folderEntity) {
        ProjectConfigsEntity projectEntity = folderEntity.getProject();
        List<KeyParameterConfiguration> keyParameters = folderEntity.getKeyParameterEntities()
                .stream().map(KeyParameterConfiguration::new).collect(Collectors.toList());
        getRepoForConfig(projectEntity).updateKeyParameters(projectEntity, keyParameters, folderName);
    }
}
