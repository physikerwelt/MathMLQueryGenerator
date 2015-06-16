package com.formulasearchengine.mathmlquerygenerator;


import com.formulasearchengine.mathosphere.utils.XMLHelper;
import com.formulasearchengine.xmlhelper.NonWhitespaceNodeList;
import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static com.formulasearchengine.xmlhelper.NonWhitespaceNodeList.getFirstChild;

/**
 * Converts MathML queries into XQueries.
 * The result is then wrapped around with a header and a footer (defaults to a DB2 header/footer if not given).
 * The variable $x always represents a hit, so you can refer to $x in the footer as the result node.
 * If addQvarMap is turned on, the variable $q always represents a map of qvars to their respective formula ID,
 * so you can refer to $q in the footer to return qvar results.
 * Created by Moritz Schubotz on 9/3/14.
 * Translated from http://git.wikimedia.org/blob/mediawiki%2Fextensions%2FMathSearch.git/31a80ae48d1aaa50da9103cea2e45a8dc2204b39/XQueryGenerator.php
 */
@SuppressWarnings("WeakerAccess")
public class XQueryGenerator {
	private static final Pattern ANNOTATION_XML_PATTERN = Pattern.compile( "annotation(-xml)?" );
	//Qvar map of qvar name to XPaths referenced by each qvar
	private Map<String, ArrayList<String>> qvar = new LinkedHashMap<>();
	private String relativeXPath = "";
	private String exactMatchXQuery = "";
	private String lengthConstraint = "";
	private String qvarConstraint = "";
	private String qvarMapVariable = "";
	private String header = "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
			"for $m in db2-fn:xmlcolumn(\"math.math_mathml\") return\n";
	private String footer = "data($m/*[1]/@alttext)";
	private Node mainElement = null;
	private boolean restrictLength = true;
	private boolean addQvarMap = true;

	/**
	 * Constructs a basic generator from an XML document given as a string.
	 * @param input XML Document as a string
	 */
	public XQueryGenerator( String input )
			throws IOException, SAXException, ParserConfigurationException {
		final Document xml = XMLHelper.String2Doc( input, true );
		this.mainElement = getMainElement( xml );
	}

	/**
	 * Constructs a generator from a Document XML object.
	 * @param xml Document XML object
	 */
	public XQueryGenerator( Document xml ) {
		this.mainElement = getMainElement( xml );
	}

	public boolean isRestrictLength() {
		return restrictLength;
	}

	/**
	 * If set to true a query like $x+y$ does not match $x+y+z$.
	 * @param restrictLength
	 */
	public XQueryGenerator setRestrictLength( boolean restrictLength ) {
		this.restrictLength = restrictLength;
		return this;
	}

	public boolean isAddQvarMap() {
		return addQvarMap;
	}
	/**
	 * Determines whether or not the $q variable is generated with a map of qvar names to their respective xml:id
	 */
	public XQueryGenerator setAddQvarMap( boolean addQvarMap ) {
		this.addQvarMap = addQvarMap;
		return this;
	}

	/**
	 * Returns the main element for which to begin generating the XQuery
	 * @param xml XML Document to find main element of
	 * @return Node for main element
	 */
	public static Node getMainElement( Document xml ) {
		// Try to get main mws:expr first
		NodeList expr = xml.getElementsByTagName( "mws:expr" );

		if ( expr.getLength() > 0 ) {
			return new NonWhitespaceNodeList( expr ).item( 0 );
		}
		// if that fails try to get content MathML from an annotation tag
		expr = xml.getElementsByTagName( "annotation-xml" );
		for ( Node node : new NonWhitespaceNodeList( expr ) ) {
			if ( node.hasAttributes() &&
					node.getAttributes().getNamedItem( "encoding" ).getNodeValue().equals( "MathML-Content" ) ) {
				return node;
			}
		}
		// if that fails too interprete content of first semantic element as content MathML
		expr = xml.getElementsByTagNameNS( "*", "semantics" );
		if ( expr.getLength() > 0 ) {
			return new NonWhitespaceNodeList( expr ).item( 0 );
		}
		// if that fails too interprete content of root MathML element as content MathML
		expr = xml.getElementsByTagName( "math" );
		if ( expr.getLength() > 0 ) {
			return new NonWhitespaceNodeList( expr ).item( 0 );
		}

		return null;
	}

