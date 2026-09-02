package autograder.service.testing;

import autograder.model.QuestionSpec;
import autograder.model.TestCaseDefinition;
import autograder.service.api.TestSuiteProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adapts the supplied monolithic tester format into one temporary class per try/catch case. */
public final class LegacyJavaTesterProvider implements TestSuiteProvider {
    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "public\\s+class\\s+\\w+\\s+extends\\s+(\\w+)");

    @Override
    public List<TestCaseDefinition> createCases(QuestionSpec question, Path testerSource,
            Path outputDirectory) throws IOException {
        String source = Files.readString(testerSource, StandardCharsets.UTF_8);
        Matcher classMatcher = CLASS_PATTERN.matcher(source);
        if (!classMatcher.find()) {
            throw new IOException("Tester must declare a public class with an extends clause: "
                    + testerSource);
        }
        String parentClass = classMatcher.group(1);
        String imports = source.substring(0, classMatcher.start());
        int gradeToken = source.indexOf("void grade");
        if (gradeToken < 0) {
            throw new IOException("Tester has no grade() method: " + testerSource);
        }
        int gradeOpen = findNextCodeCharacter(source, gradeToken, '{');
        int gradeClose = matchingDelimiter(source, gradeOpen, '{', '}');
        List<String> testBlocks = extractTryCatchBlocks(source, gradeOpen + 1, gradeClose);
        if (testBlocks.isEmpty()) {
            throw new IOException("Tester has no adaptable try/catch cases: " + testerSource);
        }

        Files.createDirectories(outputDirectory);
        List<TestCaseDefinition> cases = new ArrayList<>();
        for (int index = 0; index < testBlocks.size(); index++) {
            int caseNumber = index + 1;
            String className = sanitize(question.getId()) + "AutograderCase" + caseNumber;
            Path generatedFile = outputDirectory.resolve(className + ".java");
            String generatedSource = imports
                    + "public final class " + className + " extends " + parentClass + " {\n"
                    + "    private static double score;\n\n"
                    + "    public static void main(String[] args) {\n"
                    + "        int tcNum = " + caseNumber + ";\n"
                    + indent(testBlocks.get(index), 8) + "\n"
                    + "        System.out.println(\"AUTOGRADER_SCORE=\" + score);\n"
                    + "    }\n"
                    + "}\n";
            Files.writeString(generatedFile, generatedSource, StandardCharsets.UTF_8);
            cases.add(new TestCaseDefinition(caseNumber, className, generatedFile));
        }
        return cases;
    }

    private List<String> extractTryCatchBlocks(String source, int start, int end)
            throws IOException {
        List<String> blocks = new ArrayList<>();
        int cursor = start;
        while (cursor < end) {
            int tryStart = findKeyword(source, "try", cursor, end);
            if (tryStart < 0) {
                break;
            }
            int tryOpen = findNextCodeCharacter(source, tryStart + 3, '{');
            int chainEnd = matchingDelimiter(source, tryOpen, '{', '}') + 1;
            int next = skipWhitespace(source, chainEnd);
            while (startsKeyword(source, next, "catch") || startsKeyword(source, next, "finally")) {
                int blockOpen = findNextCodeCharacter(source, next, '{');
                chainEnd = matchingDelimiter(source, blockOpen, '{', '}') + 1;
                next = skipWhitespace(source, chainEnd);
            }
            blocks.add(source.substring(tryStart, chainEnd).trim());
            cursor = chainEnd;
        }
        return blocks;
    }

    private int findKeyword(String source, String keyword, int start, int end) {
        LexicalState state = new LexicalState();
        for (int index = start; index <= end - keyword.length(); index++) {
            state.advance(source, index);
            if (state.isCode() && startsKeyword(source, index, keyword)) {
                return index;
            }
        }
        return -1;
    }

    private boolean startsKeyword(String source, int index, String keyword) {
        if (index < 0 || index + keyword.length() > source.length()
                || !source.regionMatches(index, keyword, 0, keyword.length())) {
            return false;
        }
        boolean leftBoundary = index == 0
                || !Character.isJavaIdentifierPart(source.charAt(index - 1));
        int right = index + keyword.length();
        boolean rightBoundary = right == source.length()
                || !Character.isJavaIdentifierPart(source.charAt(right));
        return leftBoundary && rightBoundary;
    }

    private int findNextCodeCharacter(String source, int start, char wanted) throws IOException {
        LexicalState state = new LexicalState();
        for (int index = start; index < source.length(); index++) {
            state.advance(source, index);
            if (state.isCode() && source.charAt(index) == wanted) {
                return index;
            }
        }
        throw new IOException("Could not find '" + wanted + "' in tester source");
    }

    private int matchingDelimiter(String source, int openingIndex, char open, char close)
            throws IOException {
        LexicalState state = new LexicalState();
        int depth = 0;
        for (int index = openingIndex; index < source.length(); index++) {
            state.advance(source, index);
            if (!state.isCode()) {
                continue;
            }
            char current = source.charAt(index);
            if (current == open) {
                depth++;
            } else if (current == close && --depth == 0) {
                return index;
            }
        }
        throw new IOException("Unbalanced tester delimiters");
    }

    private int skipWhitespace(String source, int index) {
        while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        return index;
    }

    private String indent(String block, int spaces) {
        String prefix = " ".repeat(spaces);
        return prefix + block.replace("\n", "\n" + prefix);
    }

    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_$]", "");
    }

    /** Tracks comments and literals so braces in text are ignored. */
    private static final class LexicalState {
        private boolean lineComment;
        private boolean blockComment;
        private boolean stringLiteral;
        private boolean characterLiteral;
        private boolean escaped;

        private void advance(String source, int index) {
            char current = source.charAt(index);
            char previous = index > 0 ? source.charAt(index - 1) : '\0';
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                }
                return;
            }
            if (blockComment) {
                if (previous == '*' && current == '/') {
                    blockComment = false;
                }
                return;
            }
            if (stringLiteral || characterLiteral) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (stringLiteral && current == '"') {
                    stringLiteral = false;
                } else if (characterLiteral && current == '\'') {
                    characterLiteral = false;
                }
                return;
            }
            if (current == '/' && next == '/') {
                lineComment = true;
            } else if (current == '/' && next == '*') {
                blockComment = true;
            } else if (current == '"') {
                stringLiteral = true;
            } else if (current == '\'') {
                characterLiteral = true;
            }
        }

        private boolean isCode() {
            return !lineComment && !blockComment && !stringLiteral && !characterLiteral;
        }
    }
}
