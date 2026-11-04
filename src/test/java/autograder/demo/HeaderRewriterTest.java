package autograder.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderRewriterTest {
    private static final String BODY = "import java.util.*;\r\n\r\npublic class Q1a {\r\n"
            + "    int name = 1;\r\n}\r\n";

    @Test
    void replacesExistingIdentityValuesAndKeepsCodeUntouched() {
        String source = "/*\r\n * Name: Teo Chee\r\n * Email ID: chee.teo.2022\r\n */\r\n\r\n" + BODY;

        String result = HeaderRewriter.rewrite(source, "Aiden Tan", "aiden.tan.2021@x.sg", "u");

        assertEquals("/*\r\n * Name: Aiden Tan\r\n * Email ID: aiden.tan.2021@x.sg\r\n */\r\n\r\n"
                + BODY, result);
    }

    @Test
    void fillsEmptyValuesAndReplacesUsernameLine() {
        String source = "/*\n * Name:\n * Email ID:\n * Username: old\n */\nclass A {}\n";

        String result = HeaderRewriter.rewrite(source, "N N", "e@x", "new.user.2020");

        assertEquals("/*\n * Name: N N\n * Email ID: e@x\n * Username: new.user.2020\n */\n"
                + "class A {}\n", result);
    }

    @Test
    void insertsHeaderWhenAbsentUsingTheFileLineEnding() {
        String result = HeaderRewriter.rewrite(BODY, "Aiden Tan", "a@x", "u");

        assertTrue(result.startsWith("/*\r\n * Name: Aiden Tan\r\n * Email ID: a@x\r\n */\r\n\r\n"));
        assertTrue(result.endsWith(BODY));
    }

    @Test
    void addsOnlyTheMissingLineInsideAnOpenHeader() {
        String source = "/*\n * Name: Old\n */\nclass A {}\n";

        String result = HeaderRewriter.rewrite(source, "New", "e@x", "u");

        assertEquals("/*\n * Name: New\n * Email ID: e@x\n */\nclass A {}\n", result);
    }
}
