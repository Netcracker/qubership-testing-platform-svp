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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.junit.Before;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.integration.configuration.interceptors.MdcChannelInterceptor;
import org.qubership.atp.svp.core.enums.DisplayType;
import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.core.enums.ValidationType;
import org.qubership.atp.svp.model.bulkvalidator.GettingTestCaseIdsResponse;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.db.pot.session.PotSessionParameterEntity;
import org.qubership.atp.svp.model.environments.Environment;
import org.qubership.atp.svp.model.impl.ErConfig;
import org.qubership.atp.svp.model.impl.Source;
import org.qubership.atp.svp.model.impl.SutParameter;
import org.qubership.atp.svp.model.logcollector.ComponentSearchResults;
import org.qubership.atp.svp.model.logcollector.ComponentSearchStatus;
import org.qubership.atp.svp.model.logcollector.ConfigurationType;
import org.qubership.atp.svp.model.logcollector.SearchResult;
import org.qubership.atp.svp.model.logcollector.SearchStatus;
import org.qubership.atp.svp.model.logcollector.SearchThreadFindResult;
import org.qubership.atp.svp.model.logcollector.SearchThreadStatus;
import org.qubership.atp.svp.model.logcollector.SystemSearchResults;
import org.qubership.atp.svp.model.pot.PotSessionParameter;
import org.qubership.atp.svp.model.pot.SessionExecutionConfiguration;
import org.qubership.atp.svp.model.pot.SutParameterExecutionContext;
import org.qubership.atp.svp.model.pot.validation.ValidationInfo;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.LogCollectorValueObject;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.model.project.ProjectRepositoryConfig;
import org.qubership.atp.svp.model.table.JsonCell;
import org.qubership.atp.svp.model.table.JsonSimpleCell;
import org.qubership.atp.svp.model.table.JsonTable;
import org.qubership.atp.svp.model.table.JsonTableRow;
import org.qubership.atp.svp.model.ui.ProjectConfigResponseModel;
import org.qubership.atp.svp.utils.CryptoUtilsTest;

public abstract class TestWithTestData extends CryptoUtilsTest {

    protected final String fileProject = "323eda51-47b5-414a-951a-27221fa374a2";
    protected final String folderProject = "e984e5d1-5f89-4113-84a0-b658800e65c4";
    protected final String fileProjectName = "test";
    protected final String gitProject = "1183662d-51ae-438a-92be-0a4bb71620d9";
    protected final String gitProjectName = "test-git";

    protected ObjectMapper objectMapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    protected ObjectWriter objectWriter;

    @MockBean
    private Decryptor decryptor;

    @MockBean
    private MdcChannelInterceptor mdcChannelInterceptor;

    @Before
    public void beforeClass() {
        objectMapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
    }

    public String loadFileToString(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }

    public void writeToFile(String fileName, String content) throws IOException {
        Files.write(Paths.get(fileName), content.getBytes());
    }

    public static String getSshResponse(String row_1, String row_2) {
        return String.format(SSH_RESPONSE, row_1, row_2);
    }

    private static final String SSH_RESPONSE = "b%1$s\n"
            + "%2$s\n"
            + "dev\n"
            + "dumps\n"
            + "etc\n"
            + "home\n"
            + "lib\n"
            + "lib64\n"
            + "logs\n"
            + "lost+found\n"
            + "media\n"
            + "mnt\n"
            + "opt\n"
            + "proc\n"
            + "root\n"
            + "run\n"
            + "sbin\n"
            + "srv\n"
            + "sys\n"
            + "tmp\n"
            + "u01\n"
            + "u02\n"
            + "ub01\n"
            + "usr\n"
            + "var";

