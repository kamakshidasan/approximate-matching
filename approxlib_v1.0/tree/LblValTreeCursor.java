package tree;

import intervalenc.Cursor;
import intervalenc.IntervalEncNode;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;

public class LblValTreeCursor implements Cursor {

	IntervalEncNode[] ieNode;
	int currentNode;
	
	public LblValTreeCursor(LblValTree t) {
		this.ieNode = new IntervalEncNode[t.getNodeCount()];
		this.currentNode = 0;
		storeTree(t, 0); // changes this.currentnode!
		Arrays.sort(ieNode, new Comparator() {
			public int compare(Object o1, Object o2) {
				IntervalEncNode n1 = (IntervalEncNode)o1;		
				IntervalEncNode n2 = (IntervalEncNode)o2;		
				return n1.getLft() - n2.getLft();
			}
			
		});
		this.currentNode = -1;
	}
	/**
	 * Recursive method used by {@link #LblValTreeCursor(LblValTree)}.
	 * 
	 * @param t the root node of the tree to store
	 * @param lft the left number of n
	 * @return the right number of n
	 */
	private int storeTree(LblValTree t, int lft) {
		int lftNum = lft;
		for (Enumeration e = t.children(); e.hasMoreElements();) {
			lft = storeTree((LblValTree)e.nextElement(), lft + 1);
		}
		int rgtNum = lft + 1;
		ieNode[currentNode++] = new IntervalEncNode(t.getTreeID(), t.getLabel(), t.getValue(), lftNum, rgtNum); 
		return rgtNum;
	}	
	
	/* (non-Javadoc)
	 * @see intervalenc.Cursor#fetchNode()
	 */
	public IntervalEncNode fetchNode() throws SQLException {
		return ieNode[currentNode];
	}
	/* (non-Javadoc)
	 * @see intervalenc.Cursor#isAfterLast()
	 */
	public boolean isAfterLast() throws SQLException {
		return currentNode >= ieNode.length;
	}
	/* (non-Javadoc)
	 * @see intervalenc.Cursor#next()
	 */
	public boolean next() throws SQLException {
		if (currentNode < ieNode.length) {
			currentNode++;
		}
		return currentNode < ieNode.length;
	}
	
	
}
