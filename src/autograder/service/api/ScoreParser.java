package autograder.service.api;

import java.util.OptionalDouble;

/** Extracts a score from a tester process's protocol output. */
public interface ScoreParser {
    OptionalDouble parse(String standardOutput);
}
