package com.formulasearchengine.mathmlquerygenerator;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static com.formulasearchengine.mathmlquerygenerator.XQueryGeneratorTest.getFileContents;
import static org.junit.Assert.assertEquals;

public class NtcirTopicReaderTest{
	public static final String BASEX_HEADER = "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
		"for $m in //*:expr return \n";
	public static final String BASEX_FOOTER = "<a href=\"http://demo.formulasearchengine.com/index.php?curid={$m/@url}\">result</a>\n";
	public static final String WIKIPEDIA_RESOURCE = "jp/ac/nii/Ntcir11MathWikipediaTopicsParticipants.xml";


	@Test
	public void testExtractPatterns () throws Exception {
		assertEquals( "Count in Wikipedia testfile incorrect" , 100,  countFormulaeInTopics( WIKIPEDIA_RESOURCE ) );
		assertEquals( "Count in arXiv testfile incorrect", 55,  countFormulaeInTopics( "jp/ac/nii/NTCIR-11-Math-test.xml" ) );
	}
	private int countFormulaeInTopics (String resourceName) throws URISyntaxException, IOException, SAXException, ParserConfigurationException, XPathExpressionException {
		final List<NtcirPattern> ntcirPatterns = getTopicReader( resourceName ).extractPatterns();
		return ntcirPatterns.size();
	}

	private  NtcirTopicReader getTopicReader (String resourceName) throws ParserConfigurationException, IOException, SAXException, URISyntaxException, XPathExpressionException, NullPointerException {
		URL resource = getClass().getClassLoader().getResource( resourceName );
		return new NtcirTopicReader( new File( resource.toURI() ) );
	}
    public  NtcirPattern getFirstTopic() throws URISyntaxException, ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        NtcirTopicReader tr = getTopicReader( WIKIPEDIA_RESOURCE );
        return tr.extractPatterns().get(0);
    }

	@Test
	public void checkBaseX() throws Exception {
		final String referenceString = getFileContents( "jp/ac/nii/basexReferenceQueries.txt" );
		NtcirTopicReader tr = getTopicReader( WIKIPEDIA_RESOURCE );
		tr.setHeader( BASEX_HEADER );
		tr.setFooter( BASEX_FOOTER );
		StringBuilder sb = new StringBuilder();
		for ( NtcirPattern ntcirPattern : tr.extractPatterns() ) {
			sb.append( ntcirPattern.getxQueryExpression() );
		}
		assertEquals( referenceString,  sb.toString());

	}
	@Test
	public void extractPattern() throws Exception {
		NtcirTopicReader tr = getTopicReader( WIKIPEDIA_RESOURCE );
		tr.setHeader( BASEX_HEADER );
		tr.setFooter( BASEX_FOOTER );
		for ( NtcirPattern ntcirPattern : tr.extractPatterns() ) {
			if (ntcirPattern.getNum().endsWith( "29" )){
				System.out.println(ntcirPattern.getxQueryExpression());
			}
		}

	}


    @Test
    public void testSetRestricLength() throws Exception {
        final String NoLenghtxQuery = getTopicReader(WIKIPEDIA_RESOURCE).setRestrictLength(false)
                .extractPatterns().get(0).getxQueryExpression();
        assertEquals( "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
                "for $m in db2-fn:xmlcolumn(\"math.math_mathml\") return\n" +
                "for $x in $m//*:apply\n" +
                "[*[1]/name() = 'gt' and *[2]/name() = 'apply' and *[2][*[1]/name() = 'times' and *[2]/name() = 'ci' and *[2][./text() = 'W'] and *[3]/name() = 'interval' and *[3][*[1]/name() = 'cn' and *[1][./text() = '2'] and *[2]/name() = 'ci' and *[2][./text() = 'k']]] and *[3]/name() = 'apply' and *[3][*[1]/name() = 'divide' and *[2]/name() = 'apply' and *[2][*[1]/name() = 'csymbol' and *[1][./text() = 'superscript'] and *[2]/name() = 'cn' and *[2][./text() = '2'] and *[3]/name() = 'ci' and *[3][./text() = 'k']] and *[3]/name() = 'apply' and *[3][*[1]/name() = 'csymbol' and *[1][./text() = 'superscript'] and *[2]/name() = 'ci' and *[2][./text() = 'k'] and *[3]/name() = 'ci' and *[3][./text() = 'ε']]]]\n" +
                "return\n" +
                "data($m/*[1]/@alttext)", NoLenghtxQuery );
    }
}