package autograder.service.process;

import autograder.model.ProcessResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Compiles a question workspace using the configured javac executable. */
public final class JavaCompilerService {
    private final String compilerCommand;
    private final ExternalProcessRunner processRunner;

    public JavaCompilerService(String compilerCommand, ExternalProcessRunner processRunner) {
        this.compilerCommand = compilerCommand;
        this.processRunner = processRunner;
    }

    public ProcessResult compile(Path sourceDirectory, Path classesDirectory, Duration timeout)
            throws IOException, InterruptedException {
        Files.createDirectories(classesDirectory);
        List<Path> sourceFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.list(sourceDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(sourceFiles::add);
        }
        List<String> command = new ArrayList<>();
        command.add(compilerCommand);
        command.add("-encoding");
        command.add("UTF-8");
        command.add("-classpath");
        command.add(classesDirectory.toAbsolutePath().toString());
        command.add("-d");
        command.add(classesDirectory.toAbsolutePath().toString());
        for (Path source : sourceFiles) {
            command.add(source.toAbsolutePath().toString());
        }
        return processRunner.run(command, sourceDirectory, timeout);
    }
}
