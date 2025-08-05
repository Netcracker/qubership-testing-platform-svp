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
import java.util.stream.Collectors;

import org.qubership.atp.svp.model.configuration.KeyParameterConfiguration;
import org.qubership.atp.svp.repo.jpa.KeyParametersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KeyParameterServiceJpa {

    private final KeyParametersRepository keyParametersRepository;

    @Autowired
    public KeyParameterServiceJpa(KeyParametersRepository keyParametersRepository) {
        this.keyParametersRepository = keyParametersRepository;
    }

    public List<KeyParameterConfiguration> getKeyParameters(UUID folderId) {
        return keyParametersRepository.findByFolderFolderId(folderId).stream()
                .map(KeyParameterConfiguration::new).collect(Collectors.toList());
    }
}