	public String getFooter() {
		return footer;
	}

	public XQueryGenerator setFooter( String footer ) {
		this.footer = footer;
		return this;
	}

	public String getHeader() {
		return header;
	}

	public XQueryGenerator setHeader( String header ) {
		this.header = header;
		return this;
	}

	/**
	 * Resets the current xQuery expression and sets a new main element.
	 * @param mainElement
	 */
	public void setMainElement( Node mainElement ) {
		this.mainElement = mainElement;
		qvar = new HashMap<>();
		relativeXPath = "";
		lengthConstraint = "";
	}

	/**
	 * Generates the constraints of the XQuery and then builds the XQuery and returns it as a string
	 * @return XQuery as string. Returns null if no main element set.
	 */
	public String toString() {
		if ( mainElement == null ) {
			return null;
		}
		exactMatchXQuery = generateSimpleConstraints( mainElement, true );
		generateQvarConstraints();

		if (addQvarMap) {
			return getString( mainElement, exactMatchXQuery, lengthConstraint, qvarConstraint, qvarMapVariable, header, footer );
		} else {
			return getString( mainElement, exactMatchXQuery, lengthConstraint, qvarConstraint, "", header, footer );
		}
	}

	/**
	 * Builds the XQuery as a string given constraint strings, header, footer, qvar map, and the main element.
	 * @param mainElement          Node from which to build XQuery
	 * @param fixedConstraints     Constraint string for basic exact formula matching
	 * @param lengthConstraint     Constraint string for length of variables
	 * @param qvarConstraintString Constraint string for qvar matching
	 * @param qvarMapVariable      Qvar map variable
	 * @param header               Header
	 * @param footer               Footer
	 * @return XQuery as string
	 */
	public static String getString( Node mainElement, String fixedConstraints, String lengthConstraint,
									String qvarConstraintString, String qvarMapVariable, String header, String footer ) {
		String out = header;
		out += "for $x in $m//*:" + getFirstChild( mainElement ).getLocalName() + "\n" +
				fixedConstraints;
		if ( !lengthConstraint.isEmpty() || !qvarConstraintString.isEmpty() ) {
			out += "\n" + "where" + "\n";
			if ( lengthConstraint.isEmpty() ) {
				out += qvarConstraintString;
			} else {
				out += lengthConstraint + (qvarConstraintString.isEmpty() ? "" : "\n and " + qvarConstraintString);
			}
		}
		if (!qvarMapVariable.isEmpty()) {
			out += "\n" + qvarMapVariable;
		}
		out += "\n" + "\n" + "return" + "\n" + footer;
		return out;
	}

	/**
	 * Uses the qvar map to generate a XQuery string containing qvar constraints,
	 * and the qvar map variable which maps qvar names to their respective formula ID's in the result.
	 */
	private void generateQvarConstraints() {
		final StringBuilder qvarConstrBuilder = new StringBuilder();
		final StringBuilder qvarMapStrBuilder = new StringBuilder();
		final Iterator<Map.Entry<String, ArrayList<String>>> entryIterator = qvar.entrySet().iterator();
		if ( entryIterator.hasNext() ) {
			qvarMapStrBuilder.append( "let $q := map {" );

			while ( entryIterator.hasNext() ) {
				final Map.Entry<String, ArrayList<String>> currentEntry = entryIterator.next();

				final Iterator<String> valueIterator = currentEntry.getValue().iterator();
				final String firstValue = valueIterator.next();

				qvarMapStrBuilder.append( '"' ).append( currentEntry.getKey() ).append( '"' )
						.append( " : (data($x" ).append( firstValue ).append( "/@xml:id)" );

				//check if there are additional values that we need to constrain
				if ( valueIterator.hasNext() ) {
					if ( qvarConstrBuilder.length() > 0 ) {
						//only add beginning and if it's an additional constraint in the aggregate qvar string
						qvarConstrBuilder.append( "\n and " );
					}
					while ( valueIterator.hasNext() ) {
						//process second value onwards
						final String currentValue = valueIterator.next();
						qvarMapStrBuilder.append( ",data($x" ).append( currentValue ).append( "/@xml-id)" );
						//These constraints specify that the same qvars must refer to the same nodes,
						//using the XQuery "=" equality
						//This is equality based on: same text, same node names, and same children nodes
						qvarConstrBuilder.append( "$x" ).append( firstValue ).append( " = $x" ).append( currentValue );
						if ( valueIterator.hasNext() ) {
							qvarConstrBuilder.append( " and " );
						}
					}
				}
				qvarMapStrBuilder.append( ')' );
				if ( entryIterator.hasNext() ) {
					qvarMapStrBuilder.append( ',' );
				}
			}
			qvarMapStrBuilder.append( '}' );
		}
		qvarMapVariable = qvarMapStrBuilder.toString();
		qvarConstraint = qvarConstrBuilder.toString();
	}


