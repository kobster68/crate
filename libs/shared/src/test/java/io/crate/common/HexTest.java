/*
 * Licensed to Crate.io GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HexTest {

    @Test
    void testLowercaseEncodingPreservesByteBoundaries() {
        byte[] input = {0, 127, -128, -1};

        assertThat(Hex.encodeHexString(input)).isEqualTo("007f80ff");
    }

    @Test
    void testUppercaseEncoding() {
        byte[] input = {0, 127, -128, -1};

        assertThat(Hex.encodeHex(input, false)).containsExactly("007F80FF".toCharArray());
    }

    @Test
    void testDecodeMixedCase() {
        byte[] expected = {-85, 1};

        assertThat(Hex.decodeHex("aB01")).isEqualTo(expected);
    }

    @Test
    void testDecodeEmptyInput() {
        assertThat(Hex.decodeHex("")).isEmpty();
    }

    @Test
    void testDecodeRejectsOddLength() {
        assertThatThrownBy(() -> Hex.decodeHex("abc"))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Odd number of characters.");
    }

    @Test
    void testDecodeReportsInvalidCharacterPosition() {
        assertThatThrownBy(() -> Hex.decodeHex("0g"))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Illegal hexadecimal character g at index 1");
    }

    @Test
    void testStripHexFlagReportsOriginalCharacterPosition() {
        assertThatThrownBy(() -> Hex.stripHexFormatFlag("\\x0g"))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Illegal hexadecimal character g at index 3");
    }
}
