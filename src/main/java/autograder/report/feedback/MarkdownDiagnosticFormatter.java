package autograder.report.feedback;

import java.util.ArrayList;
import java.util.List;

/** Formats recorded stdout and stderr as trimmed, fenced Markdown code blocks. */
final class MarkdownDiagnosticFormatter {
    private static final int MIN_FENCE_LENGTH = 3;
    private static final String COMPILE_WORKSPACE_PATH = "\\S*[/\\\\]src[/\\\\](?=[^/\\\\\\s]+\\.java)";
    private static final String JVM_OPTIONS_NOTICE = "Picked up (JAVA_TOOL_OPTIONS|_JAVA_OPTIONS|JDK_JAVA_OPTIONS):.*";

    private MarkdownDiagnosticFormatter() { }

    static void appendDiagnostic(StringBuilder markdown, String text, int maxDiagnosticLines) {
        String[] lines = withoutJvmNotices(text).strip().split("\\R", -1);
        int shown = Math.min(lines.length, maxDiagnosticLines);
        List<String> kept = new ArrayList<>();
        for (int index = 0; index < shown; index++) {
            kept.add(lines[index]);
        }
        String body = String.join("\n", kept);
        String fence = "`".repeat(Math.max(MIN_FENCE_LENGTH, longestBacktickRun(body) + 1));
        markdown.append(fence).append("text\n").append(body).append('\n').append(fence).append("\n\n");
        if (lines.length > shown) {
            markdown.append("(").append(lines.length - shown)
                    .append(" more lines omitted; see cases.csv for the full recorded output)\n\n");
        }
    }

    static String withoutJvmNotices(String text) {
        List<String> kept = new ArrayList<>();
        for (String line : text.split("\\R", -1)) {
            if (!line.matches(JVM_OPTIONS_NOTICE)) {
                kept.add(line);
            }
        }
        return String.join("\n", kept);
    }

    static String redactCompileWorkspacePaths(String text) {
        return text.replaceAll(COMPILE_WORKSPACE_PATH, "");
    }

    private static int longestBacktickRun(String text) {
        int longest = 0;
        int current = 0;
        for (char character : text.toCharArray()) {
            current = character == '`' ? current + 1 : 0;
            longest = Math.max(longest, current);
        }
        return longest;
    }
}
