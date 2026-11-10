package autograder.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownHtmlTest {
    @Test
    void rendersHeadingsAndBullets() {
        String html = MarkdownHtml.render("# Title\n\n- one\n- two\n");
        assertTrue(html.contains("<h1>Title</h1>"), html);
        assertTrue(html.contains("<ul><li>one</li><li>two</li></ul>"), html);
    }

    @Test
    void rendersTablesWithRightAlignedColumns() {
        String html = MarkdownHtml.render("| Question | Marks |\n| --- | ---: |\n| Q1a | 3 / 3 |\n");
        assertTrue(html.contains("<th>Question</th><th align=\"right\">Marks</th>"), html);
        assertTrue(html.contains("<td>Q1a</td><td align=\"right\">3 / 3</td>"), html);
    }

    @Test
    void escapedPipeStaysInsideItsCell() {
        String html = MarkdownHtml.render("| A |\n| --- |\n| a\\|b |\n");
        assertTrue(html.contains("<td>a|b</td>"), html);
    }

    @Test
    void studentTextCannotBecomeMarkup() {
        String html = MarkdownHtml.render("<img src=\"http://x/a.png\"> and \\<b\\>");
        assertFalse(html.contains("<img"), html);
        assertTrue(html.contains("&lt;img"), html);
        assertTrue(html.contains("&lt;b&gt;"), html);
    }

    @Test
    void codeFenceKeepsItsTextLiteral() {
        String html = MarkdownHtml.render("````text\n**not bold** ``` <x>\n````\n");
        assertTrue(html.contains("<pre>**not bold** ``` &lt;x&gt;\n</pre>"), html);
    }

    @Test
    void rendersBoldAndQuotes() {
        String html = MarkdownHtml.render("> **Score withheld.** Check it\n");
        assertTrue(html.contains("<blockquote><b>Score withheld.</b> Check it<br></blockquote>"), html);
    }
}
