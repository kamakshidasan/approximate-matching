/*
 * Created on Mar 17, 2005
 */
package xml;

import java.io.Writer;
import java.io.StringWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * @author augsten
 */
public class BraceFormatHandler extends MyDefaultHandler {

	Writer out;
	
	/**
	 * 
	 */
	public BraceFormatHandler(Writer out) {
		super();
		this.out = out;
	}
	
	public static String parseFromFile(String filename, 
			boolean validating, 
			boolean nameSpaceAware) {
		StringWriter writer = new StringWriter();
        BraceFormatHandler handler = new BraceFormatHandler(writer);
        Parser.parseFile(filename, handler, validating, nameSpaceAware);
		return handler.out.toString();
	}

	public static void parseFromFile(String infile, String outfile, 
			boolean validating, 
			boolean nameSpaceAware) throws IOException {
		FileWriter writer = new FileWriter(outfile);
        BraceFormatHandler handler = new BraceFormatHandler(writer);
        Parser.parseFile(infile, handler, validating, nameSpaceAware);        
	}
	
	
	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#startElement(java.lang.String)
	 */
	@Override
	public void startElement(String elementName) {
		try{
			out.write("{" + elementName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#endElement(java.lang.String)
	 */
	@Override
	public void endElement(String elementName) {
		try{
			out.write("}");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundAttrNode(java.lang.String, java.lang.String)
	 */
	@Override
	public void foundAttrNode(String name, String value) {
		try{
			out.write("{" + name + "{" + value + "}}");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundTextNode(java.lang.String)
	 */
	@Override
	public void foundTextNode(String text) {
		try{
			out.write("{" + text + "}");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		try {
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	 
	
}
