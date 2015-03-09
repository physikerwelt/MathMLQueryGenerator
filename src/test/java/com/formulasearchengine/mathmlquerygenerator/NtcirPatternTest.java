package com.formulasearchengine.mathmlquerygenerator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NtcirPatternTest {

    private NtcirPattern topic;

    @Before
    public void setUp() throws Exception {
        topic = (new NtcirTopicReaderTest()).getFirstTopic();
    }

    @Test
    public void testGetNum() throws Exception {
        assertEquals("NTCIR11-Math-1",topic.getNum());
    }



    @Test
    public void testGetFormulaID() throws Exception {
        assertEquals("f1.0",topic.getFormulaID());
    }

    @Test
    public void testGetxQueryExpression() throws Exception {
        assertEquals("declare default element namespace \"http://www.w3.org/1998/Math/MathML\";\n" +
                "for $m in db2-fn:xmlcolumn(\"math.math_mathml\") return\n" +
                "for $x in $m//*:apply\n" +
                "[*[1]/name() = 'gt' and *[2]/name() = 'apply' and *[2][*[1]/name() = 'times' and *[2]/name() = 'ci' and *[2][./text() = 'W'] and *[3]/name() = 'interval' and *[3][*[1]/name() = 'cn' and *[1][./text() = '2'] and *[2]/name() = 'ci' and *[2][./text() = 'k']]] and *[3]/name() = 'apply' and *[3][*[1]/name() = 'divide' and *[2]/name() = 'apply' and *[2][*[1]/name() = 'csymbol' and *[1][./text() = 'superscript'] and *[2]/name() = 'cn' and *[2][./text() = '2'] and *[3]/name() = 'ci' and *[3][./text() = 'k']] and *[3]/name() = 'apply' and *[3][*[1]/name() = 'csymbol' and *[1][./text() = 'superscript'] and *[2]/name() = 'ci' and *[2][./text() = 'k'] and *[3]/name() = 'ci' and *[3][./text() = 'ε']]]]\n" +
                "where\n" +
                "fn:count($x/*[2]/*[2]/*) = 0\n" +
                " and fn:count($x/*[2]/*[3]/*[1]/*) = 0\n" +
                " and fn:count($x/*[2]/*[3]/*[2]/*) = 0\n" +
                " and fn:count($x/*[2]/*[3]/*) = 2\n" +
                " and fn:count($x/*[2]/*) = 3\n" +
                " and fn:count($x/*[3]/*[2]/*[1]/*) = 0\n" +
                " and fn:count($x/*[3]/*[2]/*[2]/*) = 0\n" +
                " and fn:count($x/*[3]/*[2]/*[3]/*) = 0\n" +
                " and fn:count($x/*[3]/*[2]/*) = 3\n" +
                " and fn:count($x/*[3]/*[3]/*[1]/*) = 0\n" +
                " and fn:count($x/*[3]/*[3]/*[2]/*) = 0\n" +
                " and fn:count($x/*[3]/*[3]/*[3]/*) = 0\n" +
                " and fn:count($x/*[3]/*[3]/*) = 3\n" +
                " and fn:count($x/*[3]/*) = 3\n" +
                " and fn:count($x/*) = 3\n" +
                "\n" +
                "return\n" +
                "data($m/*[1]/@alttext)",topic.getxQueryExpression());
    }

    @Test
    public void testGetMathMLNode() throws Exception {
        assertEquals("http://www.w3.org/1998/Math/MathML",topic.getMathMLNode().getNamespaceURI());
    }
}