/*
 * Created on Sep 6, 2005
 */
package index;

import tree.Node;
import mysqltools.MySqlInsertBuffer;
import sqltools.InsertBuffer;
import adjacencyenc.AdjacencyEncForest;
import adjacencyenc.PQGramFactory;
import hash.FixedLengthHash;
import sqltools.SQLTools;
import java.sql.SQLException;
import java.sql.ResultSet;
import adjacencyenc.AdjNode;

/**
 * <p>Given a node <code>n</code>, a distance <code>d</code> and, optionally, 
 * two integers <code>k</code> and <code>m</code>,
 * the following temporary tables are created: 
 * <ul>
 * <li> <code>ppart(n)</code>
 * <li><code>Q(n)</code> &ndash; only rows for children <code>c<sub>i</sub></code>
 * <li><code>ppart(c<sub>i</sub>)</code> and <code>Q(c<sub>i</sub>)</code>
 * <li><code>ppart(x)</code> and <code>Q(x)</code> with 
 *     <code>x &isin; {x|x &isin; desc(c<sub>i</sub>), dist(x,n) &lt; d}</code>,
 * </ul>
 * <code>c<sub>i</sub></code> is the <code>i</code>-th child of <code>n</code>. 
 * If <code>k</code> and <code>m</code> are given, <code>k &le; i &lt; k+m</code>.</p>
 * 
 * <p>Further, for a node <code>v</code> the following tables are created:
 * <ul>
 * <li> <code>ppart(v)</code>
 * <li><code>Q(v)</code> &ndash; only rows for children <code>c<sub>i</sub></code>
 * </ul>
 * where <code>c<sub>i</sub></code> is the <code>i</code>-th child of <code>v</code>. 
 * If <code>k</code> and <code>m</code> are given, <code>k &le; i &lt; k+m</code>.</p>
 * </p> 
 * @author augsten
 */
public class DeltaProfile implements Editable {
	
	/**
	 * If true, print debug messages. 
	 */
	public static boolean DEBUG = false;

	// p- and q-part tables
//	private PPartTbl p_v;
//	private QPartTbl Q_v;
//	private 
//	private QPartTbl[] Qi;
	
	private PPartTbl pparts;
	private QPartTbl Qparts;
	
	// table name prefixes
	private String tblP;

	// p- and q-values of pq-grams
	int p;
	int q;
	
	// other
	private AdjacencyEncForest f;
	private FixedLengthHash hf;

	private boolean computeDelta = true;
	private boolean updateDelta = true;

	/**
	 * @param f
	 * @param p
	 * @param q
	 * @param hf
	 * @param tblP name of PPartTbl table, prefix for temporary P-tables
	 * @param tblQ name of QPartTbl table, prefix for temporary Q-tables
	 * @param insBuff
	 * @parem reset if true, the tables are newly created and an index is defined on them, otherwise the existing tables are opened.
	 * @throws SQLException
	 */
	public DeltaProfile(AdjacencyEncForest f, int p, int q, FixedLengthHash hf, String tblP, String tblQ, InsertBuffer insBuff, boolean reset) 
		throws SQLException {
		this.f = f;
		//this.insBuff = insBuff;
		this.hf = hf;
		this.tblP = tblP;
		this.p = p;
		this.q = q;
		this.pparts = new PPartTbl(f.getCon(), new MySqlInsertBuffer(), tblP, hf, this.p);
		this.Qparts = new QPartTbl(f.getCon(), new MySqlInsertBuffer(), tblQ, hf, this.q);		
		
		if (reset) {
			this.reset();
		} else {
			pparts.open();
			Qparts.open();
		}
	}
	
	/**
	 * @see index.Editable#deleteNode(java.lang.Object, java.lang.Object)
	 */
	public void deleteNode(Object tree, Object node) throws SQLException {
		int treeID = ((Integer)tree).intValue();
		int nodeID = ((Integer)node).intValue();
		if (computeDelta) {
			this.computeDeltaDelete(treeID, nodeID);
		}
		if (updateDelta) {
			this.updateDeltaDelete(treeID, nodeID);
		}
	}

	/**
	 *
	 * Overridden method.
	 *
	 * @see index.Editable#insertNode(java.lang.Object, java.lang.Object, java.lang.String, java.lang.Object, int, int)
	 */
	public void insertNode(Object tree, Object newNode, String newLabel,
			Object parentNode, int k, int m) throws SQLException {
		
		
		int treeID = ((Integer)tree).intValue();
		int parentID = ((Integer)parentNode).intValue();
		// adjust too large m-values!
		m = Math.min(f.getChildCount(treeID, parentID) - k + 1, m);		
		if (computeDelta) {
			this.computeDeltaInsert(treeID, parentID, k, m);
		}
		if (updateDelta) {
			int nodeID = ((Integer)newNode).intValue();
			this.updateDeltaInsert(treeID, nodeID, newLabel, parentID, k, m);
		}
	}

