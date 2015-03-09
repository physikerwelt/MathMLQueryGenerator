package com.formulasearchengine.xmlhelper;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

/**
 * Created by Moritz Schubotz on 3/10/15.
 */
public class DomDocumentHelper {
	private DomDocumentHelper() {
	}

	public static DocumentBuilderFactory getDocumentBuilderFactory() {
		final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware( true );
		return documentBuilderFactory;
	}

	public static XPath namespaceAwareXpath( final String prefix, final String nsURI ) {
		final XPathFactory xPathfactory = XPathFactory.newInstance();
		final XPath xpath = xPathfactory.newXPath();
		final NamespaceContext ctx = new NamespaceContext() {
			@Override
			public String getNamespaceURI( String aPrefix ) {
				if ( aPrefix.equals( prefix ) ) {
					return nsURI;
				}
				throw new IllegalArgumentException(aPrefix);
			}

			@Override
			public Iterator getPrefixes( String val ) {
				throw new UnsupportedOperationException();
			}

			@Override
			public String getPrefix( String uri ) {
				throw new UnsupportedOperationException();
			}
		};
		xpath.setNamespaceContext( ctx );
		return xpath;
	}

	/**
	 * Helper program: Transforms a String to a XML Document.
	 *
	 * @param InputXMLString the input xml string
	 * @return parsed document
	 * @throws javax.xml.parsers.ParserConfigurationException the parser configuration exception
	 * @throws java.io.IOException                  Signals that an I/O exception has occurred.
	 */
	public static Document String2Doc( String InputXMLString )
		throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilder builder = getDocumentBuilderFactory().newDocumentBuilder();
		InputSource is = new InputSource( new StringReader( InputXMLString ) );
		is.setEncoding( "UTF-8" );
		return builder.parse( is );
	}
}
