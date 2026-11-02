package autograder.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Remembers the last used paths between launches; never touches config.properties. */
public final class GuiPreferences {
    static final String NODE = "autograder/gui";
    private static final String CONFIG_KEY = "config";
    private static final String SUBMISSIONS_KEY = "submissions";
    private static final String SCORE_SHEET_KEY = "scoreSheet";
    private static final String OUTPUT_KEY = "output";

    /** Remembered paths; each is null when unset or no longer present. */
    public record Remembered(Path config, Path submissions, Path scoreSheet, Path output) {
    }

    private final Preferences node;

    public GuiPreferences(Preferences node) {
        this.node = node;
    }

    public static GuiPreferences system() {
        return new GuiPreferences(Preferences.userRoot().node(NODE));
    }

    public Remembered load() {
        try {
            return new Remembered(existing(CONFIG_KEY, false), existing(SUBMISSIONS_KEY, false),
                    existing(SCORE_SHEET_KEY, false), existing(OUTPUT_KEY, true));
        } catch (SecurityException | IllegalStateException exception) {
            return new Remembered(null, null, null, null);
        }
    }

    public boolean save(GuiInputs inputs) {
        try {
            put(CONFIG_KEY, inputs.config());
            put(SUBMISSIONS_KEY, inputs.submissions());
            put(SCORE_SHEET_KEY, inputs.scoreSheet());
            put(OUTPUT_KEY, inputs.output());
            node.flush();
            return true;
        } catch (BackingStoreException | SecurityException | IllegalStateException exception) {
            return false;
        }
    }

    private void put(String key, Path value) {
        if (value == null) {
            node.remove(key);
        } else {
            node.put(key, value.toString());
        }
    }

    // output may not exist yet, so a remembered one only needs an existing parent
    private Path existing(String key, boolean parentIsEnough) {
        String stored = node.get(key, null);
        if (stored == null || stored.isBlank()) {
            return null;
        }
        try {
            Path path = Path.of(stored);
            Path probe = parentIsEnough && path.getParent() != null ? path.getParent() : path;
            return Files.exists(probe) ? path : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
