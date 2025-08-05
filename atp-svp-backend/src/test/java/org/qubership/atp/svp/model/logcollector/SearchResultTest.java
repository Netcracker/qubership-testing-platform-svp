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

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class SearchResultTest {

    @Test
    public void hasErrors_HasErrors_returnsTrue() {
        SearchResult searchResult = new SearchResult();
        searchResult.setErrorCode("SearchError");
        searchResult.setComponentSearchResults(Collections.emptyList());
        searchResult.setStatus(SearchStatus.IN_PROGRESS);

        boolean actualHasErrorFlag = searchResult.hasError();

        Assert.assertTrue(actualHasErrorFlag);
    }

    @Test
    public void hasErrors_DontHaveErrors_returnsFalse() {
        SearchResult searchResult = new SearchResult();
        searchResult.setComponentSearchResults(Collections.emptyList());

        boolean actualHasErrorFlag = searchResult.hasError();

        Assert.assertFalse(actualHasErrorFlag);
    }

    @Test
    public void hasErrors_ErrorCode1029_returnsFalse() {
        SearchResult searchResult = new SearchResult();
        searchResult.setErrorCode("LC-1029");
        searchResult.setComponentSearchResults(Collections.emptyList());

        boolean actualHasErrorFlag = searchResult.hasError();

        Assert.assertFalse(actualHasErrorFlag);
    }

    @Test
    public void hasErrors_ErrorCodeAndStatusCompleted_returnsFalse() {
        SearchResult searchResult = new SearchResult();
        searchResult.setComponentSearchResults(Collections.emptyList());
        searchResult.setErrorCode("testCode");
        searchResult.setStatus(SearchStatus.COMPLETED);

        boolean actualHasErrorFlag = searchResult.hasError();

        Assert.assertFalse(actualHasErrorFlag);
    }
}
