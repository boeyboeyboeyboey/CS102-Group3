package autograder.autotester;

import java.io.Serializable;
import java.nio.file.Path;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** instructor question definition and answer key contract */
public abstract class Question {
    private static final int MAX_PARAMETERS = 100;
    private static final int MAX_TEST_CASES = 1_000;
    private String className;
    private String methodName;
    private Class<?> returnType;
    private String returnSourceType;
    private String expectedSourceType;
    private String description = "";
    private final List<Parameter> parameters = new ArrayList<>();
    private final List<RandomTestCase> testCases = new ArrayList<>();
    private final List<Artifact> artifacts = new ArrayList<>();
    private final List<String> companionSourceNames = new ArrayList<>();
    private boolean compareByText;
    private boolean staticMethod;

    protected final void signature(String className, String methodName, Class<?> returnType) {
        signature(className, methodName, returnType,
                returnType == null ? null : returnType.getCanonicalName());
    }

    protected final void signature(String className, String methodName, Class<?> returnType,
                                   String returnSourceType) {
        if (this.className != null) {
            throw new IllegalStateException("question signature is already defined");
        }
        requireIdentifier(className, "class name");
        requireIdentifier(methodName, "method name");
        if (returnType == null || returnType == void.class || !isSupportedType(returnType)) {
            throw new IllegalArgumentException("unsupported or missing return type");
        }
        this.className = className;
        this.methodName = methodName;
        this.returnType = returnType;
        this.returnSourceType = requireSourceType(returnSourceType, "return type");
        this.expectedSourceType = this.returnSourceType;
    }

    protected final void expectedSourceType(String sourceType) {
        expectedSourceType = requireSourceType(sourceType, "expected result type");
    }

    protected final void describe(String text) {
        description = text == null ? "" : text.strip();
    }

    protected final void param(String name, Class<?> type) {
        param(name, type, type == null ? null : type.getCanonicalName());
    }

    protected final void param(String name, Class<?> type, String sourceType) {
        if (parameters.size() >= MAX_PARAMETERS) {
            throw new IllegalArgumentException("question exceeds the " + MAX_PARAMETERS + " parameter limit");
        }
        requireIdentifier(name, "parameter name");
        if (type == null || !isSupportedType(type)) {
            throw new IllegalArgumentException("unsupported parameter type for " + name);
        }
        for (Parameter parameter : parameters) {
            if (parameter.name().equals(name)) {
                throw new IllegalArgumentException("duplicate parameter name: " + name);
            }
        }
        parameters.add(new Parameter(name, type, requireSourceType(sourceType, "parameter type")));
    }

    protected final void includeFixture(String sourcePath) {
        includeArtifact(sourcePath, ArtifactKind.FIXTURE);
    }

    protected final void includeSupportClass(String sourcePath) {
        includeArtifact(sourcePath, ArtifactKind.SUPPORT_CLASS);
    }

    protected final void includeSupportSource(String sourcePath) {
        includeArtifact(sourcePath, ArtifactKind.SUPPORT_SOURCE);
    }

    protected final void includeStarterSource(String sourcePath) {
        Artifact artifact = includeArtifact(sourcePath, ArtifactKind.STARTER_SOURCE);
        companionSourceNames.add(artifact.source().getFileName().toString());
    }

    protected final void includeCompanionSource(String sourceName) {
        if (sourceName == null || !sourceName.matches("[A-Za-z_$][A-Za-z0-9_$]*\\.java")) {
            throw new IllegalArgumentException("invalid companion source name: " + sourceName);
        }
        if (!companionSourceNames.contains(sourceName)) {
            companionSourceNames.add(sourceName);
        }
    }

    protected final void compareByText() {
        compareByText = true;
    }

    protected final void staticMethod() {
        staticMethod = true;
    }

