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

import org.qubership.atp.svp.model.db.CommonParameterEntity;
import org.qubership.atp.svp.repo.jpa.CommonParametersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommonParametersServiceJpa {

    private final CommonParametersRepository commonParametersRepository;

    @Autowired
    public CommonParametersServiceJpa(CommonParametersRepository commonParametersRepository) {
        this.commonParametersRepository = commonParametersRepository;
    }

    /**
     * Get CommonParameterEntity from DB.
     *
     * @param projectId project id
     * @param folderName folder name
     */
    public List<CommonParameterEntity> getCommonParameter(UUID projectId, String folderName) {
        return commonParametersRepository
                .findByFolderProjectProjectIdAndFolderNameOrderBySutParameterEntityParameterOrder(projectId,
                        folderName);
    }

    public List<CommonParameterEntity> getCommonParameterEntitiesByFolderId(UUID folderId) {
        return commonParametersRepository.findAllByFolderFolderId(folderId);
    }
}
