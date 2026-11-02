package autograder.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Finds the configuration file to start with, without touching the disk beyond existence checks. */
public final class ConfigLocator {
    static final String CONFIG_NAME = "config.properties";

    private ConfigLocator() {
    }

    public static Optional<Path> locate(String[] args, Path remembered,
                                        Path workingDir, Path codeLocation) {
        List<Path> candidates = new ArrayList<>();
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            candidates.add(Path.of(args[0]));
        }
        candidates.add(remembered);
        if (workingDir != null) {
            candidates.add(workingDir.resolve(CONFIG_NAME));
        }
        addNearCode(candidates, codeLocation);
        return candidates.stream()
                .filter(path -> path != null && Files.isRegularFile(path))
                .map(path -> path.toAbsolutePath().normalize())
                .findFirst();
    }

    private static void addNearCode(List<Path> candidates, Path codeLocation) {
        if (codeLocation == null) {
            return;
        }
        Path parent = codeLocation.toAbsolutePath().getParent();
        for (int level = 0; level < 2 && parent != null; level++) {
            candidates.add(parent.resolve(CONFIG_NAME));
            parent = parent.getParent();
        }
    }
}
