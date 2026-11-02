package autograder.analysis.boilerplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class MethodComparison {
    private static final long MAX_DIFF_CELLS = 1_000_000;

    record Difference(String signature, String status, String explanation,
                      JavaMethods.Method starter, JavaMethods.Method answer) {
    }

    private MethodComparison() {
    }

    static List<Difference> compare(String starter, String answer, int limit)
            throws IOException {
        var before = JavaMethods.read(starter, limit);
        var after = JavaMethods.read(answer, limit);
        var keys = new LinkedHashSet<>(before.keySet());
        keys.addAll(after.keySet());
        List<Difference> differences = new ArrayList<>();
        for (String key : keys) {
            var first = before.get(key);
            var second = after.get(key);
            String status;
            String explanation;
            if (first == null) {
                status = "ADDED";
                explanation = "Method signature occurs only "
                        + "in the student file";
            } else if (second == null) {
                status = "REMOVED";
                explanation = "Starter method signature is absent "
                        + "from the student file";
            } else if (first.tokens().equals(second.tokens())) {
                status = "UNCHANGED";
                explanation = "Method code tokens are unchanged; "
                        + "comments and formatting are ignored";
            } else {
                status = "MODIFIED";
                explanation = edits(first.tokens(), second.tokens())
                        + (first.bodyTokens().equals(second.bodyTokens())
                        ? "; method body tokens are unchanged; "
                                + "declaration changed"
                        : "; method body tokens changed");
            }
            differences.add(new Difference(
                    key, status, explanation, first, second));
        }
        return List.copyOf(differences);
    }

    private static String edits(List<String> before, List<String> after) {
        if ((long) (before.size() + 1) * (after.size() + 1) > MAX_DIFF_CELLS) {
            return "Code tokens differ; detailed edit counts "
                    + "exceed the comparison budget";
        }
        int[][] distance = new int[before.size() + 1][after.size() + 1];
        for (int i = 0; i <= before.size(); i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= after.size(); j++) {
            distance[0][j] = j;
        }
        for (int i = 1; i <= before.size(); i++) {
            for (int j = 1; j <= after.size(); j++) {
                int cost = before.get(i - 1).equals(after.get(j - 1))
                        ? 0 : 1;
                distance[i][j] = Math.min(distance[i - 1][j - 1] + cost,
                        Math.min(distance[i - 1][j] + 1,
                                distance[i][j - 1] + 1));
            }
        }
        return countEdits(before, after, distance);
    }

    private static String countEdits(List<String> before, List<String> after,
                                     int[][] distance) {
        int i = before.size();
        int j = after.size();
        int inserted = 0;
        int deleted = 0;
        int replaced = 0;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && before.get(i - 1).equals(after.get(j - 1))) {
                i--;
                j--;
            } else if (i > 0 && j > 0
                    && distance[i][j] == distance[i - 1][j - 1] + 1) {
                replaced++;
                i--;
                j--;
            } else if (i > 0 && distance[i][j] == distance[i - 1][j] + 1) {
                deleted++;
                i--;
            } else {
                inserted++;
                j--;
            }
        }
        return inserted + " token(s) inserted, " + deleted + " deleted, "
                + replaced + " replaced (one minimum-edit alignment)";
    }
}
