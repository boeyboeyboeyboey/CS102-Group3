package autograder.analysis.boilerplate;

import autograder.config.BoilerplateConfig;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

final class ComparisonSourceReader {
    private ComparisonSourceReader() {
    }

    static String read(Path directory, Path relative, BoilerplateConfig config)
            throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || !file.toRealPath().startsWith(root.toRealPath())) {
            throw new IOException("missing or unsafe comparison file: "
                    + relative);
        }
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(config.maxSourceBytes() + 1);
            if (bytes.length > config.maxSourceBytes()) {
                throw new IOException("source exceeds configured byte limit "
                        + config.maxSourceBytes());
            }
            String source = StandardCharsets.UTF_8.newDecoder()
                    .decode(ByteBuffer.wrap(bytes)).toString();
            JavaSourceTokens.read(source, config.maxTokens());
            return source;
        }
    }
}
