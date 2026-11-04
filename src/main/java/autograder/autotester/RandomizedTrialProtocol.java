package autograder.autotester;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Bounded serialization boundary between the host grader and student JVM. */
final class RandomizedTrialProtocol {
    private static final long MAX_RESULT_BYTES = 64L * 1024 * 1024;

    private RandomizedTrialProtocol() { }

    static void writeInputs(Path file, List<Object[]> trials) throws IOException {
        List<Object[]> encodedTrials = new ArrayList<>();
        for (Object[] trial : trials) {
            Object[] encoded = new Object[trial.length];
            for (int index = 0; index < trial.length; index++) {
                encoded[index] = encodeValue(trial[index]);
            }
            encodedTrials.add(encoded);
        }
        try (ObjectOutputStream output = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(file)))) {
            output.writeObject(encodedTrials);
        }
    }

    static List<?> readOutcomes(Path file) throws IOException {
        long size = Files.size(file);
        if (size > MAX_RESULT_BYTES) throw new IOException("student result data exceeded the 64 MiB limit");
        try (ObjectInputStream input = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {
            input.setObjectInputFilter(RandomizedTrialProtocol::filter);
            Object result = input.readObject();
            if (result instanceof List<?> outcomes) return outcomes;
            throw new IOException("student runner wrote an invalid result file");
        } catch (ClassNotFoundException exception) {
            throw new IOException("student runner returned an unsupported value", exception);
        }
    }

    static Object[] requireOutcome(Object value) throws IOException {
        if (!(value instanceof Object[] outcome) || outcome.length != 2
                || !(outcome[0] instanceof Boolean)) {
            throw new IOException("student runner returned an invalid trial result");
        }
        if (Boolean.TRUE.equals(outcome[0])) ResourceGenerator.requireSupportedValue(outcome[1]);
        else if (!(outcome[1] instanceof String)) {
            throw new IOException("student runner returned an invalid error");
        }
        return outcome;
    }

    private static Object encodeValue(Object value) {
        if (value instanceof ShapeInput shape) return shape.runnerValue();
        if (value instanceof Object[] values) {
            Object[] copy = new Object[values.length];
            for (int index = 0; index < values.length; index++) copy[index] = encodeValue(values[index]);
            return copy;
        }
        if (value instanceof java.util.Collection<?> values) {
            List<Object> copy = new ArrayList<>();
            for (Object item : values) copy.add(encodeValue(item));
            return copy;
        }
        if (value instanceof int[] values) return values.clone();
        if (value instanceof String[] values) return values.clone();
        return value;
    }

    private static ObjectInputFilter.Status filter(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 20 || info.references() > 100_000 || info.streamBytes() > MAX_RESULT_BYTES
                || info.arrayLength() > 100_000) return ObjectInputFilter.Status.REJECTED;
        Class<?> type = info.serialClass();
        if (type == null) return ObjectInputFilter.Status.UNDECIDED;
        if (type == String.class || type == Integer.class || type == Boolean.class || type == Long.class
                || type == Double.class || type == Number.class || type == ArrayList.class
                || type == Object[].class || type == int[].class || type == String[].class) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    }
}
