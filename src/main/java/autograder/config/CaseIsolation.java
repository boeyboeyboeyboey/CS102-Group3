package autograder.config;

import java.util.Locale;

/** How test cases share jvms: part reuses one jvm per question part, case starts one per case. */
public enum CaseIsolation {
    PART,
    CASE;

    static CaseIsolation parse(String value) {
        if (value == null || value.isBlank()) {
            return PART;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("execution.case.isolation must be part or case",
                    exception);
        }
    }
}