	/**
	 * Overridden method.
	 *
	 * @see index.Editable#renameNode(java.lang.Object, java.lang.Object, java.lang.String)
	 */
	public void renameNode(Object tree, Object node, String label)
			throws SQLException {
		int treeID = ((Integer)tree).intValue();
		int nodeID = ((Integer)node).intValue();
		if (computeDelta) {
			this.computeDeltaRename(treeID, nodeID);
		}
		if (updateDelta) {
			this.updateDeltaRename(treeID, nodeID, label);
		}
	}
	
	/**
	 *  
	 * @param getDelta if true, delta is added to profile
	 * @param updateDelta if true, delta is assumed to be in the profile and is updated
	 */
	public void setUpdateMode(boolean getDelta, boolean updateDelta) {
		this.computeDelta = getDelta;
		this.updateDelta = updateDelta;
	}
	
	/**
	 * Compute all pq-grams of tree <code>treeID</code>
	 * 
	 * @param treeID
	 * @throws SQLException
	 */
	public void createProfile(int treeID) throws SQLException {
		AdjNode root = f.getRootNode(treeID);
		this.createProfile(treeID, root.getNodeID());
	}
	
	/**
	 * <p>Creates for node <code>nodeID<code> and all its descendants the p- and respective q-parts. 
	 * 
	 * <p>This is used for subtree-operations and computation of a tree profile</p>.  
	 * 
	 * @param treeID
	 * @param nodeID
	 * @throws SQLException
	 */
	private void createProfile(int treeID, int nodeID) throws SQLException {
		PPartTbl ppart_old;
		PPartTbl ppart_new = null;

		ppart_old = new PPartTbl(f.getCon(), new MySqlInsertBuffer(), tblP + "0", hf, this.p);
		ppart_old.reset();
		PQGramFactory.getPPart(f, ppart_old, treeID, nodeID);
		pparts.union(ppart_old);
		int d = 1;
		int size = Integer.MAX_VALUE;
		while (size > 0) {
			ppart_new = new PPartTbl(f.getCon(), new MySqlInsertBuffer(), tblP + d, hf, this.p);
			size = PQGramFactory.addPQGrams(f, treeID, ppart_old, ppart_new, Qparts);
			pparts.union(ppart_new);
			ppart_old.drop();
			ppart_old = ppart_new;
			d++;
		}
		// remove last (=empty) ppart
		ppart_old.drop();
	}
	
	
	/**
	 * <p>Creates for node <code>nodeID<code> the p- (and respective q-parts) at distance 0 (the node itself) 
	 * to distance <code>depth</code> from node <code>nodeID</code>.</p>
	 * 
	 * <p>This is used for rename and delete.</p>  
	 * 
	 * @param treeID
	 * @param nodeID
	 * @param depth
	 * @throws SQLException
	 */
	private void createProfile(int treeID, int nodeID, int depth) throws SQLException {
		PPartTbl ppart_old;
		PPartTbl ppart_new = null;

		ppart_old = new PPartTbl(f.getCon(), new MySqlInsertBuffer(), tblP + "0", hf, this.p);
		ppart_old.reset();
		PQGramFactory.getPPart(f, ppart_old, treeID, nodeID);
		this.pparts.union(ppart_old);
		for (int d = 1; d <= depth; d++) {
			ppart_new = new PPartTbl(f.getCon(), new MySqlInsertBuffer(), tblP + d, hf, this.p);
			PQGramFactory.addPQGrams(f, treeID, ppart_old, ppart_new, Qparts);
			pparts.union(ppart_new);
			ppart_old.drop();
			ppart_old = ppart_new;
		}
		PQGramFactory.addPQGrams(f, treeID, ppart_old, Qparts);
		ppart_old.drop(); // = ppart_old
	}
	

