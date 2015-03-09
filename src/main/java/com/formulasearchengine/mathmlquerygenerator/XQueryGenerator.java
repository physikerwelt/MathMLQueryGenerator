package com.formulasearchengine.mathmlquerygenerator;


import com.formulasearchengine.xmlhelper.NonWhitespaceNodeList;
import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.formulasearchengine.xmlhelper.NonWhitespaceNodeList.getFirstChild;

/**
 * Created by Moritz Schubotz on 9/3/14.
 * Translated from http://git.wikimedia.org/blob/mediawiki%2Fextensions%2FMathSearch.git/31a80ae48d1aaa50da9103cea2e45a8dc2204b39/XQueryGenerator.php
 */
@SuppressWarnings ("WeakerAccess")
public class XQueryGenerator {
	private final Document xml;
	private Map<String, ArrayList<String>> qvar = new HashMap<>();
	private String relativeXPath = "";
	private String lengthConstraint = "";
	private String header = "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
		"for $m in db2-fn:xmlcolumn(\"math.math_mathml\") return\n";
	private String footer = "data($m/*[1]/@alttext)";
	private Node mainElement = null;

	public boolean isRestrictLength () {
		return restrictLength;
	}

	/**
	 * If set to true a query like $x+y$ does not match $x+y+z$.
	 * @param restrictLength
	 */
	public XQueryGenerator setRestrictLength ( boolean restrictLength ) {
		this.restrictLength = restrictLength;
		return this;
	}

	private boolean restrictLength = true;

	public XQueryGenerator (Document xml) {
		this.xml = xml;
	}

	public static Node getMainElement (Document xml) {
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

	public String getFooter () {
		return footer;
	}

	public XQueryGenerator setFooter (String footer) {
		this.footer = footer;
		return this;
	}

	public String getHeader () {
		return header;
	}

	public XQueryGenerator setHeader (String header) {
		this.header = header;
		return this;
	}


	private Node getMainElement () {
		if ( mainElement == null ) {
			return getMainElement( xml );
		} else {
			return mainElement;
		}

	}

	/**
	 * Resets the current xQuery expression and sets a new main element.
	 * @param mainElement
	 */
	public void setMainElement (Node mainElement) {
		this.mainElement = mainElement;
		qvar = new HashMap<>();
		relativeXPath = "";
		lengthConstraint = "";
	}

	public String toString () {
		Node mainElement = getMainElement();
		if ( mainElement == null ) {
            return null;
        }
		String fixedConstraints = generateConstraint( mainElement, true );
		String qvarConstraintString = "";
		for ( Map.Entry<String, ArrayList<String>> entry : qvar.entrySet() ) {
			String addString = "";
			boolean newContent = false;
			if ( entry.getValue().size() > 1 ) {
				String first = entry.getValue().get( 0 );
				if ( qvarConstraintString.length() > 0 ) {
					addString += "\n  and ";
				}
				String lastSecond = "";
				for ( String second : entry.getValue() ) {
					if ( !second.equals( first ) ) {
						if ( lastSecond.length() > 0 ) {
							addString += " and ";
						}
						addString += "$x" + first + " = $x" + second;
						lastSecond = second;
						newContent = true;
					}
				}
				if ( newContent ) {
					qvarConstraintString += addString;
				}

			}

		}


		return getString( mainElement, fixedConstraints, qvarConstraintString );

	}

	public String getString (Node mainElement, String fixedConstraints, String qvarConstraintString) {
		String out = getHeader();
		out += "for $x in $m//*:" + getFirstChild( mainElement ).getLocalName() + "\n" +
			fixedConstraints + "\n";
		out += getConstraints( qvarConstraintString );
		out +=
			"return" + "\n" + getFooter();
		return out;
	}

	private String getConstraints (String qvarConstraintString) {
		String out = lengthConstraint +
			(((qvarConstraintString.length() > 0) && (lengthConstraint.length() > 0)) ? " and " : "") +
			qvarConstraintString;
		if ( out.trim().length() > 0 ) {
			return "where" + "\n" + out + "\n";
		} else {
			return "";
		}
	}

	private String generateConstraint (Node node) {
		return generateConstraint( node, false );
	}

	private String generateConstraint (Node node, boolean isRoot) {
		int i = 0;
		String out = "";
		boolean hasText = false;
		if ( node == null ) {
			return null;
		}
		NonWhitespaceNodeList nodeList = new NonWhitespaceNodeList( node.getChildNodes() );
		for ( Node child : nodeList ) {
			if ( child.getNodeName().equals( "mws:qvar" ) ) {
				i++;
				String qvarName = child.getTextContent();
				if (qvarName.equals( "" )){
					qvarName = child.getAttributes().getNamedItem( "name" ).getTextContent();
				}
				if ( qvar.containsKey( qvarName ) ) {
					qvar.get( qvarName ).add( relativeXPath + "/*[" + i + "]" );
				} else {
					qvar.put( qvarName, Lists.newArrayList( relativeXPath + "/*[" + i + "]" ) );
				}
			} else {
				if ( child.getNodeType() == Node.ELEMENT_NODE ) {
					if ( child.getLocalName().matches( "annotation(-xml)?" ) ) {
						continue;
					}
					i++;
					if ( hasText ) {
						out += " and ";
					}
					if ( !isRoot ) {
						out += "*[" + i + "]/name() = '" + child.getLocalName() + "'";
					}
					hasText = true;
					if ( child.hasChildNodes() ) {
						if ( !isRoot ) {
							relativeXPath += "/*[" + i + "]";
							out += " and *[" + i + "]";
						}
						final String constraint = generateConstraint( child );
						if ( constraint.length() > 0 ) {
							out += "[" + constraint + "]";
						}
					}

				} else if ( child.getNodeType() == Node.TEXT_NODE ) {
					out = "./text() = '" + child.getNodeValue().trim() + "'";
				}
			}
		}
		if ( !isRoot && restrictLength ) {
			if ( lengthConstraint.equals( "" )  ) {
				lengthConstraint += "fn:count($x" + relativeXPath + "/*) = " + i + "\n";
			} else {
				lengthConstraint += " and fn:count($x" + relativeXPath + "/*) = " + i + "\n";
			}
		}
		if ( relativeXPath.length() > 0 ) {
			relativeXPath = relativeXPath.substring( 0, relativeXPath.lastIndexOf( "/" ) );
		}
		return out;
	}

}