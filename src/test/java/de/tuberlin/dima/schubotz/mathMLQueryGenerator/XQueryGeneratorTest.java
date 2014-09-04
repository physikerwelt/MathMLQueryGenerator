package de.tuberlin.dima.schubotz.mathMLQueryGenerator;

import junit.framework.TestCase;
import org.w3c.dom.Document;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;

public class XQueryGeneratorTest extends TestCase {

    private File getResources(String resourceName){
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

    private void runTestCollection(String resourceName){
        runTestCollection(getResources(resourceName));
    }

    private void runTestCollection(File dir){
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

}