	/**
	 * <p>Creates for node <code>nodeID<code> the p- (and respective q-parts) at distance 1 
	 * (children <code>k</code> through <code>k+m-1</code>) to distance <code>depth</code> 
	 * from node <code>nodeID</code>.</p>
	 * 
	 * <p>This is used for insert.</p>  
	 * 
	 * @param treeID
	 * @param nodeID
	 * @param depth
	 * @param k start producing p-parts with child number <code>k</code>
	 * @param m stop producing p-parts with child number <code>k+m-1</code>
	 * @throws SQLException
	 */
	private void createProfile(int treeID, int nodeID, int depth, int k, int m) throws SQLException {
		
		PPartTbl ppart_old;
		PPartTbl ppart_new = null;
		
		ppart_old = new PPartTbl(f.getCon(), new MySqlInsertBuffer(), tblP + "1", hf, this.p);
		PQGramFactory.getPParts(f, ppart_old, treeID, nodeID, k, m);
		this.pparts.union(ppart_old);
		for (int d = 2; d <= depth; d++) {
			ppart_new = new PPartTbl(f.getCon(), new MySqlInsertBuffer(), tblP + d, hf, this.p);
			PQGramFactory.addPQGrams(f, treeID, ppart_old, ppart_new, Qparts);
			pparts.union(ppart_new);
			ppart_old.drop();
			ppart_old = ppart_new;
		}
		PQGramFactory.addPQGrams(f, treeID, ppart_old, Qparts);		
		ppart_old.drop(); // = ppart_old
	}	
	
	/**
	 * Get all pq-grams that change, when node <code>nodeID</code> is renamed.
	 * 
	 * @param treeID
	 * @param nodeID
	 * @throws SQLException
	 */
	public void computeDeltaRename(int treeID, int nodeID) throws SQLException {
		AdjNode nd = f.getNode(treeID, nodeID);
		
		if (nd != null) {
			// parent's pq-grams
			if (nd.getParentID() != Node.NO_NODE) { 
				PQGramFactory.getPQGrams(f, treeID, nd.getParentID(), nd.getSibPos(), 1, pparts, Qparts);
			} else { // node = root node 
			}		
			// node's pq-grams
			this.createProfile(treeID, nodeID, this.p - 1);
		}
	}

	/**
 	 * Get all pq-grams that change, when node <code>nodeID</code> is deleted.

	 * @param treeID
	 * @param nodeID node different from root node
	 * @throws SQLException
	 */
	public void computeDeltaDelete(int treeID, int nodeID) throws SQLException {
		
		AdjNode nd = f.getNode(treeID, nodeID);

		if (nd != null) {
			// parent's pq-grams
			if (nd.getParentID() != Node.NO_NODE) { 
				PQGramFactory.getPQGrams(f, treeID, nd.getParentID(), nd.getSibPos(), -1, pparts, Qparts);
			} else { // node = root node 
				throw new RuntimeException("Delete operation not defined for root node!");
			}		
			// node's pq-grams
			this.createProfile(treeID, nodeID, this.p - 1);
			// TODO: do we need this? (only if compute/updateDelta are mixed!)
			//f.getPParts(pparts, treeID, nd.getParentID(), nd.getSibPos() + 1, -1);
		}
	}

	/**
	 * Get all pq-grams that change, when a node is inserted at position <code>k</code> to <code>parentID</code> 
	 * and <code>m</code> consecutive children are moved under the new node.
	 *
	 * @param treeID
	 * @param parentID 
	 * @param k
	 * @param m
	 * @throws SQLException
	 */
	public void computeDeltaInsert(int treeID, int parentID, int k, int m) throws SQLException {

		// get p_v and Q_v
		if (f.getChildCount(treeID, parentID) != 0) { 
			PQGramFactory.getPQGrams(f, treeID, parentID, k, m, pparts, Qparts);			
		} else { // parentID is a leaf
			Qparts.addLeafQPart(treeID, parentID, 0);
			Qparts.flush();
			PQGramFactory.getPPart(f, pparts, treeID, parentID);
		}

		// children's pq-grams
		this.createProfile(treeID, parentID, this.p - 1, k, m); // empty pi and Qi for m=0		
	}

