package autograder.analysis.boilerplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class JavaSourceTokens {
    // multi-character operators, checked in this order after the first character is consumed
    private static final List<String> OPERATORS = List.of(">>>=", "<<=", ">>=", ">>>", "...", "::", "->",
            "++", "--", "&&", "||", "==", "!=", "<=", ">=", "+=", "-=", "*=",
            "/=", "%=", "&=", "|=", "^=", "<<", ">>");

    private JavaSourceTokens() {
    }

    static List<String> read(String source, int limit) throws IOException {
        if (source.contains("\\u")) {
            throw new IOException("Unicode escape syntax requires manual comparison");
        }
        List<String> tokens = new ArrayList<>();
        int index = 0;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (Character.isWhitespace(current) || current == '\ufeff') {
                index++;
                continue;
            }
            if (source.startsWith("//", index)) {
                while (index < source.length() && source.charAt(index) != '\n'
                        && source.charAt(index) != '\r') {
                    index++;
                }
                continue;
            }
            if (source.startsWith("/*", index)) {
                int end = source.indexOf("*/", index + 2);
                if (end < 0) {
                    throw new IOException("unterminated block comment");
                }
                index = end + 2;
                continue;
            }
            int start = index;
            if (current == '"' || current == '\'') {
                String delimiter = source.startsWith("\"\"\"", index) ? "\"\"\"" : String.valueOf(current);
                index += delimiter.length();
                boolean closed = false;
                while (index < source.length()) {
                    if (source.charAt(index) == '\\') {
                        index += 2;
                    } else if (source.startsWith(delimiter, index)) {
                        index += delimiter.length();
                        closed = true;
                        break;
                    } else {
                        index++;
                    }
                }
                if (!closed) {
                    throw new IOException("unterminated string, character literal or text block");
                }
            } else if (Character.isJavaIdentifierPart(current)) {
                index++;
                while (index < source.length() && Character.isJavaIdentifierPart(source.charAt(index))) {
                    index++;
                }
            } else {
                index++;
                for (String operator : OPERATORS) {
                    if (source.startsWith(operator, start)) {
                        index = start + operator.length();
                        break;
                    }
                }
            }
            tokens.add(source.substring(start, index));
            if (tokens.size() > limit) {
                throw new IOException("source exceeds configured token limit " + limit);
            }
        }
        return List.copyOf(tokens);
    }

    static int editDistance(List<String> first, List<String> second) {
        int[] previous = new int[second.size() + 1];
        for (int j = 0; j < previous.length; j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= first.size(); i++) {
            int[] current = new int[previous.length];
            current[0] = i;
            for (int j = 1; j < current.length; j++) {
                int substitution = previous[j - 1] + (first.get(i - 1).equals(second.get(j - 1)) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            previous = current;
        }
        return previous[second.size()];
    }
}
