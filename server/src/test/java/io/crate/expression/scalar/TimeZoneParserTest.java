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

package io.crate.expression.scalar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.joda.time.DateTimeZone;
import org.junit.Test;

// Tests TimeZoneParser class
// Contains one new coverage test and four new edge case tests.
public class TimeZoneParserTest {

    // Coverage: a missing time zone should give a clear error message.
    @Test
    public void test_null_timezone_is_rejected() {
        assertThatThrownBy(() -> TimeZoneParser.parseTimeZone(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("invalid time zone value NULL");
    }

    // Edge case: minutes must be between 0 and 59, so 60 should give an error.
    @Test
    public void test_out_of_range_offset_minutes_are_rejected() {
        assertThatThrownBy(() -> TimeZoneParser.parseTimeZone("+02:60"))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("invalid time zone value '+02:60'");
    }

    // Edge case: +02:30 must include the extra 30 minutes ahead of UTC.
    @Test
    public void test_positive_offset_includes_minutes() {
        DateTimeZone timezone = TimeZoneParser.parseTimeZone("+02:30");

        assertThat(timezone.isFixed()).isTrue();
        // The offset is in milliseconds: 150 minutes times 60 seconds times 1000.
        assertThat(timezone.getOffset(0L)).isEqualTo(150 * 60 * 1000);
    }

    // Edge case: -02:30 means the full 2 hours and 30 minutes are behind UTC.
    @Test
    public void test_negative_offset_applies_sign_to_minutes() {
        DateTimeZone timezone = TimeZoneParser.parseTimeZone("-02:30");

        assertThat(timezone.isFixed()).isTrue();
        assertThat(timezone.getOffset(0L)).isEqualTo(-150 * 60 * 1000);
    }

    // Edge case: minutes must be numbers, so "xx" should give a clear error message.
    @Test
    public void test_non_numeric_offset_minutes_are_rejected() {
        assertThatThrownBy(() -> TimeZoneParser.parseTimeZone("+02:xx"))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("invalid time zone value '+02:xx'");
    }
}
