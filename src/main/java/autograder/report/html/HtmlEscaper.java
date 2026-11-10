package autograder.report.html;

/** Escapes text for safe inclusion in HTML content and attribute values. */
final class HtmlEscaper {
    private HtmlEscaper() { }

    static String escape(String value) {
        if (value == null) {
            return "Not recorded";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(character);
            }
        }
        return escaped.toString();
    }
}
