/*
 * Created on 22/11/2006
 */
package adjacencyenc;

import hash.FixedLengthHash;
import index.PPartTbl;
import index.QPartTbl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import sqltools.SQLTools;

public class PQGramFactory {

	private PQGramFactory() {}
	
	/**
	 * Computes a linked list of the hashed node labels in the path from
	 * <code>nodeID</code> to its anchestor at distance <code>length</code>,
	 * preserving node order. The label of <coce>nodeID</code> is the first
	 * element in the list.
	 * 
	 * @param treeID
	 *            tree
	 * @param nodeID
	 *            the first node in the ancestor chain
	 * @param length
	 *            length of the chain
	 * @param hf
	 *            hash function to compute the hash values of the labels
	 * @return linked list of hash values
	 * 
	 * @throws SQLException
	 */
	private static LinkedList getAnchestors(AdjacencyEncForest f, int treeID, int nodeID, int length,
			FixedLengthHash hf) throws SQLException {		
		LinkedList anc = new LinkedList();
		int k = 0;
		long start = System.currentTimeMillis();
		for (int i = 0; i < length; i++) {
			String query = "SELECT " + f.getAtbLabel()+ "," + f.getAtbParentID() + " FROM `"
					+ f.getTblName() + "` WHERE " + f.getAtbTreeID() + "=" + treeID
					+ " AND " + f.getAtbNodeID() + "=" + nodeID;
			ResultSet rs = f.getStatement().executeQuery(query);
			if (rs.next()) {
				anc.addLast(hf.getHashValue(rs.getString(f.getAtbLabel())));
				k++;
				nodeID = rs.getInt(f.getAtbParentID());
			} else {
				break;
			}
		}
		if (SQLTools.DEBUG) {
			System.out.println("Got " + length + " anchestors of node " + nodeID 
					+ " in " + (System.currentTimeMillis() - start) + "ms.");
		}
		for (int i = k; i < length; i++) {
			anc.addLast(hf.getNullNode());
		}
		return anc;
	}

	/**
	 * <p>Computes <code>pqg(anchorID)&sdot;Q<sup>k..k+m-1</sup>(anchorID)</code>.</p>
	 * 
	 * <p> 
	 * Computes all pq-grams of the tree <code>treeID</code>
	 * <ul>
	 * <li>with the anchor node <code>anchorID</code>,
	 * <li>and
	 * <ul>
	 * <li> for <code>m>0</code>: at least one of the children number <code>k</code> to
	 * <code>k+m-1</code> of <code>anchorID</code> in the q-part
	 * <li> for <code>m=0</code>: 2 pq-grams: one with child <code>k</code> at position <code>q</code>, 
	 * 	one with child <code>k-1</code> at position <code>1</code>.
	 * </ul>
	 * </ul>
	 * </p>
	 * 
	 * <p>The anchor node <em>must not</em> be a leaf node.</p>
	 * 
	 * @param treeID  tree
	 * @param anchorID anchor node
	 * @param k   child number <code>k</code>(<code>k&ge;1</code>, first
	 *            child has number <code>k=1</code>)
	 * @param m   number of children for <code>n&ge;0</code>)  
	 * @param ppart is reset and used to store the p-part of the resulting pq-grams
	 * @param qpart is reset and used to store the q-parts of the resulting pq-grams
	 * 
	 * @throws SQLException
	 * @throws RuntimeException
	 */
	public static void getPQGrams(AdjacencyEncForest f, int treeID, int anchorID, int k, int m,
			PPartTbl ppart, QPartTbl qpart) throws SQLException {

		// get ancestors
		LinkedList anc = getAnchestors(f, treeID, anchorID, ppart.getP(), ppart.getHf());

		// store p-part
		ppart.open();
		AdjNode anchor = f.getNode(treeID, anchorID);
		ppart.addPPart(treeID, anchorID, anchor.getSibPos(), anchor.getParentID(), anc);
		
		// get children in specified range (plus q-1 children to the left and to the rigth) 
		int q = qpart.getQ();
		ResultSet children;
		if (m >= 0) {
			children = f.getChildren(treeID, anchorID, k - (q - 1), (k + m - 1) + (q - 1));
		} else {
			children = f.getChildren(treeID, anchorID, k - (q - 1));
		}

		// store q-parts
		qpart.open();
		LinkedList sib = qpart.getHf().getEmptyRegister(qpart.getQ());
		
		// shift to node before child k
		for (int i = 0; i < Math.min(q, k) - 1; i++) {
			if (children.next()) {
				sib.removeLast();
				sib.addFirst(qpart.getHf().getHashValue(children.getString(f.getAtbLabel())));
			}
		}
		
		int row = k - 1;
		if (m >= 0) {
			// shift from child k to k+m-1 (rows k-1 to k+m-2)
			for (int i = 0; i < m; i++) {
				if (children.next()) {			
					sib.removeLast();
					sib.addFirst(qpart.getHf().getHashValue(children.getString(f.getAtbLabel())));
					qpart.addQPart(treeID, anchorID, row, sib);
					row++;
				} else {
					break;
				}
			} // row is now k+m-1 or f-1 (f is the fanout of anchorID)
		} else {		
			// if no limit to the right for children 
			while (children.next()) {
				sib.removeLast();
				sib.addFirst(qpart.getHf().getHashValue(children.getString(f.getAtbLabel())));
				qpart.addQPart(treeID, anchorID, row, sib);
				row++;
			}
		}
		
		// shift q-1 positions to the right
		for (int i = 0; i < q - 1; i++) {
			sib.removeLast();
			if (children.next()) {			
				sib.addFirst(qpart.getHf().getHashValue(children.getString(f.getAtbLabel())));
			} else {
				sib.addFirst(qpart.getHf().getNullNode());
			}
			qpart.addQPart(treeID, anchorID, row, sib);
			row++;
		}
		
		// flush all buffers
		ppart.getInsBuff().close();
		qpart.getInsBuff().close();		
	}


