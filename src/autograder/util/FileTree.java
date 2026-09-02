package autograder.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Narrow file-tree operations used for disposable batch workspaces. */
public final class FileTree {
    private FileTree() {
    }

    public static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            Comparator<Path> deepestFirst = Comparator
                    .comparingInt(Path::getNameCount)
                    .reversed()
                    .thenComparing(Comparator.reverseOrder());
            for (Path item : (Iterable<Path>) paths.sorted(deepestFirst)::iterator) {
                Files.deleteIfExists(item);
            }
        }
    }
}
