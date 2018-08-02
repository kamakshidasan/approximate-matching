/*
 * Created on Sep 23, 2008
 */
package intervalenc;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import tree.LblValTree;

public class IEForestIterator implements Iterator<LblValTree> {	

	Cursor cursor;
	
	/** 
	 * @param f
	 * @throws SQLException
	 */
	public IEForestIterator(IntervalEncForest f) throws SQLException {		
		String qry = 		
			"SELECT " + f.getAtbTreeID() + "," + f.getAtbLabel() + ", " 
			+ f.getAtbValue() + "," + f.getAtbLft() + "," + f.getAtbRgt()  
			+ " FROM " + f.getTblName() 
			+ " ORDER BY " + f.getAtbTreeID() + "," + f.getAtbLft();
		cursor = new IntervalEncCursor(f.getStreamStatement().executeQuery(qry), f);
		// set the cursor on the root node of the first tree in the forest
		if (!cursor.isAfterLast()) {
			cursor.next();
		}
	}
		
	public boolean hasNext() {
		try {
			// if the root node is not on the first node of the next tree, then there are no more trees left
			return !cursor.isAfterLast();
		} catch (SQLException e) {
			new RuntimeException("SQLException in " + this.getClass());
			e.printStackTrace();
			return false;
		}
	}

	public LblValTree next() {			
		if (this.hasNext()) {
			try {
				// the cursor is on the root node of the next tree
				IntervalEncNode n = cursor.fetchNode();
				LblValTree t = new LblValTree(n.getLabel(), n.getValue(), n.getTreeID()); 
				loadTree(cursor, t, n.getRgt(), n.getTreeID()); 
				return t;
			} catch (SQLException e) {
				throw new NoSuchElementException("SQLException in " + this.getClass());
			}
		} else {
			throw new NoSuchElementException();
		}
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	
	/**
	 * Recursive method used by {@link #next()}.
	 * 
	 * @param cursor
	 * @param t
	 * @param previousNodeRgt
	 * @param treeID 
	 * @return the next node in the resultset that is not a descendant of n, 
	 *         null if no nodes are left.
	 * @throws SQLException
	 */
	private IntervalEncNode loadTree(Cursor cursor, LblValTree t, int previousNodeRgt, int treeID) 
	throws SQLException {	
		
		if (cursor.next()) {
			IntervalEncNode n = cursor.fetchNode();
			while  ((n != null) && (n.getLft() < previousNodeRgt) && (n.getTreeID() == treeID)) {
				LblValTree child = new LblValTree(n.getLabel(), n.getValue(), n.getTreeID());
				t.add(child);
				n = loadTree(cursor, child, n.getRgt(), treeID);
			}
			return n;
		} else {
			return null;
		}
	}
	
}
