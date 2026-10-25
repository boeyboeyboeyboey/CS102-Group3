package autograder.ui;

import autograder.api.ProgressListener;
import java.io.PrintStream;
import java.util.Map;

public final class TerminalDashboard implements ProgressListener {
    private static final int BAR_WIDTH = 12;
    private static final String CYAN = "\u001b[36m";
    private static final String GREEN = "\u001b[32m";
    private static final String RESET = "\u001b[0m";

    private final PrintStream output;
    private final boolean ansi;
    private String lastStage = "";
    private String lastItem = "";
    private int lastCompleted = -1;

    public TerminalDashboard(PrintStream output, DashboardSettings.Mode mode,
                             boolean interactive, Map<String, String> environment) {
        this.output = output;
        String term = environment.getOrDefault("TERM", "");
        this.ansi = mode == DashboardSettings.Mode.ANSI
                || (mode == DashboardSettings.Mode.AUTO && interactive
                && !"dumb".equalsIgnoreCase(term)
                && !environment.containsKey("NO_COLOR"));
    }

    @Override
    public synchronized void onProgress(String stage, String item, int completed, int total) {
        int safeTotal = Math.max(total, 1);
        int safeCompleted = Math.max(0, Math.min(completed, safeTotal));
        String safeStage = safeText(stage);
        String safeItem = safeText(item);
        if (safeStage.equals(lastStage) && safeItem.equals(lastItem)
                && safeCompleted == lastCompleted) {
            return;
        }
        String action = safeCompleted > lastCompleted && safeStage.equals(lastStage)
                ? "done" : "working";
        int filled = safeCompleted * BAR_WIDTH / safeTotal;
        String bar = "#".repeat(filled) + "-".repeat(BAR_WIDTH - filled);
        int percent = safeCompleted * 100 / safeTotal;
        String label = "[" + safeStage + "] [" + bar + "] " + safeCompleted
                + "/" + safeTotal + " (" + percent + "%) " + action + ": " + safeItem;
        if (ansi) {
            output.println((safeCompleted == safeTotal ? GREEN : CYAN) + label + RESET);
        } else {
            output.println(label);
        }
        output.flush();
        lastStage = safeStage;
        lastItem = safeItem;
        lastCompleted = safeCompleted;
    }

    private static String safeText(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder safe = new StringBuilder();
        for (int index = 0; index < value.length() && safe.length() < 40; index++) {
            char character = value.charAt(index);
            safe.append(Character.isISOControl(character) ? '?' : character);
        }
        if (value.length() > 40) {
            safe.append("...");
        }
        return safe.toString();
    }
}