	/**
	 * 
	 * @param treeID
	 * @param nodeID
	 * @param s
	 * @param depthStart
	 * @param depthStop
	 * @throws SQLException
	 */
	private void changePParts(int treeID, int nodeID, String s, int depthStart, int depthStop) throws SQLException {

		// defining temporary tables pi[0]..pi[depthStop]
		PPartTbl[] pi = new PPartTbl[depthStop + 1];
		for (int i = 0; i <= depthStop; i++) {
			pi[i] = new PPartTbl(f.getCon(), 
					new MySqlInsertBuffer(), tblP + i, hf, this.p);
		}
		
		// create p[0]
		ResultSet rs = pparts.getPPart(treeID, nodeID);
		rs.next();
		pi[0].reset();		
		pi[0].getInsBuff().insert("(" + treeID + "," + nodeID + "," + 
				rs.getInt(pparts.getAtbSibPos()) + "," + 
				rs.getInt(pparts.getAtbParentID()) + ",'')");
		pi[0].getInsBuff().flush();
		
		// create p[1]..p[depthStop]
		for (int i = 1; i <= depthStop; i++) {
			pi[i].reset();
			String qry = "INSERT INTO `" + pi[i].getTblName() + "`\n" +
				"SELECT A.* FROM `" + 
				pparts.getTblName() + "` AS A,`" + pi[i-1].getTblName() + "` AS B\n" +
				"WHERE " + 
				"A." + pparts.getAtbTreeID() + "=" + treeID + " AND " +
				"A." + pparts.getAtbParentID() + "=" + "B." + pi[i-1].getAtbAnchorID();
			SQLTools.execute(pparts.getStatement(), qry,
					"Creating p[" + i + "]...");
		}
		
		// update pparts
		long start = System.currentTimeMillis();
		for (int d = depthStart; d <= depthStop; d++) {
			String subStr = s.substring(s.length() + (d - this.p) * this.hf.getLength());
			String qry = "UPDATE `" + 
				pparts.getTblName() + "` AS A JOIN `" + 
				pi[d].getTblName() + "` AS B\n" +

				"ON A." + pparts.getAtbTreeID() + "=" + treeID + " AND " +
				"A." + pparts.getAtbAnchorID() + "=" + "B." + pi[d].getAtbAnchorID() + " " +
				"SET A." + pparts.getAtbPPart() + "=CONCAT('" + SQLTools.escapeSingleQuote(subStr) + "'," + 
				"SUBSTRING(A." + pparts.getAtbPPart() + " FROM " + ((this.p - d) * this.hf.getLength() + 1) + "))";
			this.f.getStatement().executeUpdate(qry);			
		}
		SQLTools.debugMsg("Updating p-parts", start);
		
		start = System.currentTimeMillis();
		// delete temporary tables
		for (int i = 0; i < depthStop; i++) {
			pi[i].drop(); 
		}
		SQLTools.debugMsg("Dropping tables " + pi[0].getTblName() + " to "
				+ pi[depthStop - 1].getTblName(), start);
	}
	
	/**
	 * Calls {@link #computeDeltaRename(int, int)} if pi is null.
	 * 
	 * @param treeID
	 * @param nodeID
	 * @param label
	 * @throws SQLException
	 */
	public void updateDeltaRename(int treeID, int nodeID, String label) throws SQLException {
		// get p-part with renamed node as an anchor node
		String qry = "SELECT * FROM `" + pparts.getTblName() + "`\n" + 
			"WHERE " +
			pparts.getAtbTreeID() + "=" + treeID + " AND " +
			pparts.getAtbAnchorID() + "=" + nodeID;
		ResultSet rsPPart  = 
			SQLTools.executeQuery(this.f.getStatement(), qry, "Getting ppart(nodeID)...");
		rsPPart.next();
		String ppart = rsPPart.getString(this.pparts.getAtbPPart());
		int sibPos = rsPPart.getInt(this.pparts.getAtbSibPos());
		int parentID = rsPPart.getInt(this.pparts.getAtbParentID());

		// change the p-parts pi[0]...pi[p-1]				
		String s = ppart.substring(0, (this.p - 1) * this.hf.getLength()) + this.hf.h(label);
		long update_start = System.currentTimeMillis();
		this.changePParts(treeID, nodeID, s, 0, this.p - 1);
		if (DEBUG) {
			System.err.println("Updating the profile took me " + (System.currentTimeMillis() - update_start) + "ms.");
		}
		
		// change Q_v
		if (parentID != Node.NO_NODE) {
			this.renameDiagonal(this.Qparts, treeID, parentID, sibPos, label);
		}
	}
	