	/**
	 * Appends the p-part of node <code>nodeID</code> of tree <code>treeID</code> to <code>ppart</code>.
	 * Needs lookup of (treeID, nodeID) &rArr; useful to have index on this columns for large datasets.
	 * Assumes that (treeID, nodeID) exists, NullPointerException otherwise!
	 * 
	 * @param ppart ppart table
	 * @param treeID tree index
	 * @param nodeID node index
	 * @throws SQLException
	 */
	public static void getPPart(AdjacencyEncForest f, PPartTbl ppart, int treeID, int nodeID) throws SQLException {

		// get ppart of nodeID
		LinkedList anc = getAnchestors(f, treeID, nodeID, ppart.getP(), ppart.getHf());

		// get sibPos and parentID of nodeID
		AdjNode nd = f.getNode(treeID, nodeID);
		
		// add the ppart
		ppart.addPPart(treeID, nodeID, nd.getSibPos(), nd.getParentID(), anc);
		ppart.getInsBuff().flush();
	}
	
	/**
	 * <p>Opens <code>ppart</code> and inserts the p-part of <code>nodeID</code>'s children number <code>k</code> 
	 * through <code>m</code> of tree <code>treeID</code>. If <code>m=0</code> the <code>ppart</code> is an 
	 * empty ppart. If <code>m=-1</code> then all children to the right of <code>k</code> are considered.
	 * <p>Useful to have index on columns (treeID, parentID, nodeID) for large datasets.</p>

	 * 
	 * @param ppart ppart table
	 * @param treeID tree index
	 * @param parentID node index of parent node
	 * @param k start with <code>k</code>-th child of <code>nodeID</code>
	 * @param m stop with <code>k+m-1</code>-th child of <code>nodeID</code>
	 * @throws SQLException
	 */
	public static void getPParts(AdjacencyEncForest f, PPartTbl ppart, int treeID, 
			int parentID, int k, int m) throws SQLException {

		// get ppart of nodeID
		LinkedList anc = getAnchestors(f, treeID, parentID, ppart.getP(), ppart.getHf());
		
		// Computing join for newPPart
		String T = "`" + f.getTblName() + "`";
		String qry = "SELECT\n" + 
			f.getAtbNodeID() + " AS " + ppart.getAtbAnchorID() + ",\n" + 
			f.getAtbSibPos() + " AS " + ppart.getAtbSibPos() + ",\n" +
			f.getAtbLabel() + " AS label\n" +
			"FROM " + T + "\n" + 
			"WHERE " + 
			f.getAtbTreeID() + "=" + treeID + " AND " +
			f.getAtbParentID() + "=" + parentID + " AND " +
			f.getAtbSibPos() + ">=" + k;
		if (m != -1) {
			qry = qry + " AND " + f.getAtbSibPos() + "<=" + (k + m - 1) + "\n";
		}
		ResultSet rs = 
			SQLTools.executeQuery(f.getStreamStatement(Integer.MIN_VALUE), 
					qry, "Computing pparts for `" + ppart.getTblName() + "`");		

		// store p-parts
		ppart.open();
		anc.removeLast();
		while (rs.next()) {
			anc.addFirst(ppart.getHf().getHashValue(rs.getString("label")));
			ppart.addPPart(treeID, 
					rs.getInt(ppart.getAtbAnchorID()), 
					rs.getInt(ppart.getAtbSibPos()), 
					parentID, 
					anc);
			anc.removeFirst();
		}
		ppart.getInsBuff().flush();
	}	
	
