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

package org.qubership.atp.svp.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.enums.ValidationType;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;
import org.qubership.atp.svp.model.table.Table;
import org.qubership.atp.svp.service.direct.displaytype.DisplayTypeTestConstants;

public class DbMockEntity {

    private static ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false);

    public static ProjectConfigsEntity getProjectConfigsEntity(UUID projectId, String projectName,
                                                               String pathFolderLocalProject, String gitUrl,
                                                               String lastUpdateUserName, int defLCTime,
                                                               boolean isFullInfoNeededInPot,
                                                               RepositoryType pagesSourceType) {
        ProjectConfigsEntity projectConfigsEntity = new ProjectConfigsEntity();
        projectConfigsEntity.setProjectId(projectId);
        projectConfigsEntity.setProjectName(projectName);
        projectConfigsEntity.setPathFolderLocalProject(pathFolderLocalProject);
        projectConfigsEntity.setGitUrl(gitUrl);
        projectConfigsEntity.setLastUpdateUserName(lastUpdateUserName);
        projectConfigsEntity.setDefaultLogCollectorSearchTimeRange(defLCTime);
        projectConfigsEntity.setFullInfoNeededInPot(isFullInfoNeededInPot);
        projectConfigsEntity.setPagesSourceType(pagesSourceType);
        return projectConfigsEntity;
    }

    public static FolderEntity generateFolderEntity() throws IOException {
        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setFolderId(UUID.randomUUID());
        folderEntity.setName("folderName");
        ProjectConfigsEntity projectConfigs = getProjectConfigurationEntity(UUID.fromString("e984e5d1-5f89-4113-84a0-b658800e65c4"));
        folderEntity.setProject(projectConfigs);
        return folderEntity;
    }

    protected static ProjectConfigsEntity getProjectConfigurationEntity(UUID projectId) throws IOException {
        if (projectId.toString().equals("1183662d-51ae-438a-92be-0a4bb71620d9")) {
            String gitProject = loadFileToString("src/test/resources/test_data/projectConfig/FileProject.json");
            return mapper.readValue(gitProject, ProjectConfigsEntity.class);
        } else if (projectId.toString().equals("323eda51-47b5-414a-951a-27221fa374a2")) {
            String fileProject = loadFileToString("src/test/resources/test_data/projectConfig/FileProject.json");
            return mapper.readValue(fileProject, ProjectConfigsEntity.class);
        } else if (projectId.toString().equals("e984e5d1-5f89-4113-84a0-b658800e65c4")) {
            String folderProject = loadFileToString("src/test/resources/test_data/projectConfig/folderProject.json");
            return mapper.readValue(folderProject, ProjectConfigsEntity.class);
        } else {
            String testProject = loadFileToString("src/test/resources/test_data/projectConfig/FileProject.json");
            return mapper.readValue(testProject, ProjectConfigsEntity.class);
        }
    }


    public static List<PageConfigurationEntity> generatedListPageConfigurationEntity() {
        PageConfigurationEntity pageConfigurationEntity1 = new PageConfigurationEntity();
        pageConfigurationEntity1.setName("CI");
        pageConfigurationEntity1.setOrder(3);
        pageConfigurationEntity1.setPageId(UUID.randomUUID());
        PageConfigurationEntity pageConfigurationEntity2 = new PageConfigurationEntity();
        pageConfigurationEntity2.setName("SOI");
        pageConfigurationEntity2.setOrder(2);
        pageConfigurationEntity2.setPageId(UUID.randomUUID());
        PageConfigurationEntity pageConfigurationEntity3 = new PageConfigurationEntity();
        pageConfigurationEntity3.setName("SP");
        pageConfigurationEntity3.setOrder(0);
        pageConfigurationEntity3.setPageId(UUID.randomUUID());
        PageConfigurationEntity pageConfigurationEntity4 = new PageConfigurationEntity();
        pageConfigurationEntity4.setName("I");
        pageConfigurationEntity4.setOrder(1);
        pageConfigurationEntity4.setPageId(UUID.randomUUID());


        List<PageConfigurationEntity> pagesListe = new ArrayList<>();
        pagesListe.add(pageConfigurationEntity1);
        pagesListe.add(pageConfigurationEntity2);
        pagesListe.add(pageConfigurationEntity3);
        pagesListe.add(pageConfigurationEntity4);
        return pagesListe;
    }

    public static PotSessionEntity generatePotSessionEntity(UUID sessionId) throws IOException {
        String filePath = "src/test/resources/test_data/pot.session/PotSession.json";
        PotSessionEntity session = mapper.readValue(loadFileToString(filePath), PotSessionEntity.class);
        session.setStarted(OffsetDateTime.now());
        session.setSessionId(sessionId);
        return session;
    }

    public static PotSessionParameterEntity generatePotSessionParameterEntity() throws IOException {
        String filePath = "src/test/resources/test_data/pot.session.parameter/PotSessionParameter.json";
        return mapper.readValue(loadFileToString(filePath), PotSessionParameterEntity.class);
    }

    public static PotSessionParameterEntity generateJsonParameterForValidation(String correctValue,
                                                                               ValidationType validationType,
                                                                               int quantityActualResults,
                                                                               String incorrectValue,
                                                                               Integer... incorrectResultIndexes) throws IOException {
        String filePath = "src/test/resources/test_data/pot.session.parameter/JsonRawParameter.json";
        String param = String.format(loadFileToString(filePath), validationType);
        PotSessionParameterEntity parameter = mapper.readValue(param, PotSessionParameterEntity.class);
        for (int i = 0; i < quantityActualResults; i++) {
            parameter.addArValue(new SimpleValueObject(
                    Arrays.asList(incorrectResultIndexes).contains(i)
                            ? incorrectValue : correctValue));
        }
        return parameter;
    }

    public static PotSessionParameterEntity generatePotSessionParameterForJsonTableCustom(String erRow1, String erRow2,
                                                                                          String arRow1,
                                                                                          String arRow2) throws IOException {
        String filePath = "src/test/resources/test_data/pot.session.parameter/JsonTableCustomParameter.json";
        String param = String.format(loadFileToString(filePath), erRow1, erRow2, arRow1, arRow2);
        return mapper.readValue(param, PotSessionParameterEntity.class);
    }

    public static PotSessionParameterEntity generatePotSessionParameterForJsonTablePlain(String arId_row2,
                                                                                         String erValue,
                                                                                         String validationColumnName) throws IOException {
        String filePath = "src/test/resources/test_data/pot.session.parameter/JsonTablePlainParameter.json";
        String param = String.format(loadFileToString(filePath), arId_row2, erValue, validationColumnName);
        return mapper.readValue(param, PotSessionParameterEntity.class);
    }

    public static PotSessionParameterEntity generatePotSessionParameterForJsonRawPlain(String arValue,
                                                                                       String erValue)
            throws IOException {
        String filePath = "src/test/resources/test_data/pot.session.parameter/JsonRawPlainParameter.json";
        String param = String.format(loadFileToString(filePath), arValue, erValue);
        PotSessionParameterEntity parameter = mapper.readValue(param, PotSessionParameterEntity.class);
        parameter.setEr(new SimpleValueObject(erValue));
        return parameter;
    }

    public static PotSessionParameterEntity generateJsonRawBvValidationParameter(boolean isJsonScheme,
                                                                                 String validationObjectName,
                                                                                 String testCaseId)
            throws IOException {
        String filePath = "src/test/resources/test_data/pot.session.parameter/JsonBvParameter.json";
        String param = String.format(loadFileToString(filePath), validationObjectName, testCaseId);
        PotSessionParameterEntity parameter = mapper.readValue(param, PotSessionParameterEntity.class);
        if (isJsonScheme) {
            SimpleValueObject arJsonScheme = new SimpleValueObject("rows\": [\n    [\n      \"7160963736470226842\","
                    + "\n    "
                    + "  \"2021/07/19\",\n      \"AQUOS R5G Earth Blue\"\n    ],\n    [\n      "
                    + "\"7160963736470226842\",\n  "
                    + "    \"2021/07/19\",\n      \"Galaxy A7 Blue\"\n    ],\n    [\n      \"3160963888520229405\",\n"
                    + "      \"2021/07/19\",\n      "
                    + "\"AQUOS R5G Earth Blue\"\n    ]\n  ]\n}");
            parameter.addArValue(arJsonScheme);
        } else {
            SimpleValueObject arJson = new SimpleValueObject("\"test\": \"12888\", \"status0\": \"active\"");
            parameter.addArValue(arJson);
        }
        return parameter;
    }

    public static PotSessionParameterEntity generateExpectedJsonRawBvValidationParameter(boolean isJsonScheme,
                                                                                         ValidationStatus status,
                                                                                         String validationObjectName,
                                                                                         String testCaseId) throws IOException {
        String filePath = "src/test/resources/test_data/pot.session.parameter/JsonRawBvExpectedParameter.json";
        String param = String.format(loadFileToString(filePath), status, validationObjectName, testCaseId);
        PotSessionParameterEntity parameter = mapper.readValue(param, PotSessionParameterEntity.class);

        if (isJsonScheme) {
            parameter.addArValue((new SimpleValueObject("<?xml version=\"1.0\" encoding=\"UTF-8\"?><pre>{<br>  "
                    + "\"rows\" : [ [ \"7160963736470226842\", \"2021/07/19\", \"AQUOS R5G Earth Blue\" ], [ "
                    + "\"7160963736470226842\", \"2021/07/19\", \"Galaxy A7 Blue\" ], [ \"3160963888520229405\", "
                    + "\"2021/07/19\", \"AQUOS R5G Earth Blue\" ] ]<br>}</pre>")));
        } else {
            parameter.addArValue(new SimpleValueObject("<?xml version=\"1.0\" encoding=\"UTF-8\"?><pre>{<br>  "
                    + "\"test\" : "
                    + "<span data-block-id=\"pc-highlight-block\" class=\"SIMILAR\" title=\"Similar property or "
                    + "object\">\"1288\"</span>,<br>  \"status0\" : \"active\"<br>}</pre>"));

            parameter.setEr(new SimpleValueObject("<?xml version=\"1.0\" encoding=\"UTF-8\"?><pre>{<br>  "
                    + "\"test\" : <span data-block-id=\"pc-highlight-block\" class=\"SIMILAR\" title=\"Similar "
                    + "property or object\">\"12888\"</span>,<br>  \"status0\" : \"active\"<br>}</pre>"));
        }
        return parameter;
    }

    public static PotSessionParameterEntity generatePotSessionParameterForSsh(String ar, String er) throws IOException {
        String filePath = "src/test/resources/test_data/pot.session.parameter/SshParameter.json";
        PotSessionParameterEntity parameter = mapper.readValue(loadFileToString(filePath),
                PotSessionParameterEntity.class);
        SimpleValueObject arValue = new SimpleValueObject(ar);
        parameter.getArValues().add(arValue);
        SimpleValueObject erValue = new SimpleValueObject(er);
        parameter.setEr(erValue);

        return parameter;
    }

    public static PotSessionParameterEntity generatePotSessionParameterForXml(String correctValue,
                                                                              String incorrectValue,
                                                                              int quantityActualResults,
                                                                              Integer... incorrectResultIndexes) throws IOException {
        String filePath = "src/test/resources/test_data/pot.session.parameter/XmlParameter.json";
        PotSessionParameterEntity parameter = mapper.readValue(loadFileToString(filePath),
                PotSessionParameterEntity.class);
        for (int i = 0; i < quantityActualResults; i++) {
            parameter.addArValue(new SimpleValueObject(
                    Arrays.asList(incorrectResultIndexes).contains(i)
                            ? incorrectValue : correctValue));
        }
        SimpleValueObject erValue = new SimpleValueObject(correctValue);
        parameter.setEr(erValue);
        return parameter;
    }

    public static PotSessionParameterEntity generatePotSessionParameterForTable(ValidationType validationType,
                                                                                int quantityActualResults,
                                                                                Integer... incorrectResultIndexes) throws IOException {
        String correctTableStr =
                loadFileToString(DisplayTypeTestConstants.TABLE_RESPONSE_FILE_PATH);
        Table correctTable = mapper.readValue(correctTableStr, Table.class);
        String incorrectTableStr =
                loadFileToString(DisplayTypeTestConstants.INCORRECT_TABLE_RESPONSE_FILE_PATH);
        Table incorrectTable = mapper.readValue(incorrectTableStr, Table.class);
        String filePath = "src/test/resources/test_data/pot.session.parameter/TablePlainParameter.json";
        PotSessionParameterEntity parameter = mapper.readValue(loadFileToString(filePath),
                PotSessionParameterEntity.class);

        for (int i = 0; i < quantityActualResults; i++) {
            parameter.addArValue(new TableValueObject(
                    Arrays.asList(incorrectResultIndexes).contains(i)
                            ? incorrectTable : correctTable));
        }
        parameter.getParameterConfig().getErConfig().setType(validationType);
        if (validationType.equals(ValidationType.CUSTOM)) {
            parameter.setEr(new TableValueObject(correctTable));
        }
        return parameter;
    }

    private static String loadFileToString(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }

    public static PotSessionParameterEntity generateMockedParameterForBv(boolean isVariable) throws IOException {
        String filePath = "src/test/resources/test_data/pot.session.parameter/TableBvParameter.json";
        String parameter = loadFileToString(filePath);
        if (isVariable) {
            parameter = String.format(parameter, "${Test}");
        } else {
            parameter = String.format(parameter, "61abd5ac-efbe-49cc-b7f5-925a7f543481");
        }
        return mapper.readValue(parameter, PotSessionParameterEntity.class);
    }

    public static List<FolderEntity> generateFoldersForExportCandidates() throws IOException {
        String filePath = "src/test/resources/test_data/folderentity/folders.json";
        String folders = loadFileToString(filePath);
        return Arrays.asList(mapper.readValue(folders, FolderEntity[].class));
    }

    public static PageConfigurationEntity getPageFromTestFile(String pageFileName) throws IOException {
        String pages = loadFileToString("src/test/resources/test_data/page_configurations/" + pageFileName);
        return PageConfigurationEntity.createPageEntity(mapper.readValue(pages, PageConfiguration.class), null);
    }
}
