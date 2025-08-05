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

package org.qubership.atp.svp.utils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.api.Encryptor;
import org.qubership.atp.crypt.exception.AtpDecryptException;

public class CryptoUtilsTest {

    @InjectMocks
    private CryptoUtils target;

    @Mock
    private Decryptor decryptor;
    @Mock
    private Encryptor encryptor;

    @Before
    public void setUp()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, AtpDecryptException {
        MockitoAnnotations.initMocks(this);

        when(decryptor.decryptIfEncrypted(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return (String) args[0];
            }
        });

        //call post-constructor
        Method postConstruct = CryptoUtils.class.getDeclaredMethod("init", null); // methodName,parameters
        postConstruct.setAccessible(true);
        postConstruct.invoke(target);
    }
}
