/*
 * Created on Mar 17, 2005
 */
package xml;

import java.util.TreeMap;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author augsten
 */
abstract public class MyDefaultHandler extends DefaultHandler {

	private StringBuffer chars;
	private boolean hasChildren; // element node has at least one child
	
	public MyDefaultHandler() {
		super();
		chars = new StringBuffer();
	}
		
	/**
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String namespaceURI,
			String lName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {
		try {
			// add any text that is between elements (xml document)
			if (chars.length() != 0) {
				this.foundTextNode(chars.toString());
				chars.setLength(0);
			}			
			// if element has attributes, then the attributes are children nodes
			hasChildren = (attrs.getLength() != 0);

			String eName = lName; // element name
			if ("".equals(eName)) {
				eName = qName; // namespaceAware = false
			}
			startElement(eName);

//			for (int i = 0; i < attrs.getLength(); i++) {
//				String aName = attrs.getLocalName(i); // Attr name 
//				if ("".equals(aName)) aName = attrs.getQName(i);
//				this.foundAttrNode(aName, attrs.getValue(i));
//			}
//			
			// sort the attributes
			TreeMap sortedAttrs = new TreeMap();
			for (int i = 0; i < attrs.getLength(); i++) {
				String aName = attrs.getLocalName(i); // Attr name 
				if ("".equals(aName)) {
					aName = attrs.getQName(i);
				}
				sortedAttrs.put(aName, attrs.getValue(i));
			}
			// "output" attributes in sorted order 
			for (int i = 0; i < attrs.getLength(); i++) {
				String aName = (String)sortedAttrs.firstKey();
				String aValue = (String)sortedAttrs.get(aName);
				sortedAttrs.remove(aName);
				this.foundAttrNode(aName, aValue);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}    

	/**
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
    @Override
	public void endElement(String namespaceURI,
            String sName, // simple name
            String qName  // qualified name
           ) throws SAXException {
    	try {
    		// add any text that is between elements (xml document)
    		if (chars.length() != 0) {
    			this.foundTextNode(chars.toString());
    			chars.setLength(0);
    		} else if (!hasChildren) {
    			// this is an empty node with no attributes
    			this.foundTextNode("");
    		}
    		String eName = sName; // element name
    		if ("".equals(eName)) {
				eName = qName; // namespaceAware = false
			}
    		endElement(eName);
    		
    		// the parent node has at least this element as a child
    		hasChildren = true;
    	} catch (Exception e){
    		e.printStackTrace();
    	}
    }
        
    @Override
	public void characters(char buf[], int offset, int len) throws SAXException {
        chars.append(new String(buf, offset, len));
    }
	        
    abstract public void startElement(String elementName) throws Exception;    	    
    abstract public void endElement(String elementName) throws Exception;    
    abstract public void foundAttrNode(String name, String value) throws Exception;
    abstract public void foundTextNode(String text) throws Exception;
    
}