	/**
	 * Calls {@link #computeDeltaDelete(int, int)} if pi is null.
	 * 
	 * @param treeID
	 * @param nodeID
	 * @throws SQLException
	 */
	public void updateDeltaDelete(int treeID, int nodeID) throws SQLException {

		// get p-part with deleted node as an anchor node
		String qry = "SELECT * FROM `" + pparts.getTblName() + "`\n" + 
			"WHERE " +
			pparts.getAtbTreeID() + "=" + treeID + " AND " +
			pparts.getAtbAnchorID() + "=" + nodeID;
		ResultSet rsPPart  = 
			SQLTools.executeQuery(this.f.getStatement(), qry, "Getting ppart(nodeID)...");
		rsPPart.next();
		String ppart = rsPPart.getString(pparts.getAtbPPart());
		int sibPos = rsPPart.getInt(pparts.getAtbSibPos());
		int parentID = rsPPart.getInt(pparts.getAtbParentID());
		int chCnt = pparts.getChildCount(treeID, nodeID);
		
		// update the remaining p-parts
		String s = ppart.substring(0, (this.p - 1) * this.hf.getLength());
		long update_start = System.currentTimeMillis();
		// update pparts
		this.changePParts(treeID, nodeID, s, 1, this.p - 1);
		// delete the old ppart(nodeID)
		// delete p-part that has deleted node as an anchor
		pparts.deletePPart(treeID, nodeID);	
		
		Qparts.deleteNode(treeID, nodeID, sibPos, parentID);
		
		// update structure
		String sb = pparts.getAtbSibPos();
		// 1) right siblings of deleted node
		if (chCnt != 1) {
			qry = "UPDATE `" + pparts.getTblName() + "` " +
			"SET " + sb + "=" + sb + "+" + chCnt + "-1 " + 
			"WHERE " + 
			pparts.getAtbTreeID() + "=" + treeID + " AND " +
			pparts.getAtbParentID() + "=" + parentID + " AND " +
			sb + ">" + sibPos;
			if (chCnt < 1) {
				qry += " ORDER BY " + sb + " ASC";
			} else {
				qry += " ORDER BY " + sb + " DESC";					
			}
			SQLTools.executeUpdate(pparts.getStatement(), qry,
					"Updating structure of right hand siblings of deleted nodeID " + nodeID);
		}
		// 2) children of deleted node
		int div = sibPos - 1;
		if (div != 0) {
			qry = "UPDATE `" + pparts.getTblName() + "` " +
				"SET " + sb + "=" + sb + "+" + div + "," +
				pparts.getAtbParentID() + "=" + parentID + " " +
				"WHERE " + 
				pparts.getAtbTreeID() + "=" + treeID + " AND " +
				pparts.getAtbParentID() + "=" + nodeID;
			if (div < 0) {
				qry += " ORDER BY " + sb + " ASC";				
			} else {
				qry += " ORDER BY " + sb + " DESC";									
			}		
			SQLTools.executeUpdate(pparts.getStatement(), qry,
					"Updating structure of children of deleted nodeID " + nodeID);
		}
				
		if (DEBUG) {
			System.err.println("Updating the profile took me " + (System.currentTimeMillis() - update_start) + "ms.");
		}
			
	}
	
	/**
	 * Calls {@link #computeDeltaInsert(int, int, int, int)} if pi is null.
	 * 
	 * @param treeID
	 * @param newNodeID newly inserted node
	 * @param label label of <code>anchorID</code>
	 * @param parentID parent of <code>anchorID</code>
	 * @param k first sibling
	 * @param m move m siblings
	 * @throws SQLException
	 */
	public void updateDeltaInsert(int treeID, int newNodeID, String label, 
			int parentID, int k, int m) throws SQLException {	
		
		System.out.println("IM HERE");

		String hLabel = this.hf.getHashValue(label).toString();
		String hNull = this.hf.getNullNode().toString();
		
		// convert m to the one I use in the VLDB2006 paper...
		m = k + m - 2; // move siblings k to m, first row is 0 
		k = k - 1; 

		int chCnt = Qparts.getChildCount(treeID, parentID);
		
		if (chCnt == -1) {
			throw new RuntimeException("Can not insert new node under " + parentID + ", because " + parentID + " does not exist!");
		} 

		
		if (chCnt == 0) {
			// TODO: what about the new leaf-qpart?
			Qparts.updateAnchorIDs(treeID, parentID, newNodeID, 0, 0);
			Qparts.storeQMat(treeID, parentID, 0, 
					new QMat(q, hLabel, hNull)); 
			Qparts.getInsBuff().flush();
		} else {			
			QMat qm = Qparts.loadQMat(treeID, parentID, k, m);
			
			// System.out.println(qm + "(" + qm.getRows() + "," + qm.getQ() + ")");
			
			Qparts.deleteQMat(treeID, parentID, k, m);
			Qparts.updateAnchorIDs(treeID, parentID, newNodeID, k + q - 1, m);
			
			if (k <= m) { // move nodes
				QMat newNodeQm = qm.extractDiagonals(this.hf.getNullNode().toString());
				//System.out.println(newNodeQm);
				Qparts.storeQMat(treeID, newNodeID, 0, newNodeQm.getHead(q - 1));
				Qparts.storeQMat(treeID, newNodeID, m + 1, newNodeQm.getTail(q - 1));
			} else {
				Qparts.addLeafQPart(treeID, newNodeID, 0);				
			}
				
			
			QMat parentQm = qm.replaceDiagonals(hLabel);
			//System.out.println(parentQm);
			Qparts.storeQMat(treeID, parentID, k, parentQm);
			Qparts.updateRows(treeID, parentID, 
					k + parentQm.getRows() + 1, parentQm.getRows() - qm.getRows());		
		}
					
		// pparts
		
		
		// get p-part with parent node as an anchor
		String ppart = pparts.getPPartStr(treeID, parentID);
		int len = this.hf.getLength();
		
		String s = ppart.substring(len, this.p * len) + hLabel;

		// update the remaining p-parts
		int[] chIDs = pparts.getChildren(treeID, parentID, k + 1, m + 1);
		String[] chLbl = pparts.getHashedLabels(treeID, parentID, k + 1, m + 1);
		for (int i = 0; i < chIDs.length; i++) {	
			String sc = s.substring(len, this.p * len) + chLbl[i];
			this.changePParts(treeID, chIDs[i], sc, 0, this.p - 2);
		}
		
		pparts.updateParentID(treeID, parentID, newNodeID, k + 1, m + 1, -k);
		pparts.updateSibPos(treeID, parentID, m + 2, k - m);

		// insert new ppart
		pparts.addPPart(treeID, newNodeID, k + 1, parentID, ppart, hLabel);			
		pparts.flush();
	}
	
