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

package org.qubership.atp.svp.model.db.pot.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.qubership.atp.svp.core.enums.DisplayType;
import org.qubership.atp.svp.core.enums.EngineType;
import org.qubership.atp.svp.core.enums.ValidationStatus;
import org.qubership.atp.svp.core.enums.ValidationType;
import org.qubership.atp.svp.model.db.SutParameterEntity;
import org.qubership.atp.svp.model.pot.validation.ValidationInfo;
import org.qubership.atp.svp.model.pot.values.AbstractValueObject;
import org.qubership.atp.svp.model.pot.values.ErrorValueObject;
import org.qubership.atp.svp.model.pot.values.LogCollectorValueObject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pot_session_parameter", indexes = {
        @Index(name = "pot_session_parameter_pot_session_id_index", columnList = "pot_session_id"),
        @Index(name = "pot_session_parameter_pot_session_tab_id_index", columnList = "pot_session_tab_id")
})
@Getter
@Setter
@NoArgsConstructor
@TypeDef(name = "json", typeClass = JsonType.class)
public class PotSessionParameterEntity {

    @Id
    @GeneratedValue
    @Column(name = "parameter_id")
    private UUID parameterId;

    private String page;

    private String tab;

    @Column(name = "group_name")
    private String group;

    @Column(name = "synchronous_loading")
    private boolean synchronousLoading;

    @Type(type = "json")
    @Column(columnDefinition = "jsonb", name = "er")
    private AbstractValueObject er;

    @Type(type = "json")
    @Column(columnDefinition = "jsonb", name = "ar_values")
    private List<AbstractValueObject> arValues = new ArrayList<>();

    @Type(type = "json")
    @Column(columnDefinition = "jsonb", name = "validation_info")
    private ValidationInfo validationInfo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sut_parameter_id", foreignKey = @ForeignKey(name = "FK_sut_parameter_id"))
    private SutParameterEntity parameterConfig;

    @ManyToOne
    @JoinColumn(name = "pot_session_tab_id", foreignKey = @ForeignKey(name = "FK_pot_session_tab_id"))
    private PotSessionTabEntity potSessionTabEntity;

    @ManyToOne
    @JoinColumn(name = "pot_session_id", foreignKey = @ForeignKey(name = "FK_pot_session_id"))
    private PotSessionEntity potSessionEntity;

    /**
     * The constructor of PotSessionParameterEntity for CommonParameters.
     */
    public PotSessionParameterEntity(PotSessionEntity session, SutParameterEntity commonParameter) {
        this.potSessionEntity = session;
        this.parameterConfig = commonParameter;
        this.validationInfo = getInitialValidationInfo();
    }

    /**
     * The constructor of PotSessionParameterEntity for Page.
     */
    public PotSessionParameterEntity(SutParameterEntity sutParameter, PotSessionTabEntity tab, String pageName,
                                     String groupName, boolean isSynchronousLoading) {
        this.group = groupName;
        this.page = pageName;
        this.tab = tab.getName();
        this.parameterConfig = sutParameter;
        this.validationInfo = getInitialValidationInfo();
        this.potSessionTabEntity = tab;
        this.synchronousLoading = isSynchronousLoading;
    }

    @JsonIgnore
    public String getName() {
        return parameterConfig.getName();
    }

    @JsonIgnore
    public String getComponentName() {
        return parameterConfig.getComponent();
    }


    public void setIsSynchronousLoading(boolean synchronousLoading) {
        this.synchronousLoading = synchronousLoading;
    }

    /**
     * Gets parameter's path representation separated by slashes.
     */
    @JsonIgnore
    public String getPath() {
        return page + "_" + tab + "_" + group + "_" + getName();
    }

    public void addArValue(AbstractValueObject abstractValueObject) {
        arValues.add(abstractValueObject);
    }

    /**
     * Fill {@link #validationInfo} with validation status.
     */
    @JsonIgnore
    public void setValidationStatus(ValidationStatus status) {
        validationInfo.setStatus(status);
    }

