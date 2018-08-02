/*
 * Created on Mar 17, 2005
 */
package xml;

import tree.LblTree;
import tree.Node;
import org.xml.sax.SAXException;

/**
 * @author augsten
 */
public class LblTreeHandler extends MyDefaultHandler {

	private LblTree tree;
	private boolean trimWhiteSpace;
	private boolean omitEmptyValue;

	
	/**
	 * Default:
	 * <ul>
	 * <li> {@link #trimWhiteSpace}=true
	 * <li> {@link #omitEmptyValue}=true
	 * </ul>
	 */
	public LblTreeHandler() {
		this(true, true);
	}
	
	/**
	 * If {@link #trimWhiteSpace} is true, leading and trailing whitespace characters
	 * are removed. Whitespaces have codecs smaller or equal to "\u0020" (=blank).
	 * <p>
	 * If {@link #omitEmptyValue} is true, empty text values between elements are not
	 * transformed into nodes but are ignored. Only nodes of empty elements have a child
	 * with an empty label. 
	 * 
	 * @param trimWhiteSpace trim white space from element text values
	 * @param omitEmptyValue omit empty text values between elements
	 */
	public LblTreeHandler(boolean trimWhiteSpace, boolean omitEmptyValue) {
		super();
		this.trimWhiteSpace = trimWhiteSpace;
		this.omitEmptyValue = omitEmptyValue;
	}


	public static LblTree parseFromFile(String filename) {
		return parseFromFile(filename, true, true);
	}
	
	public static LblTree parseFromFile(String filename, 
			boolean validating, 
			boolean nameSpaceAware) {
        LblTreeHandler handler = new LblTreeHandler();
        Parser.parseFile(filename, handler, validating, nameSpaceAware);        
		return handler.tree;
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {
		tree = new LblTree("dummy_root", Node.NO_TREE_ID);
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
			LblTree dummy_root = (LblTree)tree.getRoot();
			tree = (LblTree)dummy_root.getFirstChild(); 
		    tree.removeFromParent();
		}
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#startElement(java.lang.String)
	 */
	@Override
	public void startElement(String elementName) {
		tree.add(new LblTree(elementName, tree.getTreeID()));
		tree = (LblTree)tree.getLastChild();
	}
    	
	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#endElement()
	 */
	@Override
	public void endElement(String elementName) {
		if (tree.isLeaf()) {
			tree.add(new LblTree("", tree.getTreeID()));
		}
		tree = (LblTree)tree.getParent();
	}

	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundAttrNode(java.lang.String, java.lang.String)
	 */
	@Override
	public void foundAttrNode(String name, String value) {
		LblTree attrNode = new LblTree(name, tree.getTreeID());
		attrNode.add(new LblTree(value, tree.getTreeID()));
		tree.add(attrNode);
     }
	
	/* (non-Javadoc)
	 * @see xml.MyDefaultHandler#foundTextNode(java.lang.String)
	 */
	@Override
	public void foundTextNode(String text) {
		if (trimWhiteSpace) text = text.trim();
		if (!omitEmptyValue || (text.length() > 0)) {
			tree.add(new LblTree(text, tree.getTreeID()));
		}
	}
	    
}
