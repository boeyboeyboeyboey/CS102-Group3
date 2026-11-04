/*
 * DynamicRunner -- property-based ("metamorphic") dynamic test runner for the CS102 auto-grader.
 *
 * It is compiled into the student's classes directory exactly like the fixed Instructor adapters
 * (see StudentCompiler), so it runs on the same classpath the student code runs on. It is a
 * default-package class that invokes the student's static methods purely through reflection, which
 * means it has NO compile-time dependency on any student class and therefore compiles for every
 * question part.
 *
 * For each question it generates many random inputs (seeded for reproducibility) and asserts an
 * INVARIANT that the question's own specification guarantees -- it never computes a "correct"
 * expected answer, so the team does not have to act as an oracle / answer key. A student solution
 * that hard-codes the fixed instructor cases returns the same wrong output for novel random inputs
 * and therefore fails the invariant.
 *
 * Result token is written to the result file and must be one of PASSED / FAILED / ERROR
 * (the pipeline caps the result file at 16 bytes).
 */

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class DynamicRunner {

    private static final int TRIALS = 25;
    // Surnames the instructor's fixed adapter already exercises; we deliberately query OTHER
    // surnames so a hard-coded solution is forced to produce a wrong answer.
    private static final Set<String> FIXED_Q2A_SURNAMES =
            new HashSet<>(Arrays.asList("TAY", "LEE", "WONG", "ONG"));
    // Courses the instructor's fixed adapter already exercises.
    private static final Set<String> FIXED_Q2B_COURSES =
            new HashSet<>(Arrays.asList("SMU101", "CS101", "CS102", "CS103"));

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: DynamicRunner <questionId> <seed> <resultFilePath>");
            System.exit(1);
        }
        String questionId = args[0];
        long seed;
        try {
            seed = Long.parseLong(args[1]);
        } catch (NumberFormatException exception) {
            seed = args[1].hashCode();
        }
        String resultFilePath = args[2];

        String status = "ERROR";
        try {
            Random random = new Random(seed);
            boolean allPassed = true;
            String failure = null;
            for (int trial = 0; trial < TRIALS; trial++) {
                String result = runTrial(questionId, random);
                if (result != null) {
                    allPassed = false;
                    failure = result;
                    break;
                }
            }
            status = allPassed ? "PASSED" : "FAILED";
            if (!allPassed) {
                System.err.println("DynamicRunner failure for " + questionId + ": " + failure);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
            status = "ERROR";
        }
        writeResult(resultFilePath, status);
    }

    private static String runTrial(String questionId, Random random) throws Exception {
        switch (questionId) {
            case "Q1a": return trialQ1a(random);
            case "Q1b": return trialQ1b(random);
            case "Q2a": return trialQ2a(random);
            case "Q2b": return trialQ2b(random);
            case "Q3": return trialQ3(random);
            default: return "unsupported question: " + questionId;
        }
    }

    // ---- Q1a: getIsogramWords(ArrayList<String>) -> ArrayList<String> ----
    // Invariant: the result contains exactly the input words that are isograms (case-insensitive,
    // letters only). This is the question's own definition -- no expected answer is ever computed.
    private static String trialQ1a(Random random) {
        ArrayList<String> inputs = randomWordList(random);
        Object resultObj;
        try {
            resultObj = invokeStatic("Q1a", "getIsogramWords", new Class[] {ArrayList.class}, inputs);
        } catch (Throwable t) {
            return "invocation threw: " + t;
        }
        if (!(resultObj instanceof ArrayList)) {
            return "result is not a list";
        }
        @SuppressWarnings("unchecked")
        ArrayList<String> result = (ArrayList<String>) resultObj;

        Map<String, Integer> inputFreq = frequency(inputs);
        Map<String, Integer> resultFreq = frequency(result);
        // Every word in the result must come from the input.
        for (String word : resultFreq.keySet()) {
            if (!inputFreq.containsKey(word)) {
                return "result contains a word not in the input: " + word;
            }
        }
        // Each input word must appear in the result exactly when it is an isogram.
        for (Map.Entry<String, Integer> entry : inputFreq.entrySet()) {
            String word = entry.getKey();
            int expectedCount = isIsogram(word) ? entry.getValue() : 0;
            int actualCount = resultFreq.getOrDefault(word, 0);
            if (expectedCount != actualCount) {
                return "wrong isogram membership/count for: " + word;
            }
        }
        return null;
    }

    // ---- Q1b: getSumOfEvenIntegers(ArrayList<Object>) -> int ----
    // Invariant: the result equals the sum of the even-valued Integer elements (Booleans, Doubles
    // and Strings are ignored), exactly as the question defines it.
    private static String trialQ1b(Random random) {
        ArrayList<Object> inputs = randomMixedList(random);
        Object resultObj;
        try {
            resultObj = invokeStatic("Q1b", "getSumOfEvenIntegers", new Class[] {ArrayList.class}, inputs);
        } catch (Throwable t) {
            return "invocation threw: " + t;
        }
        if (!(resultObj instanceof Integer)) {
            return "result is not an Integer";
        }
        int actual = (Integer) resultObj;
        int expected = 0;
        for (Object value : inputs) {
            if (value instanceof Integer number && number % 2 == 0) {
                expected += number;
            }
        }
        if (actual != expected) {
            return "sum mismatch: expected " + expected + " but got " + actual;
        }
        return null;
    }

    // ---- Q2a: getAverageAge(String file, String surname) -> double ----
    // Invariant: the returned average must equal the mean age of persons whose SURNAME (the last
    // whitespace token of the name, matched case-insensitively) equals the queried surname. We
    // query only surnames the fixed adapter does NOT cover, so a hard-coded solution fails.
    private static String trialQ2a(Random random) throws IOException {
        List<String> lines = readLines("personstester.txt");
        List<String> candidates = surnameCandidates(lines);
        if (candidates.isEmpty()) {
            return "no non-fixed surnames available to probe";
        }
        String surname = candidates.get(random.nextInt(candidates.size()));
        double expected = averageAgeForSurname(lines, surname);

        Object resultObj;
        try {
            resultObj = invokeStatic("Q2a", "getAverageAge",
                    new Class[] {String.class, String.class}, "personstester.txt", surname);
        } catch (Throwable t) {
            return "invocation threw: " + t;
        }
        if (!(resultObj instanceof Double)) {
            return "result is not a Double";
        }
        double actual = (Double) resultObj;
        if (Math.abs(actual - expected) > 1e-9) {
            return "average mismatch for " + surname + ": expected " + expected + " but got " + actual;
        }
        return null;
    }

    // ---- Q2b: getTopStudent(String file, String course) -> String ----
    // Invariant: the returned "Name-GPA" must name a real student enrolled in the course with that
    // exact GPA, and no other enrolled student may have a higher GPA. We probe only courses the
    // fixed adapter does NOT cover, so a hard-coded solution fails.
    private static String trialQ2b(Random random) throws IOException {
        List<String> lines = readLines("studentstester.txt");
        Set<String> candidates = new HashSet<>();
        for (String line : lines) {
            for (String course : coursesOf(line)) {
                if (!FIXED_Q2B_COURSES.contains(course.toUpperCase())) {
                    candidates.add(course);
                }
            }
        }
        if (candidates.isEmpty()) {
            return "no non-fixed courses available to probe";
        }
        List<String> courseList = new ArrayList<>(candidates);
        String course = courseList.get(random.nextInt(courseList.size()));
        TopStudent expected = topStudent(lines, course);
        if (expected == null) {
            return "internal: could not determine expected top student for " + course;
        }

        Object resultObj;
        try {
            resultObj = invokeStatic("Q2b", "getTopStudent",
                    new Class[] {String.class, String.class}, "studentstester.txt", course);
        } catch (Throwable t) {
            return "invocation threw: " + t;
        }
        if (!(resultObj instanceof String)) {
            return "result is not a String";
        }
        String actual = (String) resultObj;
        // Parse "Name-GPA"; names contain no dash, so split on the last dash.
        int dash = actual.lastIndexOf('-');
        if (dash < 0) {
            return "result not in Name-GPA format: " + actual;
        }
        String name = actual.substring(0, dash).trim();
        double gpa;
        try {
            gpa = Double.parseDouble(actual.substring(dash + 1).trim());
        } catch (NumberFormatException e) {
            return "result GPA not numeric: " + actual;
        }
        if (!name.equals(expected.name)) {
            return "wrong top student for " + course + ": expected " + expected.name + " but got " + name;
        }
        if (Math.abs(gpa - expected.gpa) > 1e-9) {
            return "wrong GPA for top student " + name + ": expected " + expected.gpa + " but got " + gpa;
        }
        return null;
    }

    // ---- Q3: sortShapes(List<Shape>) -> List<Shape> ----
    // Invariant: retain eligible inputs and sort by descending area, then perimeter.
    private static String trialQ3(Random random) throws Exception {
        List<Object> shapes = randomShapes(random);
        Object resultObj;
        try {
            resultObj = invokeStatic("Q3", "sortShapes", new Class[] {List.class}, shapes);
        } catch (Throwable t) {
            return "invocation threw: " + t;
        }
        if (!(resultObj instanceof List)) {
            return "result is not a List";
        }
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) resultObj;
        return verifySortedEligibleShapes(shapes, result);
    }

    private static List<Object> randomShapes(Random random) throws Exception {
        int n = 3 + random.nextInt(4); // 3..6 shapes
        ArrayList<Object> shapes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String id = "S" + i;
            if (i == 0) {
                double radius = 18 + random.nextDouble() * 3;
                shapes.add(newShape("Circle", id, radius));
            } else if (i == 1) {
                shapes.add(newShape("Rectangle", id, 6, 4));
            } else if (i == 2) {
                shapes.add(newShape("Rectangle", id, 8, 3));
            } else if (random.nextBoolean()) {
                double radius = 1 + random.nextDouble() * 20;
                shapes.add(newShape("Circle", id, radius));
            } else {
                double width = 1 + random.nextDouble() * 20;
                double height = 1 + random.nextDouble() * 20;
                shapes.add(newShape("Rectangle", id, width, height));
            }
        }
        return shapes;
    }

    private static String verifySortedEligibleShapes(List<Object> shapes, List<Object> result)
            throws Exception {
        List<String> expectedIds = new ArrayList<>();
        for (Object shape : shapes) {
            if (shapeMetric(shape, "getArea") <= 1000.0) {
                expectedIds.add(shapeId(shape));
            }
        }

        List<String> resultIds = new ArrayList<>();
        List<Double> areas = new ArrayList<>();
        List<Double> perimeters = new ArrayList<>();
        for (Object shape : result) {
            double area = shapeMetric(shape, "getArea");
            if (area > 1000.0) {
                return "result contains a shape whose area exceeds 1000";
            }
            resultIds.add(shapeId(shape));
            areas.add(area);
            perimeters.add(shapeMetric(shape, "getPerimeter"));
        }
        if (!isPermutation(expectedIds, resultIds)) {
            return "result does not contain exactly the shapes with area at most 1000";
        }
        for (int i = 1; i < areas.size(); i++) {
            double area = areas.get(i);
            double previousArea = areas.get(i - 1);
            if (area > previousArea + 1e-9
                    || Math.abs(area - previousArea) <= 1e-9
                    && perimeters.get(i) > perimeters.get(i - 1) + 1e-9) {
                return "result is not sorted by descending area and perimeter";
            }
        }
        return null;
    }

    private static double shapeMetric(Object shape, String methodName) throws Exception {
        Object value = shape.getClass().getMethod(methodName).invoke(shape);
        if (!(value instanceof Number)) {
            throw new IllegalStateException(methodName + " did not return a number");
        }
        double metric = ((Number) value).doubleValue();
        if (!Double.isFinite(metric)) {
            throw new IllegalStateException(methodName + " returned a non-finite value");
        }
        return metric;
    }

    private static String shapeId(Object shape) {
        String text = shape.toString();
        int separator = text.indexOf("=>");
        if (separator < 0) {
            throw new IllegalStateException("cannot parse shape id from: " + text);
        }
        return text.substring(0, separator).trim();
    }

    // ---- reflection helpers ----

    private static Object invokeStatic(String className, String methodName, Class<?>[] paramTypes,
                                       Object... args) throws Exception {
        Class<?> clazz = Class.forName(className);
        Method method = clazz.getMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static Object newShape(String className, String id, double... dims) throws Exception {
        Class<?>[] paramTypes = new Class[dims.length + 1];
        paramTypes[0] = String.class;
        Object[] ctorArgs = new Object[dims.length + 1];
        ctorArgs[0] = id;
        for (int i = 0; i < dims.length; i++) {
            paramTypes[i + 1] = double.class;
            ctorArgs[i + 1] = dims[i];
        }
        Class<?> clazz = Class.forName(className);
        return clazz.getConstructor(paramTypes).newInstance(ctorArgs);
    }

    // ---- file / parsing helpers ----

    private static List<String> readLines(String file) throws IOException {
        return Files.readAllLines(Path.of(file));
    }

    private static List<String> surnameCandidates(List<String> lines) {
        Set<String> candidates = new LinkedHashSet<>();
        for (String line : lines) {
            String[] tokens = nameTokens(line);
            if (tokens.length == 0) {
                continue;
            }
            addSurnameCandidate(candidates, tokens[0]);
            addSurnameCandidate(candidates, tokens[tokens.length - 1]);
        }
        return new ArrayList<>(candidates);
    }

    private static void addSurnameCandidate(Set<String> candidates, String token) {
        String normalized = token.toUpperCase(Locale.ROOT);
        if (token.equals(normalized) && !FIXED_Q2A_SURNAMES.contains(normalized)) {
            candidates.add(token);
        }
    }

    private static String[] nameTokens(String line) {
        int dash = line.lastIndexOf('-');
        if (dash < 0 || line.substring(0, dash).trim().isEmpty()) {
            return new String[0];
        }
        return line.substring(0, dash).trim().split("\\s+");
    }

    private static double averageAgeForSurname(List<String> lines, String query) {
        double sum = 0;
        int count = 0;
        for (String line : lines) {
            int dash = line.lastIndexOf('-');
            if (dash < 0) {
                continue;
            }
            String[] tokens = nameTokens(line);
            if (tokens.length == 0) {
                continue;
            }
            String ageText = line.substring(dash + 1).trim();
            int age;
            try {
                age = Integer.parseInt(ageText);
            } catch (NumberFormatException e) {
                continue;
            }
            if (tokens[0].equalsIgnoreCase(query)
                    || tokens[tokens.length - 1].equalsIgnoreCase(query)) {
                sum += age;
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    private static List<String> coursesOf(String line) {
        List<String> courses = new ArrayList<>();
        int comma = line.indexOf(',');
        if (comma < 0) {
            return courses;
        }
        String rest = line.substring(comma + 1).trim();
        for (String pair : rest.split("-")) {
            int hash = pair.indexOf('#');
            if (hash > 0) {
                courses.add(pair.substring(0, hash).trim());
            }
        }
        return courses;
    }

    private static TopStudent topStudent(List<String> lines, String course) {
        String bestName = null;
        double bestGpa = Double.NEGATIVE_INFINITY;
        for (String line : lines) {
            int comma = line.indexOf(',');
            if (comma < 0) {
                continue;
            }
            String name = line.substring(0, comma).trim();
            for (String pair : line.substring(comma + 1).trim().split("-")) {
                int hash = pair.indexOf('#');
                if (hash < 0) {
                    continue;
                }
                if (!pair.substring(0, hash).trim().equalsIgnoreCase(course)) {
                    continue;
                }
                double gpa;
                try {
                    gpa = Double.parseDouble(pair.substring(hash + 1).trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (gpa > bestGpa) {
                    bestGpa = gpa;
                    bestName = name;
                }
            }
        }
        return bestName == null ? null : new TopStudent(bestName, bestGpa);
    }

    private static final class TopStudent {
        final String name;
        final double gpa;
        TopStudent(String name, double gpa) {
            this.name = name;
            this.gpa = gpa;
        }
    }

    // ---- input generators ----

    private static final String[] WORD_BANK = {
            "alice", "ben", "cathy", "ellen", "alan", "zoe", "Elle", "Daniel", "Canny",
            "test", "hello", "world", "Anna", "Bob", "xYz", "aa", "a", "book", "deer", "moon"
    };

    private static ArrayList<String> randomWordList(Random random) {
        ArrayList<String> words = new ArrayList<>();
        int count = 2 + random.nextInt(5); // 2..6 words
        for (int i = 0; i < count; i++) {
            String word = WORD_BANK[random.nextInt(WORD_BANK.length)];
            if (random.nextInt(3) == 0 && word.length() > 1) {
                // Make it a guaranteed non-isogram by duplicating the first letter.
                word = word.charAt(0) + word;
            }
            words.add(word);
        }
        return words;
    }

    private static ArrayList<Object> randomMixedList(Random random) {
        ArrayList<Object> items = new ArrayList<>();
        int count = 1 + random.nextInt(10); // 1..10 items
        for (int i = 0; i < count; i++) {
            switch (random.nextInt(4)) {
                case 0: items.add(random.nextInt(100) - 50); break;
                case 1: items.add(random.nextBoolean()); break;
                case 2: items.add(random.nextDouble() * 100); break;
                default: items.add("string" + random.nextInt(100)); break;
            }
        }
        return items;
    }

    // ---- small utilities ----

    private static boolean isIsogram(String word) {
        String lower = word.toLowerCase();
        boolean[] seen = new boolean[26];
        for (char c : lower.toCharArray()) {
            if (c < 'a' || c > 'z') {
                continue;
            }
            int idx = c - 'a';
            if (seen[idx]) {
                return false;
            }
            seen[idx] = true;
        }
        return true;
    }

    private static Map<String, Integer> frequency(List<String> list) {
        java.util.HashMap<String, Integer> freq = new java.util.HashMap<>();
        for (String item : list) {
            freq.merge(item, 1, Integer::sum);
        }
        return freq;
    }

    private static boolean isPermutation(List<String> a, List<String> b) {
        if (a.size() != b.size()) {
            return false;
        }
        return frequency(a).equals(frequency(b));
    }

    private static void writeResult(String resultFilePath, String status) {
        try {
            Files.writeString(Path.of(resultFilePath), status);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
