package autograder.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Replaces the identity lines of a Java source header, or inserts a header when none exists. */
final class HeaderRewriter {
    private static final int HEADER_LINES = 12;
    private static final Pattern NAME = linePattern("Name");
    private static final Pattern EMAIL = linePattern("Email ID");
    private static final Pattern USERNAME = linePattern("Username");

    private HeaderRewriter() {
    }

    static String rewrite(String source, String name, String email, String username) {
        String eol = source.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = splitKeepingTerminators(source);
        boolean[] found = new boolean[2];
        int lastMatch = -1;
        for (int i = 0; i < Math.min(HEADER_LINES, lines.size()); i++) {
            String line = lines.get(i);
            String replaced = replaceValue(line, NAME, name);
            if (replaced == null) {
                replaced = replaceValue(line, EMAIL, email);
                found[1] |= replaced != null;
            } else {
                found[0] = true;
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
        if (!found[0]) {
            missing.add(" * Name: " + name);
        }
        if (!found[1]) {
            missing.add(" * Email ID: " + email);
        }
        if (!missing.isEmpty()) {
            insertMissing(lines, missing, lastMatch, eol);
        }
        return String.join("", lines);
    }

    private static void insertMissing(List<String> lines, List<String> missing, int lastMatch,
            String eol) {
        int at = lastMatch >= 0 ? lastMatch : opensComment(lines) ? 0 : -2;
        if (at == -2) {
            List<String> block = new ArrayList<>();
            block.add("/*" + eol);
            missing.forEach(entry -> block.add(entry + eol));
            block.add(" */" + eol);
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
        return !lines.isEmpty() && lines.get(0).trim().equals("/*");
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
