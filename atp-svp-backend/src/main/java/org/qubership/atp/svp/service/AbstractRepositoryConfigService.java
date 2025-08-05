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

package org.qubership.atp.svp.service;

import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.repo.PageConfigurationRepository;
import org.qubership.atp.svp.repo.impl.FilePageConfigurationRepository;
import org.qubership.atp.svp.repo.impl.GitPageConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public abstract class AbstractRepositoryConfigService {

    private FilePageConfigurationRepository fileRepo;
    private GitPageConfigurationRepository gitRepo;

    @Autowired
    public final void setFileRepo(FilePageConfigurationRepository fileRepo) {
        this.fileRepo = fileRepo;
    }

    @Autowired
    public final void setGitRepo(GitPageConfigurationRepository gitRepo) {
        this.gitRepo = gitRepo;
    }

    /**
     * Returns the instance of repository based on type set in configuration.
     */
    protected PageConfigurationRepository getRepoForConfig(ProjectConfigsEntity config) {
        switch (config.getPagesSourceType()) {
            case GIT:
                return this.gitRepo;
            case LOCAL:
            default:
                return this.fileRepo;
        }
    }
}
