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

import javax.annotation.PostConstruct;

import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.api.Encryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CryptoUtils {

    private static Decryptor decryptorStatic;
    private static Encryptor encryptorStatic;

    @Autowired
    private Decryptor decryptor;
    @Autowired
    private Encryptor encryptor;

    /**
     * Decrypt text if encrypted.
     *
     * @param textToDecrypt text to decrypt
     * @return decrypted text
     */
    public static String decryptValue(String textToDecrypt) {
        try {
            return decryptorStatic.decryptIfEncrypted(textToDecrypt);
        } catch (Exception e) {
            throw Utils.error(log, "Problem with parsing encrypted data", e, RuntimeException.class);
        }
    }

    /**
     * Encrypt text if not encrypted.
     *
     * @param textToEncrypt text to encrypt
     * @return encrypted text
     */
    public static String encryptValue(String textToEncrypt) {
        try {
            return encryptorStatic.encrypt(textToEncrypt);
        } catch (Exception e) {
            throw Utils.error(log, "Problem with encrypt data", e, RuntimeException.class);
        }
    }

    @PostConstruct
    public void init() {
        CryptoUtils.decryptorStatic = decryptor;
        CryptoUtils.encryptorStatic = encryptor;
    }
}
