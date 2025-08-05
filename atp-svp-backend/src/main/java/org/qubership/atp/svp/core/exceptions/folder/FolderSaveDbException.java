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

package org.qubership.atp.svp.core.exceptions.folder;

import org.qubership.atp.svp.core.exceptions.AtpSvpException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "SVP-2002")
public class FolderSaveDbException extends AtpSvpException {

    private static final String DEFAULT_MESSAGE = "The folder can't be saved in DB";

    public FolderSaveDbException() {
        super(DEFAULT_MESSAGE);
    }
}
