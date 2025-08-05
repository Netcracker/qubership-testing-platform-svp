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

package org.qubership.atp.svp.service.direct.displaytype.jsonparse;

import org.qubership.atp.svp.model.impl.JsonParseSettings;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.service.JsonParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RawSimpleServiceImpl implements JsonParseService {

    private CommonJsonParseService commonJsonParseService;

    @Autowired
    public RawSimpleServiceImpl(CommonJsonParseService commonJsonParseService) {
        this.commonJsonParseService = commonJsonParseService;
    }

    @Override
    public AbstractValueObject parse(String jsonAsString, JsonParseSettings settings) {
        if (settings.getIsFirstValueParseHowRaw()) {
            return new SimpleValueObject(commonJsonParseService
                    .getFirstValueByJsonAsStringByJsonPath(jsonAsString, settings.getJsonPath()));
        } else {
            return new SimpleValueObject(commonJsonParseService
                    .getJsonAsStringByJsonPath(jsonAsString, settings.getJsonPath()));
        }
    }
}
