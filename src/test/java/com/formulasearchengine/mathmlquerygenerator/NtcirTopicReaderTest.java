package com.formulasearchengine.mathmlquerygenerator;

import com.formulasearchengine.xmlhelper.DomDocumentHelper;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static com.formulasearchengine.mathmlquerygenerator.XQueryGeneratorTest.getFileContents;
import static org.junit.Assert.assertEquals;

public class NtcirTopicReaderTest {
	//Search the <mws:expr> tags
	public static final String BASEX_HEADER = "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
		"for $m in //*:expr return \n";
	//Return the ID of the <mws:expr> in a link
	public static final String BASEX_FOOTER = "<a href=\"http://demo.formulasearchengine.com/index.php?curid={$m/@url}\">result</a>\n";

	//Search MathML xml columns in a DB2 database instance
	public static final String DB2_HEADER = "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
		"for $m in db2-fn:xmlcolumn(\"math.math_mathml\") return\n";
	//Return the alternate text of the first child of each XML column that matches the query
	public static final String DB2_FOOTER = "data($m/*[1]/@alttext)";

	public static final String WIKIPEDIA_RESOURCE = "jp/ac/nii/Ntcir11MathWikipediaTopicsParticipants.xml";
	public static final String ARXIV_RESOURCE = "jp/ac/nii/NTCIR-11-Math-test.xml";



	@Test
	public void testExtractPatterns() throws Exception {
		assertEquals( "Count in Wikipedia testfile incorrect", 100, countFormulaeInTopics( WIKIPEDIA_RESOURCE ) );
		assertEquals( "Count in arXiv testfile incorrect", 55, countFormulaeInTopics( ARXIV_RESOURCE ) );
	}

	private int countFormulaeInTopics( String resourceName ) throws URISyntaxException,
			IOException, SAXException, ParserConfigurationException, XPathExpressionException {
		final List<NtcirPattern> ntcirPatterns = new NtcirTopicReader( getResourceAsFile( resourceName ),
				DB2_HEADER, DB2_FOOTER, true).extractPatterns();
		return ntcirPatterns.size();
	}

	private File getResourceAsFile( String resourceName ) throws URISyntaxException, NullPointerException {
		final URL resource = getClass().getClassLoader().getResource( resourceName );
		return new File( resource.toURI() );
	}

	public NtcirPattern getFirstTopic() throws URISyntaxException, ParserConfigurationException, SAXException, XPathExpressionException, IOException {
		final NtcirTopicReader tr = new NtcirTopicReader( getResourceAsFile( WIKIPEDIA_RESOURCE ),
				DB2_HEADER, DB2_FOOTER, true);
		return tr.extractPatterns().get( 0 );
	}

	@Test
	public void checkBaseX() throws Exception {
		final String referenceString = getFileContents( "jp/ac/nii/basexReferenceQueries.txt" );
		final NtcirTopicReader tr = new NtcirTopicReader( getResourceAsFile( WIKIPEDIA_RESOURCE ), BASEX_HEADER,
				BASEX_FOOTER, true );
		final StringBuilder sb = new StringBuilder();
		for ( final NtcirPattern ntcirPattern : tr.extractPatterns() ) {
			sb.append( ntcirPattern.getxQueryExpression() );
		}
		assertEquals( referenceString, sb.toString() );

	}

	@Test
	public void extractPattern() throws Exception {
		final NtcirTopicReader tr = new NtcirTopicReader( getResourceAsFile( WIKIPEDIA_RESOURCE ), BASEX_HEADER,
				BASEX_FOOTER, true );
		for ( final NtcirPattern ntcirPattern : tr.extractPatterns() ) {
			if ( ntcirPattern.getNum().endsWith( "29" ) ) {
				System.out.println( ntcirPattern.getxQueryExpression() );
			}
		}

	}

	@Test
	public void testSetRestricLength() throws Exception {
		final String NoLenghtxQuery = new NtcirTopicReader( getResourceAsFile( WIKIPEDIA_RESOURCE ), DB2_HEADER,
				DB2_FOOTER, false )
					.extractPatterns().get( 0 ).getxQueryExpression();
		assertEquals( "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
			"for $m in db2-fn:xmlcolumn(\"math.math_mathml\") return\n" +
			"for $x in $m//*:apply\n" +
			"[*[1]/name() = 'gt' and *[2]/name() = 'apply' and *[2][*[1]/name() = 'times' and *[2]/name() = 'ci' and *[2][./text() = 'W'] and *[3]/name() = 'interval' and *[3][*[1]/name() = 'cn' and *[1][./text() = '2'] and *[2]/name() = 'ci' and *[2][./text() = 'k']]] and *[3]/name() = 'apply' and *[3][*[1]/name() = 'divide' and *[2]/name() = 'apply' and *[2][*[1]/name() = 'csymbol' and *[1][./text() = 'superscript'] and *[2]/name() = 'cn' and *[2][./text() = '2'] and *[3]/name() = 'ci' and *[3][./text() = 'k']] and *[3]/name() = 'apply' and *[3][*[1]/name() = 'csymbol' and *[1][./text() = 'superscript'] and *[2]/name() = 'ci' and *[2][./text() = 'k'] and *[3]/name() = 'ci' and *[3][./text() = 'ε']]]]\n" +
			"return\n" +
			"data($m/*[1]/@alttext)", NoLenghtxQuery );
	}

	@Test
	public void testAlternativeConstructor() throws Exception{
		final URL resource = getClass().getClassLoader().getResource( ARXIV_RESOURCE );
		DocumentBuilder documentBuilder = DomDocumentHelper.getDocumentBuilderFactory().newDocumentBuilder();
		Document topics = documentBuilder.parse( new File( resource.toURI() ) );
		new NtcirTopicReader( topics, DB2_HEADER, DB2_FOOTER, true );
	}
}