	/**
	 * Used by {@link #addPQGrams(int, PPartTbl, PPartTbl, QPartTbl)} to store p- and q-part of a pq-gram 
	 * of a non-leaf node. (Leaves have null-node q-part, i.e. they are different.)
	 *
	 * @param treeID   
	 * @param rs
	 * @param ppart
	 * @param qpart
	 * @param storePPart if true, the p-parts are stored in <code>ppart</code>, otherwise only the q-parts are stored
	 * @throws SQLException
	 * 
	 * @return tuple added to <code>ppart</code>
	 */
	private static int storeNonLeaf(int treeID, ResultSet rs, PPartTbl ppart, QPartTbl qpart, boolean storePPart) throws SQLException {
		
		LinkedList sib = qpart.getHf().getEmptyRegister(qpart.getQ());
		int row = 0;
		int parentID;
		int size = 0;
		do {
			String label = ppart.getHf().getHashValue(rs.getString("label")).toString();
			parentID =  rs.getInt(ppart.getAtbParentID());
			// add p-part
			if (storePPart) {
				ppart.addPPart(treeID, 
						rs.getInt(ppart.getAtbAnchorID()), 
						rs.getInt(ppart.getAtbSibPos()), 
						rs.getInt(ppart.getAtbParentID()), 
						rs.getString(ppart.getAtbPPart()),
						label);
				size++;
			}
			// add q-part
			sib.removeLast();
			sib.addFirst(qpart.getHf().getHashValue(rs.getString("label")));
			qpart.addQPart(treeID, rs.getInt(ppart.getAtbParentID()), row, sib);			
			row++;
		} while (rs.next() && (parentID == rs.getInt(ppart.getAtbParentID())));
		// add q-parts with right-hand null nodes 
		for (int i = 0; i < qpart.getQ() - 1; i++) {
			sib.removeLast();
			sib.addFirst(qpart.getHf().getNullNode());
			qpart.addQPart(treeID, parentID, row + i, sib);
		}			
		return size;		
	}

	/**
	 * Calculates the q-part that belong to <code>ppart<code>, no new p-part is stored.
	 * This is called in the last iteration, as you need the q-parts for the previous level,
	 * but no new p-parts. 
	 * 
	 * @param treeID treeID
	 * @param ppart p-part q<sub>i</sub>
	 * @param qpart q-part Q<sub>i</sub>
	 * @throws SQLException
	 */
	public static void addPQGrams(AdjacencyEncForest f, int treeID, PPartTbl ppart, QPartTbl qpart) throws SQLException {

		// Computing join for newPPart
		String T = "`" + f.getTblName() + "`";
		String P = "`" + ppart.getTblName() + "`";
		String qry = " SELECT\n" + 
			T + "." + f.getAtbNodeID() + " AS " + ppart.getAtbAnchorID() + ",\n" + 
			T + "." + f.getAtbSibPos() + " AS " + ppart.getAtbSibPos() + ",\n" +
			P + "." + ppart.getAtbAnchorID() + " AS " + ppart.getAtbParentID() + ",\n" +
			P + "." + ppart.getAtbPPart() + " AS " + ppart.getAtbPPart() + ",\n" +
			T + "." + f.getAtbLabel() + " AS label\n" +
			"FROM " + P + " LEFT JOIN " + T + " ON \n" + 
			T + "." + f.getAtbTreeID() + " = " + treeID + " AND " +
			T + "." + f.getAtbParentID() + " = " + P + "." + ppart.getAtbAnchorID() + "\n" +
			"ORDER BY " + ppart.getAtbParentID() + "," + ppart.getAtbSibPos();
		ResultSet rs = 
			SQLTools.executeQuery(f.getStreamStatement(Integer.MIN_VALUE), 
					qry, "Computing " + qpart.getTblName());
		
		// storing q-parts of the p-parts in 'ppart' to 'qpart'
		if (SQLTools.DEBUG) {
			System.err.print("Storing q-parts...");
		}
		long start = System.currentTimeMillis();

		qpart.open();
		if (rs.next()) {
			while (!rs.isAfterLast()) {
				String label = rs.getString("label");
				if (label != null) {
					// store p-part and q-parts for this anchor node
					storeNonLeaf(treeID, rs, ppart, qpart, false);
				} else {		
					qpart.addQPart(treeID, rs.getInt(ppart.getAtbParentID()), 
							0, qpart.getHf().getEmptyRegister(qpart.getQ()));
					rs.next();
				}
			}
		}
		qpart.getInsBuff().flush();

		if (SQLTools.DEBUG) {
			System.err.println("took me " + (System.currentTimeMillis() - start) + "ms");
		}		
	}	
	
