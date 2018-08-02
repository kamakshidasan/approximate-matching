/*
 * Created on Mar 17, 2005
 */
package tasmTED;

import java.util.LinkedList;

import xml.MyDefaultHandler;
import xml.Parser;

/**
 * @author augsten
 */
public class PostorderQueueHandler extends MyDefaultHandler {

	PostorderQueue ted;

	LinkedList<Integer> stack;

	boolean omitEmptyText;

	/**
	 * 
	 */
	public PostorderQueueHandler(PostorderQueue ted, boolean omitEmptyText) {
		super();
		this.ted = ted;
		this.stack = new LinkedList<Integer>();
		this.omitEmptyText = omitEmptyText;

	}

	public static void parseFromFile(String filename, boolean validating,
			boolean nameSpaceAware, boolean omitEmptyText, PostorderQueue output) {
		
		PostorderQueueHandler handler = 
			new PostorderQueueHandler(output, omitEmptyText);
		Parser.parseFile(filename, handler, validating, nameSpaceAware);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see xml.MyDefaultHandler#startElement(java.lang.String)
	 */
	@Override
	public void startElement(String elementName) {
		stack.addFirst(1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see xml.MyDefaultHandler#endElement(java.lang.String)
	 */
	@Override
	public void endElement(String elementName) {
		int size = stack.pop();
		ted.append(elementName, size);
		if (!stack.isEmpty()) {
			stack.addFirst(stack.pop() + size);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see xml.MyDefaultHandler#foundAttrNode(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public void foundAttrNode(String name, String value) {
		ted.append(value, 1);
		ted.append(name, 2);
		stack.addFirst(stack.pop() + 2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see xml.MyDefaultHandler#foundTextNode(java.lang.String)
	 */
	@Override
	public void foundTextNode(String text) {
		if (text.trim().isEmpty() && this.omitEmptyText) {
			return;
		}
		ted.append(text, 1);
		stack.addFirst(stack.pop() + 1);
	}

}
