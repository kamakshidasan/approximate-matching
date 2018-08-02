/*
 * Created on Sep 28, 2005
 */
package index;

import java.sql.SQLException;

/**
 * @author augsten
 */
public class InsertNode extends EditOperation {
	
	private Object tree, newNode, parent;
	private String newLabel;
	private int k, m;

	/**
	 * Insert node <code>node</code> (not in <code>tree</code>) as child of <code>parent</code> 
	 * at position <code>k</code> substituting <code>m</code> consecuteive children of <code>parent</code> 
	 * (<code>c<sub>k</sub>...c<sub>k+m-1</sub></code>, where c<sub>i</sub> is the <code>i</code>-th
	 * child of <code>parent</code>. 
	 * 
	 * @param tree 
	 * @param newNode
	 * @param newLabel label of newNode 
	 * @param parent 
	 * @param k 
	 * @param m  
	 */
	public InsertNode(Object tree, Object newNode, String newLabel, Object parent, int k, int m) {
		this.tree = tree;
		this.newNode = newNode;
		this.newLabel = newLabel;
		this.parent = parent;
		this.k = k;
		this.m = m;
	}
	
	/**
	 * Overridden method.
	 *
	 * @see index.EditOperation#applyTo(index.Editable)
	 * @throws SQLException
	 */
	@Override
	public void applyTo(Editable editable) throws SQLException {
		editable.insertNode(tree, newNode, newLabel, parent, k, m);
	}

	/**
	 * Overridden method.
	 *
	 * @see index.EditOperation#reverseEditOp()
	 */
	@Override
	public EditOperation reverseEditOp() {
		return new DeleteNode(tree, newNode, newLabel, parent, k, m);
	}
	
	

	/**
	 * Overridden method.
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ("ins (" + ((Integer)tree).intValue() + ","
				+ ((Integer)parent).intValue() + ") (" 
				+ ((Integer)tree).intValue() + ","
				+ ((Integer)newNode).intValue() + "," 
				+ "'" + newLabel + "') k=" + k + ", m=" + m); 
	}
}
