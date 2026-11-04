package autograder.execution;

import autograder.model.QuestionDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class StudentCompiler {
    private static final String ADAPTER_RESOURCE = "/autograder/adapters/";

    public ProcessResult compile(QuestionDefinition question, Path workingRoot,
                                 Path partWorkspace, String javacExecutable,
                                 long timeoutMillis, int maxOutputBytes)
            throws IOException, InterruptedException {
        Path workspace = partWorkspace.toAbsolutePath().normalize();
        Path sourceDirectory = workspace.resolve("src");
        Path classesDirectory = workspace.resolve("classes");
        recreateDirectory(sourceDirectory);
        recreateDirectory(classesDirectory);

        Path studentQuestionDirectory = workingRoot.resolve(question.getFolder());
        List<Path> sources = new ArrayList<>();
        sources.add(copyStudentSource(studentQuestionDirectory, question.getSourceFile(), sourceDirectory));
        for (String companion : question.getCompanionSourceFiles()) {
            sources.add(copyStudentSource(studentQuestionDirectory, companion, sourceDirectory));
        }

        for (Path supportFile : question.getSupportFiles()) {
            if (!Files.isRegularFile(supportFile) || !supportFile.getFileName().toString().endsWith(".class")) {
                throw new IOException("configured support class is missing or not a .class file: " + supportFile);
            }
            Files.copy(supportFile, classesDirectory.resolve(supportFile.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        String adapterName = question.getId() + "Adapter.java";
        String resourceName = ADAPTER_RESOURCE + adapterName;
        Path adapterPath = sourceDirectory.resolve(adapterName);
        try (InputStream adapter = StudentCompiler.class.getResourceAsStream(resourceName)) {
            if (adapter == null) {
                throw new IOException("required adapter resource is missing: " + resourceName);
            }
            Files.copy(adapter, adapterPath, StandardCopyOption.REPLACE_EXISTING);
        }
        sources.add(adapterPath);

        // Dynamic test runner (Feature 4) is compiled into the student classes directory so it can
        // be executed on the same classpath. It uses reflection only, so it has no compile-time
        // dependency on any student class and compiles for every question part.
        String dynamicRunnerName = "DynamicRunner.java";
        Path dynamicRunnerPath = sourceDirectory.resolve(dynamicRunnerName);
        try (InputStream dynamicRunner =
                     StudentCompiler.class.getResourceAsStream(ADAPTER_RESOURCE + dynamicRunnerName)) {
            if (dynamicRunner != null) {
                Files.copy(dynamicRunner, dynamicRunnerPath, StandardCopyOption.REPLACE_EXISTING);
                sources.add(dynamicRunnerPath);
            }
        }

        List<String> command = new ArrayList<>();
        command.add(javacExecutable);
        command.add("-proc:none");
        command.add("-encoding");
        command.add("UTF-8");
        command.add("-d");
        command.add(classesDirectory.toString());
        command.add("-classpath");
        command.add(classesDirectory.toString());
        command.add("-sourcepath");
        command.add(sourceDirectory.toString());
        for (Path source : sources) {
            command.add(source.toString());
        }
        return ProcessRunner.run(command, workspace, timeoutMillis, maxOutputBytes);
    }

    private static Path copyStudentSource(Path studentQuestionDirectory, String fileName,
                                          Path sourceDirectory) throws IOException {
        Path source = studentQuestionDirectory.resolve(fileName).normalize();
        if (!source.getParent().equals(studentQuestionDirectory.normalize())) {
            throw new IOException("student source must be a file name: " + fileName);
        }
        Path target = sourceDirectory.resolve(fileName);
        if (Files.isRegularFile(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static void recreateDirectory(Path directory) throws IOException {
        new DirectoryCleaner().deleteRecursively(directory);
        Files.createDirectories(directory);
    }
}
