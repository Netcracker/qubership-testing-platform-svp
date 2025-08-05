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

import org.junit.Assert;
import org.junit.Test;

public class SystemSearchResultTest {

    @Test
    public void hasErrors_HasErrors_returnsTrue() {
        SystemSearchResults systemSearchResults = new SystemSearchResults();
        systemSearchResults.setSystemName("testSystem");
        systemSearchResults.setErrorDetails("testSystem");
        systemSearchResults.setErrorCode("testSystem");
        systemSearchResults.setStatus(SearchThreadStatus.FAILED);

        boolean actualHasErrorFlag = systemSearchResults.hasError();

        Assert.assertTrue(actualHasErrorFlag);
    }

    @Test
    public void hasErrors_ErrorCode1031_returnsFalse() {
        SystemSearchResults systemSearchResults = new SystemSearchResults();
        systemSearchResults.setSystemName("testSystem");
        systemSearchResults.setErrorDetails("testSystem");
        systemSearchResults.setErrorCode("LC-1031");

        boolean actualHasErrorFlag = systemSearchResults.hasError();

        Assert.assertFalse(actualHasErrorFlag);
    }

    @Test
    public void hasErrors_DontHaveError_returnsFalse() {
        SystemSearchResults systemSearchResults = new SystemSearchResults();

        boolean actualHasErrorFlag = systemSearchResults.hasError();

        Assert.assertFalse(actualHasErrorFlag);
    }

    @Test
    public void hasErrors_ErrorAndStatusCompleted_returnsFalse() {
        SystemSearchResults systemSearchResults = new SystemSearchResults();
        systemSearchResults.setSystemName("testSystem");
        systemSearchResults.setErrorDetails("testSystem");
        systemSearchResults.setErrorCode("testSystem");
        systemSearchResults.setStatus(SearchThreadStatus.COMPLETED);

        boolean actualHasErrorFlag = systemSearchResults.hasError();

        Assert.assertFalse(actualHasErrorFlag);
    }
}
