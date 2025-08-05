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

import java.util.ArrayList;
import java.util.List;

import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.core.exceptions.VariableException;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.Server;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.repo.impl.SshRepository;
import org.qubership.atp.svp.service.DefaultDisplayTypeService;
import org.qubership.atp.svp.service.direct.ExecutionVariablesServiceImpl;
import org.qubership.automation.pc.comparator.impl.FullTextComparator;
import org.qubership.automation.pc.compareresult.DiffMessage;
import org.qubership.automation.pc.configuration.parameters.Parameters;
import org.qubership.automation.pc.core.exceptions.ComparatorException;
import org.qubership.automation.pc.core.helpers.BuildColoredText;
import org.qubership.automation.pc.models.HighlighterResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class SshResponseDisplayTypeServiceImpl extends DefaultDisplayTypeService {

    private final FullTextComparator fullTextComparator = new FullTextComparator();

    private final ExecutionVariablesServiceImpl executionVariablesService;
    private final SshRepository sshRepository;

    @Autowired
    public SshResponseDisplayTypeServiceImpl(ExecutionVariablesServiceImpl executionVariablesService,
                                             SshRepository sshRepository) {
        this.executionVariablesService = executionVariablesService;
        this.sshRepository = sshRepository;
    }

    @Override
    public AbstractValueObject getValueFromSource(Source source, AbstractParameterExecutionContext context)
            throws GettingValueException {

        try {
            Server server = context.getSessionConfiguration()
                    .getEnvironment()
                    .getSystem(source.getSystem())
                    .getServer(source.getConnection());
            String sshCommand = executionVariablesService.getSourceWithExecutionVariables(source.getScript(),
                    context.getExecutionVariables());
            String resultAsString = "";
            if (source.getEngineType() == EngineType.SSH) {
                resultAsString = sshRepository.executeCommandSsh(server, sshCommand);
            } else {
                throw new IllegalStateException("Unexpected EngineType value: "
                        + source.getEngineType() + " for DisplayType: SSH_RESPONSE");
            }
            return new SimpleValueObject(resultAsString);
        } catch (VariableException e) {
            throw new GettingValueException(e.getMessage(), e.getMessage());
        } catch (Exception e) {
            throw new GettingValueException(e.getMessage());
        }
    }

    @Override
    public void validateParameter(AbstractParameterExecutionContext context)
            throws ValidationException {
        PotSessionParameterEntity parameter = context.getParameter();
        try {
            List<DiffMessage> overallDiffs = new ArrayList<>();
            for (AbstractValueObject arValue : parameter.getArValues()) {
                SimpleValueObject er = (SimpleValueObject) parameter.getEr();
                SimpleValueObject ar = (SimpleValueObject) arValue;
                List<DiffMessage> diffs = fullTextComparator.compare(er.getValue(), ar.getValue(), new Parameters());
                overallDiffs.addAll(diffs);
                if (context.getSessionConfiguration().shouldHighlightDiffs()) {
                    HighlighterResult highlighterResult = BuildColoredText.highlight(diffs,
                            er.getValue(), ar.getValue());
                    er.setHighlightedEr(highlighterResult.getEr().getComposedValue(false));
                    ar.setHighlightedAr(highlighterResult.getAr().getComposedValue(false));
                }
            }
            parameter.setValidationStatus(calculateStatusByDiffs(overallDiffs));
        } catch (ComparatorException ex) {
            throw new ValidationException("Error occurred on trying to validate '"
                    + parameter.getPath() + "' as request command ssh!", ex);
        }
    }
}