    private Artifact includeArtifact(String sourcePath, ArtifactKind kind) {
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException("artifact source path is required");
        }
        Path source = Path.of(sourcePath).normalize();
        if (source.isAbsolute() || source.startsWith("..")) {
            throw new IllegalArgumentException("artifact path must stay within the project: " + sourcePath);
        }
        Artifact artifact = new Artifact(source, kind);
        artifacts.add(artifact);
        return artifact;
    }

    protected final void addTestCase(String id, Generator<?>... generators) {
        if (testCases.size() >= MAX_TEST_CASES) {
            throw new IllegalArgumentException("question exceeds the " + MAX_TEST_CASES + " test case limit");
        }
        if (id == null || id.isBlank() || generators == null) {
            throw new IllegalArgumentException("test case id and generators are required");
        }
        if (generators.length != parameters.size()) {
            throw new IllegalArgumentException("test case " + id + " has " + generators.length
                    + " generators for " + parameters.size() + " parameters");
        }
        List<Generator<?>> copy = List.of(generators.clone());
        for (RandomTestCase existing : testCases) {
            if (existing.id().equals(id)) {
                throw new IllegalArgumentException("duplicate test case id: " + id);
            }
        }
        testCases.add(new RandomTestCase(id, copy));
    }

    public abstract Object answerKey(List<Object> arguments) throws Exception;

    public final String getClassName() { return className; }
    public final String getMethodName() { return methodName; }
    public final Class<?> getReturnType() { return returnType; }
    public final String getDescription() { return description; }
    public final String getReturnSourceType() { return returnSourceType; }
    public final String getExpectedSourceType() { return expectedSourceType; }
    public final List<Parameter> getParameters() { return List.copyOf(parameters); }
    public final List<RandomTestCase> getTestCases() { return List.copyOf(testCases); }
    public final List<Artifact> getArtifacts() { return List.copyOf(artifacts); }
    public final List<String> getCompanionSourceNames() { return List.copyOf(companionSourceNames); }
    public final boolean isComparedByText() { return compareByText; }
    public final boolean isStaticMethod() { return staticMethod; }

    public final void validate() {
        if (className == null || methodName == null || returnType == null) {
            throw new IllegalArgumentException("question signature is incomplete");
        }
        if (testCases.isEmpty()) {
            throw new IllegalArgumentException("question must define at least one test case");
        }
        if (Modifier.isAbstract(getClass().getModifiers())) {
            throw new IllegalArgumentException("question implementation must be concrete");
        }
    }

    final void validateArguments(List<?> arguments) {
        if (arguments.size() != parameters.size()) {
            throw new IllegalArgumentException("generated argument count does not match the question signature");
        }
        for (int index = 0; index < arguments.size(); index++) {
            Object value = arguments.get(index);
            Class<?> parameterType = parameters.get(index).type();
            if (value == null ? parameterType.isPrimitive() : !boxed(parameterType).isInstance(value)) {
                throw new IllegalArgumentException("generated value does not match parameter "
                        + parameters.get(index).name() + " (" + parameterType.getTypeName() + ")");
            }
        }
    }

    final void validateExpected(Object value) {
        if (value instanceof ExpectedException expectedException) {
            if (expectedException.className().isBlank()) {
                throw new IllegalArgumentException("expected exception class name is required");
            }
            return;
        }
        if (!isSupportedValue(value)) {
            throw new IllegalArgumentException("answer key returned an unsupported value type");
        }
        if (value == null ? returnType.isPrimitive() : !boxed(returnType).isInstance(value)) {
            throw new IllegalArgumentException("answer key result does not match declared return type "
                    + returnType.getTypeName());
        }
    }

    static boolean isSupportedType(Class<?> type) {
        return type == int.class || type == Integer.class || type == boolean.class
                || type == Boolean.class || type == long.class || type == Long.class
                || type == double.class || type == Double.class || type == String.class
                || type == int[].class || type == String[].class
                || type == ArrayList.class || type == List.class;
    }

    private Class<?> boxed(Class<?> type) {
        if (type == int.class) return Integer.class;
        if (type == boolean.class) return Boolean.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        return type;
    }

    static boolean isSupportedValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof ShapeInput) {
            return true;
        }
        Class<?> type = value.getClass();
        if (value instanceof ExpectedException) {
            return true;
        }
        if (value instanceof Collection<?> values) {
            return values.stream().allMatch(Question::isSupportedValue);
        }
        if (type.isArray()) {
            Class<?> component = type.getComponentType();
            if (component == Object.class) {
                for (Object item : (Object[]) value) {
                    if (!isSupportedValue(item)) return false;
                }
                return true;
            }
            return (component.isPrimitive() && component != char.class && component != float.class
                    && component != short.class && component != byte.class)
                    || component == String.class;
        }
        return value instanceof Serializable && isSupportedType(type);
    }

    private String requireSourceType(String sourceType, String label) {
        if (sourceType == null || !sourceType.matches("[A-Za-z_$][A-Za-z0-9_$.]*(?:<[^;{}]+>)?(?:\\[\\])?")) {
            throw new IllegalArgumentException("invalid Java " + label + ": " + sourceType);
        }
        return sourceType;
    }

    static void requireIdentifier(String value, String label) {
        if (value == null || !value.matches("[A-Za-z_$][A-Za-z0-9_$]*")
                || JAVA_KEYWORDS.contains(value)) {
            throw new IllegalArgumentException("invalid Java " + label + ": " + value);
        }
    }

    private static final java.util.Set<String> JAVA_KEYWORDS = java.util.Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "var", "yield", "record", "sealed",
            "permits", "non-sealed");

    public record Parameter(String name, Class<?> type, String sourceType) { }

    public record ExpectedException(String className) implements Serializable {
        public ExpectedException {
            if (className == null || className.isBlank()) {
                throw new IllegalArgumentException("expected exception class name is required");
            }
        }
    }

    public record Artifact(Path source, ArtifactKind kind) { }

    public enum ArtifactKind { FIXTURE, SUPPORT_CLASS, SUPPORT_SOURCE, STARTER_SOURCE }

    public record RandomTestCase(String id, List<Generator<?>> generators) {
        public RandomTestCase {
            generators = List.copyOf(generators);
        }
    }
}
