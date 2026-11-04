import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

// marks a case as started before any student class loads, then runs the case runner in this jvm
// so the grader can start the case time limit here instead of at jvm start-up
public final class GraderCaseLauncher {
    private static final int USAGE_EXIT_STATUS = 2;

    public static void main(String[] args) throws Throwable {
        if (args.length < 2) {
            System.err.println("usage: GraderCaseLauncher <started marker> <runner class> [arguments]");
            System.exit(USAGE_EXIT_STATUS);
        }
        Files.writeString(Path.of(args[0]), "");
        Class<?> runner = Class.forName(args[1]);
        Method main = runner.getMethod("main", String[].class);
        try {
            main.invoke(null, (Object) Arrays.copyOfRange(args, 2, args.length));
        } catch (InvocationTargetException exception) {
            // rethrown so the jvm reports it and exits with status 1, as a direct launch would
            throw exception.getCause();
        }
    }
}
