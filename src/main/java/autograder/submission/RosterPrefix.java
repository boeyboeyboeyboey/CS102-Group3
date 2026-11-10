package autograder.submission;

import autograder.config.AppConfig;

/** Trims roster ids and usernames and removes the configured roster prefix. */
final class RosterPrefix {
    private RosterPrefix() {
    }

    static String strip(AppConfig config, String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith(config.getRosterPrefix())) {
            return trimmed.substring(config.getRosterPrefix().length());
        }
        return trimmed;
    }
}
