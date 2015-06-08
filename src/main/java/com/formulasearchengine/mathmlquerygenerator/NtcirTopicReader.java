package com.formulasearchengine.mathmlquerygenerator;

import com.formulasearchengine.xmlhelper.DomDocumentHelper;
import com.formulasearchengine.xmlhelper.NonWhitespaceNodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.formulasearchengine.xmlhelper.NonWhitespaceNodeList.getFirstChild;

/**
 * Reads the topic format specified in
 * http://ntcir-math.nii.ac.jp/wp-content/blogs.dir/13/files/2014/05/NTCIR11-Math-topics.pdf
 * Converts each query into an XQuery using XQueryGenerator and then into a NtcirPattern.
 * Created by Moritz on 08.11.2014.
 */
public class NtcirTopicReader {
	public static final String NS_NII = "http://ntcir-math.nii.ac.jp/";
	private final Document topics;
	private String header;
	private String footer;
	private final boolean restrictLength;


	public NtcirTopicReader( Document topics, String header, String footer, boolean restrictLength ) {
		this.topics = topics;
		this.header = header;
		this.footer = footer;
		this.restrictLength = restrictLength;
	}

	public void setHeader( String header ) {
		this.header = header;
	}

	public void setFooter( String footer ) {
		this.footer = footer;
	}

	public NtcirTopicReader( File topicFile, String header, String footer, boolean restrictLength )
			throws ParserConfigurationException, IOException, SAXException {
		this(DomDocumentHelper.getDocumentBuilderFactory().newDocumentBuilder().parse( topicFile ), header, footer,
				restrictLength);
	}

	/**
	 * Splits the given NTCIR query file into individual queries, converts each query into an XQuery using
	 * XQueryGenerator, and returns the result as a list of NtcirPatterns for each individual query.
	 * @return List of NtcirPatterns for each query
	 * @throws XPathExpressionException Thrown if xpaths fail to compile or fail to evaluate
	 */
	public final List<NtcirPattern> extractPatterns() throws XPathExpressionException {
		final List<NtcirPattern> patterns = new ArrayList<>();
		final XPath xpath = DomDocumentHelper.namespaceAwareXpath( "t", NS_NII );
		final XPathExpression xNum = xpath.compile( "./t:num" );
		final XPathExpression xFormula = xpath.compile( "./t:query/t:formula" );
		final NonWhitespaceNodeList topicList = new NonWhitespaceNodeList(
			topics.getElementsByTagNameNS( NS_NII, "topic" ) );
		for ( final Node node : topicList ) {
			final String num = xNum.evaluate( node );
			final NonWhitespaceNodeList formulae = new NonWhitespaceNodeList(
					(NodeList) xFormula.evaluate( node, XPathConstants.NODESET ) );
			for ( final Node formula : formulae ) {
				final String id = formula.getAttributes().getNamedItem( "id" ).getTextContent();
				final Node mathMLNode = getFirstChild( formula );
				final XQueryGenerator queryGenerator = new XQueryGenerator( getFirstChild( mathMLNode ) );
				queryGenerator.setRestrictLength(restrictLength);
				patterns.add(new NtcirPattern(num, id, header + queryGenerator.toString() + footer, mathMLNode));
			}
		}
		return patterns;
	}
}
