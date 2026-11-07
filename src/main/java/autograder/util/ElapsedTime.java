package autograder.util;

import java.util.Locale;

/** Formats an elapsed run time for people: a summary such as "2 min 05 s" or a ticking clock such as "2:05". */
public final class ElapsedTime {
    private static final long SECOND = 1_000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;

    private ElapsedTime() {
    }

    /** "4.2 s" under a minute, "2 min 05 s" under an hour, otherwise "1 h 02 min 05 s". */
    public static String describe(long millis) {
        long value = Math.max(0, millis);
        if (value < MINUTE) {
            return String.format(Locale.ROOT, "%.1f s", value / (double) SECOND);
        }
        long hours = value / HOUR;
        long minutes = value % HOUR / MINUTE;
        long seconds = value % MINUTE / SECOND;
        if (hours == 0) {
            return String.format(Locale.ROOT, "%d min %02d s", minutes, seconds);
        }
        return String.format(Locale.ROOT, "%d h %02d min %02d s", hours, minutes, seconds);
    }

    /** Whole seconds as "0:42" or "1:02:05", for a display that updates every second. */
    public static String clock(long millis) {
        long totalSeconds = Math.max(0, millis) / SECOND;
        long hours = totalSeconds / 3600;
        long minutes = totalSeconds % 3600 / 60;
        long seconds = totalSeconds % 60;
        if (hours == 0) {
            return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
        }
        return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
    }
}
