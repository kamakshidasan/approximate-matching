/*
 * Created on Mar 17, 2005
 */
package xml;

import tree.LblValTree;
import tree.Node;
import org.xml.sax.SAXException;

/**
 * @author augsten
 */
public class LblValTreeHandler extends MyDefaultHandler {

	private LblValTree tree;
	private boolean ignoreWhiteSpace;
	
	public LblValTreeHandler() {
		this(true);
	}
	
	/**
	 * If {@link #trimWhiteSpace} is true, leading and trailing whitespace characters
	 * are removed. Whitespaces have codecs smaller or equal to "\u0020" (=blank).
	 * 
	 * @param trimWhiteSpace trim white space from element text values
	 */
	public LblValTreeHandler(boolean ignoreWhiteSpace) {
		super();
		this.ignoreWhiteSpace = ignoreWhiteSpace;
	}

	public static LblValTree parseFromFile(String filename) {
		return parseFromFile(filename, true, true);
	}
	
	public static LblValTree parseFromFile(String filename, 
			boolean validating, 
			boolean nameSpaceAware) {
        LblValTreeHandler handler = new LblValTreeHandler();
        Parser.parseFile(filename, handler, validating, nameSpaceAware);        
		return handler.tree;
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {
		tree = new LblValTree("dummy tag", "",  Node.NO_TREE_ID);
	}
		
	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	@Override
	public void endDocument() throws SAXException {
		if (tree.isLeaf()) {
			tree = null;
		} else {
			// remove dummy root
			LblValTree dummy_root = (LblValTree)tree.getRoot();
			tree = (LblValTree)dummy_root.getFirstChild(); 
		    tree.removeFromParent();
		}
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#startElement()
	 */
	@Override
	public void startElement(String elementName) {
		tree.add(new LblValTree(elementName, "", tree.getTreeID()));
		tree = (LblValTree)tree.getLastChild();
	}
    	
	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#endElement()
	 */
	@Override
	public void endElement(String elementName) {
		tree = (LblValTree)tree.getParent();
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundAttrNode(java.lang.String, java.lang.String)
	 */
	@Override
	public void foundAttrNode(String name, String value) {
		LblValTree attrNode = new LblValTree(name, value, tree.getTreeID());
		tree.add(attrNode);
     }
	
	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundTextNode(java.lang.String)
	 */
	@Override
	public void foundTextNode(String text) {
		if (ignoreWhiteSpace) text = text.trim();		
		if (text.length() != 0) {
			LblValTree tagNode = tree;
			if (tagNode.getValue().length() == 0) {
				tagNode.setValue(text);
			} else {
				throw new RuntimeException(
						"Text value '" + text + "' is too much. " +
						"LblValTree allows only one text value per XML element. " + 
				"Use 'ignoreWhiteSpace' to avoid that white space is counted as a text value.");
			}
		}
	}
	    
}
