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

package org.qubership.atp.svp.service.direct.pot;

import static org.qubership.atp.svp.core.RegexpConstants.HTML_LINK_REGEXP;
import static org.qubership.atp.svp.core.RegexpConstants.PATTERN_FULL_URL;
import static org.qubership.atp.svp.core.RegexpConstants.URI_REGEXP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

import javax.annotation.Nullable;

import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRelation;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDecimalNumber;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.exceptions.execution.ExecutionParameterNotFoundException;
import org.qubership.atp.svp.core.exceptions.pot.PotUnexpectedException;
import org.qubership.atp.svp.model.db.GroupEntity;
import org.qubership.atp.svp.model.db.SutParameterEntity;
import org.qubership.atp.svp.model.db.TabEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionPageEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.logcollector.ComponentSearchResults;
import org.qubership.atp.svp.model.logcollector.ConfigurationType;
import org.qubership.atp.svp.model.logcollector.SearchThreadFindResult;
import org.qubership.atp.svp.model.logcollector.SystemSearchResults;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.ErrorValueObject;
import org.qubership.atp.svp.model.pot.values.HttpLink;
import org.qubership.atp.svp.model.pot.values.LinkPotValue;
import org.qubership.atp.svp.model.pot.values.LogCollectorValueObject;
import org.qubership.atp.svp.model.pot.values.PotValue;
import org.qubership.atp.svp.model.pot.values.SimplePotValue;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.JsonCell;
import org.qubership.atp.svp.model.table.JsonGroupedCell;
import org.qubership.atp.svp.model.table.JsonSimpleCell;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.model.table.JsonTableRow;
import org.qubership.atp.svp.model.table.Table;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionPageRepository;
import org.qubership.atp.svp.repo.jpa.pot.session.PotSessionParameterRepository;
import org.qubership.atp.svp.service.PotGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WordDocumentPotGenerator implements PotGenerator {

    private final PotSessionPageRepository potSessionPageRepository;
    private final PotSessionParameterRepository potSessionParameterRepository;

    @Autowired
    public WordDocumentPotGenerator(PotSessionPageRepository potSessionPageRepository,
                                    PotSessionParameterRepository potSessionParameterRepository) {
        this.potSessionPageRepository = potSessionPageRepository;
        this.potSessionParameterRepository = potSessionParameterRepository;
    }

    @Override
    public byte[] generatePotFile(PotSessionEntity session, boolean isFullInfoNeededInPot) {
        log.info("[Session - {}] Generating MS Word document with POT", session.getSessionId());
        try {
            XWPFDocument potDocument = new XWPFDocument();
            setDocumentToLandscapeView(potDocument);
            printContents(potDocument);
            addCustomHeadingStyle(potDocument, WordStyleConstants.PAGE_STYLE, 1);
            addCustomHeadingStyle(potDocument, WordStyleConstants.TAB_STYLE, 2);
            addCustomHeadingStyle(potDocument, WordStyleConstants.GROUP_STYLE, 3);
            addCustomHeadingStyle(potDocument, WordStyleConstants.PARAMETER_STYLE, 4);
            addCustomHeadingStyle(potDocument, WordStyleConstants.INTEGRATION_LOG_STYLE, 5);
            Environment environment = session.getExecutionConfiguration().getEnvironment();
            printKeyParameters(potDocument, session.getKeyParameters());

            List<PotSessionParameterEntity> commons =
                    potSessionParameterRepository.findByPotSessionEntitySessionId(session.getSessionId());
            printCommonParameters(potDocument, commons, environment);

            List<String> listOrderPages = session.getPageOrder();
            List<PotSessionPageEntity> pages =
                    potSessionPageRepository.findByPotSessionEntitySessionId(session.getSessionId());

            for (String pageName : listOrderPages) {
                Optional<PotSessionPageEntity> page = pages.stream()
                        .filter(asd -> asd.getName().equals(pageName))
                        .findFirst();
                if (page.isPresent()) {
                    printPage(potDocument, page.get(), environment, isFullInfoNeededInPot);
                    potDocument.createParagraph().createRun().addBreak();
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            potDocument.write(baos);
            potDocument.close();
            baos.close();
            log.info("[Session - {}] Document with POT was generated successfully.", session.getSessionId());
            return baos.toByteArray();
        } catch (IOException ioEx) {
            log.error("POT generation I/O error!" + ioEx.getMessage(), ioEx);
            throw new PotUnexpectedException();
        } catch (ExecutionParameterNotFoundException pnfEx) {
            log.error("POT generation failed because parameter wasn't found in session!", pnfEx);
            throw new PotUnexpectedException();
        } catch (Throwable ex) {
            log.error("Unexpected POT generation error!" + ex.getMessage(), ex);
            throw new PotUnexpectedException();
        }
    }

    private void printContents(XWPFDocument document) {
        XWPFRun run = printLine(document, "Contents", true, WordStyleConstants.FONT_VERY_LARGE,
                WordStyleConstants.PAGE_STYLE);
        run.setColor(WordStyleConstants.LIGHT_BLUE);
        XWPFParagraph paragraph = document.createParagraph();
        CTP ctP = paragraph.getCTP();
        CTSimpleField toc = ctP.addNewFldSimple();
        toc.setInstr("TOC \\o \"" + WordStyleConstants.CONTENT_LEVEL_FIRST
                + "-" + WordStyleConstants.CONTENT_LEVEL_END + "\" \\h");
        toc.setDirty(STOnOff.TRUE);
        XWPFRun runP = paragraph.createRun();
        runP.addBreak(BreakType.PAGE);
    }

    private void printKeyParameters(XWPFDocument potDocument, Map<String, String> keyParameters) {
        printLine(potDocument, "Key Parameters", true, WordStyleConstants.FONT_VERY_LARGE,
                WordStyleConstants.PAGE_STYLE);
        for (Map.Entry<String, String> keyParameter : keyParameters.entrySet()) {
            printLine(potDocument, keyParameter.getKey() + ": " + keyParameter.getValue(), true);
        }
    }

    private void printPage(XWPFDocument potDocument, PotSessionPageEntity page, Environment environment,
                           boolean isFullInfoNeededInPot) {
        addPageBreak(potDocument);
        printLine(potDocument, page.getName(), true, WordStyleConstants.FONT_VERY_LARGE,
                WordStyleConstants.PAGE_STYLE);
        for (TabEntity tab : page.getPageConfiguration().getTabEntities()) {
            printLine(potDocument, tab.getName(), true, WordStyleConstants.FONT_LARGE,
                    WordStyleConstants.TAB_STYLE);
            for (GroupEntity group : tab.getGroupEntities()) {
                if (!group.isHide()) {
                    printLine(potDocument, group.getName(), true, WordStyleConstants.FONT_NORMAL,
                            WordStyleConstants.GROUP_STYLE);
                    for (SutParameterEntity parameter : group.getSutParameterEntities()) {
                        printParameter(potDocument,
                                page.findParameter(tab.getName(), group.getName(), parameter.getName())
                                        .orElseThrow(() ->
                                                new ExecutionParameterNotFoundException(parameter.getName())),
                                environment, isFullInfoNeededInPot);
                    }
                }
            }
        }
    }

    private void printParameter(XWPFDocument potDocument, PotSessionParameterEntity potParameter,
                                Environment environment,
                                boolean isFullInfoNeededInPot) {
        printParameterHeader(potDocument, potParameter);
        printExpectedResultErrorIfPresent(potDocument, potParameter);
        for (AbstractValueObject arValue : potParameter.getArValues()) {
            if (arValue instanceof ErrorValueObject) {
                printError(potDocument, (ErrorValueObject) arValue);
            } else {
                if (arValue.getValueAsFile() != null && isFullInfoNeededInPot) {
                    String linkFile = arValue.getValueAsFile().getName();
                    printLink(potDocument.createParagraph(), WordStyleConstants.FONT_SMALL,
                            new LinkPotValue(linkFile, linkFile), false, true);
                }
                switch (potParameter.getParameterConfig().getDisplayType()) {
                    case PARAM:
                    case XML:
                    case SSH_RESPONSE:
                        printPlainParameter(potDocument, (SimpleValueObject) arValue);
                        break;
                    case TABLE:
                        printTable(potDocument, (TableValueObject) arValue, environment);
                        break;
                    case JSON:
                        if (arValue instanceof TableValueObject) {
                            printTable(potDocument, (TableValueObject) arValue, environment);
                        } else {
                            printPlainParameter(potDocument, (SimpleValueObject) arValue);
                        }
                        break;
                    case GENERATE_LINK:
                    case LINK:
                        SimpleValueObject castValueObject = (SimpleValueObject) arValue;
                        LinkPotValue link = (LinkPotValue) extractLinksFromContent(castValueObject.getValue(),
                                environment, true);
                        printLink(potDocument.createParagraph(), WordStyleConstants.FONT_SMALL, link,
                                false, true);
                        break;
                    case INTEGRATION_LOG:
                        printIntegrationLog(potDocument, (LogCollectorValueObject) arValue);
                        break;
                    default:
                        break;
                }
            }
            addLineBreak(potDocument);
        }
    }

    private PotValue extractLinksFromContent(String content, Environment environment,
                                             boolean forceReplace) {
        String businessSolutionUrl = "";
        if (environment.isExistSystem("Business Solution")) {
            businessSolutionUrl = environment.getSystem("Business Solution")
                    .getServer("HTTP")
                    .getProperties()
                    .get("url");
            log.debug("WordDocumentPotGenerator - convertLinkContentToBusinessSolutionLink "
                    + "- Business Solution Server not found!!");
        }

        Matcher linkMatcher = HTML_LINK_REGEXP.matcher(content);
        Matcher urlMatcher = PATTERN_FULL_URL.matcher(content);
        String linkPrep = content.replace("&nbsp;", "");
        if (linkMatcher.find()) {
            return new LinkPotValue(getListHttpLink(linkPrep, businessSolutionUrl));
        } else if (forceReplace) {
            String businessSolutionObjectIdPart = "/ncobject.jsp?id=";
            String resultHref = businessSolutionUrl + businessSolutionObjectIdPart + linkPrep;
            return urlMatcher.find()
                    ? new LinkPotValue(linkPrep.trim(), linkPrep.trim())
                    : new LinkPotValue(resultHref, resultHref);
        }
        return new SimplePotValue(linkPrep.trim());
    }

    private void printParameterHeader(XWPFDocument potDocument, PotSessionParameterEntity potParameter) {
        XWPFParagraph paragraph = potDocument.createParagraph();
        paragraph.setStyle("Parameter");
        XWPFRun nameRun = paragraph.createRun();
        setNoProof(nameRun);
        nameRun.setBold(true);
        nameRun.setText(potParameter.getName());
        nameRun.setText(WordStyleConstants.RUN_MARGIN);
        nameRun.setFontSize(WordStyleConstants.FONT_NORMAL);
        ValidationStatus status = potParameter.getValidationInfo().getStatus();
        if (!status.equals(ValidationStatus.NONE)) {
            XWPFRun statusRun = paragraph.createRun();
            setNoProof(statusRun);
            statusRun.setBold(true);
            statusRun.setText(WordStyleConstants.RUN_PADDING);
            statusRun.setText(status.getValidationStatusToString());
            statusRun.setText(WordStyleConstants.RUN_PADDING);
            statusRun.setFontSize(WordStyleConstants.FONT_SMALL);
            statusRun.setColor(WordStyleConstants.WHITE);
            //This is where the statutes are highlighted
            CTShd ctshd = statusRun.getCTR().addNewRPr().addNewShd();
            ctshd.setVal(STShd.CLEAR);
            ctshd.setColor(WordStyleConstants.AUTO);
            setColorForValidationStatusLabel(ctshd, status);
        }
        XWPFRun parEndStrRun = paragraph.createRun();
        setNoProof(parEndStrRun);
        //This character is necessary in order to highlight the spaces before it.
        parEndStrRun.setText(WordStyleConstants.RUN_FINISH_CHAR);
        parEndStrRun.setColor(WordStyleConstants.WHITE);
    }

    private void setColorForValidationStatusLabel(CTShd ctshd, ValidationStatus status) {
        switch (status) {
            case PASSED:
                ctshd.setFill(WordStyleConstants.GREEN);
                break;
            case FAILED:
                ctshd.setFill(WordStyleConstants.RED);
                break;
            case WARNING:
            case LC_WARNING:
                ctshd.setFill(WordStyleConstants.YELLOW);
                break;
            default:
                ctshd.setFill(WordStyleConstants.GRAY);
                break;
        }
    }

    private XWPFRun printLine(XWPFDocument document, String text, boolean isBold) {
        XWPFRun run = document.createParagraph().createRun();
        setNoProof(run);
        run.setBold(isBold);
        run.setText(text);
        return run;
    }

    private XWPFRun printLine(XWPFDocument document, String text, boolean isBold, int fontSize, String style) {
        XWPFRun run = printLine(document, text, isBold);
        setNoProof(run);
        run.getParagraph().setStyle(style);
        run.setFontSize(fontSize);
        return run;
    }

    private void printPlainParameter(XWPFDocument document, SimpleValueObject value) {
        printLine(document, value.getValue(), false);
    }

    private void printLink(XWPFParagraph paragraph, int fontSize, LinkPotValue linksContent, boolean bold,
                           boolean shadow) {
        for (int i = 0; i < linksContent.getHttpLinkList().size(); i++) {
            HttpLink link = linksContent.getHttpLinkList().get(i);
            XWPFHyperlinkRun hyperlinkRun = createHyperLink(paragraph, link.getLink());
            if (Objects.nonNull(hyperlinkRun)) {
                setNoProof(hyperlinkRun);
                hyperlinkRun.setFontSize(fontSize);
                hyperlinkRun.setText(link.getName());
                hyperlinkRun.setShadow(shadow);
                hyperlinkRun.setBold(bold);
                hyperlinkRun.setColor(WordStyleConstants.BLUE);
                hyperlinkRun.setUnderline(UnderlinePatterns.SINGLE);
                if (i < linksContent.getHttpLinkList().size() - 1) {
                    hyperlinkRun.addBreak();
                }
            }
        }
    }

    private void printTable(XWPFDocument document, TableValueObject value, Environment environment) {
        XWPFTable table = document.createTable();
        table.removeRow(0);
        table.setCellMargins(WordStyleConstants.MARGIN_TOP, WordStyleConstants.MARGIN_LEFT,
                WordStyleConstants.MARGIN_BOTTOM, WordStyleConstants.MARGIN_RIGHT);
        setParameterTableStyle(table, WordStyleConstants.LIGHT_GRAY);
        XWPFTableRow headersRow = table.createRow();
        List<String> headers = value.getTable().getHeaders();
        for (int i = 0; i < headers.size() && i < WordStyleConstants.MAX_TABLE_COLUMNS_QUANTITY; i++) {
            createTableHeaderCell(headersRow, headers.get(i));
        }
        if (value.getTable() instanceof Table) {
            printSimpleTable(value, table, environment);
        } else {
            printJsonTable(value, environment, table);
        }
    }

    private void printSimpleTable(TableValueObject value, XWPFTable table, Environment environment) {
        List<Map<String, String>> rows = ((Table) value.getTable()).getRows();
        for (int rowNumber = 0; rowNumber < rows.size()
                && rowNumber < WordStyleConstants.MAX_TABLE_RAW_QUANTITY; rowNumber++) {
            Map<String, String> tableRow = rows.get(rowNumber);
            if (!tableRow.isEmpty()) {
                XWPFTableRow row = table.createRow();
                for (int i = 0; i < row.getTableCells().size()
                        && i < WordStyleConstants.MAX_TABLE_COLUMNS_QUANTITY; i++) {
                    String rowValue = tableRow.getOrDefault(value.getTable().getHeaders().get(i), "");
                    PotValue links = extractLinksFromContent(rowValue, environment, false);
                    XWPFParagraph paragraph = row.getCell(i).addParagraph();
                    row.getCell(i).removeParagraph(0);
                    setTableCellValue(paragraph, WordStyleConstants.FONT_VERY_SMALL,
                            links, WordStyleConstants.BLACK, false);
                }
            }
        }
    }

    private void printJsonTable(TableValueObject value, Environment environment, XWPFTable table) {
        List<JsonTableRow> rows = ((JsonTable) value.getTable()).getRows();
        for (int rowNumber = 0; rowNumber < rows.size()
                && rowNumber < WordStyleConstants.MAX_TABLE_RAW_QUANTITY; rowNumber++) {
            JsonTableRow tableRow = rows.get(rowNumber);
            XWPFTableRow row = table.createRow();
            for (int i = 0; i < row.getTableCells().size()
                    && i < WordStyleConstants.MAX_TABLE_COLUMNS_QUANTITY; i++) {
                JsonCell jsonCell = tableRow.getCells().get(i);
                if (jsonCell instanceof JsonSimpleCell) {
                    printSimpleJsonValue(jsonCell, tableRow, environment, i, row);
                } else {
                    printGroupJsonValue(row, jsonCell, tableRow, i);
                }
            }
        }
    }

    private void printSimpleJsonValue(JsonCell jsonCell, JsonTableRow tableRow, Environment environment,
                                      int columnNumber, XWPFTableRow row) {
        String simpleCell = ((JsonSimpleCell) jsonCell).getSimpleValue();
        String rowValue = cutString(simpleCell);
        PotValue links = extractLinksFromContent(rowValue, environment, false);
        XWPFParagraph paragraph = row.getCell(columnNumber).addParagraph();
        setIndentationLeft(tableRow.getNestingDepth(), columnNumber, paragraph);
        row.getCell(columnNumber).removeParagraph(0);
        setTableCellValue(paragraph, WordStyleConstants.FONT_VERY_SMALL,
                links, WordStyleConstants.BLACK, false);
    }

    private void printGroupJsonValue(XWPFTableRow row, JsonCell jsonCell, JsonTableRow tableRow, int columnNumber) {
        ArrayList<String> jsonGroup = new ArrayList<>();
        Map<String, String> groupedValue = ((JsonGroupedCell) jsonCell).getGroupedValue();
        groupedValue.forEach((key, v) -> {
            jsonGroup.add("> " + key);
        });
        XWPFParagraph paragraph = row.getCell(columnNumber).addParagraph();
        setIndentationLeft(tableRow.getNestingDepth(), columnNumber, paragraph);
        row.getCell(columnNumber).removeParagraph(0);
        setTableGroupJsonCellValue(paragraph, WordStyleConstants.FONT_VERY_SMALL,
                jsonGroup, WordStyleConstants.BLACK, false);
    }

    private String cutString(String text) {
        if (text.length() > 100) {
            return text.substring(0, 100) + "...";
        } else {
            return text;
        }
    }

    private void setIndentationLeft(int nestingDepth, int columnNumber, XWPFParagraph paragraph) {
        if (nestingDepth != 0 && columnNumber == 0) {
            paragraph.setIndentationLeft(250 * nestingDepth);
        }
    }

    private void setParameterTableStyle(XWPFTable table, String color) {
        int sizeNormal = WordStyleConstants.BORDER_SIZE_NORMAL;
        int spaceNone = WordStyleConstants.BORDER_SPACE_NONE;
        table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, sizeNormal, spaceNone, color);
        table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, sizeNormal, spaceNone, color);
        table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, sizeNormal, spaceNone, color);
        table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, sizeNormal, spaceNone, color);
        table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, sizeNormal, spaceNone, color);
        table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, sizeNormal, spaceNone, color);
    }

    //    TODO Analysis and refactoring to factory pattern
    private void printIntegrationLog(XWPFDocument document, LogCollectorValueObject value) {
        if (!value.hasData()) {
            printLine(document, "No data found", false);
        } else {
            value.getSearchResult().getComponentSearchResults().forEach(componentSearchResults -> {
                ConfigurationType componentType = componentSearchResults.getComponentType();
                switch (componentType) {
                    case GRAYLOG:
                        printIntegrationLogGraylogType(document, componentSearchResults);
                        break;
                    case FILEBASED:
                        printIntegrationLogFilebasedType(document, componentSearchResults);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + componentType);
                }
            });
        }
    }

    private void printIntegrationLogGraylogType(XWPFDocument document, ComponentSearchResults componentSearchResults) {
        printLine(document, "Graylog messages", true, WordStyleConstants.FONT_NORMAL,
                WordStyleConstants.INTEGRATION_LOG_STYLE);
        for (SystemSearchResults system : componentSearchResults.getSystemSearchResults()) {
            for (SearchThreadFindResult log : system.getSearchThreadResult()) {
                printLine(document, log.getTimestamp(), true);
                printLine(document, "Message", true);
                printLine(document, log.getMessageAsSingleString(), false);
                log.getParameters().forEach((k, v) -> {
                    printLine(document, k, true);
                    printLine(document, v, false);
                });
            }
        }
    }

    private void printIntegrationLogFilebasedType(XWPFDocument document,
                                                  ComponentSearchResults componentSearchResults) {
        printLine(document, "Filebased messages", true, WordStyleConstants.FONT_NORMAL,
                WordStyleConstants.INTEGRATION_LOG_STYLE);
        for (SystemSearchResults system : componentSearchResults.getSystemSearchResults()) {
            for (SearchThreadFindResult log : system.getSearchThreadResult()) {
                printLine(document, log.getFoundInDirectory() + "/" + log.getLogFileName(), true);
                printLine(document, log.getMessageAsSingleString(), false);
            }
        }
    }

    private void createTableHeaderCell(XWPFTableRow headersRow, String header) {
        XWPFTableCell cell = headersRow.createCell();
        XWPFParagraph paragraph = cell.addParagraph();
        cell.removeParagraph(0);
        cell.setColor(WordStyleConstants.LIGHT_GRAY);
        setTableCellValue(paragraph, WordStyleConstants.FONT_VERY_SMALL, new SimplePotValue(header),
                WordStyleConstants.GRAY, true);
    }

    private void printExpectedResultErrorIfPresent(XWPFDocument document, PotSessionParameterEntity parameter) {
        if (parameter.getEr() instanceof ErrorValueObject) {
            ErrorValueObject errorValueObject = (ErrorValueObject) parameter.getEr();
            printLine(document, "Couldn't get expected result. Reason:", false);
            printLine(document, errorValueObject.getErrorMessage(), false);
        }
    }

    private void printError(XWPFDocument document, ErrorValueObject valueObject) {
        printLine(document, valueObject.getErrorMessage(), false);
    }

    @Nullable
    private XWPFHyperlinkRun createHyperLink(XWPFParagraph paragraph, String destination) {
        String hyperlinkId;
        try {
            hyperlinkId = paragraph.getDocument().getPackagePart().addExternalRelationship(
                    destination,
                    XWPFRelation.HYPERLINK.getRelation()
            ).getId();
            CTHyperlink cthyperLink = paragraph.getCTP().addNewHyperlink();
            cthyperLink.setId(hyperlinkId);
            cthyperLink.addNewR();
            return new XWPFHyperlinkRun(
                    cthyperLink,
                    cthyperLink.getRArray(0),
                    paragraph
            );
        } catch (IllegalArgumentException e) {
            log.warn("Cannot create hyperlink by destination {}", destination);
            return null;
        }
    }

    private List<HttpLink> getListHttpLink(String htmlTag, String businessSolutionUrl) {
        Matcher uriMatcher = URI_REGEXP.matcher(htmlTag);
        Document htmlDoc = Jsoup.parse(htmlTag);
        Elements links = htmlDoc.select("a");
        List<HttpLink> result = new ArrayList<>();
        StringBuilder strHref = new StringBuilder();
        links.forEach(link -> {
            String linkName = link.text().trim();
            if (!linkName.isEmpty()) {
                if (uriMatcher.find()) {
                    strHref.append(businessSolutionUrl).append(link.attr("href"));
                } else {
                    strHref.append(link.attr("href"));
                }
                result.add(new HttpLink(linkName, strHref.toString()));
                strHref.setLength(0);
            }
        });
        return result;
    }

    private void printCommonParameters(XWPFDocument potDocument, List<PotSessionParameterEntity> potSessionParameters,
                                       Environment environment) {
        if (!potSessionParameters.isEmpty()) {
            printLine(potDocument, "Common Parameters", true, WordStyleConstants.FONT_VERY_LARGE,
                    WordStyleConstants.PAGE_STYLE);
            potSessionParameters.forEach(potSessionParameter -> printParameter(potDocument, potSessionParameter,
                    environment, false));
        }
    }

    private static void addCustomHeadingStyle(XWPFDocument docxDocument, String strStyleId, int headingLevel) {
        CTStyle ctStyle = CTStyle.Factory.newInstance();
        ctStyle.setStyleId(strStyleId);
        CTString styleName = CTString.Factory.newInstance();
        styleName.setVal(strStyleId);
        ctStyle.setName(styleName);
        CTDecimalNumber indentNumber = CTDecimalNumber.Factory.newInstance();
        indentNumber.setVal(BigInteger.valueOf(headingLevel));
        // lower number > style is more prominent in the formats bar
        ctStyle.setUiPriority(indentNumber);
        CTOnOff onoffnull = CTOnOff.Factory.newInstance();
        ctStyle.setUnhideWhenUsed(onoffnull);
        // style shows up in the formats bar
        ctStyle.setQFormat(onoffnull);
        // style defines a heading of the given level
        CTPPr ppr = CTPPr.Factory.newInstance();
        ppr.setOutlineLvl(indentNumber);
        ctStyle.setPPr(ppr);
        XWPFStyle style = new XWPFStyle(ctStyle);
        // is a null op if already defined
        XWPFStyles styles = docxDocument.createStyles();
        style.setType(STStyleType.PARAGRAPH);
        styles.addStyle(style);
    }

    private void setDocumentToLandscapeView(XWPFDocument document) {
        // Stolen this code from StackOverflow
        CTBody body = document.getDocument().getBody();
        if (!body.isSetSectPr()) {
            body.addNewSectPr();
        }
        CTSectPr section = body.getSectPr();
        if (!section.isSetPgSz()) {
            section.addNewPgSz();
        }
        CTPageSz pageSize = section.getPgSz();
        pageSize.setOrient(STPageOrientation.LANDSCAPE);
        //A4 = 595x842 / multiply 20 since BigInteger represents 1/20 Point
        pageSize.setW(BigInteger.valueOf(WordStyleConstants.PAGE_SIZE_WIDTH));
        pageSize.setH(BigInteger.valueOf(WordStyleConstants.PAGE_SIZE_HEIGHT));
    }

    private void setTableGroupJsonCellValue(XWPFParagraph paragraph, int fontSize, ArrayList<String> value,
                                            String color, boolean bold) {
        XWPFRun textRun = paragraph.createRun();
        setNoProof(textRun);
        textRun.setFontSize(fontSize);
        textRun.setBold(bold);
        textRun.setColor(color);
        value.forEach(text -> {
            textRun.setText(text);
            textRun.addBreak();
        });
    }

    private void setTableCellValue(XWPFParagraph paragraph, int fontSize, PotValue value, String color, boolean bold) {
        if (value instanceof SimplePotValue) {
            XWPFRun textRun = paragraph.createRun();
            setNoProof(textRun);
            textRun.setFontSize(fontSize);
            textRun.setBold(bold);
            textRun.setColor(color);
            textRun.setText(((SimplePotValue) value).getValue());
        } else if (value instanceof LinkPotValue) {
            printLink(paragraph, WordStyleConstants.FONT_VERY_SMALL, (LinkPotValue) value, false, false);
        }
    }

    private void addPageBreak(XWPFDocument document) {
        document.createParagraph().createRun().addBreak(BreakType.PAGE);
    }

    private void addLineBreak(XWPFDocument document) {
        document.createParagraph().createRun();
    }

    /**
     * Removes the spell check in the Runs in paragraphs.
     */
    private void setNoProof(XWPFRun run) {
        CTR ctR = run.getCTR();
        CTRPr ctRPr = ctR.isSetRPr() ? ctR.getRPr() : ctR.addNewRPr();
        if (!ctRPr.isSetNoProof()) {
            // If the noProof property is missing, add it
            ctRPr.addNewNoProof();
        } else {
            // If the noProof property is present, make sure it is not
            // FALSE, OFF, or X_0
            CTOnOff noProof = ctRPr.getNoProof();
            if (noProof.isSetVal() && (noProof.getVal() == STOnOff.FALSE
                    || noProof.getVal() == STOnOff.OFF
                    || noProof.getVal() == STOnOff.X_0)) {
                noProof.setVal(STOnOff.TRUE);
            }
        }
    }
}