	/**
	 * Calculates p-parts and q-part for the next level, starting with the p-parts of the
	 * previous label. Actually, the q-parts belong to the p-parts of the previous level! 
	 * Non-recursive way to compute the pq-grams... 
	 * 
	 * @param treeID treeID
	 * @param ppart p-part P<sub>i</sub>
	 * @param newPPart p-part P<sub>i+1</sub>
	 * @param qpart q-part 
	 * @throws SQLException
	 * @return size of <code>newPPart</code>
	 */
	public static int addPQGrams(AdjacencyEncForest f, int treeID, PPartTbl ppart, PPartTbl newPPart, QPartTbl qpart) throws SQLException {

		// Computing join for newPPart
		String T = "`" + f.getTblName() + "`";
		String P = "`" + ppart.getTblName() + "`";
		ppart.createAnchorIDIndex();
		String qry = " SELECT\n" + 
			T + "." + f.getAtbNodeID() + " AS " + ppart.getAtbAnchorID() + ",\n" + 
			T + "." + f.getAtbSibPos() + " AS " + ppart.getAtbSibPos() + ",\n" +
			P + "." + ppart.getAtbAnchorID() + " AS " + ppart.getAtbParentID() + ",\n" +
			P + "." + ppart.getAtbPPart() + " AS " + ppart.getAtbPPart() + ",\n" +
			T + "." + f.getAtbLabel() + " AS label\n" +
			"FROM " + P + " LEFT JOIN " + T + " ON \n" + 
			T + "." + f.getAtbTreeID() + " = " + treeID + " AND " +
			T + "." + f.getAtbParentID() + " = " + P + "." + ppart.getAtbAnchorID() + "\n" +
			"ORDER BY " + ppart.getAtbParentID() + "," + ppart.getAtbSibPos();
		ResultSet rs = 
			SQLTools.executeQuery(f.getStreamStatement(Integer.MIN_VALUE), 
					qry, "Computing " + newPPart.getTblName());
		
		// storing 'newPPart' and adding q-parts of the p-parts in 'ppart' (the 'old' pparts!) to 'qpart'
		if (SQLTools.DEBUG) {
			System.err.print("Storing p-part and q-part...");
		}
		long start = System.currentTimeMillis();

		newPPart.reset();
		int size = 0;
		qpart.open();
		if (rs.next()) {
			while (!rs.isAfterLast()) {
				String label = rs.getString("label");
				if (label != null) {
					// store p-part and q-parts for this anchor node
					size += storeNonLeaf(treeID, rs, newPPart, qpart, true);
				} else {		
					qpart.addQPart(treeID, rs.getInt(ppart.getAtbParentID()), 
							0, qpart.getHf().getEmptyRegister(qpart.getQ()));
					rs.next();
				}
			}
		}
		newPPart.getInsBuff().flush();
		qpart.getInsBuff().flush();

//		index is not used...
//		String indexQry = "ALTER TABLE `" + newPPart.getTblName() + "` ADD UNIQUE INDEX (" + 
//		newPPart.getAtbParentID() + "," + newPPart.getAtbSibPos() + ")";
//		f.execute(indexQry, "\nCreating index for " + newPPart.getTblName());

		if (SQLTools.DEBUG) {
			System.err.println("took me " + (System.currentTimeMillis() - start) + "ms");
		}		
		
		return size;
	}



}
