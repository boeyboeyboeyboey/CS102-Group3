package autograder.service.testing;

import autograder.service.api.ScoreParser;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses only the grader-owned score sentinel, ignoring arbitrary student output. */
public final class SentinelScoreParser implements ScoreParser {
    private static final Pattern SCORE_PATTERN = Pattern.compile(
            "(?m)^AUTOGRADER_SCORE=([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))\\s*$");

    @Override
    public OptionalDouble parse(String standardOutput) {
        Matcher matcher = SCORE_PATTERN.matcher(standardOutput);
        OptionalDouble score = OptionalDouble.empty();
        while (matcher.find()) {
            score = OptionalDouble.of(Double.parseDouble(matcher.group(1)));
        }
        return score;
    }
}
