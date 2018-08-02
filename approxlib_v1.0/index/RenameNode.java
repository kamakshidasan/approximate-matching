/*
 * Created on Sep 28, 2005
 */
package index;

import java.sql.SQLException;

/**
 * @author augsten
 */
public class RenameNode extends EditOperation {

	private Object tree, node;
	private String oldLabel, newLabel;
	
	/**
	 * Rename node <code>node</code> it tree <code>tree</code>.
	 * 
	 * @param tree
	 * @param node
	 * @param oldLabel label before rename operation
	 * @param newLabel label after rename operation
	 */
	public RenameNode(Object tree, Object node, String oldLabel, String newLabel) {
		this.tree = tree;
		this.node = node;
		this.oldLabel = oldLabel;
		this.newLabel = newLabel;
	}
	
	/**
	 * Overridden method.
	 *
	 * @see index.EditOperation#applyTo(index.Editable)
	 * @throws SQLException
	 */
	@Override
	public void applyTo(Editable editable) throws SQLException {
		editable.renameNode(tree, node, newLabel);
	}

	/**
	 * Overridden method.
	 *
	 * @see index.EditOperation#reverseEditOp()
	 */
	@Override
	public EditOperation reverseEditOp() {
		return new RenameNode(tree, node, newLabel, oldLabel);
	}
	
	

	/**
	 * Overridden method.
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ("ren (" + ((Integer)tree).intValue() + ","
				+ ((Integer)node).intValue() + ",'" 
				+ oldLabel + "') --> '" + newLabel + "'");
	}
}
