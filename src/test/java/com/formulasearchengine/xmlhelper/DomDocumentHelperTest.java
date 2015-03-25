package com.formulasearchengine.xmlhelper;

import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

public class DomDocumentHelperTest {

	@Test
	public void testGetDocumentBuilderFactory() throws Exception {
		DocumentBuilderFactory df = DomDocumentHelper.getDocumentBuilderFactory( true );
		InputStream is = getClass().getClassLoader().getResourceAsStream(
			"com/formulasearchengine/mathmlquerygenerator/cmml/q1.xml" );
		//is.setEncoding( "UTF-8" );
		df.newDocumentBuilder().parse( is );
	}
}