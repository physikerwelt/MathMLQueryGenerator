package com.formulasearchengine.mathmlquerygenerator;


import com.formulasearchengine.xmlhelper.DomDocumentHelper;
import com.formulasearchengine.xmlhelper.NonWhitespaceNodeList;
import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.formulasearchengine.xmlhelper.NonWhitespaceNodeList.getFirstChild;

/**
 * Converts MathML queries into XQueries.
 * The result is then wrapped around with a header and a footer (defaults to a DB2 header/footer if not given).
 * The variable $x always represents a hit, so you can refer to $x in the footer as the result node.
 * Created by Moritz Schubotz on 9/3/14.
 * Translated from http://git.wikimedia.org/blob/mediawiki%2Fextensions%2FMathSearch.git/31a80ae48d1aaa50da9103cea2e45a8dc2204b39/XQueryGenerator.php
 */
@SuppressWarnings("WeakerAccess")
public class XQueryGenerator {
	private static final Pattern ANNOTATION_XML_PATTERN = Pattern.compile( "annotation(-xml)?" );
	//Qvar map of qvar name to XPaths referenced by each qvar
	private Map<String, ArrayList<String>> qvar = new HashMap<>();
	private String relativeXPath = "";
	private String lengthConstraint = "";
	private String header = "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
			"for $m in db2-fn:xmlcolumn(\"math.math_mathml\") return\n";
	private String footer = "data($m/*[1]/@alttext)";
	private Node mainElement = null;
	private boolean restrictLength = true;

	/**
	 * Constructs a basic generator from an XML document given as a string.
	 * @param input XML Document as a string
	 */
	public XQueryGenerator( String input )
			throws IOException, SAXException, ParserConfigurationException {
		final Document xml = DomDocumentHelper.String2Doc( input );
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
	 * @return XQuery as string
	 */
	public String toString() {
		if ( mainElement == null ) {
			return null;
		}
		final String fixedConstraints = generateConstraints( mainElement, true );
		final StringBuilder qvarConstraintString = new StringBuilder();
		//Generate qvar constraint string
		//This specifies that the same qvars must refer to the same nodes, using the XQuery "=" equality
		//This is equality based on: same text, same node names, and same children by the "=" equality
		for ( Map.Entry<String, ArrayList<String>> entry : qvar.entrySet() ) {
			final StringBuilder addString = new StringBuilder();
			if ( entry.getValue().size() > 1 ) {
				final String firstEntry = entry.getValue().get( 0 );
				if ( qvarConstraintString.length() != 0 ) {
					addString.append("\n  and ");
				}
				String lastEntry = "";
				boolean newContent = false;
				//begins at second entry
				for ( final String currentEntry : entry.getValue() ) {
					if ( !currentEntry.equals( firstEntry ) ) {
						if ( !lastEntry.isEmpty() ) {
							addString.append(" and ");
						}
						addString.append("$x").append(firstEntry).append(" = $x").append(currentEntry);
						lastEntry = currentEntry;
						newContent = true;
					}
				}
				if ( newContent ) {
					qvarConstraintString.append(addString);
				}
			}
		}
		return getString( mainElement, fixedConstraints, qvarConstraintString.toString() );
	}

	/**
	 * Builds the XQuery as a string with the set header and footer, given constraint strings and the main element.
	 * @param mainElement          Node from which to build XQuery
	 * @param fixedConstraints     Constraint string for basic exact formula matching
	 * @param qvarConstraintString Constraint string for qvar matching
	 * @return XQuery as string
	 */
	public String getString( Node mainElement, String fixedConstraints, String qvarConstraintString ) {
		String out = header;
		out += "for $x in $m//*:" + getFirstChild( mainElement ).getLocalName() + "\n" +
				fixedConstraints + "\n";
		out += getConstraints( qvarConstraintString );
		out +=
				"return" + "\n" + getFooter();
		return out;
	}

	/**
	 * Appends constraint strings together
	 * @param qvarConstraintString Qvar constraint portion of query
	 * @return All constraint strings appended together
	 */
	private String getConstraints( String qvarConstraintString ) {
		String out = lengthConstraint +
				(((qvarConstraintString.length() > 0) && (lengthConstraint.length() > 0)) ? " and " : "") +
				qvarConstraintString;
		if ( out.trim().length() > 0 ) {
			return "where" + "\n" + out + "\n";
		} else {
			return "";
		}
	}

	private String generateConstraint( Node node ) {
		return generateConstraints( node, false );
	}

	/**
	 * Generates qvar map, length constraint, and returns exact match XQuery query for all child nodes of the given node.
	 * Called recursively to generate the query for the entire query document.
	 * @param node   Element from which to get children to generate constraints.
	 * @param isRoot Whether or not node should be treated as the root element of the document (the root element
	 *               is not added as a constraint here, but in getString())
	 * @return Exact match XQuery string
	 */
	private String generateConstraints( Node node, boolean isRoot ) {
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
						final String constraint = generateConstraint( child );
						if ( !constraint.isEmpty() ) {
							//This constraint appears as a child of the relative constraint above (e.g. [*1][constraint])
							out.append( "[" ).append( constraint ).append( "]");
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
				lengthConstraint += "fn:count($x" + relativeXPath + "/*) = " + childElementIndex + "\n";
			} else {
				//Add as additional constraint
				lengthConstraint += " and fn:count($x" + relativeXPath + "/*) = " + childElementIndex + "\n";
			}
		}

		if ( !relativeXPath.isEmpty() ) {
			relativeXPath = relativeXPath.substring( 0, relativeXPath.lastIndexOf( "/" ) );
		}

		return out.toString();
	}

}