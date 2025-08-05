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

package org.qubership.atp.svp.model.impl;

import java.util.List;
import java.util.Objects;

import org.assertj.core.util.Strings;
import org.qubership.atp.svp.core.enums.JsonParseViewType;
import org.qubership.atp.svp.model.pot.values.SimpleValueObject;
import org.qubership.atp.svp.model.pot.values.TableValueObject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JsonParseSettings implements SourceSettings {

    /**
     * Defines the representation of the parsed json.
     * If TABLE or HIERARCHY_TABLE or HIERARCHY_TREE_OBJECT_TABLE - Table representation {@link TableValueObject}
     * If RAW - Simple string representation {@link SimpleValueObject}
     */
    private JsonParseViewType jsonViewType = JsonParseViewType.RAW;

    /**
     * In case {@link JsonParseSettings#jsonViewType} is JSON TABLE or HIERARCHY_TABLE or HIERARCHY_TREE_OBJECT_TABLE,
     * this parameter represents the json path to an array of table objects.
     * <p/>
     * In case {@link JsonParseSettings#jsonViewType} is RAW,
     * this parameter represents the json path to any node for display
     * it as a plain string parameter or json node.
     */
    private String jsonPath;

    /**
     * In case {@link JsonParseSettings#jsonViewType} is JSON TABLE,
     * this parameter is divided into the names of the group parameters.
     */
    private String groupNameDivider;

    /**
     * In case {@link JsonParseSettings#jsonViewType} is JSON TABLE,
     * This is used as a mock alternative source.
     */
    private String mockJsonScript;

    /**
     * In case {@link JsonParseSettings#jsonViewType} is JSON TABLE,
     * This boolean flag is used to determine whether or not an alternative SQL source is used.
     */
    private Boolean isMockJsonSwitcher;

    /**
     * In case {@link JsonParseSettings#jsonViewType} is JSON TABLE,
     * this parameter represents the settings of column representation to table.
     */
    private List<JsonDataColumnSettings> columnsData;

    /**
     * In case {@link JsonParseSettings#jsonViewType} is JSON TABLE at HIERARCHY_TABLE,
     * this parameter contains the node names (id, rootId, parentId) for building the hierarchy.
     */
    private JsonHierarchyNodeNames hierarchyNodeNames;

    /**
     * In case {@link JsonParseSettings#jsonViewType} JSON TABLE at HIERARCHY_TREE_OBJECT_TABLE,
     * this parameter contains the list object node names for building the hierarchy tree object.
     */
    private List<String> hierarchyTreeObjectNodeNames;

    /**
     * In case includes a switch if you want to set the conditions for joining tables by key fields.
     * Works with all types of json pasting except RAW.
     */
    private Boolean isJoinConditionSwitcher;

    /**
     * In case contains a list of settings objects for join tables.
     */
    private List<JsonJoinConditionSettings> jsonJoinConditionSettings;

    /**
     * In case {@link JsonParseSettings#jsonViewType} is RAW,
     * this parameter get first value from json.
     */
    private Boolean isFirstValueParseHowRaw;

    public Boolean getIsMockJsonSwitcher() {
        return !Strings.isNullOrEmpty(mockJsonScript) && Objects.isNull(isMockJsonSwitcher)
                || !Strings.isNullOrEmpty(mockJsonScript) && isMockJsonSwitcher;
    }

    public Boolean getIsJoinConditionSwitcher() {
        return Objects.nonNull(isJoinConditionSwitcher) && isJoinConditionSwitcher
                && jsonViewType != JsonParseViewType.RAW && Objects.nonNull(jsonJoinConditionSettings)
                && !jsonJoinConditionSettings.isEmpty();
    }

    public Boolean getIsFirstValueParseHowRaw() {
        return Objects.nonNull(isFirstValueParseHowRaw) && isFirstValueParseHowRaw;
    }

    @Override
    public boolean equals(Object settings) {
        return settings instanceof JsonParseSettings;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass().getName());
    }
}
