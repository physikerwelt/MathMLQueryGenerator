package de.tuberlin.dima.schubotz.mathMLQueryGenerator;


import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Moritz Schubotz on 9/3/14.
 * Translated from http://git.wikimedia.org/blob/mediawiki%2Fextensions%2FMathSearch.git/31a80ae48d1aaa50da9103cea2e45a8dc2204b39/XQueryGenerator.php
 */

@SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
public class XQueryGenerator {
    private final Map<String, ArrayList<String>> qvar = new HashMap<String, ArrayList<String>>();
    private String relativeXPath = "";
    private String lengthConstraint = "";
    private String db2ColumnName = "math.math_mathml";

    public String getDb2ColumnName() {
        return db2ColumnName;
    }

    public void setDb2ColumnName(String db2ColumnName) {
        this.db2ColumnName = db2ColumnName;
    }

    private final Document xml;

    XQueryGenerator(Document xml) {
        this.xml = xml;
    }

    private NdLst getMainElements() {
        return new NdLst(xml.getElementsByTagName("mws:expr"));
    }

    public String toString() {
        String fixedConstraints = generateConstraint(getMainElements().item(0), true);
        String qvarConstraintString = "";
        for (Map.Entry<String, ArrayList<String>> entry : qvar.entrySet()) {
            String addString = "";
            boolean newContent = false;
            if (entry.getValue().size() > 1) {
                String first = entry.getValue().get(0);
                if (qvarConstraintString.length() > 0) {
                    addString += "\n  and ";
                }
                String lastSecond = "";
                for (String second : entry.getValue()) {
                    if (!second.equals(first)) {
                        if (lastSecond.length() > 0) {
                            addString += " and ";
                        }
                        addString += "$x" + first + " = $x" + second;
                        lastSecond = second;
                        newContent = true;
                    }
                }
                if (newContent) {
                    qvarConstraintString += addString;
                }

            }

        }


        return getHeader() + "for $x in $m//*:" +
                (new NdLst(getMainElements().item(0).getChildNodes())).item(0).getLocalName() + "\n" +
                fixedConstraints + "\n" +
                "where" + "\n" +
                lengthConstraint +
                (((qvarConstraintString.length() > 0) && (lengthConstraint.length() > 0)) ? " and " : "") +
                qvarConstraintString + "\n" +
                "return" + "\n" + getFooter();

    }

    private String generateConstraint(Node node) {
        return generateConstraint(node, false);
    }

    private String generateConstraint(Node node, boolean isRoot) {
        int i = 0;
        String out = "";
        boolean hasText = false;
        NdLst nodeList = new NdLst(node.getChildNodes());
        for (Node child : nodeList) {
            if (child.getNodeName().equals("mws:qvar")) {
                i++;
                String qvarName = child.getTextContent();
                if (qvar.containsKey(qvarName)) {
                    qvar.get(qvarName).add(relativeXPath + "/*[" + i + "]");
                } else {
                    qvar.put(qvarName, Lists.newArrayList(relativeXPath + "/*[" + i + "]"));
                }
            } else {
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    i++;
                    if (hasText) {
                        out += " and ";
                    }
                    if (!isRoot) {
                        out += "*[" + i + "]/name() = '" + child.getLocalName() + "'";
                    }
                    hasText = true;
                    if (child.hasChildNodes()) {
                        if (!isRoot) {
                            relativeXPath += "/*[" + i + "]";
                            out += " and *[" + i + "]";
                        }
                        out += "[" + generateConstraint(child) + "]";
                    }

                } else if (child.getNodeType() == Node.TEXT_NODE) {
                    out = "./text() = '" + child.getNodeValue() + "'";
                }
            }
        }
        if (!isRoot) {
            if (lengthConstraint.equals("")) {
                lengthConstraint += "fn:count($x" + relativeXPath + "/*) = " + i + "\n";
            } else {
                lengthConstraint += " and fn:count($x" + relativeXPath + "/*) = " + i + "\n";
            }
        }
        if (relativeXPath.length() > 0) {
            relativeXPath = relativeXPath.substring(0, relativeXPath.lastIndexOf("/"));
        }
        return out;
    }

    protected String getHeader() {
        return "declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
                "for $m in db2-fn:xmlcolumn(\"" + db2ColumnName + "\") return\n";
    }

    @SuppressWarnings("SameReturnValue")
    protected String getFooter() {
        return "data($m/*[1]/@alttext)";
    }
}