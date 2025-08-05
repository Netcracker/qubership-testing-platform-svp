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

import org.qubership.atp.svp.core.exceptions.common.parameters.CommonParametersDbSaveException;
import org.qubership.atp.svp.core.exceptions.common.parameters.CommonParametersGetException;
import org.qubership.atp.svp.model.db.CommonParameterEntity;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.impl.SutParameter;
import org.qubership.atp.svp.service.AbstractRepositoryConfigService;
import org.qubership.atp.svp.service.CommonParametersService;
import org.qubership.atp.svp.service.jpa.CommonParametersServiceJpa;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommonParametersServiceImpl extends AbstractRepositoryConfigService implements CommonParametersService {

    private final FolderServiceJpa folderServiceJpa;
    private final CommonParametersServiceJpa commonParametersServiceJpa;

    public CommonParametersServiceImpl(FolderServiceJpa folderServiceJpa,
                                       CommonParametersServiceJpa commonParametersServiceJpa) {
        this.folderServiceJpa = folderServiceJpa;
        this.commonParametersServiceJpa = commonParametersServiceJpa;
    }

    /**
     * Update common parameters file.
     *
     * @param commonParameters list {@link SutParameter}.
     */
    @Override
    @Transactional
    public void createOrUpdateCommonParameters(UUID projectId, List<SutParameter> commonParameters, String folderName) {
        log.info("Updating Common Parameters with projectId: " + projectId + ", folder Name: " + folderName);
        try {
            FolderEntity folderEntity = folderServiceJpa.getFolderByProjectIdAndName(projectId, folderName);
            createAndUpdate(commonParameters, folderEntity);
            updateLocalAndGit(folderName, folderEntity);
        } catch (DataAccessException e) {
            log.error("CommonParametersServiceImpl - createOrUpdateCommonParameters(), projectId: {}, folder: {} ",
                    projectId, folderName, e);
            throw new CommonParametersDbSaveException();
        }
        log.info("Common Parameters with projectId: {} in folder: {} updated successfully", projectId, folderName);
    }

    private void createAndUpdate(List<SutParameter> commonParameters, FolderEntity folderEntity) {
        folderEntity.getCommonParameters().removeAll(folderEntity.getCommonParameters());
        for (int order = 0; order < commonParameters.size(); order++) {
            SutParameter param = commonParameters.get(order);
            folderEntity.getCommonParameters().add(new CommonParameterEntity(folderEntity, param, order));
        }
        folderServiceJpa.update(folderEntity);
    }

    private void updateLocalAndGit(String folderName, FolderEntity folderEntity) {
        List<SutParameter> commonParameters =
                folderEntity.getCommonParameters().stream().map(SutParameter::new).collect(Collectors.toList());
        ProjectConfigsEntity projectEntity = folderEntity.getProject();
        getRepoForConfig(projectEntity).updateCommonParameters(projectEntity, commonParameters, folderName);
    }

    /**
     * Get list {@link SutParameter}.
     */
    @Override
    @Transactional
    public List<SutParameter> getCommonParameters(UUID projectId, String folderName) {
        log.info("Getting the common parameters with project {} in folder {}", projectId, folderName);
        try {
            List<CommonParameterEntity> commonParameterEntities =
                    commonParametersServiceJpa.getCommonParameter(projectId, folderName);
            return commonParameterEntities.stream()
                    .map(parameter -> new SutParameter(parameter.getSutParameterEntity()))
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("CommonParametersServiceImpl - getCommonParameters(), projectId: {}, folder: {} ",
                    projectId, folderName, e);
            throw new CommonParametersGetException();
        }
    }
}
