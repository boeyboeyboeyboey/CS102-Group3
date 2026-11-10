package autograder.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Replaces the identity lines of a Java source header, or inserts a header when none exists. */
final class HeaderRewriter {
    private static final int HEADER_LINES = 12;
    private static final String LABEL_NAME = "Name";
    private static final String LABEL_EMAIL = "Email ID";
    private static final String LABEL_USERNAME = "Username";
    private static final String NAME_LINE_PREFIX = " * " + LABEL_NAME + ": ";
    private static final String EMAIL_LINE_PREFIX = " * " + LABEL_EMAIL + ": ";
    private static final String COMMENT_OPEN = "/*";
    private static final String COMMENT_CLOSE = " */";
    private static final String CRLF = "\r\n";
    private static final int NAME_FOUND = 0;
    private static final int EMAIL_FOUND = 1;
    private static final int INSERT_NEW_BLOCK = -2;
    private static final Pattern NAME = linePattern(LABEL_NAME);
    private static final Pattern EMAIL = linePattern(LABEL_EMAIL);
    private static final Pattern USERNAME = linePattern(LABEL_USERNAME);

    private HeaderRewriter() {
    }

    static String rewrite(String source, String name, String email, String username) {
        String eol = source.contains(CRLF) ? CRLF : "\n";
        List<String> lines = splitKeepingTerminators(source);
        boolean[] found = new boolean[EMAIL_FOUND + 1];
        int lastMatch = -1;
        for (int i = 0; i < Math.min(HEADER_LINES, lines.size()); i++) {
            String line = lines.get(i);
            String replaced = replaceValue(line, NAME, name);
            if (replaced == null) {
                replaced = replaceValue(line, EMAIL, email);
                found[EMAIL_FOUND] |= replaced != null;
            } else {
                found[NAME_FOUND] = true;
            }
            if (replaced == null) {
                replaced = replaceValue(line, USERNAME, username);
            }
            if (replaced != null) {
                lines.set(i, replaced);
                lastMatch = i;
            }
        }
        List<String> missing = new ArrayList<>();
        if (!found[NAME_FOUND]) {
            missing.add(NAME_LINE_PREFIX + name);
        }
        if (!found[EMAIL_FOUND]) {
            missing.add(EMAIL_LINE_PREFIX + email);
        }
        if (!missing.isEmpty()) {
            insertMissing(lines, missing, lastMatch, eol);
        }
        return String.join("", lines);
    }

    private static void insertMissing(List<String> lines, List<String> missing, int lastMatch,
            String eol) {
        int at = lastMatch >= 0 ? lastMatch : opensComment(lines) ? 0 : INSERT_NEW_BLOCK;
        if (at == INSERT_NEW_BLOCK) {
            List<String> block = new ArrayList<>();
            block.add(COMMENT_OPEN + eol);
            missing.forEach(entry -> block.add(entry + eol));
            block.add(COMMENT_CLOSE + eol);
            block.add(eol);
            lines.addAll(0, block);
            return;
        }
        String previous = lines.get(at);
        if (!previous.endsWith("\n")) {
            lines.set(at, previous + eol);
        }
        for (int i = 0; i < missing.size(); i++) {
            lines.add(at + 1 + i, missing.get(i) + eol);
        }
    }

    private static boolean opensComment(List<String> lines) {
        return !lines.isEmpty() && lines.get(0).trim().equals(COMMENT_OPEN);
    }

    private static String replaceValue(String line, Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(stripTerminator(line));
        if (!matcher.matches()) {
            return null;
        }
        String prefix = matcher.group(1);
        if (!Character.isWhitespace(prefix.charAt(prefix.length() - 1))) {
            prefix += " ";
        }
        return prefix + value + line.substring(stripTerminator(line).length());
    }

    private static String stripTerminator(String line) {
        int end = line.length();
        while (end > 0 && (line.charAt(end - 1) == '\n' || line.charAt(end - 1) == '\r')) {
            end--;
        }
        return line.substring(0, end);
    }

    private static List<String> splitKeepingTerminators(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(text.substring(start, i + 1));
                start = i + 1;
            }
        }
        if (start < text.length()) {
            lines.add(text.substring(start));
        }
        return lines;
    }

    private static Pattern linePattern(String label) {
        return Pattern.compile("(?i)^(\\s*(?:\\*|//)?\\s*" + label + "\\s*:\\s*).*?\\s*$");
    }
}
