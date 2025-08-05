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

package org.qubership.atp.svp.model.impl;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogCollectorSettingsParameters {

    private String searchText;
    private String graylogSearchText;
    private int lines;
    private int minutes;
    private String query;
    private boolean isRegexp;
    private boolean readOnlyMode;
    private boolean searchPerOneDay;
    private String searchStrategy;
    private List<String> previewParameters;
    private boolean isLogMessageColumnShowed;

    public boolean getIsLogMessageColumnShowed() {
        return isLogMessageColumnShowed;
    }

    public void setIsLogMessageColumnShowed(boolean isLogMessageColumnShowed) {
        this.isLogMessageColumnShowed = isLogMessageColumnShowed;
    }
}
