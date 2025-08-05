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

import static org.qubership.atp.svp.core.RegexpConstants.HTML_LINK_REGEXP;
import static org.qubership.atp.svp.core.RegexpConstants.PATTERN_FULL_URL;
import static org.qubership.atp.svp.core.RegexpConstants.URI_REGEXP;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import javax.transaction.Transactional;

import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.qubership.atp.svp.core.enums.DisplayType;
import org.qubership.atp.svp.model.api.ram.SessionDto;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionPageEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.impl.LogCollectorSettings;
import org.qubership.atp.svp.model.logcollector.SearchResult;
import org.qubership.atp.svp.model.pot.validation.TableVsTableValidationInfo;
import org.qubership.atp.svp.model.pot.validation.ValidationInfo;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.ErrorValueObject;
import org.qubership.atp.svp.model.pot.values.LogCollectorValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.AbstractTable;
import org.qubership.atp.svp.model.table.JsonSimpleCell;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.model.table.Table;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionPageRepository;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionParameterRepository;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Data
@AllArgsConstructor
public class SessionDtoProcessorService {

    private final PotSessionPageRepository potSessionPageRepository;
    private final PotSessionParameterRepository potSessionParameterRepository;
    public static final String TIME_HEADER = "TIME";
    public static final String MESSAGE_HEADER = "MESSAGE";
    public static final String BUSINESS_SOLUTION_OBJECT_ID_PART = "/ncobject.jsp?id=";

    public static final String BUSINESS_SOLUTION = "Business Solution";

    /**
     * Converts potSessionParameter to SessionDto.
     *
     * @param session PotSession
     * @return SessionDto
     */
    @Transactional
    public SessionDto potSessionParameterConverterToDto(PotSessionEntity session) {
        List<PotSessionPageEntity> pages =
                potSessionPageRepository.findByPotSessionEntitySessionId(session.getSessionId());
        List<PotSessionParameterEntity> parameterEntities =
                potSessionParameterRepository.findByPotSessionEntitySessionId(session.getSessionId());
        log.info("[Session - {}] Started session convert for SessionDto", session.getSessionId());
        pages.forEach(potSessionPage -> potSessionPage.getPotSessionTabs()
                .forEach(potSessionParameter -> potSessionParameter.getPotSessionParameterEntities()
                        .forEach(parameter -> convertParameterFactory(parameter, session))));
        SessionDto sessionDto = new SessionDto(parameterEntities, pages);
        log.info("[Session - {}] Finished session convert for SessionDto", session.getSessionId());
        return sessionDto;
    }

    private void convertParameterFactory(PotSessionParameterEntity parameter, PotSessionEntity session) {
        DisplayType displayType = parameter.getParameterConfig().getDisplayType();
        switch (displayType) {
            case PARAM:
                break;
            case XML:
                break;
            case SSH_RESPONSE:
                break;
            case GENERATE_LINK:
                break;
            case TABLE:
            case JSON:
                handleAbstractTableParseLinkValues(parameter, session);
                break;
            case LINK:
                handleLinkValues(parameter, session);
                break;
            case INTEGRATION_LOG:
                handleLogCollectorValues(parameter);
                break;
            default:
                break;
        }
    }

    private void handleAbstractTableParseLinkValues(PotSessionParameterEntity parameter, PotSessionEntity session) {
        parameter.getArValues().forEach(abstractValueObject -> replaceValuesTableToLink(session, abstractValueObject));
        replaceValuesTableToLink(session, parameter.getEr());

        ValidationInfo validationInfo = parameter.getValidationInfo();
        if (validationInfo instanceof TableVsTableValidationInfo) {
            Map<Integer, List<String>> groupingValues =
                    ((TableVsTableValidationInfo) validationInfo).getGroupingValues();
            groupingValues.forEach((integer, strings) -> strings.replaceAll(str ->
                    extractLinksFromContent(str, session, false)));
        }
    }

