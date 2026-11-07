package autograder.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElapsedTimeTest {
    @Test
    void describesShortRunsInTenthsOfASecond() {
        assertEquals("0.0 s", ElapsedTime.describe(0));
        assertEquals("4.2 s", ElapsedTime.describe(4_249));
        assertEquals("59.9 s", ElapsedTime.describe(59_900));
    }

    @Test
    void describesMinutesAndHoursWithPaddedParts() {
        assertEquals("1 min 00 s", ElapsedTime.describe(60_000));
        assertEquals("2 min 05 s", ElapsedTime.describe(125_999));
        assertEquals("1 h 02 min 05 s", ElapsedTime.describe(3_725_000));
    }

    @Test
    void clockShowsWholeSeconds() {
        assertEquals("0:00", ElapsedTime.clock(999));
        assertEquals("0:42", ElapsedTime.clock(42_500));
        assertEquals("12:03", ElapsedTime.clock(723_000));
        assertEquals("1:02:05", ElapsedTime.clock(3_725_000));
    }

    @Test
    void negativeDurationsAreTreatedAsZero() {
        assertEquals("0.0 s", ElapsedTime.describe(-5));
        assertEquals("0:00", ElapsedTime.clock(-5));
    }
}
