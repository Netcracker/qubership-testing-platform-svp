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

import org.qubership.atp.svp.core.enums.JsonParseViewType;
import org.qubership.atp.svp.service.JsonParseService;
import org.qubership.atp.svp.service.JsonParseTypeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonParseTypeFactoryImpl implements JsonParseTypeFactory {

    private RawSimpleServiceImpl rawSimpleServiceImpl;
    private TableServiceImpl tableServiceImpl;
    private HierarchyTableServiceImpl hierarchyTableServiceImpl;
    private HierarchyTreeObjectTableServiceImpl hierarchyTreeObjectTableServiceImpl;

    /**
     * Constructor for full initialization of factory.
     */
    @Autowired
    public JsonParseTypeFactoryImpl(RawSimpleServiceImpl rawSimpleServiceImpl,
                                    TableServiceImpl tableServiceImpl,
                                    HierarchyTableServiceImpl hierarchyTableServiceImpl,
                                    HierarchyTreeObjectTableServiceImpl hierarchyTreeObjectTableServiceImpl) {

        this.rawSimpleServiceImpl = rawSimpleServiceImpl;
        this.tableServiceImpl = tableServiceImpl;
        this.hierarchyTableServiceImpl = hierarchyTableServiceImpl;
        this.hierarchyTreeObjectTableServiceImpl = hierarchyTreeObjectTableServiceImpl;
    }

    @Override
    public JsonParseService getJsonParseForType(JsonParseViewType type) {
        switch (type) {
            case RAW:
                return rawSimpleServiceImpl;
            case TABLE:
                return tableServiceImpl;
            case HIERARCHY_TABLE:
                return hierarchyTableServiceImpl;
            case HIERARCHY_TREE_OBJECT_TABLE:
                return hierarchyTreeObjectTableServiceImpl;
            default:
                throw new IllegalStateException("Could not processing json value for type: " + type);
        }
    }
}
