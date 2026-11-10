package autograder.plagiarism;

/** A run of lines in one file, used to show where matching code sits. */
public record LineRange(String file, int startLine, int endLine) {
    public boolean continuesInto(LineRange next) {
        return file.equals(next.file) && next.startLine >= startLine && next.startLine <= endLine + 1;
    }

    public LineRange extendTo(LineRange next) {
        return new LineRange(file, startLine, Math.max(endLine, next.endLine));
    }

    public String describe() {
        if (startLine == endLine) {
            return file + " line " + startLine;
        }
        return file + " lines " + startLine + "-" + endLine;
    }
}
