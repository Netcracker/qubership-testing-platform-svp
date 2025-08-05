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

package org.qubership.atp.svp.core;

import java.util.regex.Pattern;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class RegexpConstants {

    public static final Pattern HTML_LINK_REGEXP = Pattern.compile("<a.*?>.*?</a>");
    public static final Pattern URI_REGEXP = Pattern.compile(".*href=\"?/.*");
    public static final Pattern PATTERN_FULL_URL =
            Pattern.compile("^(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
}
