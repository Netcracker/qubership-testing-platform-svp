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

package org.qubership.atp.svp.service.direct.displaytype;

import org.qubership.atp.svp.core.enums.DisplayType;
import org.qubership.atp.svp.service.DisplayTypeService;
import org.qubership.atp.svp.service.DisplayTypeServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DisplayTypeServiceFactoryImpl implements DisplayTypeServiceFactory {

    private JsonDisplayTypeServiceImpl jsonDisplayTypeService;
    private LinkDisplayTypeServiceImpl linkDisplayTypeService;
    private ParamDisplayTypeServiceImpl paramDisplayTypeService;
    private SshResponseDisplayTypeServiceImpl sshResponseDisplayTypeService;
    private TableDisplayTypeServiceImpl tableDisplayTypeService;
    private XmlDisplayTypeServiceImpl xmlDisplayTypeService;
    private IntegrationLogDisplayTypeServiceImpl integrationLogDisplayType;

    private GenerateLinkDisplayTypeServiceImpl generateLinkDisplayTypeService;

    /**
     * Constructor for full initialization of factory.
     */
    @Autowired
    public DisplayTypeServiceFactoryImpl(JsonDisplayTypeServiceImpl jsonDisplayTypeService,
                                         LinkDisplayTypeServiceImpl linkDisplayTypeService,
                                         ParamDisplayTypeServiceImpl paramDisplayTypeService,
                                         SshResponseDisplayTypeServiceImpl sshResponseDisplayTypeService,
                                         TableDisplayTypeServiceImpl tableDisplayTypeService,
                                         XmlDisplayTypeServiceImpl xmlDisplayTypeService,
                                         IntegrationLogDisplayTypeServiceImpl integrationLogDisplayType,
                                         GenerateLinkDisplayTypeServiceImpl generateLinkDisplayTypeService) {
        this.jsonDisplayTypeService = jsonDisplayTypeService;
        this.linkDisplayTypeService = linkDisplayTypeService;
        this.paramDisplayTypeService = paramDisplayTypeService;
        this.sshResponseDisplayTypeService = sshResponseDisplayTypeService;
        this.tableDisplayTypeService = tableDisplayTypeService;
        this.xmlDisplayTypeService = xmlDisplayTypeService;
        this.integrationLogDisplayType = integrationLogDisplayType;
        this.generateLinkDisplayTypeService = generateLinkDisplayTypeService;
    }

    @Override
    public DisplayTypeService getServiceForType(DisplayType type) {
        switch (type) {
            case XML:
                return xmlDisplayTypeService;
            case JSON:
                return jsonDisplayTypeService;
            case LINK:
                return linkDisplayTypeService;
            case GENERATE_LINK:
                return generateLinkDisplayTypeService;
            case TABLE:
                return tableDisplayTypeService;
            case SSH_RESPONSE:
                return sshResponseDisplayTypeService;
            case PARAM:
                return paramDisplayTypeService;
            case INTEGRATION_LOG:
                return integrationLogDisplayType;
            default:
                throw new UnsupportedOperationException("Can't get service for unknown display type!");
        }
    }
}
