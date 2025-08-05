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

import org.apache.logging.log4j.util.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DisplayTypeTestConstants {

    /* JsonDisplayTypeServiceImplTest */

    public static final String GROUP_NAME_DIVIDER = "||";

    public static final String JSON_RESPONSE_FILE_PATH = "src/test/resources/test_data/sut-parameter-results"
            + "/json-response";
    public static final String JSON_FIRST_VALUE_FROM_JSON = "src/test/resources/test_data/sut-parameter-results"
            + "/json-first-value-from-json";
    public static final String JSON_FIRST_VALUE_FROM_JSON_WHEN_PRIMITIVE = "src/test/resources/test_data/sut-parameter-results"
            + "/json-first-value-from-json-when-primitive";
    public static final String JSON_RESPONSE_TREE_OBJECT_FILE_PATH = "src/test/resources/test_data/sut-parameter"
            + "-results/json-response-tree-object";
    public static final String JSON_RESPONSE_TABLE_PRIMARY_FILE_PATH = "src/test/resources/test_data/sut"
            + "-parameter-results/jsonForJoinTable/jsonResponseTablePrimary";
    public static final String JSON_RESPONSE_TABLE_HIERARCHY_PRIMARY_FILE_PATH =
            "src/test/resources/test_data/sut"
                    + "-parameter-results/jsonForJoinTable/jsonResponseTableHierarchy";

    public static final String NOT_JSON_RESPONSE_FILE_PATH = "src/test/resources/test_data/sut-parameter"
            + "-results/not-json-response";

    public static final String BAD_JSON_RESPONSE_VALUE = "{I'M BAD A JSON STRING";

    public static final String JSON_CORRECT_VALUE = "{\n"
            + "   \"to\": \"Person2\",\n"
            + "   \"from\": \"Person1\",\n"
            + "   \"body\": \"Message\"\n"
            + "}";

    public static final String JSON_INCORRECT_VALUE = "{\n"
            + "   \"aaa\": \"Person2\",\n"
            + "   \"from\": \"Person1\",\n"
            + "   \"body\": \"IncorrectMessage\"\n"
            + "}";

    public static final String JSON_NODE_CORRECT_VALUE = "{\n"
            + "  \"startDateTime\": \"2021-03-02T14:49:19.937Z\",\n"
            + "  \"endDateTime\": \"2021-05-31T14:49:19.937Z\"\n"
            + "}";

    public static final String JSON_TABLE_CORRECT_VALUE = "{\"rows\":[{\"nestingDepth\":0,"
            + "\"cells\":[{\"simpleValue\":\"398887\","
            + "\"columnHeader\":\"HEADER\"}]}],\"headers\":[\"HEADER\"]}";

    public static final String JSON_TABLE_EMPTY_TABLE_VALUE = "{\"rows\":[],\"headers\":[\"HEADER\"]}";

    public static final String JSON_TABLE_JOIN_2REF_TABLE_CORRECT_VALUE = "{\"rows\":[{\"nestingDepth\":0,\"cells\":"
            + "[{\"simpleValue\":\"e0628a18-9c5f-44d0-b586-5e8a749eb590\","
            + "\"columnHeader\":\"ROOTQUOTEITEM3 (tablePrim)\"},{\"simpleValue\":\"e0628a18-9c5f-44d0-b586-5e8a749"
            + "eb590\",\"columnHeader\":\"PARENTQUOTEITEM (tablePrim)\"},{\"simpleValue\":\"68cc75d7-a9ff-43b4-805"
            + "d-9c8cc24e9597\",\"columnHeader\":\"ID (tablePrim)\"},{\"simpleValue\":\"500 MB bonus Public #1\","
            + "\"columnHeader\":\"PRODUCT.NAME (tablePrim)\"},{\"simpleValue\":\"aADD\",\"columnHeader\":"
            + "\"ACTION (tablePrim)\"},{\"simpleValue\":\"e0628a18-9c5f-44d0-b586-5e8a749eb590\",\"columnHeader\":"
            + "\"ROOTQUOTEITEM1 (group.tableRef1)\"},{\"simpleValue\":\"500 MB bonus Public #1\",\"columnHeader\":"
            + "\"PRODUCT.NAME (group.tableRef1)\"},{\"simpleValue\":\"aADD\",\"columnHeader\":"
            + "\"ACTION (group.tableRef1)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ROOTQUOTEITEM2 "
            + "(group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"PARENTQUOTEITEM (group.tableRef2)\"},"
            + "{\"simpleValue\":\"—\",\"columnHeader\":\"ID (group.tableRef2)\"}]},{\"nestingDepth\":0,\"cells\":"
            + "[{\"simpleValue\":\"e0628a18-9c5f-44d0-b586-5e8a749eb590\",\"columnHeader\":\"ROOTQUOTEITEM3 "
            + "(tablePrim)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"PARENTQUOTEITEM (tablePrim)\"},"
            + "{\"simpleValue\":\"e0628a18-9c5f-44d0-b586-5e8a749eb590\",\"columnHeader\":\"ID (tablePrim)\"},"
            + "{\"simpleValue\":\"$35 2GB Plan #1\",\"columnHeader\":\"PRODUCT.NAME (tablePrim)\"},{\"simpleValue\":"
            + "\"bADD\",\"columnHeader\":\"ACTION (tablePrim)\"},{\"simpleValue\":"
            + "\"e0628a18-9c5f-44d0-b586-5e8a749eb590\",\"columnHeader\":\"ROOTQUOTEITEM1 (group.tableRef1)\"},"
            + "{\"simpleValue\":\"$35 2GB Plan #1\",\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},"
            + "{\"simpleValue\":\"bADD\",\"columnHeader\":\"ACTION (group.tableRef1)\"},{\"simpleValue\":\"—\","
            + "\"columnHeader\":\"ROOTQUOTEITEM2 (group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":"
            + "\"PARENTQUOTEITEM (group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ID (group.tableRef2)"
            + "\"}]},{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"e0628a18-9c5f-44d0-b586-5e8a749eb590111\","
            + "\"columnHeader\":\"ROOTQUOTEITEM3 (tablePrim)\"},{\"simpleValue\":\"e0628a18-9c5f-44d0-b586-5e8a749eb590"
            + "\",\"columnHeader\":\"PARENTQUOTEITEM (tablePrim)\"},{\"simpleValue\":"
            + "\"68cc75d7-a9ff-43b4-805d-9c8cc24e9597111\",\"columnHeader\":\"ID (tablePrim)\"},{\"simpleValue\":"
            + "\"500 MB bonus Public #1\",\"columnHeader\":\"PRODUCT.NAME (tablePrim)\"},{\"simpleValue\":\"ADD\","
            + "\"columnHeader\":\"ACTION (tablePrim)\"},{\"simpleValue\":\"e0628a18-9c5f-44d0-b586-5e8a749eb590111\","
            + "\"columnHeader\":\"ROOTQUOTEITEM1 (group.tableRef1)\"},{\"simpleValue\":\"500 MB bonus Public #1\","
            + "\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},{\"simpleValue\":\"ADD\",\"columnHeader\":"
            + "\"ACTION (group.tableRef1)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ROOTQUOTEITEM2 "
            + "(group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"PARENTQUOTEITEM (group.tableRef2)"
            + "\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ID (group.tableRef2)\"}]},{\"nestingDepth\":0,\"cells"
            + "\":[{\"simpleValue\":\"e0628a18-9c5f-44d0-b586-5e8a749eb590111\",\"columnHeader\":\"ROOTQUOTEITEM3 "
            + "(tablePrim)\"},{\"simpleValue\":\"e0628a18-9c5f-44d0-b586-5e8a749eb590\",\"columnHeader\":"
            + "\"PARENTQUOTEITEM (tablePrim)\"},{\"simpleValue\":\"68cc75d7-a9ff-43b4-805d-9c8cc24e9597111\","
            + "\"columnHeader\":\"ID (tablePrim)\"},{\"simpleValue\":\"500 MB bonus Public #1\",\"columnHeader\":"
            + "\"PRODUCT.NAME (tablePrim)\"},{\"simpleValue\":\"ADD\",\"columnHeader\":\"ACTION (tablePrim)\"},"
            + "{\"simpleValue\":\"—\",\"columnHeader\":\"ROOTQUOTEITEM1 (group.tableRef1)\"},{\"simpleValue\":\"—"
            + "\",\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},{\"simpleValue\":\"—\",\"columnHeader\":"
            + "\"ACTION (group.tableRef1)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ROOTQUOTEITEM2 "
            + "(group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"PARENTQUOTEITEM "
            + "(group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ID (group.tableRef2)\"}]},"
            + "{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"fb5bdb66-c1e2-476a-87d5-62aea203007b\","
            + "\"columnHeader\":\"ROOTQUOTEITEM3 (tablePrim)\"},{\"simpleValue\":"
            + "\"fb5bdb66-c1e2-476a-87d5-62aea203007b\",\"columnHeader\":\"PARENTQUOTEITEM (tablePrim)\"},"
            + "{\"simpleValue\":\"da207a4c-fe4c-4c00-8c51-3384dc33a896\",\"columnHeader\":\"ID (tablePrim)\"},"
            + "{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"PRODUCT.NAME (tablePrim)\"},"
            + "{\"simpleValue\":\"ADD\",\"columnHeader\":\"ACTION (tablePrim)\"},{\"simpleValue\":"
            + "\"fb5bdb66-c1e2-476a-87d5-62aea203007b\",\"columnHeader\":\"ROOTQUOTEITEM1 (group.tableRef1)\"},"
            + "{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},"
            + "{\"simpleValue\":\"ADD\",\"columnHeader\":\"ACTION (group.tableRef1)\"},{\"simpleValue\":"
            + "\"—\",\"columnHeader\":\"ROOTQUOTEITEM2 (group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":"
            + "\"PARENTQUOTEITEM (group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ID (group.tableRef2)"
            + "\"}]},{\"nestingDepth\":1,\"cells\":[{\"simpleValue\":\"\",\"columnHeader\":\"ROOTQUOTEITEM3 (tablePrim)"
            + "\"},{\"simpleValue\":\"\",\"columnHeader\":\"PARENTQUOTEITEM (tablePrim)\"},{\"simpleValue\":\"\","
            + "\"columnHeader\":\"ID (tablePrim)\"},{\"simpleValue\":\"\",\"columnHeader\":\"PRODUCT.NAME (tablePrim)"
            + "\"},{\"simpleValue\":\"\",\"columnHeader\":\"ACTION (tablePrim)\"},{\"simpleValue\":"
            + "\"fb5bdb66-c1e2-476a-87d5-62aea203007b\",\"columnHeader\":\"ROOTQUOTEITEM1 (group.tableRef1)\"},"
            + "{\"simpleValue\":\"$40 2GB Plan #1\",\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},"
            + "{\"simpleValue\":\"ADD\",\"columnHeader\":\"ACTION (group.tableRef1)\"},{\"simpleValue\":\"—\","
            + "\"columnHeader\":\"ROOTQUOTEITEM2 (group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":"
            + "\"PARENTQUOTEITEM (group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ID (group.tableRef2)"
            + "\"}]},{\"nestingDepth\":1,\"cells\":[{\"simpleValue\":\"\",\"columnHeader\":\"ROOTQUOTEITEM3 (tablePrim)"
            + "\"},{\"simpleValue\":\"\",\"columnHeader\":\"PARENTQUOTEITEM (tablePrim)\"},{\"simpleValue\":"
            + "\"\",\"columnHeader\":\"ID (tablePrim)\"},{\"simpleValue\":\"\",\"columnHeader\":\"PRODUCT.NAME "
            + "(tablePrim)\"},{\"simpleValue\":\"\",\"columnHeader\":\"ACTION (tablePrim)\"},{\"simpleValue\""
            + ":\"fb5bdb66-c1e2-476a-87d5-62aea203007b\",\"columnHeader\":\"ROOTQUOTEITEM1 (group.tableRef1)"
            + "\"},{\"simpleValue\":\"Speed Booster #1\",\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},"
            + "{\"simpleValue\":\"ADD\",\"columnHeader\":\"ACTION (group.tableRef1)\"},{\"simpleValue\":\"—\","
            + "\"columnHeader\":\"ROOTQUOTEITEM2 (group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":"
            + "\"PARENTQUOTEITEM (group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ID (group.tableRef2)"
            + "\"}]},{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"fb5bdb66-c1e2-476a-87d5-62aea203007b--\","
            + "\"columnHeader\":\"ROOTQUOTEITEM3 (tablePrim)\"},{\"simpleValue\":\"—\",\"columnHeader\":"
            + "\"PARENTQUOTEITEM (tablePrim)\"},{\"simpleValue\":\"fb5bdb66-c1e2-476a-87d5-62aea203007b\","
            + "\"columnHeader\":\"ID (tablePrim)\"},{\"simpleValue\":\"$40 2GB Plan #1\",\"columnHeader\":"
            + "\"PRODUCT.NAME (tablePrim)\"},{\"simpleValue\":\"ADD\",\"columnHeader\":\"ACTION (tablePrim)"
            + "\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ROOTQUOTEITEM1 (group.tableRef1)\"},{\"simpleValue"
            + "\":\"—\",\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},{\"simpleValue\":\"—\",\"columnHeader"
            + "\":\"ACTION (group.tableRef1)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ROOTQUOTEITEM2 "
            + "(group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"PARENTQUOTEITEM (group.tableRef2)"
            + "\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ID (group.tableRef2)\"}]},{\"nestingDepth\":0,\"cells"
            + "\":[{\"simpleValue\":\"fb5bdb66-c1e2-476a-87d5-62aea203007b--\",\"columnHeader\":"
            + "\"ROOTQUOTEITEM3 (tablePrim)\"},{\"simpleValue\":\"fb5bdb66-c1e2-476a-87d5-62aea203007b\","
            + "\"columnHeader\":\"PARENTQUOTEITEM (tablePrim)\"},{\"simpleValue\":"
            + "\"ce247f89-3e73-4844-99b7-9bb87da38011\",\"columnHeader\":\"ID (tablePrim)\"},{\"simpleValue"
            + "\":\"Speed Booster #1\",\"columnHeader\":\"PRODUCT.NAME (tablePrim)\"},{\"simpleValue\":\"ADD\","
            + "\"columnHeader\":\"ACTION (tablePrim)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ROOTQUOTEITEM1 "
            + "(group.tableRef1)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)"
            + "\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ACTION (group.tableRef1)\"},{\"simpleValue\":"
            + "\"—\",\"columnHeader\":\"ROOTQUOTEITEM2 (group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader"
            + "\":\"PARENTQUOTEITEM (group.tableRef2)\"},{\"simpleValue\":\"—\",\"columnHeader\":"
            + "\"ID (group.tableRef2)\"}]}],\"headers\":[\"ROOTQUOTEITEM3 (tablePrim)\",\"PARENTQUOTEITEM (tablePrim)"
            + "\",\"ID (tablePrim)\",\"PRODUCT.NAME (tablePrim)\",\"ACTION (tablePrim)\",\"ROOTQUOTEITEM1 "
            + "(group.tableRef1)\",\"PRODUCT.NAME (group.tableRef1)\",\"ACTION (group.tableRef1)\","
            + "\"ROOTQUOTEITEM2 (group.tableRef2)\",\"PARENTQUOTEITEM (group.tableRef2)\",\"ID (group.tableRef2)\"]}";

    public static final String JSON_TABLE_HIERARCHY_JOIN_1REF_TABLE_CORRECT_VALUE = "{\"rows\":[{\"nestingDepth\":0,"
            + "\"cells\":[{\"simpleValue\":\"$40 2GB Plan #1\",\"columnHeader\":\"PRODUCT.NAME (tablePrim)\"},"
            + "{\"simpleValue\":\"fb5bdb66-c1e2-476a-87d5-62aea203007b\",\"columnHeader\":\"ID (tablePrim)\"},"
            + "{\"simpleValue\":\"ADD\",\"columnHeader\":\"ACTION (tablePrim)\"},{\"simpleValue\":\"$40 2GB Plan #1\","
            + "\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},{\"simpleValue\":"
            + "\"fb5bdb66-c1e2-476a-87d5-62aea203007b\",\"columnHeader\":\"ID (group.tableRef1)\"}]},"
            + "{\"nestingDepth\":1,\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":"
            + "\"PRODUCT.NAME (tablePrim)\"},{\"simpleValue\":\"da207a4c-fe4c-4c00-8c51-3384dc33a896\","
            + "\"columnHeader\":\"ID (tablePrim)\"},{\"simpleValue\":\"ADD\",\"columnHeader\":\"ACTION (tablePrim)\"},"
            + "{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},{\"simpleValue\":"
            + "\"da207a4c-fe4c-4c00-8c51-3384dc33a896\",\"columnHeader\":\"ID (group.tableRef1)\"}]},{\"nestingDepth\":"
            + "2,\"cells\":[{\"simpleValue\":\"\",\"columnHeader\":\"PRODUCT.NAME (tablePrim)\"},{\"simpleValue\":\"\","
            + "\"columnHeader\":\"ID (tablePrim)\"},{\"simpleValue\":\"\",\"columnHeader\":\"ACTION (tablePrim)\"},"
            + "{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},{\"simpleValue\":"
            + "\"da207a4c-fe4c-4c00-8c51-3384dc33a111\",\"columnHeader\":\"ID (group.tableRef1)\"}]},{\"nestingDepth\":"
            + "2,\"cells\":[{\"simpleValue\":\"\",\"columnHeader\":\"PRODUCT.NAME (tablePrim)\"},{\"simpleValue\":\"\","
            + "\"columnHeader\":\"ID (tablePrim)\"},{\"simpleValue\":\"\",\"columnHeader\":\"ACTION (tablePrim)\"},"
            + "{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},{\"simpleValue\":"
            + "\"da207a4c-fe4c-4c00-8c51-3384dc33a111444\",\"columnHeader\":\"ID (group.tableRef1)\"}]},{\"nestingDepth"
            + "\":2,\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"PRODUCT.NAME (tablePrim)\"},"
            + "{\"simpleValue\":\"da207a4c-fe4c-4c00-8c51-3384dc33a111\",\"columnHeader\":\"ID (tablePrim)\"},"
            + "{\"simpleValue\":\"ADD\",\"columnHeader\":\"ACTION (tablePrim)\"},{\"simpleValue\":\"—\",\"columnHeader\""
            + ":\"PRODUCT.NAME (group.tableRef1)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ID (group.tableRef1)\"}]},"
            + "{\"nestingDepth\":3,\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"PRODUCT.NAME "
            + "(tablePrim)\"},{\"simpleValue\":\"da207a4c-fe4c-4c00-8c51-3384dc33a111444\",\"columnHeader\":\"ID "
            + "(tablePrim)\"},{\"simpleValue\":\"ADD\",\"columnHeader\":\"ACTION (tablePrim)\"},{\"simpleValue\":\"—\","
            + "\"columnHeader\":\"PRODUCT.NAME (group.tableRef1)\"},{\"simpleValue\":\"—\",\"columnHeader\":\"ID"
            + " (group.tableRef1)\"}]}],\"headers\":[\"PRODUCT.NAME (tablePrim)\",\"ID (tablePrim)\",\"ACTION "
            + "(tablePrim)\",\"PRODUCT.NAME (group.tableRef1)\",\"ID (group.tableRef1)\"]}";

    public static final String JSON_TABLE_GROUPING_CELL_CORRECT_VALUE = "{\"rows\":[{\"nestingDepth\":0,"
            + "\"cells\":[{\"groupedValue\":{\"Sub-Total RC||Sub-Total RC by Product\":\"{\\n  \\\"priceType\\\": "
            + "\\\"Sub-Total RC\\\",\\n  \\\"name\\\": \\\"Sub-Total RC by Product\\\",\\n  \\\"price\\\": {}\\n}\"},"
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,\"cells\":[{\"groupedValue\":{\"Sub-Total "
            + "RC||Monthly Fee Monthly Fee Monthly Fee Monthly Fee\":\"{\\n  \\\"priceType\\\": \\\"Sub-Total RC\\\","
            + "\\n  \\\"recurringChargePeriod\\\": \\\"Month\\\",\\n  \\\"name\\\": \\\"Monthly Fee Monthly Fee "
            + "Monthly Fee Monthly Fee\\\",\\n  \\\"price\\\": {\\n    \\\"taxIncludedAmount\\\": \\\"40.00\\\",\\n  "
            + "  \\\"dutyFreeAmount\\\": \\\"40.00\\\",\\n    \\\"valueBasePriceExcludingTax\\\": \\\"40\\\",\\n    "
            + "\\\"valueBasePriceExcludingTaxRounded\\\": \\\"40.00\\\",\\n    \\\"valueBasePriceIncludingTax\\\": "
            + "\\\"40\\\",\\n    \\\"valueBasePriceIncludingTaxRounded\\\": \\\"40.00\\\",\\n    "
            + "\\\"valueExcludingTax\\\": \\\"40\\\",\\n    \\\"valueExcludingTaxRounded\\\": \\\"40.00\\\",\\n    "
            + "\\\"valueIncludingTax\\\": \\\"40\\\",\\n    \\\"valueIncludingTaxRounded\\\": \\\"40.00\\\",\\n    "
            + "\\\"valueSubTotalPriceExcludingTax\\\": \\\"40\\\",\\n    "
            + "\\\"valueSubTotalPriceExcludingTaxRounded\\\": \\\"40.00\\\",\\n    "
            + "\\\"valueSubTotalPriceIncludingTax\\\": \\\"40\\\",\\n    "
            + "\\\"valueSubTotalPriceIncludingTaxRounded\\\": \\\"40.00\\\"\\n  },\\n  \\\"priceCurrency\\\": "
            + "\\\"CAD\\\"\\n}\"},\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,"
            + "\"cells\":[{\"groupedValue\":{\"Sub-Total RC||Monthly Fee\":\"{\\n  \\\"priceType\\\": \\\"Sub-Total "
            + "RC\\\",\\n  \\\"recurringChargePeriod\\\": \\\"Month\\\",\\n  \\\"name\\\": \\\"Monthly Fee\\\",\\n  "
            + "\\\"price\\\": {\\n    \\\"taxIncludedAmount\\\": \\\"33.00\\\",\\n    \\\"dutyFreeAmount\\\": \\\"33"
            + ".00\\\",\\n    \\\"valueBasePriceExcludingTax\\\": \\\"35\\\",\\n    "
            + "\\\"valueBasePriceExcludingTaxRounded\\\": \\\"35.00\\\",\\n    \\\"valueBasePriceIncludingTax\\\": "
            + "\\\"35\\\",\\n    \\\"valueBasePriceIncludingTaxRounded\\\": \\\"35.00\\\",\\n    "
            + "\\\"valueExcludingTax\\\": \\\"33\\\",\\n    \\\"valueExcludingTaxRounded\\\": \\\"33.00\\\",\\n    "
            + "\\\"valueIncludingTax\\\": \\\"33\\\",\\n    \\\"valueIncludingTaxRounded\\\": \\\"33.00\\\",\\n    "
            + "\\\"valueSubTotalPriceExcludingTax\\\": \\\"33\\\",\\n    "
            + "\\\"valueSubTotalPriceExcludingTaxRounded\\\": \\\"33.00\\\",\\n    "
            + "\\\"valueSubTotalPriceIncludingTax\\\": \\\"33\\\",\\n    "
            + "\\\"valueSubTotalPriceIncludingTaxRounded\\\": \\\"33.00\\\"\\n  },\\n  \\\"priceCurrency\\\": "
            + "\\\"CAD\\\"\\n}\"},\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,"
            + "\"cells\":[{\"groupedValue\":{\"Sub-Total RC||Sub-Total RC by Product\":\"{\\n  \\\"priceType\\\": "
            + "\\\"Sub-Total RC\\\",\\n  \\\"name\\\": \\\"Sub-Total RC by Product\\\",\\n  \\\"price\\\": {}\\n}\"},"
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,\"cells\":[{\"groupedValue\":{\"Sub-Total "
            + "RC||Monthly Fee\":\"{\\n  \\\"priceType\\\": \\\"Sub-Total RC\\\",\\n  \\\"recurringChargePeriod\\\": "
            + "\\\"Month\\\",\\n  \\\"name\\\": \\\"Monthly Fee\\\",\\n  \\\"price\\\": {\\n    "
            + "\\\"taxIncludedAmount\\\": \\\"0.00\\\",\\n    \\\"dutyFreeAmount\\\": \\\"0.00\\\",\\n    "
            + "\\\"valueBasePriceExcludingTax\\\": \\\"10\\\",\\n    \\\"valueBasePriceExcludingTaxRounded\\\": "
            + "\\\"10.00\\\",\\n    \\\"valueBasePriceIncludingTax\\\": \\\"10\\\",\\n    "
            + "\\\"valueBasePriceIncludingTaxRounded\\\": \\\"10.00\\\",\\n    \\\"valueExcludingTax\\\": \\\"0"
            + ".00\\\",\\n    \\\"valueExcludingTaxRounded\\\": \\\"0.00\\\",\\n    \\\"valueIncludingTax\\\": \\\"0"
            + ".00\\\",\\n    \\\"valueIncludingTaxRounded\\\": \\\"0.00\\\",\\n    "
            + "\\\"valueSubTotalPriceExcludingTax\\\": \\\"0.00\\\",\\n    "
            + "\\\"valueSubTotalPriceExcludingTaxRounded\\\": \\\"0.00\\\",\\n    "
            + "\\\"valueSubTotalPriceIncludingTax\\\": \\\"0.00\\\",\\n    "
            + "\\\"valueSubTotalPriceIncludingTaxRounded\\\": \\\"0.00\\\"\\n  },\\n  \\\"priceCurrency\\\": "
            + "\\\"CAD\\\"\\n}\"},\"columnHeader\":\"HEADER\"}]}],\"headers\":[\"HEADER\"]}";

    public static final String JSON_TABLE_GROUPING_CELL_AND_COLUMN_SETTINGS_JSON_PATH_NOT_EXISTENT_CORRECT_VALUE =
            "{\"rows\":[{\"nestingDepth\":0,\"cells\":[{\"groupedValue\":{\"—||—\":\"\\\"—\\\"\"},"
                    + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,"
                    + "\"cells\":[{\"groupedValue\":{\"—||—\":\"\\\"—\\\"\"},\"columnHeader\":\"HEADER\"}]},"
                    + "{\"nestingDepth\":0,\"cells\":[{\"groupedValue\":{\"—||—\":\"\\\"—\\\"\"},"
                    + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,"
                    + "\"cells\":[{\"groupedValue\":{\"—||—\":\"\\\"—\\\"\"},\"columnHeader\":\"HEADER\"}]},"
                    + "{\"nestingDepth\":0,\"cells\":[{\"groupedValue\":{\"—||—\":\"\\\"—\\\"\"},"
                    + "\"columnHeader\":\"HEADER\"}]}],\"headers\":[\"HEADER\"]}";

    public static final String JSON_TABLE_GROUPING_CELL_AND_COLUMN_SETTINGS_JSON_PATH_NOT_ARRAY_CORRECT_VALUE =
            "{\"rows\":[{\"nestingDepth\":0,\"cells\":[{\"groupedValue\":{\"—||—\":\"\\\"6f3360b5-ba90-469a-8f38"
                    + "-d8bc2dcbfae5\\\"\"},\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,"
                    + "\"cells\":[{\"groupedValue\":{\"—||—\":\"\\\"6f3360b5-ba90-469a-8f38-d8bc2dcbfae5\\\"\"},"
                    + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,"
                    + "\"cells\":[{\"groupedValue\":{\"—||—\":\"\\\"6f3360b5-ba90-469a-8f38-d8bc2dcbfae5\\\"\"},"
                    + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,"
                    + "\"cells\":[{\"groupedValue\":{\"—||—\":\"\\\"6f3360b5-ba90-469a-8f38-d8bc2dcbfae5\\\"\"},"
                    + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,"
                    + "\"cells\":[{\"groupedValue\":{\"—||—\":\"\\\"6f3360b5-ba90-469a-8f38-d8bc2dcbfae5\\\"\"},"
                    + "\"columnHeader\":\"HEADER\"}]}],\"headers\":[\"HEADER\"]}";

    public static final String JSON_HIERARCHY_TABLE_CORRECT_VALUE = "{\"rows\":[{\"nestingDepth\":0,"
            + "\"cells\":[{\"simpleValue\":\"$40 2GB Plan #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":1,"
            + "\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":2,"
            + "\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":3,"
            + "\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":2,"
            + "\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":2,"
            + "\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":3,"
            + "\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":3,"
            + "\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":4,"
            + "\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":3,"
            + "\"cells\":[{\"simpleValue\":\"5 GB Data #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":1,"
            + "\"cells\":[{\"simpleValue\":\"Speed Booster #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":2,"
            + "\"cells\":[{\"simpleValue\":\"Speed Booster #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":2,"
            + "\"cells\":[{\"simpleValue\":\"Speed Booster #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,"
            + "\"cells\":[{\"simpleValue\":\"$35 2GB Plan #1\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":1,"
            + "\"cells\":[{\"simpleValue\":\"501 MB bonus Public\",\"columnHeader\":\"HEADER\"}]},"
            + "{\"nestingDepth\":1,\"cells\":[{\"simpleValue\":\"500 MB bonus Public\","
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":2,\"cells\":[{\"simpleValue\":\"300 MB bonus Public "
            + "#1\",\"columnHeader\":\"HEADER\"}]}],\"headers\":[\"HEADER\"]}";

    public static final String JSON_HIERARCHY_TREE_OBJECT_TABLE_TWO_NODE_NAMES_CORRECT_VALUE = "{\"rows"
            + "\":[{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"Add Mobile Line 328adc7200000\","
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"Add Mobile Line "
            + "328adc7211111\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":1,"
            + "\"cells\":[{\"simpleValue\":\"d10f5a74-4aff-46d1-bdff-07a5de5d164b_1\",\"columnHeader\":\"HEADER\"}]},"
            + "{\"nestingDepth\":2,\"cells\":[{\"simpleValue\":\"2bddc176-0d69-45c9-855d-815d8f728e8b1111\","
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":2,"
            + "\"cells\":[{\"simpleValue\":\"2bddc176-0d69-45c9-855d-815d8f728e8b2222\","
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":2,"
            + "\"cells\":[{\"simpleValue\":\"2bddc176-0d69-45c9-855d-815d8f728e8b3333\","
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":1,\"cells\":[{\"simpleValue\":\"Add Add-On "
            + "222a1bd0\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":2,"
            + "\"cells\":[{\"simpleValue\":\"d10f5a74-4aff-46d1-bdff-07a5de5d164b_1\",\"columnHeader\":\"HEADER\"}]},"
            + "{\"nestingDepth\":3,\"cells\":[{\"simpleValue\":\"2bddc176-0d69-45c9-855d-815d8f728e8b\","
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"Add Mobile Line "
            + "328adc7222222\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":1,"
            + "\"cells\":[{\"simpleValue\":\"d10f5a74-4aff-46d1-bdff-07a5de5d164b_1\",\"columnHeader\":\"HEADER\"}]},"
            + "{\"nestingDepth\":2,\"cells\":[{\"simpleValue\":\"2bddc176-0d69-45c9-855d-815d8f728e8b\","
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":1,"
            + "\"cells\":[{\"simpleValue\":\"2bddc176-0d69-45c9-855d-815d8f728e8b\",\"columnHeader\":\"HEADER\"}]}],"
            + "\"headers\":[\"HEADER\"]}";

    public static final String JSON_HIERARCHY_TREE_OBJECT_TABLE_ONE_NODE_NAMES_CORRECT_VALUE = "{\"rows"
            + "\":[{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"Add Mobile Line 328adc7200000\","
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"Add Mobile Line "
            + "328adc7211111\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":1,"
            + "\"cells\":[{\"simpleValue\":\"d10f5a74-4aff-46d1-bdff-07a5de5d164b_1\",\"columnHeader\":\"HEADER\"}]},"
            + "{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"Add Mobile Line 328adc7222222\","
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":1,"
            + "\"cells\":[{\"simpleValue\":\"d10f5a74-4aff-46d1-bdff-07a5de5d164b_1\","
            + "\"columnHeader\":\"HEADER\"}]}],\"headers\":[\"HEADER\"]}";

    public static final String JSON_HIERARCHY_TREE_OBJECT_TABLE_ONE_NODE_NAMES_NOT_TYPICAL_CORRECT_VALUE = "{\"rows"
            + "\":[{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"Add Mobile Line 328adc7200000\","
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":1,"
            + "\"cells\":[{\"simpleValue\":\"2bddc176-0d69-45c9-855d-815d8f728e8b1111\","
            + "\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"Add Mobile Line "
            + "328adc7211111\",\"columnHeader\":\"HEADER\"}]},{\"nestingDepth\":0,\"cells\":[{\"simpleValue\":\"Add "
            + "Mobile Line 328adc7222222\",\"columnHeader\":\"HEADER\"}]}],\"headers\":[\"HEADER\"]}";

    public static final String HIGHLIGHTED_JSON_AR_WITH_DIFF_MESSAGES = "<?xml version=\"1.0\" "
            + "encoding=\"UTF-8\"?><pre>{<br><span data-block-id=\"pc-highlight-block\" class=\"EXTRA\" title=\"AR "
            + "has extra node(s).\">  \"aaa\" : \"Person2\",</span><br>  \"from\" : \"Person1\",<br>  \"body\" : "
            + "<span data-block-id=\"pc-highlight-block\" class=\"SIMILAR\" title=\"Similar property or "
            + "object\">\"IncorrectMessage\"</span><br>}</pre>";


    public static final String HIGHLIGHTED_JSON_AR_WITH_NORMAL_DIFF_MESSAGES = "<?xml version=\"1.0\" "
            + "encoding=\"UTF-8\"?><pre>{<br>  \"to\" : \"Person2\",<br>  \"from\" : \"Person1\",<br>  \"body\" : "
            + "\"Message\"<br>}</pre>";

    /* TableDisplayTypeServiceImplTest */

    public static final String TABLE_RESPONSE_FILE_PATH = "src/test/resources/test_data/sut-parameter-results"
            + "/table-response";
    public static final String INCORRECT_TABLE_RESPONSE_FILE_PATH = "src/test/resources/test_data/sut-parameter"
            + "-results"
            + "/incorrect-table-response";
    public static final String TABLE_VALIDATION_BV_FILE_PATH = "src/test/resources/test_data/sut-parameter"
            + "-results"
            + "/table-validation-bv";
    public static final String BULK_VALIDATOR_COMPARING_PASSED_RESPONSE_FILE_PATH = "src/test/resources"
            + "/test_data/"
            + "sut-parameter-results/bulk-validator-comparing-process-passed-response";
    public static final String BULK_VALIDATOR_COMPARING_FAILED_RESPONSE_FILE_PATH = "src/test/resources"
            + "/test_data/"
            + "sut-parameter-results/bulk-validator-comparing-process-failed-response";
    public static final String BULK_VALIDATOR_CREATE_TEST_RUN_RESPONSE_FILE_PATH = "src/test/resources/test_data/"
            + "sut-parameter-results/bulk-validator-create-test-run-response";

    /* XmlDisplayTypeServiceImplTest */

    public static final String XML_CORRECT_VALUE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<message>" + Strings.LINE_SEPARATOR
            + "   <to>Person2</to>" + Strings.LINE_SEPARATOR
            + "   <from>Person1</from>" + Strings.LINE_SEPARATOR
            + "   <body>Message</body>" + Strings.LINE_SEPARATOR
            + "</message>"+ Strings.LINE_SEPARATOR;

    public static final String XML_INCORRECT_VALUE = "<aaa>\n"
            + "<to>Person2</to>\n"
            + "<from>Person1</from>\n"
            + "<body>IncorrectMessage</body>\n"
            + "</aaa>";

    public static final String HIGHLIGHTED_XML_ER_WITH_DIFF_MESSAGES = "<div style=\"margin-left: 0px\"><div "
            + "class=\"NORMAL\"><span data-block-id=\"pc-highlight-block\" class=\"MISSED\" >&lt;message&gt;"
            + "</span></div></div><div style=\"margin-left: 15px\"><div class=\"NORMAL\"><span "
            + "data-block-id=\"pc-highlight-block\" class=\"MISSED\" >&lt;to&gt;Person2&lt;/to&gt;"
            + "</span></div></div><div style=\"margin-left: 15px\"><div class=\"NORMAL\"><span "
            + "data-block-id=\"pc-highlight-block\" class=\"MISSED\" >&lt;from&gt;Person1&lt;/from&gt;"
            + "</span></div></div><div style=\"margin-left: 15px\"><div class=\"NORMAL\"><span "
            + "data-block-id=\"pc-highlight-block\" class=\"MISSED\" >&lt;body&gt;Message&lt;/body&gt;"
            + "</span></div></div><div style=\"margin-left: 0px\"><div class=\"NORMAL\"><span "
            + "data-block-id=\"pc-highlight-block\" class=\"MISSED\" >&lt;/message&gt;</span></div></div>";

    public static final String HIGHLIGHTED_XML_AR_WITH_DIFF_MESSAGES = "<div style=\"margin-left: 0px\"><div "
            + "class=\"NORMAL\"><span data-block-id=\"pc-highlight-block\" class=\"EXTRA\" >&lt;aaa&gt;"
            + "</span></div></div><div style=\"margin-left: 15px\"><div class=\"NORMAL\"><span "
            + "data-block-id=\"pc-highlight-block\" class=\"EXTRA\" >&lt;to&gt;Person2&lt;/to&gt;"
            + "</span></div></div><div style=\"margin-left: 15px\"><div class=\"NORMAL\"><span "
            + "data-block-id=\"pc-highlight-block\" class=\"EXTRA\" >&lt;from&gt;Person1&lt;/from&gt;"
            + "</span></div></div><div style=\"margin-left: 15px\"><div class=\"NORMAL\"><span "
            + "data-block-id=\"pc-highlight-block\" class=\"EXTRA\" >&lt;body&gt;IncorrectMessage&lt;/body&gt;"
            + "</span></div></div><div style=\"margin-left: 0px\"><div class=\"NORMAL\"><span "
            + "data-block-id=\"pc-highlight-block\" class=\"EXTRA\" >&lt;/aaa&gt;</span></div></div>";

    public static final String HIGHLIGHTED_XML_ER_WITH_NORMAL_DIFF_MESSAGES = "<div style=\"margin-left: 0px\"><div "
            + "class=\"NORMAL\">&lt;message&gt;</div></div><div style=\"margin-left: 15px\"><div "
            + "class=\"NORMAL\">&lt;to&gt;Person2&lt;/to&gt;</div></div><div style=\"margin-left: 15px\"><div "
            + "class=\"NORMAL\">&lt;from&gt;Person1&lt;/from&gt;</div></div><div style=\"margin-left: 15px\"><div "
            + "class=\"NORMAL\">&lt;body&gt;Message&lt;/body&gt;</div></div><div style=\"margin-left: 0px\"><div "
            + "class=\"NORMAL\">&lt;/message&gt;</div></div>";

    public static final String HIGHLIGHTED_XML_AR_WITH_NORMAL_DIFF_MESSAGES = "<div style=\"margin-left: 0px\"><div "
            + "class=\"NORMAL\">&lt;message&gt;</div></div><div style=\"margin-left: 15px\"><div "
            + "class=\"NORMAL\">&lt;to&gt;Person2&lt;/to&gt;</div></div><div style=\"margin-left: 15px\"><div "
            + "class=\"NORMAL\">&lt;from&gt;Person1&lt;/from&gt;</div></div><div style=\"margin-left: 15px\"><div "
            + "class=\"NORMAL\">&lt;body&gt;Message&lt;/body&gt;</div></div><div style=\"margin-left: 0px\"><div "
            + "class=\"NORMAL\">&lt;/message&gt;</div></div>";

    public static final String HIGHLIGHTED_SSH_AR_WITH_DIFF_MESSAGES = "<div style=\"margin-left: 0px\"><span "
            + "data-block-id=\"pc-highlight-block\" class=\"SIMILAR\">brow_2\n"
            + "</span></div><div style=\"margin-left: 0px\"><span data-block-id=\"pc-highlight-block\" "
            + "class=\"SIMILAR\">row_1\n"
            + "</span></div><div style=\"margin-left: 0px\">dev\n"
            + "</div><div style=\"margin-left: 0px\">dumps\n"
            + "</div><div style=\"margin-left: 0px\">etc\n"
            + "</div><div style=\"margin-left: 0px\">home\n"
            + "</div><div style=\"margin-left: 0px\">lib\n"
            + "</div><div style=\"margin-left: 0px\">lib64\n"
            + "</div><div style=\"margin-left: 0px\">logs\n"
            + "</div><div style=\"margin-left: 0px\">lost+found\n"
            + "</div><div style=\"margin-left: 0px\">media\n"
            + "</div><div style=\"margin-left: 0px\">mnt\n"
            + "</div><div style=\"margin-left: 0px\">opt\n"
            + "</div><div style=\"margin-left: 0px\">proc\n"
            + "</div><div style=\"margin-left: 0px\">root\n"
            + "</div><div style=\"margin-left: 0px\">run\n"
            + "</div><div style=\"margin-left: 0px\">sbin\n"
            + "</div><div style=\"margin-left: 0px\">srv\n"
            + "</div><div style=\"margin-left: 0px\">sys\n"
            + "</div><div style=\"margin-left: 0px\">tmp\n"
            + "</div><div style=\"margin-left: 0px\">u01\n"
            + "</div><div style=\"margin-left: 0px\">u02\n"
            + "</div><div style=\"margin-left: 0px\">ub01\n"
            + "</div><div style=\"margin-left: 0px\">usr\n"
            + "</div><div style=\"margin-left: 0px\">var\n"
            + "</div>";

    public static final String HIGHLIGHTED_SSH_ER_WITH_DIFF_MESSAGES = "<div style=\"margin-left: 0px\"><span "
            + "data-block-id=\"pc-highlight-block\" class=\"SIMILAR\">brow_1\n"
            + "</span></div><div style=\"margin-left: 0px\"><span data-block-id=\"pc-highlight-block\" "
            + "class=\"SIMILAR\">row_2\n"
            + "</span></div><div style=\"margin-left: 0px\">dev\n"
            + "</div><div style=\"margin-left: 0px\">dumps\n"
            + "</div><div style=\"margin-left: 0px\">etc\n"
            + "</div><div style=\"margin-left: 0px\">home\n"
            + "</div><div style=\"margin-left: 0px\">lib\n"
            + "</div><div style=\"margin-left: 0px\">lib64\n"
            + "</div><div style=\"margin-left: 0px\">logs\n"
            + "</div><div style=\"margin-left: 0px\">lost+found\n"
            + "</div><div style=\"margin-left: 0px\">media\n"
            + "</div><div style=\"margin-left: 0px\">mnt\n"
            + "</div><div style=\"margin-left: 0px\">opt\n"
            + "</div><div style=\"margin-left: 0px\">proc\n"
            + "</div><div style=\"margin-left: 0px\">root\n"
            + "</div><div style=\"margin-left: 0px\">run\n"
            + "</div><div style=\"margin-left: 0px\">sbin\n"
            + "</div><div style=\"margin-left: 0px\">srv\n"
            + "</div><div style=\"margin-left: 0px\">sys\n"
            + "</div><div style=\"margin-left: 0px\">tmp\n"
            + "</div><div style=\"margin-left: 0px\">u01\n"
            + "</div><div style=\"margin-left: 0px\">u02\n"
            + "</div><div style=\"margin-left: 0px\">ub01\n"
            + "</div><div style=\"margin-left: 0px\">usr\n"
            + "</div><div style=\"margin-left: 0px\">var\n"
            + "</div>";
    public static final String HIGHLIGHTED_SSH_AR_WITH_NORMAL_DIFF_MESSAGES = "<div style=\"margin-left: 0px\">brow_1\n"
            + "</div><div style=\"margin-left: 0px\">row_2\n"
            + "</div><div style=\"margin-left: 0px\">dev\n"
            + "</div><div style=\"margin-left: 0px\">dumps\n"
            + "</div><div style=\"margin-left: 0px\">etc\n"
            + "</div><div style=\"margin-left: 0px\">home\n"
            + "</div><div style=\"margin-left: 0px\">lib\n"
            + "</div><div style=\"margin-left: 0px\">lib64\n"
            + "</div><div style=\"margin-left: 0px\">logs\n"
            + "</div><div style=\"margin-left: 0px\">lost+found\n"
            + "</div><div style=\"margin-left: 0px\">media\n"
            + "</div><div style=\"margin-left: 0px\">mnt\n"
            + "</div><div style=\"margin-left: 0px\">opt\n"
            + "</div><div style=\"margin-left: 0px\">proc\n"
            + "</div><div style=\"margin-left: 0px\">root\n"
            + "</div><div style=\"margin-left: 0px\">run\n"
            + "</div><div style=\"margin-left: 0px\">sbin\n"
            + "</div><div style=\"margin-left: 0px\">srv\n"
            + "</div><div style=\"margin-left: 0px\">sys\n"
            + "</div><div style=\"margin-left: 0px\">tmp\n"
            + "</div><div style=\"margin-left: 0px\">u01\n"
            + "</div><div style=\"margin-left: 0px\">u02\n"
            + "</div><div style=\"margin-left: 0px\">ub01\n"
            + "</div><div style=\"margin-left: 0px\">usr\n"
            + "</div><div style=\"margin-left: 0px\">var\n"
            + "</div>";

    public static final String HIGHLIGHTED_SSH_ER_WITH_NORMAL_DIFF_MESSAGES = "<div style=\"margin-left: 0px\">brow_1\n"
            + "</div><div style=\"margin-left: 0px\">row_2\n"
            + "</div><div style=\"margin-left: 0px\">dev\n"
            + "</div><div style=\"margin-left: 0px\">dumps\n"
            + "</div><div style=\"margin-left: 0px\">etc\n"
            + "</div><div style=\"margin-left: 0px\">home\n"
            + "</div><div style=\"margin-left: 0px\">lib\n"
            + "</div><div style=\"margin-left: 0px\">lib64\n"
            + "</div><div style=\"margin-left: 0px\">logs\n"
            + "</div><div style=\"margin-left: 0px\">lost+found\n"
            + "</div><div style=\"margin-left: 0px\">media\n"
            + "</div><div style=\"margin-left: 0px\">mnt\n"
            + "</div><div style=\"margin-left: 0px\">opt\n"
            + "</div><div style=\"margin-left: 0px\">proc\n"
            + "</div><div style=\"margin-left: 0px\">root\n"
            + "</div><div style=\"margin-left: 0px\">run\n"
            + "</div><div style=\"margin-left: 0px\">sbin\n"
            + "</div><div style=\"margin-left: 0px\">srv\n"
            + "</div><div style=\"margin-left: 0px\">sys\n"
            + "</div><div style=\"margin-left: 0px\">tmp\n"
            + "</div><div style=\"margin-left: 0px\">u01\n"
            + "</div><div style=\"margin-left: 0px\">u02\n"
            + "</div><div style=\"margin-left: 0px\">ub01\n"
            + "</div><div style=\"margin-left: 0px\">usr\n"
            + "</div><div style=\"margin-left: 0px\">var\n"
            + "</div>";
}
