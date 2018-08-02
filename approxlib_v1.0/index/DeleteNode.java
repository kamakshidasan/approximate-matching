/*
 * Created on Sep 28, 2005
 */
package index;

import java.sql.SQLException;

/**
 * @author augsten
 */
public class DeleteNode extends EditOperation {
	
	private Object tree, node, parent;
	String label;
	private int sibPos, fanout;
	
	/**
	 * Delete node <code>node</code> from tree <code>tree</code>.
	 * 
	 * @param tree 
	 * @param node 
	 * @param label label of node
	 * @param parent parent of <code>node</code> in <code>tree</code>
	 * @param sibPos <code>node</code> is the <code>sibPos</code>-th child of <code>parent</code> in <code>tree</code> 
	 * @param fanout <code>node</code> has <code>fanout</code> children in <code>tree</code>
	 */
	public DeleteNode(Object tree, Object node, String label, Object parent, 
			int sibPos, int fanout) {
		this.tree = tree;
		this.node = node;
		this.label = label;
		this.parent = parent;
		this.fanout = fanout;
		this.sibPos = sibPos;
	}

	/**
	 * Overridden method.
	 *
	 * @see index.EditOperation#applyTo(index.Editable)
	 */
	@Override
	public void applyTo(Editable editable) throws SQLException {
		editable.deleteNode(tree, node);
	}

	/**
	 * Overridden method.
	 *
	 * @see index.EditOperation#reverseEditOp()
	 */
	@Override
	public EditOperation reverseEditOp() {
		return new InsertNode(tree, node, label, parent, sibPos, fanout);
	}
	
	

	/**
	 * Overridden method.
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ("del (" + ((Integer)tree).intValue() + ","
				+ ((Integer)node).intValue() + ",'" + label + "')"); 
	}
}
