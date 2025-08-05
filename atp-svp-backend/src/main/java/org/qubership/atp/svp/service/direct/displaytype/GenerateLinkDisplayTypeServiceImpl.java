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

import static org.qubership.atp.svp.core.RegexpConstants.PATTERN_FULL_URL;

import java.util.regex.Matcher;

import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.VariableException;
import org.qubership.atp.svp.model.impl.GenerateLinkSettings;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.service.DefaultDisplayTypeService;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GenerateLinkDisplayTypeServiceImpl extends DefaultDisplayTypeService {

    private final ExecutionVariablesServiceImpl executionVariablesService;

    @Autowired
    public GenerateLinkDisplayTypeServiceImpl(ExecutionVariablesServiceImpl executionVariablesService) {
        this.executionVariablesService = executionVariablesService;
    }

    @Override
    public AbstractValueObject getValueFromSource(Source source, AbstractParameterExecutionContext context)
            throws GettingValueException {
        try {
            GenerateLinkSettings generateLinkSettings =
                    (GenerateLinkSettings) source.getSettingsByType(GenerateLinkSettings.class);
            String generatedLink = getLink(generateLinkSettings, context);
            return new SimpleValueObject(generatedLink);
        } catch (VariableException e) {
            throw new GettingValueException(e.getMessage(), e.getMessage());
        } catch (Exception e) {
            throw new GettingValueException(e.getMessage());
        }
    }

    private String getLink(GenerateLinkSettings generateLinkSettings, AbstractParameterExecutionContext context)
            throws GettingValueException {
        String url = generateLinkSettings.getUrl();
        if (Strings.isNotBlank(url)) {
            url = executionVariablesService.getSourceWithExecutionVariables(url, context.getExecutionVariables());
            validationUrl(url);
        } else {
            throw new RuntimeException("URL is blank");
        }
        String nameLink = generateLinkSettings.getNameLink();
        if (Strings.isNotBlank(nameLink)) {
            nameLink = executionVariablesService.getSourceWithExecutionVariables(nameLink,
                    context.getExecutionVariables());
        } else {
            nameLink = url;
        }
        return String.format("<a href=\"%s\">%s</a>", url, nameLink);
    }

    private void validationUrl(String url) throws GettingValueException {
        Matcher urlMatcher = PATTERN_FULL_URL.matcher(url);
        if (!urlMatcher.find()) {
            throw new GettingValueException("URL is invalid [" + url + "]");
        }
    }
}
