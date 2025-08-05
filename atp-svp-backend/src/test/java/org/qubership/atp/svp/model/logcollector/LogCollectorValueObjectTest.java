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

package org.qubership.atp.svp.model.logcollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.qubership.atp.svp.model.pot.values.LogCollectorValueObject;
import org.qubership.atp.svp.tests.TestWithTestData;

public class LogCollectorValueObjectTest extends TestWithTestData {

    @Test
    public void hasErrors_HasErrorsOnEachLevels_returnsTrue() {
        LogCollectorValueObject logCollectorValueObject = generateLogCollectorValueObjectWithErrorsOnEachLevels();

        boolean actualHasErrorFlag = logCollectorValueObject.hasError();

        Assert.assertTrue(actualHasErrorFlag);
    }

    @Test
    public void hasErrors_HasErrorsInSearchResult_returnsTrue() {
        SearchResult searchResult = new SearchResult();
        searchResult.setErrorCode("test");
        searchResult.setErrorDetails("testing");
        searchResult.setStatus(SearchStatus.FAILED);
        searchResult.setComponentSearchResults(Collections.emptyList());
        LogCollectorValueObject logCollectorValueObject = new LogCollectorValueObject(searchResult);

        boolean actualHasErrorFlag = logCollectorValueObject.hasError();

        Assert.assertTrue(actualHasErrorFlag);
    }

    @Test
    public void hasErrors_HasErrorsInComponentSearchResult_returnsTrue() {
        SearchResult searchResult = new SearchResult();
        List<ComponentSearchResults> components = new ArrayList<>();
        ComponentSearchResults componentSearchResults = new ComponentSearchResults();
        componentSearchResults.setComponentName("testComponents");
        componentSearchResults.setErrorCode("testComponents");
        componentSearchResults.setErrorDetails("testComponents");
        componentSearchResults.setStatus(ComponentSearchStatus.FAILED);
        componentSearchResults.setSystemSearchResults(Collections.emptyList());
        components.add(componentSearchResults);
        searchResult.setComponentSearchResults(components);
        LogCollectorValueObject logCollectorValueObject = new LogCollectorValueObject(searchResult);

        boolean actualHasErrorFlag = logCollectorValueObject.hasError();

        Assert.assertTrue(actualHasErrorFlag);
    }

    @Test
    public void hasErrors_HasErrorsInSystemSearchResult_returnsTrue() {
        SearchResult searchResult = new SearchResult();
        List<ComponentSearchResults> components = new ArrayList<>();
        ComponentSearchResults componentSearchResults = new ComponentSearchResults();
        components.add(componentSearchResults);
        List<SystemSearchResults> systemsList = new ArrayList<>();
        SystemSearchResults systemSearchResults = new SystemSearchResults();
        systemSearchResults.setSystemName("testSystem");
        systemSearchResults.setErrorDetails("testSystem");
        systemSearchResults.setErrorCode("testSystem");
        systemSearchResults.setStatus(SearchThreadStatus.FAILED);
        systemSearchResults.setSearchThreadResult(Collections.emptyList());
        systemsList.add(systemSearchResults);
        componentSearchResults.setSystemSearchResults(systemsList);
        List<SearchThreadFindResult> searchThreadResult = new ArrayList<>();
        SearchThreadFindResult searchThreadFindResult = new SearchThreadFindResult();
        searchThreadFindResult.setLogFileName("sdfsdfsdf");
        searchThreadResult.add(searchThreadFindResult);
        systemSearchResults.setSearchThreadResult(searchThreadResult);
        searchResult.setComponentSearchResults(components);
        LogCollectorValueObject logCollectorValueObject = new LogCollectorValueObject(searchResult);

        boolean actualHasErrorFlag = logCollectorValueObject.hasError();

        Assert.assertTrue(actualHasErrorFlag);
    }

    @Test
    public void hasErrors_DontHaveErrors_returnsFalse() {
        SearchResult searchResult = new SearchResult();
        searchResult.setComponentSearchResults(Collections.emptyList());
        LogCollectorValueObject logCollectorValueObject = new LogCollectorValueObject(searchResult);

        boolean actualHasErrorFlag = logCollectorValueObject.hasError();

        Assert.assertFalse(actualHasErrorFlag);
    }

    @Test
    public void hasData_haveLogs_returnTrue() {
        SearchResult searchResult = new SearchResult();
        List<ComponentSearchResults> components = new ArrayList<>();
        ComponentSearchResults componentSearchResults = new ComponentSearchResults();
        components.add(componentSearchResults);
        List<SystemSearchResults> systemsList = new ArrayList<>();
        SystemSearchResults systemSearchResults = new SystemSearchResults();
        systemsList.add(systemSearchResults);
        componentSearchResults.setSystemSearchResults(systemsList);
        List<SearchThreadFindResult> searchThreadResult = new ArrayList<>();
        SearchThreadFindResult searchThreadFindResult = new SearchThreadFindResult();
        searchThreadFindResult.setLogFileName("sdfsdfsdf");
        searchThreadResult.add(searchThreadFindResult);
        systemSearchResults.setSearchThreadResult(searchThreadResult);
        searchResult.setComponentSearchResults(components);

        boolean hasData = new LogCollectorValueObject(searchResult).hasData();

        Assert.assertTrue(hasData);
    }

    @Test
    public void hasData_dontHasLogs_returnFalse() {
        SearchResult searchResult = new SearchResult();
        List<ComponentSearchResults> components = new ArrayList<>();
        ComponentSearchResults componentSearchResults = new ComponentSearchResults();
        components.add(componentSearchResults);
        List<SystemSearchResults> systemsList = new ArrayList<>();
        SystemSearchResults systemSearchResults = new SystemSearchResults();
        systemsList.add(systemSearchResults);
        componentSearchResults.setSystemSearchResults(systemsList);
        systemSearchResults.setSearchThreadResult(Collections.emptyList());
        searchResult.setComponentSearchResults(components);

        boolean hasData = new LogCollectorValueObject(searchResult).hasData();

        Assert.assertFalse(hasData);
    }

    @Test
    public void hasData_logsIsNull_returnFalse() {
        SearchResult searchResult = new SearchResult();
        List<ComponentSearchResults> components = new ArrayList<>();
        ComponentSearchResults componentSearchResults = new ComponentSearchResults();
        components.add(componentSearchResults);
        List<SystemSearchResults> systemsList = new ArrayList<>();
        SystemSearchResults systemSearchResults = new SystemSearchResults();
        systemsList.add(systemSearchResults);
        componentSearchResults.setSystemSearchResults(systemsList);
        searchResult.setComponentSearchResults(components);

        boolean hasData = new LogCollectorValueObject(searchResult).hasData();

        Assert.assertFalse(hasData);
    }
}