	/**
	 * Rename diagonal of child number <code>k</code>. This means, the row numbers <code>k - 1</code> to <code>k + q - 1</code>.
	 * 
	 * @param Q
	 * @param k
	 * @param label
	 * @throws SQLException
	 */
	private void renameDiagonal(QPartTbl Q, int treeID, int anchorID, int k, String label) throws SQLException {
		String hl = Q.getHf().h(label).toString();
		String qry = 
			"UPDATE `" + Q.getTblName() + "`\nSET " + Q.getAtbQPart() + "=" + "CONCAT(" + 
			"SUBSTRING(" + Q.getAtbQPart() + " FROM 1 FOR (" + (q - 1) + "-(" + Q.getAtbRow() + "-" + (k - 1) + "))*" + 
			               Q.getHf().getLength() + ")," + 
			"'" + SQLTools.escapeSingleQuote(hl) + "'," +
			"SUBSTRING(" + Q.getAtbQPart() + " FROM (" + q + "-(" + Q.getAtbRow() + "-" + (k -1) + "))*" +
			               Q.getHf().getLength() + "+1))\n" +
			"WHERE " + Q.getAtbTreeID() + "=" + treeID + " AND\n" +
				     Q.getAtbAnchorID() + "=" + anchorID + " AND\n" + 
					 Q.getAtbRow() + ">=" + (k - 1) + " AND " + Q.getAtbRow() + "<=" + (k + Q.getQ() - 2) ; 
						   
		SQLTools.executeUpdate(f.getStatement(), 
				qry, "Updating diagonal of Q_v");
	}
	
	/**
	 * Delete all tables associated with the profile. 
	 * @throws SQLException
	 */
	public void drop() throws SQLException {
		Qparts.drop();
		pparts.drop();
	}
	
	/**
	 * 
	 * @throws SQLException
	 */
	public void reset() throws SQLException {
		
		long start = System.currentTimeMillis();
		
		this.pparts.reset();
		this.Qparts.reset();		
		
		// add indices
		
		String qry = "ALTER TABLE `" + pparts.getTblName() + "` ADD UNIQUE (" + 
		pparts.getAtbTreeID() + "," + pparts.getAtbAnchorID() + ")";
		pparts.getStatement().execute(qry);
		
		qry = "ALTER TABLE `" + pparts.getTblName() + "` ADD PRIMARY KEY(" +
		pparts.getAtbTreeID() + "," + pparts.getAtbParentID() + "," + 
		pparts.getAtbSibPos() + ")";
		pparts.getStatement().execute(qry);
		
		qry = "ALTER TABLE `" + Qparts.getTblName() + "` ADD PRIMARY KEY(" + 
		Qparts.getAtbTreeID() + "," + Qparts.getAtbAnchorID() + "," + 
		Qparts.getAtbRow() + ")";
		Qparts.getStatement().execute(qry);

		SQLTools.debugMsg("Resetting " + pparts.getTblName() + " and "
				+ Qparts.getTblName() + " and adding indices", start);
	}
	