	private String generateSimpleConstraints( Node node ) {
		return generateSimpleConstraints( node, false );
	}

	/**
	 * Generates qvar map, length constraint, and returns exact match XQuery query for all child nodes of the given node.
	 * Called recursively to generate the query for the entire query document.
	 * @param node   Element from which to get children to generate constraints.
	 * @param isRoot Whether or not node should be treated as the root element of the document (the root element
	 *               is not added as a constraint here, but in getString())
	 * @return Exact match XQuery string
	 */
	private String generateSimpleConstraints( Node node, boolean isRoot ) {
		//Index of child node
		int childElementIndex = 0;
		final StringBuilder out = new StringBuilder();
		boolean queryHasText = false;
		final NonWhitespaceNodeList nodeList = new NonWhitespaceNodeList( node.getChildNodes() );

		for ( final Node child : nodeList ) {
			if ( child.getNodeType() == Node.ELEMENT_NODE ) {
				//If an element node and not an attribute or text node, add to xquery and increment index
				childElementIndex++;

				if ( child.getNodeName().equals( "mws:qvar" ) ) {
					//If qvar, add to qvar map
					String qvarName = child.getTextContent();
					if ( qvarName.isEmpty() ) {
						qvarName = child.getAttributes().getNamedItem( "name" ).getTextContent();
					}
					if ( qvar.containsKey( qvarName ) ) {
						qvar.get( qvarName ).add( relativeXPath + "/*[" + childElementIndex + "]" );
					} else {
						qvar.put( qvarName, Lists.newArrayList( relativeXPath + "/*[" + childElementIndex + "]" ) );
					}
				} else if ( ANNOTATION_XML_PATTERN.matcher( child.getLocalName() ).matches() ) {
					//Ignore annotations and presentation mathml
				} else {
					if ( queryHasText ) {
						//Add another condition on top of existing condition in query
						out.append( " and " );
					} else {
						queryHasText = true;
					}

					//The first direct child of the root element is added as a constraint in getString()
					//so ignore it here
					if ( !isRoot ) {
						//Add constraint for current child element
						out.append( "*[" ).append( childElementIndex ).append( "]/name() = '" ).
								append( child.getLocalName() ).append( "'" );
					}
					if ( child.hasChildNodes() ) {
						if ( !isRoot ) {
							relativeXPath += "/*[" + childElementIndex + "]";
							//Add relative constraint so this can be recursively called
							out.append( " and *[" ).append( childElementIndex ).append( "]" );
						}
						final String constraint = generateSimpleConstraints( child );
						if ( !constraint.isEmpty() ) {
							//This constraint appears as a child of the relative constraint above (e.g. [*1][constraint])
							out.append( "[" ).append( constraint ).append( "]" );
						}
					}
				}
			} else if ( child.getNodeType() == Node.TEXT_NODE ) {
				//Text nodes are always leaf nodes
				out.append( "./text() = '" ).append( child.getNodeValue().trim() ).append( "'" );
			}
		}//for child : nodelist

		if ( !isRoot && restrictLength ) {
			if ( lengthConstraint.isEmpty() ) {
				//Initialize constraint
				lengthConstraint += "fn:count($x" + relativeXPath + "/*) = " + childElementIndex;
			} else {
				//Add as additional constraint
				lengthConstraint += "\n" + " and fn:count($x" + relativeXPath + "/*) = " + childElementIndex;
			}
		}

		if ( !relativeXPath.isEmpty() ) {
			relativeXPath = relativeXPath.substring( 0, relativeXPath.lastIndexOf( "/" ) );
		}

		return out.toString();
	}

}