    /**
     * Checks if parameter has error values.
     */
    public boolean hasErrors() {
        if (Objects.nonNull(er) && er instanceof ErrorValueObject) {
            return true;
        }
        for (AbstractValueObject arValue : arValues) {
            if (arValue instanceof ErrorValueObject) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if LogCollectorValueObject has error values.
     */
    public boolean hasLogCollectorErrors() {
        if (Objects.nonNull(er) && er instanceof LogCollectorValueObject) {
            return ((LogCollectorValueObject) er).hasError();
        }
        for (AbstractValueObject arValue : arValues) {
            if (arValue instanceof LogCollectorValueObject) {
                return ((LogCollectorValueObject) arValue).hasError();
            }
        }
        return false;
    }

    /**
     * Returns parameter error values in String format.
     */
    public boolean containsData() {
        return Objects.nonNull(er) && er.hasData() || arValues.stream().anyMatch(AbstractValueObject::hasData);
    }

    /**
     * Returns parameter error values in String format.
     */
    public String getErrors() {
        StringBuilder error = new StringBuilder();
        for (int i = 0; i < this.arValues.size(); i++) {
            AbstractValueObject ar = this.arValues.get(i);
            if (ar instanceof ErrorValueObject) {
                error.append("AR")
                        .append(i + 1)
                        .append(": ")
                        .append(((ErrorValueObject) ar).getErrorMessage())
                        .append("\n");
            }
        }
        if (er instanceof ErrorValueObject) {
            error.append("ER: ")
                    .append(((ErrorValueObject) er).getErrorMessage());
        }
        return error.toString();
    }

    /**
     * <p>Returns true if the parameter contains deferred results (checked by {@link EngineType}).</p>
     * <p>Variations of deferred results are below:
     * <ul>
     *      <li>{@link EngineType#LOG_COLLECTOR}</li>
     * </ul>
     * </p>
     */
    public boolean hasDeferredResults() {
        return parameterConfig.getSource().getEngineType() == EngineType.LOG_COLLECTOR;
    }

    /**
     * <p>Returns true if the parameter has correctly loaded deferred results.</p>
     * <br>
     * <p>Checks:
     * <ul>
     *      <li>first actual result value is present</li>
     *      <li>first actual result value has type {@link LogCollectorValueObject}</li>
     *      <li>first actual result contains full results from LogCollector</li>
     * </ul>
     * </p>
     */
    public boolean hasCorrectlyLoadedDeferredResults() {
        long arValuesWithResultCount = arValues.stream()
                .filter(arValue -> arValue instanceof LogCollectorValueObject)
                .filter(arValue -> ((LogCollectorValueObject) arValue).hasResults())
                .count();
        return arValuesWithResultCount > 0 || hasErrors();
    }

    public boolean hasInProgressStatus() {
        return validationInfo.getStatus().equals(ValidationStatus.IN_PROGRESS);
    }

    private ValidationInfo getInitialValidationInfo() {
        return new ValidationInfo(ValidationStatus.IN_PROGRESS);
    }

    /**
     * Parameter allowed validation in next cases.
     * <br>
     * - onlyPreconfiguredParametersAllowed flag is false and preconfigured flag for parameter is false
     * <br>
     * - onlyPreconfiguredParametersAllowed flag is false and preconfigured flag for parameter is true
     * <br>
     * - onlyPreconfiguredParametersAllowed flag is true and preconfigured flag for parameter is true.
     *
     * @param onlyPreconfiguredParametersAllowed configuration from session,
     *         which shows if all the parameters or only preconfigured.
     * @return parameter allowed for execution flag.
     */
    public boolean isAllowedForExecution(boolean onlyPreconfiguredParametersAllowed) {
        return !onlyPreconfiguredParametersAllowed || parameterConfig.isPreconfigured();
    }

    public boolean shouldCreateSeparateFileForPotReport() {
        return Arrays.asList(DisplayType.JSON, DisplayType.XML, DisplayType.TABLE)
                .contains(parameterConfig.getDisplayType());
    }

    public boolean shouldHaveExpectedResult() {
        return Arrays.asList(ValidationType.PLAIN, ValidationType.CUSTOM)
                .contains(parameterConfig.getErConfig().getType());
    }
}
