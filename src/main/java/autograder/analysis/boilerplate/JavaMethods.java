package autograder.analysis.boilerplate;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

final class JavaMethods {
    record Method(String signature, String source, int line,
                  List<String> tokens, List<String> bodyTokens) {
    }

    private JavaMethods() {
    }

    static Map<String, Method> read(String source, int tokenLimit)
            throws IOException {
        JavaSourceTokens.read(source, tokenLimit);
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IOException("method comparison requires a full JDK");
        }
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var file = new SimpleJavaFileObject(URI.create("string:///Answer.java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreErrors) {
                return source;
            }
        };
        try (var manager = compiler.getStandardFileManager(
                diagnostics, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager,
                    diagnostics, List.of("-proc:none", "--source", "17"),
                    null, List.of(file));
            CompilationUnitTree unit = task.parse().iterator().next();
            for (var diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    throw new IOException(
                            "Java syntax could not be parsed at line "
                            + diagnostic.getLineNumber());
                }
            }
            Map<String, Method> methods = new LinkedHashMap<>();
            SourcePositions positions = Trees.instance(task)
                    .getSourcePositions();
            for (Tree declaration : unit.getTypeDecls()) {
                if (declaration instanceof ClassTree type) {
                    collect(type, "", unit, positions, source,
                            tokenLimit, methods);
                }
            }
            return methods;
        }
    }

    private static void collect(ClassTree type, String parent,
            CompilationUnitTree unit, SourcePositions positions, String source,
            int limit, Map<String, Method> methods) throws IOException {
        String owner = parent + type.getSimpleName();
        for (Tree member : type.getMembers()) {
            if (member instanceof ClassTree nested) {
                collect(nested, owner + ".", unit, positions, source,
                        limit, methods);
            } else if (member instanceof MethodTree method) {
                String parameters = String.join(",", method.getParameters()
                        .stream()
                        .map(parameter -> parameter.getType().toString()
                                .replaceAll("\\s+", "")).toList());
                String key = owner + "#" + method.getName()
                        + "(" + parameters + ")";
                int start = (int) positions.getStartPosition(unit, method);
                int end = (int) positions.getEndPosition(unit, method);
                if (start < 0 || end < start || methods.containsKey(key)) {
                    throw new IOException(
                            "ambiguous method or unavailable source range: "
                                    + key);
                }
                String text = source.substring(start, end);
                String body = method.getBody() == null ? ""
                        : source.substring(
                        (int) positions.getStartPosition(
                                unit, method.getBody()),
                        (int) positions.getEndPosition(unit, method.getBody()));
                methods.put(key, new Method(key, text,
                        (int) unit.getLineMap().getLineNumber(start),
                        JavaSourceTokens.read(text, limit),
                        JavaSourceTokens.read(body, limit)));
            }
        }
    }
}
