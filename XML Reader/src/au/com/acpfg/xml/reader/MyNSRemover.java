package au.com.acpfg.xml.reader;

import java.util.ArrayList;

import javax.xml.stream.events.Attribute;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import com.sun.org.apache.xerces.internal.util.XMLAttributesImpl;
import com.sun.org.apache.xerces.internal.xni.QName;

public class MyNSRemover extends XMLFilterImpl {
	
	/** 
	 * Default constructor
	 */
	public MyNSRemover(XMLReader parent) {
		super(parent);		
	}
	
	@Override
	public void endPrefixMapping(String pfx) {
		// does not call superclass - strip NS
	}
	
	@Override
	public void startPrefixMapping(String pfx, String key) {
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement("", localName, "");
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		super.startElement("", localName, qName, atts);
	}
	
	@Override
	public void characters(char[] s, int a1, int a2) throws SAXException {
		super.characters(s, a1, a2);
	}

}
