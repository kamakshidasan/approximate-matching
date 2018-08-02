/*
 * Created on Mar 17, 2005
 */
package xml;

import org.xml.sax.SAXException;
import intervalenc.IntervalEncForest;
import java.util.LinkedList;
import java.sql.SQLException;

/**
 * Load an XML document from a file and store it to a relational table in interval encoding.
 * 
 * The nodes are (label,value) pairs. If a node has no value (element nodes without text value),
 * then value is set to DEFAULT_TEXT_VALUE.
 * 
 * @author augsten
 */
public class IntervalEncHandler extends MyDefaultHandler {

	public static final int LFT_START_NUM = 0;
	public static final String DEFAULT_TEXT_VALUE = "";
	
	private int num;
	private LinkedList stack;
	
	private IntervalEncForest tw;
	private int treeID;
	private String text;
	
	public static void parseFromFile(String filename,
			IntervalEncForest tw,
			int treeID, 
			boolean validating, 
			boolean nameSpaceAware) {
        IntervalEncHandler handler = new IntervalEncHandler(treeID, tw);
        Parser.parseFile(filename, handler, validating, nameSpaceAware);        
	}
	
	/**
	 * The tree that represents the parsed XML document in the table tw has id treeID. 
	 *  
	 * @param treeID tree id of tree in table representing XML document
	 * @param tw where the parsed XML document should be stored
	 */	
	public IntervalEncHandler(int treeID, IntervalEncForest tw) {
		super();		
		this.tw =tw;
		this.treeID = treeID;
	}
	
	

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		try {
			tw.getInsBuff().flush();
		} catch (SQLException e) {	
			e.printStackTrace();
		}
	}
	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {
			super.startDocument();
		tw.open();
		stack = new LinkedList();
		num = LFT_START_NUM;
		text = DEFAULT_TEXT_VALUE;
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#startElement(java.lang.String)
	 */
	@Override
	public void startElement(String elementName)  {
		stack.addLast(new Integer(num));
		num++;
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#endElement(java.lang.String)
	 */
	@Override
	public void endElement(String elementName) throws SQLException {
		int lftNum = ((Integer)stack.removeLast()).intValue();
		tw.storeNode(treeID, elementName, this.text, lftNum, num);
		this.text = DEFAULT_TEXT_VALUE;
		num++;
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundAttrNode(java.lang.String, java.lang.String)
	 */
	@Override
	public void foundAttrNode(String name, String value) throws SQLException {
		tw.storeNode(treeID, name, value, num, num + 1);
		num += 2;
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundTextNode(java.lang.String)
	 */
	@Override
	public void foundTextNode(String text) throws SQLException {
		this.text = text;
	}
	
}