    protected JsonTable generateTableRef1ForJoin() {
        JsonTable jsonTable = new JsonTable();
        //Map<String, String> groupedValue = new LinkedHashMap<>();
        //groupedValue.put("KEY", "VALUE");
        String rootquoteitem1 = "ROOTQUOTEITEM1";
        String productName = "PRODUCT.NAME";
        String action = "ACTION";

        List<String> headers = new ArrayList<>();
        headers.add(rootquoteitem1);
        headers.add(productName);
        headers.add(action);
        jsonTable.setHeaders(headers);
        List<JsonCell> cells1 = new ArrayList<>();
        cells1.add(new JsonSimpleCell(rootquoteitem1, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells1.add(new JsonSimpleCell(productName, "5 GB Data #1"));
        cells1.add(new JsonSimpleCell(action, "ADD"));
        List<JsonCell> cells2 = new ArrayList<>();
        cells2.add(new JsonSimpleCell(rootquoteitem1, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells2.add(new JsonSimpleCell(productName, "$40 2GB Plan #1"));
        cells2.add(new JsonSimpleCell(action, "ADD"));
        List<JsonCell> cells3 = new ArrayList<>();
        cells3.add(new JsonSimpleCell(rootquoteitem1, "e0628a18-9c5f-44d0-b586-5e8a749eb590"));
        cells3.add(new JsonSimpleCell(productName, "$35 2GB Plan #1"));
        cells3.add(new JsonSimpleCell(action, "bADD"));
        List<JsonCell> cells4 = new ArrayList<>();
        cells4.add(new JsonSimpleCell(rootquoteitem1, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells4.add(new JsonSimpleCell(productName, "Speed Booster #1"));
        cells4.add(new JsonSimpleCell(action, "ADD"));
        List<JsonCell> cells5 = new ArrayList<>();
        cells5.add(new JsonSimpleCell(rootquoteitem1, "e0628a18-9c5f-44d0-b586-5e8a749eb590"));
        cells5.add(new JsonSimpleCell(productName, "500 MB bonus Public #1"));
        cells5.add(new JsonSimpleCell(action, "aADD"));
        List<JsonCell> cells6 = new ArrayList<>();
        cells6.add(new JsonSimpleCell(rootquoteitem1, "e0628a18-9c5f-44d0-b586-5e8a749eb590111"));
        cells6.add(new JsonSimpleCell(productName, "500 MB bonus Public #1"));
        cells6.add(new JsonSimpleCell(action, "ADD"));

        List<JsonTableRow> rows = new ArrayList<>();
        rows.add(new JsonTableRow(cells1, 0));
        rows.add(new JsonTableRow(cells2, 0));
        rows.add(new JsonTableRow(cells3, 0));
        rows.add(new JsonTableRow(cells4, 0));
        rows.add(new JsonTableRow(cells5, 0));
        rows.add(new JsonTableRow(cells6, 0));
        //JsonGroupedCell groupedCell = new JsonGroupedCell("productOffering", groupedValue);
        //cells.add(groupedCell);
        jsonTable.setRows(rows);
        return jsonTable;
    }

    protected JsonTable generateTableHierarchyForJoin() {
        JsonTable jsonTable = new JsonTable();
        String productname = "PRODUCT.NAME";
        String id = "ID";

        List<String> headers = new ArrayList<>();
        headers.add(productname);
        headers.add(id);
        jsonTable.setHeaders(headers);
        List<JsonCell> cells1 = new ArrayList<>();
        cells1.add(new JsonSimpleCell(productname, "$40 2GB Plan #1"));
        cells1.add(new JsonSimpleCell(id, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        List<JsonCell> cells2 = new ArrayList<>();
        cells2.add(new JsonSimpleCell(productname, "5 GB Data #1"));
        cells2.add(new JsonSimpleCell(id, "da207a4c-fe4c-4c00-8c51-3384dc33a896"));
        List<JsonCell> cells3 = new ArrayList<>();
        cells3.add(new JsonSimpleCell(productname, "5 GB Data #1"));
        cells3.add(new JsonSimpleCell(id, "da207a4c-fe4c-4c00-8c51-3384dc33a111"));
        List<JsonCell> cells4 = new ArrayList<>();
        cells4.add(new JsonSimpleCell(productname, "5 GB Data #1"));
        cells4.add(new JsonSimpleCell(id, "da207a4c-fe4c-4c00-8c51-3384dc33a111444"));

        List<JsonTableRow> rows = new ArrayList<>();
        rows.add(new JsonTableRow(cells1, 0));
        rows.add(new JsonTableRow(cells2, 1));
        rows.add(new JsonTableRow(cells3, 2));
        rows.add(new JsonTableRow(cells4, 3));
        jsonTable.setRows(rows);
        return jsonTable;
    }

    protected JsonTable generateTableRef2ForJoin() {
        JsonTable jsonTable = new JsonTable();
        String rootquoteitem2 = "ROOTQUOTEITEM2";
        String parentquoteitem = "PARENTQUOTEITEM";
        String id = "ID";
        String productName = "PRODUCT.NAME";
        String action = "ACTION";

        List<String> headers = new ArrayList<>();
        headers.add(rootquoteitem2);
        headers.add(parentquoteitem);
        headers.add(id);
        jsonTable.setHeaders(headers);
        List<JsonCell> cells1 = new ArrayList<>();
        cells1.add(new JsonSimpleCell(rootquoteitem2, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells1.add(new JsonSimpleCell(parentquoteitem, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells1.add(new JsonSimpleCell(id, "da207a4c-fe4c-4c00-8c51-3384dc33a896"));
        cells1.add(new JsonSimpleCell(productName, "5 GB Data #1"));
        cells1.add(new JsonSimpleCell(action, "ADD"));

        List<JsonCell> cells2 = new ArrayList<>();
        cells2.add(new JsonSimpleCell(rootquoteitem2, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells2.add(new JsonSimpleCell(parentquoteitem, "—"));
        cells2.add(new JsonSimpleCell(id, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells2.add(new JsonSimpleCell(productName, "$40 2GB Plan #1"));
        cells2.add(new JsonSimpleCell(action, "ADD"));

        List<JsonCell> cells3 = new ArrayList<>();
        cells3.add(new JsonSimpleCell(rootquoteitem2, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells3.add(new JsonSimpleCell(parentquoteitem, "—"));
        cells3.add(new JsonSimpleCell(id, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells3.add(new JsonSimpleCell(productName, "$40 2GB Plan #1"));
        cells3.add(new JsonSimpleCell(action, "ADD"));

        List<JsonCell> cells4 = new ArrayList<>();
        cells4.add(new JsonSimpleCell(rootquoteitem2, "e0628a18-9c5f-44d0-b586-5e8a749eb590"));
        cells4.add(new JsonSimpleCell(parentquoteitem, "—"));
        cells4.add(new JsonSimpleCell(id, "e0628a18-9c5f-44d0-b586-5e8a749eb590"));
        cells4.add(new JsonSimpleCell(productName, "$35 2GB Plan #1"));
        cells4.add(new JsonSimpleCell(action, "ADD"));

        List<JsonCell> cells5 = new ArrayList<>();
        cells5.add(new JsonSimpleCell(rootquoteitem2, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells5.add(new JsonSimpleCell(parentquoteitem, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells5.add(new JsonSimpleCell(id, "ce247f89-3e73-4844-99b7-9bb87da38011"));
        cells5.add(new JsonSimpleCell(productName, "Speed Booster #1"));
        cells5.add(new JsonSimpleCell(action, "ADD"));

        List<JsonCell> cells6 = new ArrayList<>();
        cells6.add(new JsonSimpleCell(rootquoteitem2, "fb5bdb66-c1e2-476a-87d5-62aea203007b--"));
        cells6.add(new JsonSimpleCell(parentquoteitem, "—"));
        cells6.add(new JsonSimpleCell(id, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells6.add(new JsonSimpleCell(productName, "$40 2GB Plan #1"));
        cells6.add(new JsonSimpleCell(action, "ADD"));

        List<JsonCell> cells7 = new ArrayList<>();
        cells7.add(new JsonSimpleCell(rootquoteitem2, "fb5bdb66-c1e2-476a-87d5-62aea203007b++"));
        cells7.add(new JsonSimpleCell(parentquoteitem, "—"));
        cells7.add(new JsonSimpleCell(id, "fb5bdb66-c1e2-476a-87d5-62aea203007b"));
        cells7.add(new JsonSimpleCell(productName, "$40 2GB Plan #1"));
        cells7.add(new JsonSimpleCell(action, "ADD"));

        List<JsonCell> cells8 = new ArrayList<>();
        cells8.add(new JsonSimpleCell(rootquoteitem2, "e0628a18-9c5f-44d0-b586-5e8a749eb590111"));
        cells8.add(new JsonSimpleCell(parentquoteitem, "e0628a18-9c5f-44d0-b586-5e8a749eb590"));
        cells8.add(new JsonSimpleCell(id, "68cc75d7-a9ff-43b4-805d-9c8cc24e9597111"));
        cells8.add(new JsonSimpleCell(productName, "500 MB bonus Public #1"));
        cells8.add(new JsonSimpleCell(action, "ADD"));

        List<JsonCell> cells9 = new ArrayList<>();
        cells9.add(new JsonSimpleCell(rootquoteitem2, "e0628a18-9c5f-44d0-b586-5e8a749eb590111"));
        cells9.add(new JsonSimpleCell(parentquoteitem, "e0628a18-9c5f-44d0-b586-5e8a749eb590"));
        cells9.add(new JsonSimpleCell(id, "68cc75d7-a9ff-43b4-805d-9c8cc24e9597111"));
        cells9.add(new JsonSimpleCell(productName, "500 MB bonus Public #1"));
        cells9.add(new JsonSimpleCell(action, "ADD"));

        List<JsonTableRow> rows = new ArrayList<>();
        rows.add(new JsonTableRow(cells1, 0));
        rows.add(new JsonTableRow(cells2, 0));
        rows.add(new JsonTableRow(cells3, 0));
        rows.add(new JsonTableRow(cells4, 0));
        rows.add(new JsonTableRow(cells5, 0));
        rows.add(new JsonTableRow(cells6, 0));
        rows.add(new JsonTableRow(cells7, 0));
        rows.add(new JsonTableRow(cells8, 0));
        rows.add(new JsonTableRow(cells9, 0));
        jsonTable.setRows(rows);
        return jsonTable;
    }

    public static final String BV_SETTINGS = "{\n"
            + "\t\"url\": \"https://validator.example.org\",\n"
            + "\t\"projectId\": \"e75c4d6c-3814-48e7-b5a2-d8636ab91d38\"\n"
            + "}";

    public static String getBvCreateTestRunResponse(String testRunId, String externalId,
                                                    String validationObjectName) {
        return String.format(BV_CREATE_TEST_RUN_RESPONSE, testRunId, externalId, validationObjectName);
    }

    private static final String BV_CREATE_TEST_RUN_RESPONSE = "{\n"
            + "\t\"trId\": \"%1$s\",\n"
            + "\t\"context\": {\n"
            + "\t\t\"inputParameters\": [],\n"
            + "\t\t\"values\": [\n"
            + "\t\t\t{\n"
            + "\t\t\t\t\"internalId\": \"-401\",\n"
            + "\t\t\t\t\"externalId\": \"%2$s\",\n"
            + "\t\t\t\t\"dataType\": \"SIMPLE\",\n"
            + "\t\t\t\t\"name\": \"%3$s\",\n"
            + "\t\t\t\t\"timeStamp\": \"2022-02-10T11:18:42.000+0000\",\n"
            + "\t\t\t\t\"contentType\": \"JSON\",\n"
            + "\t\t\t\t\"content\": \"ewogICJ0ZXN0IjogIjEyODg4IiwKICAic3RhdHVzMCI6ICJhY3RpdmUiCn0=\",\n"
            + "\t\t\t\t\"childs\": [],\n"
            + "\t\t\t\t\"orderNum\": 1\n"
            + "\t\t\t}\n"
            + "\t\t],\n"
            + "\t\t\"dataSource\": [],\n"
            + "\t\t\"usedSources\": [],\n"
            + "\t\t\"parameterStatuses\": null,\n"
            + "\t\t\"buildInfo\": null,\n"
            + "\t\t\"readObjects\": [],\n"
            + "\t\t\"errors\": [],\n"
            + "\t\t\"rules\": [],\n"
            + "\t\t\"oldProcessContext\": false\n"
            + "\t},\n"
            + "\t\"created\": null,\n"
            + "\t\"tcId\": \"61abd5ac-efbe-49cc-b7f5-925a7f543481\",\n"
            + "\t\"trStatus\": null,\n"
            + "\t\"run\": null,\n"
            + "\t\"validationContext\": null,\n"
            + "\t\"summaryValidationResult\": null,\n"
            + "\t\"contextAsString\": \"{\\\"inputParameters\\\":[],\\\"values\\\":[{\\\"internalId\\\":\\\"-401\\\","
            + "\\\"externalId\\\":\\\"a7e120ef-7f09-4054-8c00-980cc744aa95\\\",\\\"dataType\\\":\\\"SIMPLE\\\","
            + "\\\"name\\\":\\\"json message\\\",\\\"timeStamp\\\":\\\"Feb 10, 2022 11:18:42 AM\\\","
            + "\\\"contentType\\\":\\\"JSON\\\","
            + "\\\"content\\\":\\\"ewogICJ0ZXN0IjogIjEyODg4IiwKICAic3RhdHVzMCI6ICJhY3RpdmUiCn0\\\\u003d\\\","
            + "\\\"childs\\\":[],\\\"orderNum\\\":1}],\\\"dataSource\\\":[],\\\"usedSources\\\":[],"
            + "\\\"readObjects\\\":[],\\\"errors\\\":[],\\\"rules\\\":[]}\"\n"
            + "}";

    public static String getBvCompareResponse(String compareResult, String testRunId) {
        return String.format(BV_COMPARE_RESPONSE, compareResult, testRunId);
    }

    private static final String BV_COMPARE_RESPONSE = "[\n"
            + "\t{\n"
            + "\t\t\"statusCode\": 10000,\n"
            + "\t\t\"tcName\": \"SVP Quick compare\",\n"
            + "\t\t\"compareResult\": \"%1$s\",\n"
            + "\t\t\"trDate\": \"2022-02-10 11:22:07.037\",\n"
            + "\t\t\"trId\": \"%2$s\",\n"
            + "\t\t\"resultLink\": \"validation?trid=" + "%2$s\",\n"
            + "\t\t\"steps\": [\n"
            + "\t\t\t{\n"
            + "\t\t\t\t\"stepName\": \"json message\",\n"
            + "\t\t\t\t\"compareResult\": \"SIMILAR\"\n"
            + "\t\t\t}\n"
            + "\t\t]\n"
            + "\t}\n"
            + "]";

    public static String getBvGetHighLightsResponse(String validationObjectName,
                                                    String ar, String er, String rule) {
        return String.format(BV_GET_HIGH_LIGHTS_RESPONSE, validationObjectName, ar, er, rule);
    }

    private static final String BV_GET_HIGH_LIGHTS_RESPONSE = "[\n"
            + "\t{\n"
            + "\t\t\"name\": \"%1$s\",\n"
            + "\t\t\"contentType\": \"JSON\",\n"
            + "\t\t\"originalER\": \"ewoidGVzdCI6IjEyODgiLAoic3RhdHVzMCI6ImFjdGl2ZSIKfQ==\",\n"
            + "\t\t\"originalAR\": \"ewogICJ0ZXN0IjogIjEyODg4IiwKICAic3RhdHVzMCI6ICJhY3RpdmUiCn0=\",\n"
            + "\t\t\"summaryResult\": \"SIMILAR\",\n"
            + "\t\t\"summaryMessage\": \"\",\n"
            + "\t\t\"diffs\": [\n"
            + "\t\t\t{\n"
            + "\t\t\t\t\"orderId\": 1,\n"
            + "\t\t\t\t\"expected\": \"/test\",\n"
            + "\t\t\t\t\"actual\": \"/test\",\n"
            + "\t\t\t\t\"description\": \"Node values are different.\",\n"
            + "\t\t\t\t\"result\": \"SIMILAR\"\n"
            + "\t\t\t}\n"
            + "\t\t],\n"
            + "\t\t\"ER\": {\n"
            + "\t\t\t\"children\": [],\n"
            + "\t\t\t\"rowNumber\": 0,\n"
            + "\t\t\t\"linkedRow\": -1,\n"
            + "\t\t\t\"isDisplaied\": true,\n"
            + "\t\t\t\"value\": \"%3$s\",\n"
            + "\t\t\t\"isValueEncoded\": true,\n"
            + "\t\t\t\"isPlain\": true\n"
            + "\t\t},\n"
            + "\t\t\"AR\": {\n"
            + "\t\t\t\"children\": [],\n"
            + "\t\t\t\"rowNumber\": 0,\n"
            + "\t\t\t\"linkedRow\": -1,\n"
            + "\t\t\t\"isDisplaied\": true,\n"
            + "\t\t\t\"value\": \"%2$s\",\n"
            + "\t\t\t\"isValueEncoded\": true,\n"
            + "\t\t\t\"isPlain\": true\n"
            + "\t\t},\n"
            + "\t\t\"trId\": \"ed38f944-c269-4d98-8ad1-404cb2629e53\",\n"
            + "\t\t\"objectId\": \"a7e120ef-7f09-4054-8c00-980cc744aa95\",\n"
            + "\t\t\"rules\": [\n"
            + "\t\t\t{\n"
            + "\t\t\t\t\"name\": \"%4$s\",\n"
            + "\t\t\t\t\"value\": \"$.status\"\n"
            + "\t\t\t}\n"
            + "\t\t]\n"
            + "\t}\n"
            + "]";

    protected GettingTestCaseIdsResponse createGettingResponse(boolean hasFindId,
                                                               boolean hasNotFound,
                                                               boolean hasOverName) {
        GettingTestCaseIdsResponse gettingTestCaseIdsResponse = new GettingTestCaseIdsResponse();
        Map<String, List<String>> testCases = new HashMap<>();
        List<String> notFound = new ArrayList<>();
        List<String> tsIds = new ArrayList<>();
        if (hasFindId) {
            tsIds.add("61abd5ac-efbe-49cc-b7f5-925a7f543481");
            testCases.put("Test", tsIds);
        }
        if (hasOverName) {
            tsIds.add("61abd5ac-efbe-b7f5-49cc-925a7f543481");
        }
        if (hasNotFound) {
            notFound.add("lostName");
        }

        gettingTestCaseIdsResponse.setTestCases(testCases);
        gettingTestCaseIdsResponse.setNotFoundTcNames(notFound);
        return gettingTestCaseIdsResponse;
    }

    protected boolean checkSimpleParameterValueForDiffMessages(AbstractValueObject value,
                                                               @Nullable String arDiffs,
                                                               @Nullable String erDiffs) {
        SimpleValueObject castedValue = (SimpleValueObject) value;
        return Objects.equals(castedValue.getHighlightedAr(), arDiffs)
                && Objects.equals(castedValue.getHighlightedEr(), erDiffs);
    }

    protected boolean checkSshSimpleParameterValueForDiffMessages(AbstractValueObject ar,
                                                                  AbstractValueObject er,
                                                                  @Nullable String arDiffs,
                                                                  @Nullable String erDiffs) {
        SimpleValueObject castedAr = (SimpleValueObject) ar;
        SimpleValueObject castedEr = (SimpleValueObject) er;
        return Objects.equals(castedAr.getHighlightedAr(), arDiffs)
                && Objects.equals(castedEr.getHighlightedEr(), erDiffs);
    }

    protected LogCollectorValueObject generateLogCollectorValueObjectWithErrorsOnEachLevels() {
        SearchResult searchResult = new SearchResult();
        searchResult.setErrorCode("testSearchResult");
        searchResult.setErrorDetails("testSearchResult");
        searchResult.setStatus(SearchStatus.FAILED);
        List<ComponentSearchResults> components = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ComponentSearchResults componentSearchResults = new ComponentSearchResults();
            componentSearchResults.setComponentName("testComponents" + i);
            componentSearchResults.setErrorCode("testComponents" + i);
            componentSearchResults.setErrorDetails("testComponents" + i);
            componentSearchResults.setStatus(ComponentSearchStatus.FAILED);
            components.add(componentSearchResults);
            componentSearchResults.setSystemSearchResults(Collections.emptyList());
        }
        List<SystemSearchResults> systems = new ArrayList<>();
        SystemSearchResults system = new SystemSearchResults();
        system.setSystemName("testSystem");
        system.setErrorDetails("testSystem");
        system.setErrorCode("testSystem");
        system.setStatus(SearchThreadStatus.FAILED);
        systems.add(system);
        components.get(0).setSystemSearchResults(systems);
        List<SearchThreadFindResult> searchThreadResult = new ArrayList<>();
        SearchThreadFindResult searchThreadFindResult = new SearchThreadFindResult();
        searchThreadFindResult.setLogFileName("testThreadFindResult");
        searchThreadResult.add(searchThreadFindResult);
        system.setSearchThreadResult(searchThreadResult);
        searchResult.setComponentSearchResults(components);
        LogCollectorValueObject logCollectorValueObject = new LogCollectorValueObject(searchResult);
        logCollectorValueObject.setHasResults(true);
        return logCollectorValueObject;
    }

    protected SutParameterExecutionContext generateParameterExecutionContext(UUID sessionId, OffsetDateTime currentTime,
                                                                             PotSessionParameterEntity parameter,
                                                                             UUID responseId) {
        return SutParameterExecutionContext.builder()
                .sessionId(sessionId)
                .parameterStarted(currentTime)
                .sessionConfiguration(getSessionConfiguration())
                .executionVariables(new ConcurrentHashMap<>())
                .parameter(parameter)
                .isDeferredSearchResult(true)
                .countOfUnprocessedParametersUnderTab(new AtomicInteger(2))
                .countOfUnprocessedSynchronousParametersUnderPage(new AtomicInteger())
                .countOfUnprocessedTabsUnderPage(new AtomicInteger())
                .countOfUnprocessedPagesUnderSession(new AtomicInteger())
                .responseSearchId(responseId)
                .build();
    }

    private SessionExecutionConfiguration getSessionConfiguration() {
        return SessionExecutionConfiguration.builder()
                .environment(new Environment())
                .pagesName(Collections.emptyList())
                .logCollectorConfigurations(Collections.emptyList())
                .shouldHighlightDiffs(false)
                .shouldSendSessionResults(false)
                .isFullInfoNeededInPot(false)
                .onlyForPreconfiguredParams(false)
                .isPotGenerationMode(false)
                .onlyCommonParametersExecuted(false)
                .forcedLoadingCommonParameters(false)
                .build();
    }

    protected LogCollectorValueObject generateLogCollectorValueObject() {
        // Generate search Threads
        List<SearchThreadFindResult> searchThreadsUnderFirstSystemAndFirstComponent = Arrays.asList(
                generateSearchThreadFindResult(
                        Collections.singletonList(Arrays.asList("logMessage1", "logMessage2", "logMessage3")),
                        Collections.emptyMap()),
                generateSearchThreadFindResult(
                        Collections.singletonList(Collections.singletonList("logMessage4")),
                        Collections.emptyMap()),
                generateSearchThreadFindResult(
                        Collections.singletonList(Arrays.asList("logMessage4", "logMessage5")),
                        Collections.emptyMap())
        );
        List<SearchThreadFindResult> searchThreadsUnderSecondSystemAndFirstComponent = Collections.singletonList(
                generateSearchThreadFindResult(
                        Collections.singletonList(Collections.singletonList("logMessage6")),
                        Collections.emptyMap())
        );
        List<SearchThreadFindResult> searchThreadsUnderFirstSystemAndSecondComponent = Arrays.asList(
                generateSearchThreadFindResult(
                        Arrays.asList(Collections.singletonList("logMessage7"),
                                Collections.singletonList("logMessage8"),
                                Collections.singletonList("logMessage9")),
                        Collections.emptyMap()),
                generateSearchThreadFindResult(
                        Collections.singletonList(Collections.singletonList("logMessage10")),
                        Collections.emptyMap())
        );
        List<SearchThreadFindResult> searchThreadsUnderSecondSystemAndSecondComponent = Collections.emptyList();

        // Generate System search results
        List<SystemSearchResults> systemsUnderFirstComponent = Arrays.asList(
                generateSystemSearchResult("firstSystemUnderFirstComponent",
                        searchThreadsUnderFirstSystemAndFirstComponent),
                generateSystemSearchResult("secondSystemUnderFirstComponent",
                        searchThreadsUnderSecondSystemAndFirstComponent)
        );
        List<SystemSearchResults> systemsUnderSecondComponent = Arrays.asList(
                generateSystemSearchResult("firstSystemUnderSecondComponent",
                        searchThreadsUnderFirstSystemAndSecondComponent),
                generateSystemSearchResult("secondSystemUnderSecondComponent",
                        searchThreadsUnderSecondSystemAndSecondComponent)
        );

        UUID searchId = UUID.randomUUID();

        // Generate Component search results
        List<ComponentSearchResults> componentSearchResults = Arrays.asList(
                generateComponentSearchResult(searchId, ConfigurationType.GRAYLOG, systemsUnderFirstComponent),
                generateComponentSearchResult(searchId, ConfigurationType.FILEBASED, systemsUnderSecondComponent)
        );

        // Generate Search results
        SearchResult searchResult = generateLogCollectorSearchResult(searchId, componentSearchResults);
        LogCollectorValueObject logCollectorValueObject = new LogCollectorValueObject();
        logCollectorValueObject.setHasResults(true);
        logCollectorValueObject.setSearchResult(searchResult);
        return logCollectorValueObject;
    }

    private SearchThreadFindResult generateSearchThreadFindResult(List<List<String>> messages,
                                                                  Map<String, String> parameters) {
        SearchThreadFindResult searchThreadFindResult = new SearchThreadFindResult();
        searchThreadFindResult.setMessages(messages);
        searchThreadFindResult.setParameters(parameters);
        return searchThreadFindResult;
    }

    private SystemSearchResults generateSystemSearchResult(String systemName,
                                                           List<SearchThreadFindResult> searchThreadResults) {
        SystemSearchResults systemSearchResults = new SystemSearchResults();
        systemSearchResults.setSearchThreadResult(searchThreadResults);
        systemSearchResults.setSystemName(systemName);
        systemSearchResults.setSystemSearchId(UUID.randomUUID());
        systemSearchResults.setStatus(SearchThreadStatus.COMPLETED);
        return systemSearchResults;
    }

    private ComponentSearchResults generateComponentSearchResult(UUID searchId,
                                                                 ConfigurationType componentType,
                                                                 List<SystemSearchResults> componentSearchResults) {
        ComponentSearchResults componentSearchResult = new ComponentSearchResults();
        componentSearchResult.setComponentId(UUID.randomUUID());
        componentSearchResult.setComponentName(componentType.name());
        componentSearchResult.setComponentSearchId(UUID.randomUUID());
        componentSearchResult.setComponentType(componentType);
        componentSearchResult.setSearchId(searchId);
        componentSearchResult.setStatus(ComponentSearchStatus.COMPLETED);
        componentSearchResult.setSystemSearchResults(componentSearchResults);
        return componentSearchResult;
    }

    private SearchResult generateLogCollectorSearchResult(UUID searchId,
                                                          List<ComponentSearchResults> componentSearchResults) {
        SearchResult searchResult = new SearchResult();
        searchResult.setSearchId(searchId);
        searchResult.setStatus(SearchStatus.COMPLETED);
        searchResult.setComponentSearchResults(componentSearchResults);
        return searchResult;
    }

    protected ProjectConfigsEntity getProjectConfigurationEntity(UUID projectId) throws IOException {
        if (projectId.toString().equals("1183662d-51ae-438a-92be-0a4bb71620d9")) {
            String gitProject = loadFileToString("src/test/resources/test_data/projectConfig/FileProject.json");
            return objectMapper.readValue(gitProject, ProjectConfigsEntity.class);
        } else if (projectId.toString().equals("323eda51-47b5-414a-951a-27221fa374a2")) {
            String fileProject = loadFileToString("src/test/resources/test_data/projectConfig/FileProject.json");
            return objectMapper.readValue(fileProject, ProjectConfigsEntity.class);
        } else if (projectId.toString().equals("e984e5d1-5f89-4113-84a0-b658800e65c4")) {
            String folderProject = loadFileToString("src/test/resources/test_data/projectConfig/folderProject.json");
            return objectMapper.readValue(folderProject, ProjectConfigsEntity.class);
        } else {
            String testProject = loadFileToString("src/test/resources/test_data/projectConfig/FileProject.json");
            return objectMapper.readValue(testProject, ProjectConfigsEntity.class);
        }
    }

    protected ProjectConfiguration getProjectConfiguration(UUID projectId) {
        if (projectId.toString().equals("1183662d-51ae-438a-92be-0a4bb71620d9")) {
            ProjectRepositoryConfig repoConfig = new ProjectRepositoryConfig(
                    "https://github.com/qubership/svp-test-project.git",
                    "src/test/config/project/test-git");
            return new ProjectConfiguration(projectId,
                    "GIT test project",
                    true,
                    86400,
                    repoConfig);
        } else if (projectId.toString().equals("323eda51-47b5-414a-951a-27221fa374a2")) {
            ProjectRepositoryConfig repoConfig = new ProjectRepositoryConfig("src/test/config/project/test");
            return new ProjectConfiguration(projectId,
                    "Local test project",
                    true,
                    86400,
                    repoConfig);
        } else if (projectId.toString().equals("e984e5d1-5f89-4113-84a0-b658800e65c4")) {
            ProjectRepositoryConfig repoConfig = new ProjectRepositoryConfig("src/test/config/project/folder_project");
            return new ProjectConfiguration(projectId,
                    "Project with folder",
                    true,
                    86400,
                    repoConfig);
        } else {
            ProjectRepositoryConfig repoConfig = new ProjectRepositoryConfig(
                    "https://github.com/qubership/svp-test-project.git",
                    "src/test/config/project/non-existent-test-git");
            return new ProjectConfiguration(projectId,
                    "Non existent GIT test project",
                    true,
                    86400,
                    repoConfig);
        }
    }

    protected String readFileToString(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    }

    protected ProjectConfigResponseModel getProjectConfigResponseModel(String projectName, boolean isMigrateProject,
                                                                       RepositoryType type) {
        ProjectConfigResponseModel expectedConfig = new ProjectConfigResponseModel();
        expectedConfig.setProjectName(projectName);
        expectedConfig.setDefaultLogCollectorSearchTimeRange(86400);
        expectedConfig.setGitUrl("http://GitUrl");
        expectedConfig.setType(type);
        expectedConfig.setIsFullInfoNeededInPot(true);
        expectedConfig.setIsMigrateProject(isMigrateProject);
        return expectedConfig;
    }

    public <T> String convertToStr(T objects) {
        try {
            return objectMapper.writeValueAsString(objects);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ProjectConfigsEntity getProjectConfigsEntity() throws JsonProcessingException {
        String stringProjectConfigsEntity = "{\n"
                + "\t\"projectId\": \"86ad1b70-79e0-4eca-93d2-cec7d8225f19\",\n"
                + "\t\"projectName\": \"testProject\",\n"
                + "\t\"defaultLogCollectorSearchTimeRange\": 86400,\n"
                + "\t\"pagesSourceType\": \"LOCAL\",\n"
                + "\t\"gitUrl\": null,\n"
                + "\t\"pathFolderLocalProject\": \"./config/project/86ad1b70-79e0-4eca-93d2-cec7d8225f19\",\n"
                + "\t\"lastUpdateUserName\": \"INIT_PROJECT_CATALOG_NOTIFICATION_TOPIC\",\n"
                + "\t\"lastUpdateDateTime\": null,\n"
                + "\t\"folders\": null,\n"
                + "\t\"fullInfoNeededInPot\": false\n"
                + "}";
        return objectMapper.readValue(stringProjectConfigsEntity, ProjectConfigsEntity.class);
    }

    protected PotSessionParameter getPotSessionParameter(ValidationType type, boolean additionalSources) {
        PotSessionParameter potSessionParameter = new PotSessionParameter();
        potSessionParameter.setPage("TestPage");
        potSessionParameter.setTab("TestTab");
        potSessionParameter.setGroup("TestGroup");
        potSessionParameter.setParameterConfig(getSutParameter(type, additionalSources));
        potSessionParameter.setIsSynchronousLoading(false);
        potSessionParameter.setValidationInfo(new ValidationInfo());
        return potSessionParameter;
    }

    private SutParameter getSutParameter(ValidationType type, boolean additionalSources) {
        Source source = getSource();
        ErConfig erConfig = new ErConfig();
        erConfig.setType(type);
        SutParameter sutParameter = new SutParameter();
        sutParameter.setName("SOME SUT PARAM");
        sutParameter.setDisplayType(DisplayType.TABLE);
        sutParameter.setDataSource(source);
        sutParameter.setEr(erConfig);
        if (additionalSources) {
            sutParameter.getAdditionalSources().add(source);
        }
        sutParameter.setComponent("component");
        return sutParameter;
    }

    private Source getSource() {
        Source source = new Source();
        source.setSystem("SOME SYSTEM");
        source.setConnection("SOME CONNECTION");
        source.setEngineType(EngineType.SQL);
        source.setScript("SOME SCRIPT");
        source.setSettings(Collections.EMPTY_SET);
        return source;
    }

    protected String expectedExportCandidates() {
        return "[{\"id\":\"e0c238c1-5da0-490e-a106-f6115ef3383a\",\"name\":\"backup\",\"pages\":[]},"
                + "{\"id\":\"8125c2bc-31a6-4bf9-bd33-2eb034be94f7\",\"name\":\"Default\","
                + "\"pages\":[{\"id\":\"0310408e-e98c-4092-8010-e5ffdbae50d4\",\"name\":\"Integration\"},"
                + "{\"id\":\"412ce9cb-156e-4832-9d4f-66c164fdce58\",\"name\":\"Notifications\"},"
                + "{\"id\":\"9714d8b2-6285-4c15-9cb4-c11078bc499d\",\"name\":\"Billing_System_Validation\"},"
                + "{\"id\":\"59414deb-cb4c-4b7f-b669-b4aefc988884\",\"name\":\"Sales_Order_Information\"}]}]";
    }
}