    private void replaceValuesTableToLink(PotSessionEntity session, AbstractValueObject abstractValueObject) {
        if (abstractValueObject instanceof TableValueObject) {
            AbstractTable abstractTable = ((TableValueObject) abstractValueObject).getTable();
            if (abstractTable.containsRows()) {
                if (abstractTable instanceof JsonTable) {
                    ((JsonTable) abstractTable).getRows().forEach(jsonTableRow ->
                            jsonTableRow.getCells().forEach(jsonCell -> {
                                        if (jsonCell instanceof JsonSimpleCell) {
                                            JsonSimpleCell jsonSimpleCell = ((JsonSimpleCell) jsonCell);
                                            String simpleValue = jsonSimpleCell.getSimpleValue();
                                            String httpLink = extractLinksFromContent(simpleValue,
                                                    session, false);
                                            jsonSimpleCell.setSimpleValue(httpLink);
                                        }
                                    }
                            ));
                } else if (abstractTable instanceof Table) {
                    ((Table) abstractTable).getRows().forEach(tableRow ->
                            tableRow.replaceAll((key, value) ->
                                    extractLinksFromContent(value, session, false))
                    );
                }
            }
        }
    }

    private void handleLogCollectorValues(PotSessionParameterEntity parameter) {
        List<AbstractValueObject> arLcValue = new ArrayList<>();
        LogCollectorSettings sourceSettingLc = (LogCollectorSettings) parameter.getParameterConfig().getSource()
                .getSettingsByType(LogCollectorSettings.class);
        boolean isLogMessageColumnShowed = sourceSettingLc.getParameters().getIsLogMessageColumnShowed();
        List<String> previewParameters = sourceSettingLc.getParameters().getPreviewParameters();
        parameter.getArValues().forEach(abstractValueObject -> {
            if (abstractValueObject instanceof ErrorValueObject) {
                arLcValue.add(abstractValueObject);
                return;
            }
            Table table = new Table();
            List<Map<String, String>> rows = new ArrayList<>();
            List<String> headers = new ArrayList<>();
            prepareTableHeader(isLogMessageColumnShowed, previewParameters, headers);
            LogCollectorValueObject lcValueObject = (LogCollectorValueObject) abstractValueObject;
            StringBuilder lcMassageError = new StringBuilder();

            arLcValue.add(handleResponseLcErrors(lcValueObject, lcMassageError)
                    ? new ErrorValueObject(lcMassageError.toString())
                    : buildLcTable(table, rows, headers, lcValueObject));
        });
        parameter.setArValues(arLcValue);
    }

    private void prepareTableHeader(boolean isLogMessageColumnShowed, List<String> previewParameters,
                                    List<String> headers) {
        headers.add(TIME_HEADER);
        if (isLogMessageColumnShowed) {
            headers.add(MESSAGE_HEADER);
        }
        if (!previewParameters.isEmpty()) {
            headers.addAll(previewParameters);
        }
    }

    private AbstractValueObject buildLcTable(Table table, List<Map<String, String>> rows, List<String> headers,
                                             LogCollectorValueObject lcValueObject) {
        lcValueObject.getSearchResult().getComponentSearchResults().forEach(componentSearchResults ->
                componentSearchResults.getSystemSearchResults().forEach(systemSearchResults ->
                        systemSearchResults.getSearchThreadResult().forEach(searchThreadResult -> {
                            Map<String, String> row = new LinkedHashMap<>();
                            headers.forEach(header -> {
                                if (header.equals(TIME_HEADER)) {
                                    row.put(TIME_HEADER, searchThreadResult.getTimestamp());
                                } else if (header.equals(MESSAGE_HEADER)) {
                                    row.put(MESSAGE_HEADER, searchThreadResult.getMessageAsSingleString());
                                } else {
                                    row.put(header.toUpperCase(), searchThreadResult.getParameters()
                                            .get(header));
                                }
                            });
                            rows.add(row);
                            table.setRows(rows);
                        })
                ));
        headers.replaceAll(String::toUpperCase);
        table.setHeaders(headers);
        return rows.isEmpty() ? new SimpleValueObject("No data found") : new TableValueObject(table);
    }

