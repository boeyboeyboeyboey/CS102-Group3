package autograder.analysis.boilerplate;

import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodComparisonTest {
    @Test
    void matchesOverloadsConstructorsAndMemberClassesDespiteReordering() throws Exception {
        String starter = "class A { A() {} int f(int x) { return x; } "
                + "int f(String s) { return 0; } class Inner { void f() {} } }";
        String answer = "class A { class Inner { void f() {} } "
                + "int f(String renamed) { return 0; } A() {} "
                + "int f(int x) { return x; } }";
        var result = MethodComparison.compare(starter, answer, 1000);
        assertEquals(4, result.size());
        assertEquals("A#<init>()", result.get(0).signature());
        assertEquals("UNCHANGED", result.get(0).status());
        assertEquals("UNCHANGED", result.get(1).status());
        assertEquals("MODIFIED", result.get(2).status());
        assertTrue(result.get(2).explanation().contains("body tokens are unchanged"));
        assertEquals("A.Inner#f()", result.get(3).signature());
        assertEquals("UNCHANGED", result.get(3).status());
    }

    @Test
    void ignoresCommentsAndFormattingButRetainsLiteralChangesAndLineReferences() throws Exception {
        String starter = "class A {\n String f() { return \"{ // text\"; }\n}";
        String formatted = "class A {\n\n String f( ) { /* note */ return \"{ // text\"; }\n}";
        var same = MethodComparison.compare(starter, formatted, 1000).get(0);
        assertEquals("UNCHANGED", same.status());
        assertEquals(2, same.starter().line());
        assertEquals(3, same.answer().line());
        var changed = MethodComparison.compare(starter,
                formatted.replace("// text", "// different"), 1000).get(0);
        assertEquals("MODIFIED", changed.status());
        assertTrue(changed.explanation().contains("0 token(s) inserted, 0 deleted, 1 replaced"));
        assertTrue(changed.explanation().contains("body tokens changed"));
    }

    @Test
    void reportsAddedRemovedAndChangedSignaturesWithoutGuessingRenames() throws Exception {
        var result = MethodComparison.compare("class A { void f(int x) {} }",
                "class A { void f(long x) {} void extra() {} }", 1000);
        assertEquals(3, result.size());
        assertEquals("REMOVED", result.get(0).status());
        assertEquals("ADDED", result.get(1).status());
        assertEquals("ADDED", result.get(2).status());
    }

    @Test
    void parsesAnnotationsGenericsArraysVarargsAndUnavailableDependencies() throws Exception {
        String source = "class A { @Deprecated <T> T f(java.util.List<T> a, "
                + "String... b) throws Missing { return null; } "
                + "int[] array(int values[]) { return values; } }";
        var methods = JavaMethods.read(source, 1000);
        assertTrue(methods.containsKey("A#f(java.util.List<T>,String[])"));
        assertTrue(methods.containsKey("A#array(int[])"));
        assertEquals(2, methods.size());
    }

    @Test
    void rejectsMalformedAmbiguousUnicodeAndOversizeInput() {
        assertThrows(IOException.class, () -> JavaMethods.read("class A { void f( }", 1000));
        assertThrows(IOException.class, () -> JavaMethods.read(
                "class A { void f() {} void f() {} }", 1000));
        assertThrows(IOException.class, () -> JavaMethods.read("\\" + "u0061", 1000));
        assertThrows(IOException.class, () -> JavaMethods.read("class A {}", 1));
    }

    @Test
    void excludesImplicitConstructorsFieldsAndLocalClassEntries() throws Exception {
        var methods = JavaMethods.read("class A { int value; { value = 2; } "
                + "void f() { class Local { void g() {} } } }", 1000);
        assertEquals(1, methods.size());
        assertTrue(methods.containsKey("A#f()"));
        assertTrue(JavaMethods.read("class Empty { int value; }", 1000).isEmpty());
    }

    @Test
    void alignsSourceLinesAndEscapesCodeInsteadOfExecutingIt() throws Exception {
        var difference = MethodComparison.compare(
                "class A {\n void f() {\n  String s = \"<script>\";\n }\n}",
                "class A {\n void f() {\n  String s = \"&new\";\n }\n}", 1000).get(0);
        var writer = new StringWriter();
        MethodDiffHtml.write(writer, difference);
        String html = writer.toString();
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("&amp;new"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("class=\"removed\""));
        assertTrue(html.contains("class=\"added\""));
        assertTrue(html.contains("class=number>3</td>"));
    }

    @Test
    void boundsDetailedDiffWorkWhileRetainingModifiedStatus() throws Exception {
        String body = "x++;".repeat(600);
        var difference = MethodComparison.compare(
                "class A { void f() { int x=0;" + body + "} }",
                "class A { void f() { int x=1;" + body + "} }", 5000).get(0);
        assertEquals("MODIFIED", difference.status());
        assertTrue(difference.explanation().contains("comparison budget"));
        var manyLines = MethodComparison.compare(
                "class A { void f() { int x=0;\n" + "x++;\n".repeat(600) + "} }",
                "class A { void f() { int x=1;\n" + "x++;\n".repeat(600) + "} }", 5000).get(0);
        var writer = new StringWriter();
        MethodDiffHtml.write(writer, manyLines);
        assertTrue(writer.toString().contains("Line alignment budget exceeded"));
        assertFalse(writer.toString().contains("class=\"removed\""));
    }
}
