package autograder.plagiarism;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Splits Java source into tokens; identifiers become ID and literals NUM, STR or CHR so renamed code still matches. */
public final class JavaTokenizer {
    public static final String IDENTIFIER = "ID";
    public static final String NUMBER = "NUM";
    public static final String STRING = "STR";
    public static final String CHARACTER = "CHR";

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "var", "record",
            "yield", "true", "false", "null");
    private static final List<String> OPERATORS = List.of(
            ">>>=", "<<=", ">>=", ">>>", "...", "->", "::", "++", "--", "&&", "||", "==", "!=",
            "<=", ">=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<", ">>");
    private static final String TEXT_BLOCK_QUOTES = "\"\"\"";

    public List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        int line = 1;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (current == '\n') {
                line++;
                index++;
            } else if (Character.isWhitespace(current)) {
                index++;
            } else if (source.startsWith("//", index)) {
                index = endOfLine(source, index);
            } else if (source.startsWith("/*", index)) {
                int end = endOf(source, index + 2, "*/");
                line += countLines(source, index, end);
                index = end;
            } else if (source.startsWith(TEXT_BLOCK_QUOTES, index)) {
                int end = endOf(source, index + TEXT_BLOCK_QUOTES.length(), TEXT_BLOCK_QUOTES);
                tokens.add(new Token(STRING, line));
                line += countLines(source, index, end);
                index = end;
            } else if (current == '"' || current == '\'') {
                int end = endOfQuoted(source, index, current);
                tokens.add(new Token(current == '"' ? STRING : CHARACTER, line));
                index = end;
            } else if (Character.isDigit(current) || isDecimalPoint(source, index)) {
                tokens.add(new Token(NUMBER, line));
                index = endOfNumber(source, index);
            } else if (Character.isJavaIdentifierStart(current)) {
                int end = endOfIdentifier(source, index);
                String word = source.substring(index, end);
                tokens.add(new Token(KEYWORDS.contains(word) ? word : IDENTIFIER, line));
                index = end;
            } else {
                String operator = matchOperator(source, index);
                tokens.add(new Token(operator, line));
                index += operator.length();
            }
        }
        return tokens;
    }

    private int endOfLine(String source, int index) {
        int end = source.indexOf('\n', index);
        return end < 0 ? source.length() : end;
    }

    private int endOf(String source, int index, String terminator) {
        int end = source.indexOf(terminator, index);
        return end < 0 ? source.length() : end + terminator.length();
    }

    private int endOfIdentifier(String source, int index) {
        int end = index + 1;
        while (end < source.length() && Character.isJavaIdentifierPart(source.charAt(end))) {
            end++;
        }
        return end;
    }

    private int endOfQuoted(String source, int index, char quote) {
        int end = index + 1;
        while (end < source.length()) {
            char current = source.charAt(end);
            if (current == '\\') {
                end += 2;
            } else if (current == quote) {
                return end + 1;
            } else if (current == '\n') {
                // unterminated literal stops at the line break so line numbers stay correct
                return end;
            } else {
                end++;
            }
        }
        return source.length();
    }

    private boolean isDecimalPoint(String source, int index) {
        return source.charAt(index) == '.' && index + 1 < source.length()
                && Character.isDigit(source.charAt(index + 1));
    }

    private int endOfNumber(String source, int index) {
        int end = index;
        while (end < source.length()) {
            char current = source.charAt(end);
            boolean exponentSign = (current == '+' || current == '-')
                    && (source.charAt(end - 1) == 'e' || source.charAt(end - 1) == 'E')
                    && !source.startsWith("0x", index) && !source.startsWith("0X", index);
            if (Character.isLetterOrDigit(current) || current == '_' || current == '.' || exponentSign) {
                end++;
            } else {
                break;
            }
        }
        return end;
    }

    private String matchOperator(String source, int index) {
        for (String operator : OPERATORS) {
            if (source.startsWith(operator, index)) {
                return operator;
            }
        }
        return source.substring(index, index + 1);
    }

    private int countLines(String source, int start, int end) {
        int lines = 0;
        for (int index = start; index < end; index++) {
            if (source.charAt(index) == '\n') {
                lines++;
            }
        }
        return lines;
    }
}
