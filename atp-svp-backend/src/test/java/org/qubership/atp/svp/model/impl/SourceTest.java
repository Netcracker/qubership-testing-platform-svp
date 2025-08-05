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

import java.util.Collections;

import org.junit.Test;

import org.qubership.atp.svp.core.exceptions.SourceSettingNotFoundException;

public class SourceTest {

    @Test(expected = SourceSettingNotFoundException.class)
    public void getSettingsByType_settingNotExists_ThrowsSpecificException() {
        Source source = new Source();
        source.setSettings(Collections.singleton(new TableSettings("tableName")));

        source.getSettingsByType(JsonParseSettings.class);
    }
}
