package autograder.autotester;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class ResourceGenerator {
    public void generate(Question question, Path outputDirectory, int examplesPerCase, long seed)
            throws IOException {
        question.validate();
        if (examplesPerCase < 1) throw new IllegalArgumentException("examples per case must be positive");
        String stub = createStub(question);
        String tester = createTester(question, examplesPerCase, seed);
        Files.createDirectories(outputDirectory);
        copyArtifacts(question, outputDirectory);
        Files.writeString(outputDirectory.resolve(question.getClassName() + ".java"),
                stub, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.writeString(outputDirectory.resolve(question.getClassName() + "Tester.java"),
                tester, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void copyArtifacts(Question question, Path outputDirectory) throws IOException {
        for (Question.Artifact artifact : question.getArtifacts()) {
            Path source = Path.of("").toAbsolutePath().resolve(artifact.source()).normalize();
            if (!Files.isRegularFile(source)) {
                throw new IOException("question resource is missing: " + source);
            }
            Files.copy(source, outputDirectory.resolve(source.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String createStub(Question question) {
        StringBuilder source = new StringBuilder();
        if (!question.getDescription().isBlank()) {
            source.append("/** ").append(question.getDescription().replace("*/", "* /").replace("\n", " "))
                    .append(" */\n");
        }
        source.append("import java.util.*;\n\npublic class ").append(question.getClassName())
                .append(" {\n\n    public ");
        if (question.isStaticMethod()) source.append("static ");
        source.append(question.getReturnSourceType()).append(' ')
                .append(question.getMethodName()).append('(');
        appendParameters(source, question);
        source.append(") {\n        // todo: write your solution here\n        return ")
                .append(defaultValue(question.getReturnType())).append(";\n    }\n}\n");
        return source.toString();
    }

    private String createTester(Question question, int examplesPerCase, long seed) {
        List<Example> examples = examples(question, examplesPerCase, seed);
        StringBuilder source = new StringBuilder("import java.util.*;\n\npublic final class ")
                .append(question.getClassName()).append("Tester {\n")
                .append("    public static void main(String[] args) {\n");
        if (!question.isStaticMethod()) {
            source.append("        ").append(question.getClassName()).append(" solution = new ")
                    .append(question.getClassName()).append("();\n");
        }
        source.append("        int passed = 0;\n        int total = ").append(examples.size()).append(";\n");
        for (Example example : examples) appendExample(source, question, example);
        return source.append("        System.out.println(\"passed \" + passed + \" of \" + total + \" examples\");\n")
                .append("    }\n}\n").toString();
    }

    private void appendExample(StringBuilder source, Question question, Example example) {
        String label = example.caseId();
        if (example.expected() instanceof Question.ExpectedException expectedException) {
            appendExceptionExample(source, question, example, expectedException);
            return;
        }
        source.append("        try {\n            Object actual = ")
                .append(question.isStaticMethod() ? question.getClassName() : "solution").append('.')
                .append(question.getMethodName()).append('(')
                .append(generatedArguments(question, example))
                .append(");\n            Object expected = ").append(Literals.source(example.expected(),
                        question.getExpectedSourceType()))
                .append(";\n            if (")
                .append(question.isComparedByText()
                        ? "Objects.equals(expected.toString(), actual.toString())"
                        : "Objects.deepEquals(expected, actual)")
                .append(") {\n")
                .append("                passed++;\n                System.out.println(")
                .append(Literals.quote("PASS " + label)).append(");\n")
                .append("            } else {\n                System.out.println(")
                .append(Literals.quote("FAIL " + label + ": expected "))
                .append(" + expected + ").append(Literals.quote(", got ")).append(" + actual);\n")
                .append("            }\n        } catch (Throwable failure) {\n")
                .append("            System.out.println(")
                .append(Literals.quote("FAIL " + label + ": threw "))
                .append(" + failure.getClass().getSimpleName());\n        }\n");
    }

    private void appendExceptionExample(StringBuilder source, Question question, Example example,
                                        Question.ExpectedException expectedException) {
        source.append("        try {\n            ")
                .append(question.isStaticMethod() ? question.getClassName() : "solution").append('.')
                .append(question.getMethodName())
                .append('(').append(generatedArguments(question, example)).append(");\n            System.out.println(")
                .append(Literals.quote("FAIL " + example.caseId() + ": expected "
                        + expectedException.className()))
                .append(");\n        } catch (Throwable failure) {\n")
                .append("            if (failure.getClass().getSimpleName().equals(")
                .append(Literals.quote(simpleName(expectedException.className()))).append(")) {\n")
                .append("                passed++;\n                System.out.println(")
                .append(Literals.quote("PASS " + example.caseId())).append(");\n")
                .append("            } else {\n                System.out.println(")
                .append(Literals.quote("FAIL " + example.caseId() + ": expected "
                        + expectedException.className() + ", got "))
                .append(" + failure.getClass().getSimpleName());\n            }\n        }\n");
    }

    private String generatedArguments(Question question, Example example) {
        List<String> values = new ArrayList<>();
        for (int index = 0; index < example.arguments().size(); index++) {
            values.add(Literals.source(example.arguments().get(index),
                    question.getParameters().get(index).sourceType()));
        }
        return String.join(", ", values);
    }

    private String simpleName(String className) {
        int separator = className.lastIndexOf('.');
        return separator < 0 ? className : className.substring(separator + 1);
    }

    private List<Example> examples(Question question, int count, long seed) {
        Random random = new Random(seed);
        List<Example> examples = new ArrayList<>();
        for (Question.RandomTestCase testCase : question.getTestCases()) {
            Set<String> seen = new HashSet<>();
            int attempts = 0;
            while (seen.size() < count && attempts < count * 20) {
                attempts++;
                List<Object> arguments = generate(testCase, random);
                question.validateArguments(arguments);
                String key = arguments.stream().map(Literals::source).collect(Collectors.joining("\u0000"));
                if (!seen.add(key)) continue;
                try {
                    Object expected = question.answerKey(copyArguments(arguments));
                    question.validateExpected(expected);
                    examples.add(new Example(testCase.id(), arguments, expected));
                } catch (Exception exception) {
                    throw new IllegalArgumentException("answer key failed for " + testCase.id(), exception);
                }
            }
        }
        return examples;
    }

    static List<Object> generate(Question.RandomTestCase testCase, Random random) {
        List<Object> arguments = new ArrayList<>();
        for (Generator<?> generator : testCase.generators()) {
            Object value = generator.next(random);
            requireSupportedValue(value);
            arguments.add(value);
        }
        return java.util.Collections.unmodifiableList(arguments);
    }

    static List<Object> copyArguments(List<Object> arguments) {
        List<Object> copy = new ArrayList<>();
        for (Object argument : arguments) copy.add(copyValue(argument));
        return java.util.Collections.unmodifiableList(copy);
    }

    private static Object copyValue(Object value) {
        if (value instanceof int[] values) return values.clone();
        if (value instanceof String[] values) return values.clone();
        if (value instanceof Object[] values) {
            Object[] copy = new Object[values.length];
            for (int index = 0; index < values.length; index++) copy[index] = copyValue(values[index]);
            return copy;
        }
        if (value instanceof java.util.Collection<?> values) {
            List<Object> copy = new ArrayList<>();
            for (Object item : values) copy.add(copyValue(item));
            return copy;
        }
        return value;
    }

    static void requireSupportedValue(Object value) {
        if (!Question.isSupportedValue(value)) {
            throw new IllegalArgumentException("value type is not supported by the randomized runner: "
                    + (value == null ? "null" : value.getClass().getTypeName()));
        }
    }

    private void appendParameters(StringBuilder source, Question question) {
        for (int index = 0; index < question.getParameters().size(); index++) {
            if (index > 0) source.append(", ");
            Question.Parameter parameter = question.getParameters().get(index);
            source.append(parameter.sourceType()).append(' ').append(parameter.name());
        }
    }

    private String defaultValue(Class<?> type) {
        if (type == boolean.class) return "false";
        if (type == int.class) return "0";
        if (type == long.class) return "0L";
        if (type == double.class) return "0.0";
        if (type == int[].class) return "new int[0]";
        if (type == String[].class) return "new String[0]";
        if (type == java.util.List.class || type == java.util.ArrayList.class) return "new ArrayList<>()";
        if (type == String.class) return "\"\"";
        return "null";
    }

    private record Example(String caseId, List<Object> arguments, Object expected) { }
}
