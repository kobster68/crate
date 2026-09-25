package io.crate.expression.scalar;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class TimeZoneParserTest {

    @Test
    public void test_null_timezone_is_rejected() {
        assertThatThrownBy(() -> TimeZoneParser.parseTimeZone(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("invalid time zone value NULL");
    }
}
