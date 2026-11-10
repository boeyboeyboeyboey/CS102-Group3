package autograder.gui;

import autograder.csv.CsvCodec;
import autograder.report.XlsxWorkbookWriter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Exports a run's CSV reports to a folder, plus one Excel workbook with a sheet for each. */
final class ResultsExport {
    static final String WORKBOOK_NAME = "bytegrade-results.xlsx";
    private static final String TITLE = "Export all";

    private ResultsExport() {
    }

    static boolean available(List<Artifact> artifacts) {
        return !tables(artifacts).isEmpty();
    }

    private static List<Artifact> tables(List<Artifact> artifacts) {
        return artifacts.stream().filter(artifact -> artifact.view() == Artifact.View.TABLE).toList();
    }

    /** Writes the workbook, then copies each CSV; a report already in directory is left as it is. */
    static void write(List<Artifact> artifacts, Path directory) throws IOException {
        List<XlsxWorkbookWriter.Sheet> sheets = new ArrayList<>();
        for (Artifact artifact : tables(artifacts)) {
            sheets.add(new XlsxWorkbookWriter.Sheet(artifact.label(),
                    CsvCodec.parse(Files.readString(artifact.path(), StandardCharsets.UTF_8))));
        }
        XlsxWorkbookWriter.write(directory.resolve(WORKBOOK_NAME), sheets);
        for (Artifact artifact : tables(artifacts)) {
            Files.copy(artifact.path(), directory.resolve(artifact.path().getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** How many files an export to directory would replace. */
    static int filesToReplace(List<Artifact> artifacts, Path directory) throws IOException {
        int replaced = Files.exists(directory.resolve(WORKBOOK_NAME)) ? 1 : 0;
        for (Artifact artifact : tables(artifacts)) {
            Path target = directory.resolve(artifact.path().getFileName());
            if (Files.exists(target) && !Files.isSameFile(artifact.path(), target)) {
                replaced++;
            }
        }
        return replaced;
    }

    /** Asks which folder to use, then exports and reports the result. */
    static void chooseAndWrite(Component parent, List<Artifact> artifacts, Path directory) {
        JFileChooser chooser = new JFileChooser(directory.toFile());
        chooser.setDialogTitle(TITLE);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setSelectedFile(new File(directory.toString()));
        if (chooser.showDialog(parent, "Export") != JFileChooser.APPROVE_OPTION
                || chooser.getSelectedFile() == null) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        try {
            int replaced = filesToReplace(artifacts, target);
            if (replaced > 0 && JOptionPane.showConfirmDialog(parent, "Replace " + replaced
                            + " existing file(s) in this folder?", TITLE, JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return;
            }
            write(artifacts, target);
            JOptionPane.showMessageDialog(parent, "Saved " + tables(artifacts).size() + " CSV files and "
                    + WORKBOOK_NAME + " to\n" + target, TITLE, JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | RuntimeException exception) {
            JOptionPane.showMessageDialog(parent, "Couldn't export. If a file is open in another app, "
                    + "close it and try again.\n" + target, TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }
}
