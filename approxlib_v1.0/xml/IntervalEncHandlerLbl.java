/*
 * Created on Mar 17, 2005
 */
package xml;

import org.xml.sax.SAXException;
import intervalenc.IntervalEncForest;
import java.util.LinkedList;
import java.sql.SQLException;

/**
 * Load an XML document from a file and store it to a relational table.
 * 
 * @author augsten
 */
public class IntervalEncHandlerLbl extends MyDefaultHandler {

	public static final int LFT_START_NUM = 0;
	
	private int num;
	private LinkedList stack;
	
	private IntervalEncForest tw;
	private int treeID;
	
	public static void parseFromFile(String filename,
			IntervalEncForest tw,
			int treeID, 
			boolean validating, 
			boolean nameSpaceAware) {
        IntervalEncHandlerLbl handler = new IntervalEncHandlerLbl(treeID, tw);
        Parser.parseFile(filename, handler, validating, nameSpaceAware);        
	}
	
	/**
	 * The tree that represents the parsed XML document in the table tw has id treeID. 
	 *  
	 * @param treeID tree id of tree in table representing XML document
	 * @param tw where the parsed XML document should be stored
	 */	
	public IntervalEncHandlerLbl(int treeID, IntervalEncForest tw) {
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
		tw.storeNode(treeID, elementName, null, lftNum, num);
		num++;
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundAttrNode(java.lang.String, java.lang.String)
	 */
	@Override
	public void foundAttrNode(String name, String value) throws SQLException {
		tw.storeNode(treeID, name, null, num, num + 3);
		tw.storeNode(treeID, value, null, num + 1, num + 2);
		num += 4;
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundTextNode(java.lang.String)
	 */
	@Override
	public void foundTextNode(String text) throws SQLException {
		tw.storeNode(treeID, text, null, num, num + 1);
		num += 2;
	}
}