	/**
	 * Does not work (yet) for m = 0! This is the case also for nodes inserted after the last child!
	 * 
	 * @param Q from this matrix the first <code>m</code> diagonals will be 
	 *          replaced by a diagonal with all elements <code>label</code>
	 * @param m number of diagonals to replace
	 * @param label new diagonal element
	 * @param Qd table formed by the substituted diagonals; if table exists, it will be overwritten!
	 * @throws SQLException
	 */
	private void replaceDigagonals(QPartTbl Q, long m, String label, QPartTbl Qd) throws SQLException {
		// cut to large values of m
		if (m > Q.getSize() - Q.getQ() + 1) {
			m = Q.getSize() - Q.getQ() + 1;
		}
		
		if (m > 0) {
			// copy Q to Qd and empty Q
			try {
				Qd.drop();
			} catch (Exception e) {			
			}
			String oldTblName = Q.getTblName();
			Q.rename(Qd.getTblName());
			Q.setTblName(oldTblName);
			Q.reset();
			
			// index row of Qd
			String idxQry = "ALTER TABLE `" + Qd.getTblName() + "` ADD UNIQUE INDEX(" + Qd.getAtbRow() + ")";
			SQLTools.execute(f.getStatement(), 
					idxQry, "Indexing column " + Qd.getAtbRow() + " of " + Qd.getTblName());
			
			// create table with substituted diagonals
			String hl = Qd.getHf().h(label).toString();
			String qry =
				"SELECT A.*, B." + Qd.getAtbQPart() + "," + "CONCAT(" + 
				"SUBSTRING(A." + Qd.getAtbQPart() + " FROM 1 FOR (" + (q - 1) + "-A." + Qd.getAtbRow() + ")*" + 
				Qd.getHf().getLength() + ")," + 
				"'" + SQLTools.escapeSingleQuote(hl) + "'," +
				"SUBSTRING(B." + Qd.getAtbQPart() + " FROM (" + q + "-A." + Qd.getAtbRow() + ")*" +
				Qd.getHf().getLength() + "+1)) AS newQPart" +
				"\nFROM " + 
				"`" + Qd.getTblName() + "` AS A, " +
				"`" + Qd.getTblName() + "` AS B " +
				"WHERE A." + Qd.getAtbRow() + " = B." + Qd.getAtbRow() + "+1-" + m;
			ResultSet rs = SQLTools.executeQuery(
					f.getStatement(), 
					qry, "Doing selfjoin on " + Qd.getTblName() + " (former table " + Q.getTblName() + ")");
			int row = 0;
			while (rs.next()) {
				Q.addQPart(rs.getInt(Qd.getAtbTreeID()), rs.getInt(Qd.getAtbAnchorID()),  row, rs.getString("newQPart"));
				System.err.println(rs.getInt("row") + " " + rs.getString("A.qpart") + " " + rs.getString("B.qpart") + " " + rs.getString("newQPart"));
				row++;
			}
			Q.getInsBuff().flush();
			
			// create table from substitued diagonals
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < this.q - 1; i++) {
				sb.append(this.hf.getNullNode().toString());
			}
			String starStr = "'" + SQLTools.escapeSingleQuote(sb.toString()) + "'";
			
			int hlen = Qd.getHf().getLength();
			qry = "UPDATE `" + Qd.getTblName() + "` SET " + Qd.getAtbQPart() + " = CONCAT(" +
			"SUBSTRING(" + starStr + " FROM " + Qd.getAtbRow() + "*" + hlen + "+1)," +
			"SUBSTRING(" + Qd.getAtbQPart() + " FROM (" + (Qd.getQ()-1) + "-" + Qd.getAtbRow() + ")*" + hlen + "+1)" +
			") WHERE " + Qd.getAtbRow() + " < " + (Qd.getQ()-1);
			SQLTools.executeUpdate(f.getStatement(), 
					qry, "Updating upper left triangle matrix of " + Qd.getTblName());
			qry = "UPDATE `" + Qd.getTblName() + "` SET " + Qd.getAtbQPart() + " = CONCAT(" +
			"SUBSTRING(" + Qd.getAtbQPart() + " FROM 1 FOR (" + (Qd.getQ()+m-1) + "-" + Qd.getAtbRow() + ")*" + hlen + ")," +
			"SUBSTRING(" + starStr + " FROM 1 FOR (" + Qd.getAtbRow() + "-" + (m-1) + ")*" + hlen + ")" +
			") WHERE " + Qd.getAtbRow() + " >= " + m;
			SQLTools.executeUpdate(f.getStatement(), 
					qry, "Updating lower right triangle matrix of " + Qd.getTblName());
		} else {
			throw new RuntimeException("ReplaceDiagonals is not defined for m = 0.");
		}
	}
	
	private void substituteDiagonal(QPartTbl Qv, int treeID, int anchorID, int k, QPartTbl Qd) 
		throws SQLException {
		long m = Qd.getSize() - Qd.getQ() + 1;
		
		
		// index row of Qd
		String idxQry = "ALTER TABLE `" + Qd.getTblName() + "` ADD UNIQUE INDEX(" + Qd.getAtbRow() + ")";
		SQLTools.execute(f.getStatement(), 
				idxQry, "Indexing column " + Qd.getAtbRow() + " of " + Qd.getTblName());		
		
		String qpart = Qd.getAtbQPart();
		String row = Qd.getAtbRow();
		String tblQd = "`" + Qd.getTblName() + "`";
		String tblQv = "`" + Qv.getTblName() + "`";
		String qry = "CREATE TEMPORARY TABLE Q_tmp AS\n" + 
			"SELECT L." + qpart + " AS qpartL, R." + qpart + " AS qpartR, M.* FROM " +
			tblQd + " AS M " +
			"LEFT JOIN " + tblQv + " AS L ON L." + row + "=M." + row + " " +
			"LEFT JOIN " + tblQv + " AS R ON R." + row + "=M." + row + "-" + (m-1);
			
//			+ tblQv + " AS R\n" +
//			"WHERE L." + row + "=M." + row + " AND M." + row + "=R." + row;
		SQLTools.execute(f.getStatement(), 
				qry, "Joining tables " + tblQv + ", " + tblQd + " and " + tblQv + " on " + row + " attribute");
		SQLTools.executeUpdate(f.getStatement(), 
				"UPDATE Q_tmp SET qpartL='' WHERE qpartL IS NULL", "Delete NULL values from Q_tmp (1/2)");
		SQLTools.executeUpdate(f.getStatement(), 
				"UPDATE Q_tmp SET qpartR='' WHERE qpartR IS NULL", "Delete NULL values from Q_tmp (2/2)");

		int hlen = Qd.getHf().getLength();
		Qv.reset();
		qry = "INSERT INTO " + tblQv + " SELECT " + Qd.getAtbTreeID() + "," + Qd.getAtbAnchorID() + "," + row + "," + 
		      "\nCONCAT(SUBSTRING(qpartL FROM 1 FOR (" + (q-1) + "-" + row + ")*" + hlen + "),\n" +
			  	     "SUBSTRING(" + qpart + " FROM 1+(" + (q-1) + "-" + row + ")*" + hlen + 
					 " FOR " + (m*hlen) + ")," + 
		      		 "SUBSTRING(qpartR FROM 1+(" + q + "-(" + row + "-" + m + " +1))*" + hlen + " FOR " + 
					 "(" + row + "-" + (m-1) + ")*" + hlen + "))" + 
		      "\nFROM Q_tmp WHERE " + (q-1) + "-" + row + ">=0"; 
		SQLTools.execute(f.getStatement(), 
				qry, "Filling new table " + Qv.getTblName() + "(1/2)");
		qry = "INSERT INTO " + tblQv + " SELECT " + Qd.getAtbTreeID() + "," + Qd.getAtbAnchorID() + "," + row + "," + 
	      "\nCONCAT(SUBSTRING(qpartL FROM 1 FOR (" + (q-1) + "-" + row + ")*" + hlen + "),\n" +
		  	     "SUBSTRING(" + qpart + " FROM 1 FOR (" + (m + q - 1) + "-" + row + ")*" + hlen + "),\n" + 
	      		 "SUBSTRING(qpartR FROM 1+(" + q + "-(" + row + "-" + m + " +1))*" + hlen + " FOR " + 
				 "(" + row + "-" + (m-1) + ")*" + hlen + "))" + 
	      "\nFROM Q_tmp WHERE " + (q-1) + "-" + row + "<0"; 
		SQLTools.executeUpdate(f.getStatement(), 
			qry, "Filling new table " + Qv.getTblName() + "(2/2)");
		
	}
	
	/**
	 * @return Returns the pparts.
	 */
	public PPartTbl getPparts() {
		return pparts;
	}
	/**
	 * @return Returns the qparts.
	 */
	public QPartTbl getQparts() {
		return Qparts;
	}

}
