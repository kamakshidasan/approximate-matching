/*
 * Created on Jun 13, 2005
 */
package index;

import java.sql.SQLException;

/**
 * An Object that implements the standard edit operations (renameNode, deleteNode, insertNode) on nodes.
 * 
 * @author augsten
 */
public interface Editable {
	
	/**
	 * <tt>relabelNode(T,v,l):</tt> Change label of node <tt>v</tt> to <tt>l</tt>.
	 * 
	 * @param tree <tt>T</tt>
	 * @param node <tt>v</tt>
	 * @param label <tt>l</tt>
	 */
	public void renameNode(Object tree, Object node, String label) throws SQLException;

	/**
	 * <tt>deleteNode(T,v):</tt> Delete the non-root node <tt>v</tt> from <tt>T</tt> and move all its
	 * children to the parent of <tt>v</tt> (preserving order).
	 * @param tree <tt>T</tt>
	 * @param node <tt>v</tt>
	 */
	public void deleteNode(Object tree, Object node) throws SQLException;

	/**
	 * <tt>insertNode(T,v,p,k,n):</tt> Insert the node <tt>v</tt> as the <tt>k</tt>-th child of <tt>p</tt> 
     *     	into <tt>T</tt>, and move the <tt>n</tt> consecutive children <tt>v<sub>k</sub>,...,v<sub>k+n-1</sub></tt>.
     * 
	 * @param tree <tt>T</tt>
	 * @param newNode <tt>v</tt>
	 * @param parentNode <tt>p</tt>
	 * @param k insert as child number <tt>k</tt>
	 * @param n move <tt>n</tt> children
	 */
	public void insertNode(Object tree, Object newNode, String newLabel, Object parentNode, int k, int n) throws SQLException;
		
}
