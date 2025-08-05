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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.model.ui.UpdatePageOrderRequest;

public interface PageService {

    List<String> getPagesList(UUID projectId, String folder);

    PageConfiguration getPage(UUID projectId, String fileName, String folder);

    void createOrUpdatePage(UUID projectId, PageConfiguration page, String oldPageName, String pageName, String folder);

    void deletePage(UUID projectId, String fileName, String folder);

    void movePage(UUID projectId, String fileName, String folder, String targetFolder);

    void copyPage(UUID projectId, String fileName, String folder, String targetFolder);

    void updatePageOrder(UUID projectId, UpdatePageOrderRequest request);
}
