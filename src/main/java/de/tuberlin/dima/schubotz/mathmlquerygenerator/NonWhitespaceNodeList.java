package de.tuberlin.dima.schubotz.mathmlquerygenerator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * from http://stackoverflow.com/questions/229310/how-to-ignore-whitespace-while-reading-a-file-to-produce-an-xml-dom
 */
class NonWhitespaceNodeList implements NodeList, Iterable<Node> {

    private final List<Node> nodes;

    public NonWhitespaceNodeList() {
        nodes = new ArrayList<>();
    }

    public NonWhitespaceNodeList(NodeList list) {
        nodes = new ArrayList<>();
        for (int i = 0; i < list.getLength(); i++) {
            if (!isWhitespaceNode(list.item(i))) {
                nodes.add(list.item(i));
            }
        }
    }

    /**
     * Checks if is whitespace node.
     *
     * @param n the n
     * @return true, if is whitespace node
     */
    private static boolean isWhitespaceNode(Node n) {
        if (n.getNodeType() == Node.TEXT_NODE) {
            final String val = n.getNodeValue();
            return val.trim().isEmpty();
        } else {
            return false;
        }
    }

    public Node getFirstElement() {
        return nodes.get(0);
    }

    public NonWhitespaceNodeList filter(String name){
        final NonWhitespaceNodeList out = new NonWhitespaceNodeList();
        for (final Node node : this) {
            if (node.getLocalName().matches(name)) {
                out.nodes.add(node);
            }
        }
        return out;
    }

    public Node getFirstChild(String name){
        return filter(name).item(0);
    }

    /* (non-Javadoc)
     * @see org.w3c.dom.NodeList#item(int)
     */
    @Override
    public Node item(int index) {
        return nodes.get(index);
    }
   /* (non-Javadoc)
      * @see org.w3c.dom.NodeList#getLength()
      */
    @Override
    public int getLength() {
        return nodes.size();
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Node> iterator() {
        return nodes.iterator();
    }

}