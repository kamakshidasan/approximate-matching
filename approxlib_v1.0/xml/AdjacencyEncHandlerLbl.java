/*
 * Created on Jun 13, 2005
 */
package xml;

import java.sql.SQLException;
import java.util.LinkedList;

import org.xml.sax.SAXException;

import adjacencyenc.AdjacencyEncForest;


/**
 * @author augsten
 */
public class AdjacencyEncHandlerLbl extends xml.MyDefaultHandler {
	
	public static final int ROOT_ID = 0;
	
	private int nodeID;
	private static final int NO_NODE = -1;
	private int parentID = NO_NODE;
	private int sibPos = 1;
	private LinkedList sibPosStack;
	private LinkedList parentIDStack;
	
	private AdjacencyEncForest tw;
	private int treeID;	
	
	public static void parseFromFile(String filename,
			AdjacencyEncForest tw,
			int treeID, 
			boolean validating, 
			boolean nameSpaceAware) {
        AdjacencyEncHandlerLbl handler = new AdjacencyEncHandlerLbl(treeID, tw);
        Parser.parseFile(filename, handler, validating, nameSpaceAware);        
	}

	/**
	 * The tree that represents the parsed XML document in the table tw has id treeID. 
	 *  
	 * @param treeID tree id of tree in table representing XML document
	 * @param tw where the parsed XML document should be stored
	 */	
	public AdjacencyEncHandlerLbl(int treeID, AdjacencyEncForest tw) {
		super();		
		this.tw =tw;
		this.treeID = treeID;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	@Override
	public void endDocument() throws SAXException {
		try {
			tw.getInsBuff().flush();
		} catch (SQLException e) {	
			e.printStackTrace();
		}
		super.endDocument();		
	}
	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		this.nodeID = ROOT_ID;
		tw.open();
		sibPosStack= new LinkedList();
		parentIDStack= new LinkedList();
	}
	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#startElement(java.lang.String)
	 */
	@Override
	public void startElement(String elementName) throws Exception {		
		tw.storeNode(this.treeID, this.nodeID, this.sibPos, this.parentID, elementName, null);
		sibPosStack.add(this.sibPos);
		parentIDStack.add(this.parentID);
		this.parentID = this.nodeID;
		this.sibPos = 1;
		this.nodeID += 1;
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#endElement(java.lang.String)
	 */
	@Override
	public void endElement(String elementName) throws Exception {
		this.sibPos = (Integer)sibPosStack.removeLast() + 1;
		this.parentID = (Integer)parentIDStack.removeLast();
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundAttrNode(java.lang.String, java.lang.String)
	 */
	@Override
	public void foundAttrNode(String name, String value) throws Exception {
		tw.storeNode(this.treeID, this.nodeID, this.sibPos, this.parentID, name, null);
		tw.storeNode(this.treeID, this.nodeID + 1, 1, this.nodeID, value, null);
		this.nodeID += 2;
		this.sibPos += 1;
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundTextNode(java.lang.String)
	 */
	@Override
	public void foundTextNode(String text) throws Exception {
		tw.storeNode(this.treeID, this.nodeID, this.sibPos, this.parentID, text, null);
		this.nodeID += 1;
		this.sibPos += 1;
	}

}
