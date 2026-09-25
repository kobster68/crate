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
