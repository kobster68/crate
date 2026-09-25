package io.crate.expression.scalar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.joda.time.DateTimeZone;
import org.junit.Test;

public class TimeZoneParserTest {

    @Test
    public void test_null_timezone_is_rejected() {
        assertThatThrownBy(() -> TimeZoneParser.parseTimeZone(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("invalid time zone value NULL");
    }

    @Test
    public void test_utc_returns_default_timezone() {
        assertThat(TimeZoneParser.parseTimeZone("UTC")).isSameAs(DateTimeZone.UTC);
    }
}
