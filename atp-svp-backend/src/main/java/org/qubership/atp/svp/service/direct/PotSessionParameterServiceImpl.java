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

package org.qubership.atp.svp.service.direct;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.transaction.Transactional;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.svp.core.enums.DisplayType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.GettingValueException;
import org.qubership.atp.svp.core.exceptions.ValidationException;
import org.qubership.atp.svp.model.db.SutParameterEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.pot.AbstractParameterExecutionContext;
import org.qubership.atp.svp.model.pot.PotFile;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.ErrorValueObject;
import org.qubership.atp.svp.model.pot.values.LogCollectorValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.model.table.Table;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionParameterRepository;
import org.qubership.atp.svp.service.AbstractMessagingService;
import org.qubership.atp.svp.service.DisplayTypeService;
import org.qubership.atp.svp.service.DisplayTypeServiceFactory;
import org.qubership.atp.svp.service.PotSessionParameterService;
import org.qubership.atp.svp.service.PotSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PotSessionParameterServiceImpl extends AbstractMessagingService implements PotSessionParameterService {

    private final DisplayTypeServiceFactory displayTypeServiceFactory;
    private final PotSessionParameterRepository potSessionParameterRepository;

    private final PotSessionService potSessionService;

    /**
     * Constructor for {@link PotSessionParameterServiceImpl} instance.
     */
    @Autowired
    public PotSessionParameterServiceImpl(DisplayTypeServiceFactory displayTypeServiceFactory,
                                          PotSessionParameterRepository potSessionParameterRepository,
                                          PotSessionService potSessionService) {
        this.displayTypeServiceFactory = displayTypeServiceFactory;
        this.potSessionParameterRepository = potSessionParameterRepository;
        this.potSessionService = potSessionService;
    }

    @Override
    public void startGettingInfoForParameter(AbstractParameterExecutionContext context) {
        SessionExecutionConfiguration executionConfiguration = context.getSessionConfiguration();
        log.info("[Session - {}] Getting info for parameter: {}. Session execution configuration: {}.",
                context.getSessionId(), context.getParameter().getPath(), executionConfiguration);
        loadActualResults(context);
        if (executionConfiguration.getIsPotGenerationMode()) {
            processParameterResultsForPotReport(context.getParameter());
        }
        if (context.getParameter().shouldHaveExpectedResult()) {
            loadExpectedResult(context);
        }
        setValidationStatusForParameterWithError(context);
        log.info("[Session - {}] Finished getting info for parameter: {}.",
                context.getSessionId(), context.getParameter().getPath());
    }

    private void loadActualResults(AbstractParameterExecutionContext context) {
        log.info("[Session - {}] Loading actual results for {}...", context.getSessionId(),
                context.getParameter().getPath());
        SutParameterEntity parameterConfiguration = context.getParameter().getParameterConfig();
        AbstractValueObject arValue = getResultBodyForSource(context, parameterConfiguration.getSource());
        storeNonDeferredActualResultToContext(arValue, context);
        //Exclude LC since the functionality of additional AR has not yet been implemented
        if (!(arValue instanceof LogCollectorValueObject)) {
            for (Source additionalSource : parameterConfiguration.getAdditionalSources()) {
                AbstractValueObject additionalArValue = getResultBodyForSource(context, additionalSource);
                storeNonDeferredActualResultToContext(additionalArValue, context);
            }
        }
        log.info("[Session - {}] Actual results for {} were loaded successfully.",
                context.getSessionId(), context.getParameter().getPath());
    }

    private void processParameterResultsForPotReport(PotSessionParameterEntity parameter) {
        for (int i = 0; i < parameter.getArValues().size(); i++) {
            if (parameter.shouldCreateSeparateFileForPotReport()) {
                String fileNamePostfix = parameter.getParameterConfig()
                        .getAdditionalSources()
                        .isEmpty() ? StringUtils.EMPTY : "_" + (i + 1);
                String fileName = getFileNameForValueAsFile(parameter.getPath(),
                        parameter.getParameterConfig(), fileNamePostfix);
                convertAbstractValueToPotFile(parameter.getArValues().get(i), fileName);
            }
        }
    }

    private void loadExpectedResult(AbstractParameterExecutionContext context) {
        log.info("[Session - {}] Loading expected result for {}...", context.getSessionId(),
                context.getParameter().getPath());
        SutParameterEntity parameterConfiguration = context.getParameter().getParameterConfig();
        switch (parameterConfiguration.getErConfig().getType()) {
            case PLAIN:
                context.getParameter().setEr(new SimpleValueObject(parameterConfiguration.getErConfig().getValue()));
                break;
            case CUSTOM:
                context.getParameter().setEr(getResultBodyForSource(context,
                        parameterConfiguration.getErConfig().getDataSource()));
                break;
            default:
                log.warn("Unknown ER Type for getting expected result!");
                break;
        }
        log.info("[Session - {}] Expected result for {} was loaded successfully.", context.getSessionId(),
                context.getParameter().getPath());
    }

    private void setValidationStatusForParameterWithError(AbstractParameterExecutionContext context) {
        PotSessionParameterEntity parameter = context.getParameter();
        if (parameter.hasErrors()) {
            parameter.setValidationStatus(ValidationStatus.WARNING);
        } else if (parameter.hasLogCollectorErrors()) {
            parameter.setValidationStatus(ValidationStatus.LC_WARNING);
        }
    }

    private AbstractValueObject getResultBodyForSource(AbstractParameterExecutionContext context, Source source) {
        log.info("SutParameterExecutor - getResultBodyForSource - system: {}, connection: {}",
                source.getSystem(), source.getConnection());
        try {
            DisplayType parameterDisplayType = context.getParameter().getParameterConfig().getDisplayType();
            DisplayTypeService displayTypeService = displayTypeServiceFactory.getServiceForType(parameterDisplayType);
            return displayTypeService.getValueFromSource(source, context);
        } catch (GettingValueException e) {
            context.setDeferredSearchResult(false);
            if (Objects.nonNull(e.getSqlMessage())) {
                return new ErrorValueObject(e.getMessage(), e.getSqlMessage());
            } else {
                return new ErrorValueObject(e.getMessage());
            }
        } catch (RuntimeException e) {
            return new ErrorValueObject(e.getMessage());
        } finally {
            //It is necessary not to send large sql script to the websocket
            source.setScript(Strings.EMPTY);
        }
    }

    private void storeNonDeferredActualResultToContext(AbstractValueObject arValue,
                                                       AbstractParameterExecutionContext context) {
        if (!context.isDeferredSearchResult()) {
            context.getParameter().addArValue(arValue);
        }
    }

    private String getFileNameForValueAsFile(String parameterPath,
                                             SutParameterEntity parameterConfig,
                                             String fileNamePostfix) {
        String extension;
        switch (parameterConfig.getDisplayType()) {
            case XML:
                extension = ".xml";
                break;
            case JSON:
                extension = ".json";
                break;
            case TABLE:
                extension = ".csv";
                break;
            default:
                extension = StringUtils.EMPTY;
                log.error("Unexpected DisplayType {} to create value as file for parameter {}.",
                        parameterConfig.getDisplayType(), parameterPath);
                break;
        }
        return parameterPath.replaceAll(" ", "") + fileNamePostfix + extension;
    }

    private void convertAbstractValueToPotFile(AbstractValueObject value, String fileName) {
        if (value instanceof SimpleValueObject) {
            value.setValueAsFile(convertSimpleValue((SimpleValueObject) value, fileName));
        } else if (value instanceof TableValueObject) {
            if (((TableValueObject) value).getTable() instanceof Table) {
                value.setValueAsFile(convertTableValue((TableValueObject) value, fileName));
            } else if (((TableValueObject) value).getTable() instanceof JsonTable) {
                setNameJsonTablePotFile((TableValueObject) value, fileName);
            }
        }
    }

    private PotFile convertSimpleValue(SimpleValueObject value, String fileName) {
        return new PotFile(fileName, value.getValue().getBytes(StandardCharsets.UTF_8));
    }

    private PotFile convertTableValue(TableValueObject value, String fileName) {
        Table table = (Table) value.getTable();
        StringBuilder sb = new StringBuilder();
        table.getHeaders().forEach(header -> sb.append(header).append(","));
        sb.deleteCharAt(sb.lastIndexOf(","));
        sb.append("\n");
        table.getRows().forEach(row -> {
            if (!row.isEmpty()) {
                table.getHeaders().forEach(header -> sb.append(row.getOrDefault(header, "")).append(","));
                sb.deleteCharAt(sb.lastIndexOf(","));
                sb.append("\n");
            }
        });
        return new PotFile(fileName, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void setNameJsonTablePotFile(TableValueObject value, String fileName) {
        value.getValueAsFile().setName(fileName);
    }

    @Override
    public void validateParameter(AbstractParameterExecutionContext context) {
        try {
            DisplayType parameterDisplayType = context.getParameter().getParameterConfig().getDisplayType();
            DisplayTypeService displayTypeService = displayTypeServiceFactory.getServiceForType(parameterDisplayType);
            displayTypeService.validateParameter(context);
        } catch (ValidationException e) {
            log.error("Validation error for {} !", context.getParameter().getPath(), e);
            context.getParameter().setValidationStatus(ValidationStatus.FAILED);
            context.getParameter().getValidationInfo().setErrorDescription(e.getMessage());
        }
    }

    @Override
    public void sendParameterResultToSession(AbstractParameterExecutionContext context) {
        PotSessionParameterEntity parameter = context.getParameter();
        UUID sessionId = context.getSessionId();
        log.info("Session: {}. Send results with status {} for parameter {}", sessionId,
                parameter.getValidationInfo().getStatus(), parameter.getPath());
        getMessageService(sessionId).sendSutParameterResult(sessionId, parameter);
    }

    @Override
    @Transactional
    public void updatePotSessionParameter(AbstractParameterExecutionContext context) {
        try {
            potSessionParameterRepository.saveAndFlush(context.getParameter());
        } catch (Exception e) {
            log.error("Unexpected error occurred during the save parameter: {}", context.getParameter().getPath(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void addVariable(AbstractParameterExecutionContext context) {
        try {
            potSessionService.addVariable(context.getSessionId(), context.getExecutionVariables());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PotSessionParameterEntity> getPotSessionParameters(UUID tabId, boolean isSynchronous) {
        return potSessionParameterRepository.findByPotSessionTabEntityIdAndSynchronousLoading(tabId, isSynchronous);
    }
}
