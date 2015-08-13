package com.formulasearchengine.mathmlquerygenerator;

import junit.framework.TestCase;
import org.w3c.dom.Document;

import com.formulasearchengine.mathmlquerygenerator.xmlhelper.XMLHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Scanner;

public class XQueryGeneratorTest extends TestCase {

	@SuppressWarnings("SameParameterValue")
	static public String getFileContents( String fname ) throws IOException {
		try ( InputStream is = XQueryGeneratorTest.class.getClassLoader().getResourceAsStream( fname ) ) {
			final Scanner s = new Scanner( is, "UTF-8" );
			//Stupid scanner tricks to read the entire file as one token
			s.useDelimiter( "\\A" );
			return s.hasNext() ? s.next() : "";
		}
	}

	private File getResources( String resourceName ) {
		URL url = this.getClass().getClassLoader().getResource( resourceName );
		File dir = null;
		try {
			//IntelliJ would accept if (url != null) { ... } else { throw new NullPointerException(); }
			//but I don't like that
			//noinspection ConstantConditions
			dir = new File( url.toURI() );
		} catch ( Exception e ) {
			fail( "Cannot open test resource folder." );
		}
		return dir;
	}

	private void runTestCollection( String resourceName ) {
		runTestCollection( getResources( resourceName ) );
	}

	private void runTestCollection( File dir ) {
		String queryString = null;
		String reference = null;
		Document query = null;
		for ( File nextFile : dir.listFiles() ) {
			if ( nextFile.getName().endsWith( ".xml" ) ) {
				File resultPath = new File( nextFile.getAbsolutePath().replace( ".xml", ".xq" ) );
				try {
					queryString = new String( Files.readAllBytes( nextFile.toPath() ) );
					reference = new String( Files.readAllBytes( resultPath.toPath() ) );
				} catch ( Exception e ) {
					fail( "Cannot load test tuple (" + resultPath + ", ... " + nextFile.getName() + " )" );
				}
				try {
					query = XMLHelper.String2Doc( queryString, true );
				} catch ( Exception e ) {
					e.printStackTrace();
					fail( "Cannot parse reference document " + nextFile.getName() );
				}
				XQueryGenerator xQueryGenerator = new XQueryGenerator( query );
				assertEquals( "Example " + nextFile.getName() + " does not match reference.", reference, xQueryGenerator.toString() );
			}
		}
	}

	public void testMwsConversion() {
		runTestCollection( "com/formulasearchengine/mathmlquerygenerator/mws" );
	}

	public void testCmmlConversion() {
		runTestCollection( "com/formulasearchengine/mathmlquerygenerator/cmml" );
	}

	public void testFormatsConversion() {
		runTestCollection( "com/formulasearchengine/mathmlquerygenerator/formats" );
	}

	public void testCustomization() throws Exception {
		final String testNamespace = "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";";
		final String testPathToRoot = "//*:root";
		final String testResultFormat = "<hit>{$x}</hit>";
		final String testInput = getFileContents( "com/formulasearchengine/mathmlquerygenerator/cmml/q1.xml" );
		final String expectedOutput = "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
			"for $m in //*:root return\n" +
			"for $x in $m//*:ci\n" +
			"[./text() = 'E']\n" +
			"where\n" +
			"fn:count($x/*) = 0\n" +
			"\n" +
			"return\n" +
			"<hit>{$x}</hit>";
		Document query = XMLHelper.String2Doc( testInput, true );
		XQueryGenerator xQueryGenerator = new XQueryGenerator( query );
		xQueryGenerator.setReturnFormat(testResultFormat).setNamespace(testNamespace)
			.setPathToRoot(testPathToRoot);
		assertEquals(expectedOutput, xQueryGenerator.toString());
	}

	public void testNoRestriction() throws Exception {
		final String testInput = getFileContents( "com/formulasearchengine/mathmlquerygenerator/mws/qqx2x.xml" );
		final String expectedOutput = "for $m in //*:root return\n" + "for $x in $m//*:apply\n" +
			"[*[1]/name() = 'plus' and *[2]/name() = 'apply' and *[2][*[1]/name() = 'csymbol' and *[1][./text() = 'superscript'] and *[3]/name() = 'cn' and *[3][./text() = '2']]]\n" +
			"where\n" +
			"$x/*[2]/*[2] = $x/*[3]\n" +
			"let $q := map {\"x\" : (data($x/*[2]/*[2]/@xml:id),data($x/*[3]/@xml-id))}\n\n" +
			"return\n";
		Document query = XMLHelper.String2Doc( testInput, true );
		XQueryGenerator xQueryGenerator = new XQueryGenerator( query );
		xQueryGenerator.setReturnFormat("").setNamespace("").setPathToRoot("//*:root").setRestrictLength( false );
		assertEquals( expectedOutput, xQueryGenerator.toString() );
		assertFalse( xQueryGenerator.isRestrictLength() );
	}

	public void testNoMath() throws Exception {
		final String input = "<?xml version=\"1.0\"?>\n<noMath />";
		XQueryGenerator qg = new XQueryGenerator( input );
		assertNull( "Input without math should return null", qg.toString() );
	}

}