    private boolean handleResponseLcErrors(LogCollectorValueObject lcValueObject, StringBuilder lcMassageError) {
        SearchResult searchResult = lcValueObject.getSearchResult();
        if (searchResult.hasError() && Strings.isNotBlank(searchResult.getErrorDetails())) {
            lcMassageError.append("AR: ").append(searchResult.getErrorDetails()).append(" \n ");
        }
        searchResult.getComponentSearchResults().forEach(componentSearchResults -> {
            if (componentSearchResults.hasError()
                    && Strings.isNotBlank(componentSearchResults.getErrorDetails())) {
                lcMassageError.append("ComponentName [").append(componentSearchResults.getComponentName())
                        .append("]: ").append(componentSearchResults.getErrorDetails()).append(" \n ");
            }
            componentSearchResults.getSystemSearchResults().forEach(systemSearchResults -> {
                if (systemSearchResults.hasError()
                        && Strings.isNotBlank(systemSearchResults.getErrorDetails())) {
                    lcMassageError.append("SystemName [").append(systemSearchResults.getSystemName())
                            .append("]: ").append(systemSearchResults.getErrorDetails()).append(" \n ");
                }
            });
        });
        return lcMassageError.length() > 0;
    }

    private void handleLinkValues(PotSessionParameterEntity parameter, PotSessionEntity session) {
        parameter.getArValues().forEach(arValue -> setLinkToValue(session, arValue, true));
        AbstractValueObject erValue = parameter.getEr();
        setLinkToValue(session, erValue, true);
    }

    private void setLinkToValue(PotSessionEntity session, AbstractValueObject abstractValue, boolean forceReplace) {
        if (Objects.nonNull(abstractValue) && abstractValue instanceof SimpleValueObject) {
            String value = ((SimpleValueObject) abstractValue).getValue();
            ((SimpleValueObject) abstractValue).setValue(extractLinksFromContent(value, session, forceReplace));
        }
    }

    private String extractLinksFromContent(String content, PotSessionEntity session, boolean forceReplace) {
        Environment environment = session.getExecutionConfiguration().getEnvironment();
        String businessSolutionUrl = Strings.EMPTY;
        if (environment.isExistSystem(BUSINESS_SOLUTION)) {
            businessSolutionUrl = environment.getSystem(BUSINESS_SOLUTION).getServer("HTTP").getProperties().get("url");
            log.debug("SessionDtoProcessorService - extractLinksFromContent - Business Solution Server not found!");
        }
        Matcher linkMatcher = HTML_LINK_REGEXP.matcher(content);
        Matcher urlMatcher = PATTERN_FULL_URL.matcher(content);
        String linkPrep = content.replace("&nbsp;", "").trim();
        if (linkMatcher.find()) {
            return getListHttpLink(linkPrep, businessSolutionUrl);
        } else if (forceReplace) {
            String resultHref = businessSolutionUrl + BUSINESS_SOLUTION_OBJECT_ID_PART + linkPrep;
            return urlMatcher.find() ? linkPrep : resultHref;
        }
        return linkPrep;
    }

    private String makeHttpLink(String href, String name) {
        return "<a href=\"" + href + "\">" + name + "</a>";
    }

    private String getListHttpLink(String htmlTag, String businessSolutionUrl) {
        Matcher uriMatcher = URI_REGEXP.matcher(htmlTag);
        Document htmlDoc = Jsoup.parse(htmlTag);
        Elements links = htmlDoc.select("a");
        StringBuilder result = new StringBuilder();
        StringBuilder strHref = new StringBuilder();
        links.forEach(link -> {
            String linkName = link.text().trim();
            if (!linkName.isEmpty()) {
                if (uriMatcher.find()) {
                    strHref.append(businessSolutionUrl).append(link.attr("href"));
                } else {
                    strHref.append(link.attr("href"));
                }
                result.append(makeHttpLink(strHref.toString(), linkName)).append("\n");
            }
        });
        return result.toString();
    }
}
