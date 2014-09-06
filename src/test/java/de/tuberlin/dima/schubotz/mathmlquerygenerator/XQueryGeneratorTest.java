package de.tuberlin.dima.schubotz.mathmlquerygenerator;

import junit.framework.TestCase;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Scanner;

public class XQueryGeneratorTest extends TestCase {

    @SuppressWarnings("SameParameterValue")
    String getFileContents(String fname) throws IOException {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(fname)) {
            final Scanner s = new Scanner(is, "UTF-8");
            //Stupid scanner tricks to read the entire file as one token
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    private File getResources(String resourceName) {
        URL url = this.getClass().getClassLoader().getResource(resourceName);
        File dir = null;
        try {
            //IntelliJ would accept if (url != null) { ... } else { throw new NullPointerException(); }
            //but I don't like that
            //noinspection ConstantConditions
            dir = new File(url.toURI());
        } catch (Exception e) {
            fail("Cannot open test resource folder.");
        }
        return dir;
    }

    private void runTestCollection(String resourceName) {
        runTestCollection(getResources(resourceName));
    }

    private void runTestCollection(File dir) {
        String queryString = null;
        String reference = null;
        Document query = null;
        for (File nextFile : dir.listFiles()) {
            if (nextFile.getName().endsWith(".xml")) {
                File resultPath = new File(nextFile.getAbsolutePath().replace(".xml", ".xq"));
                try {
                    queryString = new String(Files.readAllBytes(nextFile.toPath()));
                    reference = new String(Files.readAllBytes(resultPath.toPath()));
                } catch (Exception e) {
                    fail("Cannot load test tuple (" + resultPath + ", ... " + nextFile.getName() + " )");
                }
                try {
                    query = XMLHelper.String2Doc(queryString);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Cannot parse reference document " + nextFile.getName());
                }
                XQueryGenerator xQueryGenerator = new XQueryGenerator(query);
                assertEquals("Example " + nextFile.getName() + " does not match reference.", reference, xQueryGenerator.toString());
            }
        }
    }

    public void testMwsConversion() {
        runTestCollection("de/tuberlin/dima/schubotz/MathMLQueryGenerator/mws");
    }

    public void testCmmlConversion() {
        runTestCollection("de/tuberlin/dima/schubotz/MathMLQueryGenerator/cmml");
    }

    public void testHeaderAndFooter() throws Exception {
        final String testHead = "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
                "<result>{\n" +
                "let $m := .";
        final String testFooter = "$x}\n" +
                "</result>";
        final String testInput = getFileContents("de/tuberlin/dima/schubotz/MathMLQueryGenerator/cmml/q1.xml");
        final String expectedOutput = "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
                "<result>{\n" +
                "let $m := .for $x in $m//*:ci\n" +
                "[./text() = 'E']\n" +
                "where\n" +
                "fn:count($x/*) = 0\n" +
                "\n" +
                "return\n" +
                "$x}\n" +
                "</result>";
        Document query = XMLHelper.String2Doc(testInput);
        XQueryGenerator xQueryGenerator = new XQueryGenerator(query);
        xQueryGenerator.setFooter(testFooter);
        xQueryGenerator.setHeader(testHead);
        assertEquals(expectedOutput, xQueryGenerator.toString());
    }
}