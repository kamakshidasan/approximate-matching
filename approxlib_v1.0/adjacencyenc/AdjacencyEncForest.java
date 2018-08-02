/*
 */
package adjacencyenc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import sqltools.InsertBuffer;
import sqltools.TableWrapper;
import sqltools.SQLTools;

import index.Editable;
import index.EditLog;
import index.DeleteNode;
import index.RenameNode;
import index.InsertNode;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import tree.Forest;
import tree.MMForest;
import tree.LblValTree;
import tree.Node;

/**
 * @author augsten
 */
public class AdjacencyEncForest extends TableWrapper 
  	implements Forest, Editable {

	public String nullLabel = "*";
	public String OPEN_NODE = "{";
	public String CLOSE_NODE = "}";
	public String ID_NODE_SEP = ":";

	public static final String ATB_TREE_ID = "treeID";
	public static final String ATB_LABEL = "label";
	public static final String ATB_VALUE = "value";
	public static final String ATB_NODE_ID = "nodeID";
	public static final String ATB_PARENT_ID = "parentID";
	public static final String ATB_SIB_POS = "sibPos";

	private String atbTreeID = ATB_TREE_ID; // treeID of this node's tree
	private String atbNodeID = ATB_NODE_ID; // this node's ID
	private String atbSibPos = ATB_SIB_POS; // position of nodeID in children
										// array of parent
	private String atbParentID = ATB_PARENT_ID; // ID of parent of this node
	private String atbLabel = ATB_LABEL; // node label
	private String atbValue = ATB_VALUE; // node value
	
	private boolean writeLog = false;
	private EditLog editLog = new EditLog();
	
	//////////////////
	// Constructors //
	////////////////// 

	/**
	 * Construct a new <code>AdjacencyEncForest</code> with seperate in-stream
	 * connection and insert buffer.
	 * 
	 * @param con
	 * @param streamCon
	 * @param insBuff
	 * @param tblName
	 */
	public AdjacencyEncForest(Connection con, Connection streamCon,
			InsertBuffer insBuff, String tblName) {
		super(con, streamCon, insBuff, tblName);
	}

	/**
	 * Constructs a new <code>AdjacencyEncForest</code> with default values,
	 * where possible.
	 * 
	 * @param con
	 * @param tblName
	 */
	public AdjacencyEncForest(Connection con, String tblName) {
		super(con, tblName);
	}

	//////////////////
	// TableWrapper //
	////////////////// 

	/**
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	public String getAtbList() {
		return atbTreeID + "," + atbNodeID + "," + atbSibPos + ","
				+ atbParentID + "," + atbLabel + "," + atbValue;
	}

	/**
	 * @see sqltools.TableWrapper#create()
	 */
	public void create() throws SQLException {
		Statement s = getCon().createStatement();
		StringBuffer qryCreate = new StringBuffer("CREATE TABLE `"
				+ getTblName() + "` (");
		qryCreate.append(atbTreeID + " INT NOT NULL,");
		qryCreate.append(atbNodeID + " INT NOT NULL,");
		qryCreate.append(atbSibPos + " INT NOT NULL,");
		qryCreate.append(atbParentID + " INT NOT NULL,");
		qryCreate.append(atbLabel + " TEXT NOT NULL,");
		qryCreate.append(atbValue + " TEXT");
		qryCreate.append(")");
		s.execute(qryCreate.toString());
	}
	
	////////////////////////
	// Implement Editable //
	////////////////////////
	
	/**
	 * @see index.Editable#renameNode(java.lang.Object, java.lang.Object,
	 *      java.lang.String)
	 */
	public void renameNode(Object tree, Object node, String label)
			throws SQLException {
		int treeID = ((Integer) tree).intValue();
		int nodeID = ((Integer) node).intValue();

		// is operation defined?
		AdjNode nd = this.getNode(treeID, nodeID);
		String oldLabel;
		if (nd != null) {
			oldLabel = nd.getLabel();
		} else {
			throw new RuntimeException("Node " + nodeID + " of tree " + treeID
					+ " not found in table '" + this.getTblName() + "'.");
		}

		// update node label - no structure update necessary
		String qry = "UPDATE " + this.getTblName() + " SET " + atbLabel + " ='"
				+ SQLTools.escapeSingleQuote(label) + "'" + " WHERE "
				+ atbTreeID + "=" + treeID + " AND " + atbNodeID + "=" + nodeID;		
		this.getStatement().executeUpdate(qry);
		
		// write the log entry
		if (this.isWriteLog()) {
			editLog.push(new RenameNode(tree, node, oldLabel, label));
		}
	}

	/**
	 * @see index.Editable#deleteNode(java.lang.Object, java.lang.Object)
	 */
	public void deleteNode(Object tree, Object node) throws SQLException {
		int treeID = ((Integer) tree).intValue();
		int nodeID = ((Integer) node).intValue();
		AdjNode nd = this.getNode(treeID, nodeID);

		if (nd == null) {
			throw new RuntimeException("Node " + nodeID + " of tree " + treeID
					+ " not found in table '" + this.getTblName() + "'.");
		}
		if (nd.getParentID() == Node.NO_NODE) {
			throw new RuntimeException("Can not delete root node.");
		}

		// get number of children of nodeOfTree
		int fanout = this.getChildCount(treeID, nodeID);

		// store log
		if (this.isWriteLog()) {
			this.editLog.push(
					new DeleteNode(tree, node, nd.getLabel(), new Integer(nd.getParentID()),
							nd.getSibPos(), fanout));
		}

		String qry;
		
		// delete nodeOfTree
		qry = "DELETE FROM " + this.getTblName() + " WHERE " + atbTreeID + "="
				+ treeID + " AND " + atbNodeID + "=" + nodeID;
		this.getStatement().executeUpdate(qry);				
		
		// update right siblings of nodeOfTree
		int div = fanout - 1;
		if (div != 0) {
			qry = "UPDATE " + this.getTblName() + " SET " + atbSibPos + " = "
				+ atbSibPos + "+" + fanout + "-1" + " WHERE " + atbTreeID
				+ "=" + treeID + " AND " + atbParentID + "=" + nd.getParentID()
				+ " AND " + atbSibPos + ">" + nd.getSibPos();
			if (div < 0) {
				qry += " ORDER BY " + atbSibPos + " ASC";
			} else {
				qry += " ORDER BY " + atbSibPos + " DESC";				
			}
			this.getStatement().executeUpdate(qry);
		}		
		
		// update children of nodeOfTree
		qry = "UPDATE " + this.getTblName() + " SET " + atbParentID + " = "
				+ nd.getParentID() + "," + atbSibPos + " = " + atbSibPos + "+" + nd.getSibPos()
				+ "-1" + " WHERE " + atbTreeID + "=" + treeID + " AND "
				+ atbParentID + "=" + nodeID;
		this.getStatement().executeUpdate(qry);

	}

	/**
	 * @see index.Editable#insertNode(Object, Object, String, Object, int, int)
	 */
	public void insertNode(Object tree, Object newNode, String newLabel, Object parentNode,
			int k, int m) throws SQLException {

		int treeID = ((Integer) tree).intValue();
		int parentID = ((Integer) parentNode).intValue();
		int nodeID = getFreeNodeID(treeID, this.getStatement());

		String qry;

		// connect children to new node
		qry = "UPDATE " + this.getTblName() + " SET " + atbParentID
				+ "=" + nodeID + "," + atbSibPos + "= (" + atbSibPos + "-" + k
				+ " + 1)" + " WHERE " + atbTreeID + "=" + treeID + " AND "
				+ atbParentID + "=" + parentID + " AND " + atbSibPos + ">=" + k
				+ " AND " + atbSibPos + "<=" + (k + m - 1);
		SQLTools.executeUpdate(this.getStatement(),qry,"");

		// update right siblings of new node
		if (m != 1) {			
			qry = "UPDATE " + this.getTblName() + " SET " + atbSibPos + "= ("
				+ atbSibPos + "-" + m + " + 1)" + " WHERE " + atbTreeID + "="
				+ treeID + " AND " + atbParentID + "=" + parentID + " AND "
				+ atbSibPos + ">=" + (k + m);
			if (m < 1) {
				qry += " ORDER BY " + atbSibPos + " DESC";
			} else {
				qry += " ORDER BY " + atbSibPos + " ASC";
			}
			SQLTools.executeUpdate(this.getStatement(),qry,"");
		}


		// insert new node and connect it to parent
		qry = "INSERT INTO " + this.getTblName() + " (" + atbTreeID + ","
				+ atbNodeID + "," + atbSibPos + "," + atbParentID + ","
				+ atbLabel + ") VALUES (" + treeID + "," + nodeID + "," + k
				+ "," + parentID + ",'" + SQLTools.escapeSingleQuote(newLabel)
				+ "')";
		SQLTools.executeUpdate(this.getStatement(),qry,"");
		
		
		if (this.isWriteLog()) {
			editLog.push(new InsertNode(tree, new Integer(nodeID), newLabel, 
					parentNode, k, m));
		}
		
	}


	////////////////////////////////////////////
	// Implementation of tree.DatabaseAdapter //
	////////////////////////////////////////////


	/**
	 * @see tree.Forest#getTreeIDs()
	 */
	public int[] getTreeIDs() throws SQLException {
		String qry = 
			"SELECT DISTINCT " + atbTreeID 
			+ " FROM " + this.getTblName() + " ORDER BY " + this.atbTreeID;
		ResultSet rs = 
			SQLTools.executeQuery(this.getStatement(), qry,
					"Loading tree-IDs from IntervalEncForest table '" 
					+ this.getTblName() + "'");
		Vector treeIDs = new Vector();
		while (rs.next()) {
			treeIDs.add(new Integer(rs.getInt(1)));
		}
		int[] a = new int[treeIDs.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = ((Integer)treeIDs.elementAt(i)).intValue();
		}
		return a;
	}	
	
	/**
	 * @see tree.Forest#loadForest(int)
	 */
	public MMForest loadForest() throws SQLException {
		int[] ids = this.getTreeIDs();
		MMForest f = new MMForest(ids.length, 100);
		for (int i = 0; i < ids.length; i++) {
			f.add(loadTree(ids[i]));
		}
		return f;
	}
	
	/**
	 * @see tree.Forest#loadTree(int, int)
	 */
	public LblValTree loadTree(int treeID)
			throws SQLException {
		AdjNode root = this.getRootNode(treeID);
		LblValTree t = new LblValTree(root.getLabel(), root.getValue(), root.getTreeID());
		return loadTree(t, root.getNodeID());
	}
	
	/**
	 * Recursive method used by {@see #loadTree(int, int)}.
	 * 
	 * @param t
	 * @param parentID
	 * @return
	 * @throws SQLException
	 */
	private LblValTree loadTree(LblValTree t, int parentID) throws SQLException {
		String query = "SELECT `" + atbNodeID + "`,`" + atbLabel + "`,`" + atbValue + "` FROM `"
			+ this.getTblName() + "` WHERE `" + atbParentID + "`=" + parentID
			+ " AND `" + atbTreeID + "`=" + t.getTreeID() + " ORDER BY " + atbSibPos;
		ResultSet rs = SQLTools.executeQuery(this.getStatement(), query,
				"Get children of node " + parentID + ".");
		while (rs.next()) {
			LblValTree newNode = 
				new LblValTree(rs.getString(atbLabel), rs.getString(atbValue), t.getTreeID());
			int newParentID = rs.getInt(atbNodeID);
			t.add(loadTree(newNode, newParentID));
		}
		return t;
	}
	/**
	 * @see tree.Forest#storeForest(tree.MMForest)
	 */
	public void storeForest(MMForest f) throws SQLException {
		for (int i = 0; i < f.size(); i++) {
			storeTree((LblValTree)f.elementAt(i), 0, -1, Node.NO_NODE);
		}
		this.flush();

	}
	/**
	 * @see tree.Forest#storeTree(tree.LblValTree)
	 */
	public void storeTree(LblValTree t) throws SQLException {
		storeTree(t, 0, -1, Node.NO_NODE);
		this.flush();
	}
	
	/**
	 * Recursive method used by {@see #storeTree(LblValTree)}
	 * and {@see #storeForest(LblForest)}.
	 * @param t
	 * @param anchorID
	 * @param parentID
	 * @return
	 * @throws SQLException
	 */
	private int storeTree(LblValTree t, int anchorID, int sibPos, int parentID) 
	throws SQLException {
		storeNode(t.getTreeID(), anchorID, sibPos, parentID,
				t.getLabel(), t.getValue());
		parentID = anchorID;
		anchorID++;
		int pos = 1;
		for (Enumeration e = t.children(); e.hasMoreElements();) {
			anchorID = storeTree((LblValTree)e.nextElement(), anchorID, pos, parentID);
			pos++;
		}
		return anchorID;
	}

	
	////////////
	// Others //
	////////////
		
	/**
	 * Write an edit log?
	 * 
	 * @return Returns the writeLog.
	 */
	public boolean isWriteLog() {
		return this.writeLog;
	}
	
	/**
	 * Write an edit log: turn on or of.
	 * 
	 * @param writeLog The writeLog to set.
	 */
	public void setWriteLog(boolean writeLog) {
		this.writeLog = writeLog;
	}
	
	/**
	 * Clear the edit log written so far.
	 */
	public void resetEditLog() {
		this.editLog = new EditLog();
	}
	
	/**
	 * @return Returns the editLog.
	 */
	public EditLog getEditLog() {
		return editLog;
	}
	
	public void undoEditLog() throws SQLException {
		boolean oldSetting = this.isWriteLog();
		this.setWriteLog(false);
		System.out.println(editLog);
		System.out.println("_____________________________");
		while (!editLog.isEmpty()) {
			System.out.println("reversing: " + editLog.getLast());
			editLog.pop().reverseEditOp().applyTo(this);
		}
		System.out.println(editLog);
		System.out.println("_____________________________");
		this.setWriteLog(oldSetting);
	}

	
	/*
	 * Useful methods for accessing nodes in the tree, etc.
	 */

	/**
	 * Get the next free nodeID number. Used by {@link #insertNode(Object, Object, String, Object, int, int)}.
	 * 
	 * @param treeID tree
	 * @param s statement for query execution
	 * @return maximum nodeID in tree + 1
	 * @throws SQLException
	 */
	public int getFreeNodeID(int treeID, Statement s) throws SQLException {
		String qry = "SELECT MAX(" + atbNodeID + ") FROM " + this.getTblName()
				+ " WHERE " + atbTreeID + "=" + treeID;
		ResultSet rs = s.executeQuery(qry);
		rs.next();
		return rs.getInt(1) + 1;
	}

	/**
	 * Load the tree with key treeID from the standard table and build its
	 * String representation.
	 * 
	 * NOTE: Does not use streaming result set, as this does not work with
	 * recursion.
	 * 
	 * 
	 * @param treeID
	 *            index of the tree in the database, stored in column atbTreeID
	 * @return the tree in OLTree format
	 * 
	 * @throws SQLException
	 */
	public StringBuffer loadTreeString(int treeID) throws SQLException {
		String query = "SELECT " + atbNodeID + "," + atbLabel + " FROM `"
				+ this.getTblName() + "` WHERE " + atbParentID + "="
				+ Node.NO_NODE + " AND " + atbTreeID + "=" + treeID;
		ResultSet rs = SQLTools.executeQuery(this.getStatement(), query,
				"Get root node...");
		StringBuffer treeStrBuff = new StringBuffer();
		if (treeID != Node.NO_TREE_ID) {
			treeStrBuff = new StringBuffer(treeID + ID_NODE_SEP);
		}
		if (rs.next()) {
			int parentID = rs.getInt(1);
			String label = rs.getString(2);
			treeStrBuff.append(traversePreorder(treeID, parentID, label));
		}
		return treeStrBuff;
	}

	/**
	 * Used by {@link #loadTreeString(int)} as a recursive auxiliary function.
	 * 
	 * @param treeID
	 * @param parentID
	 * @param label
	 * @return 
	 * @throws SQLException
	 */
	private StringBuffer traversePreorder(int treeID, int parentID, String label)
			throws SQLException {
		String query = "SELECT " + atbNodeID + "," + atbLabel + " FROM `"
				+ this.getTblName() + "` WHERE " + atbParentID + "=" + parentID
				+ " AND " + atbTreeID + "=" + treeID + " ORDER BY " + atbSibPos;
		ResultSet rs = SQLTools.executeQuery(this.getStatement(), query,
				"Get children of node " + parentID + ".");
		StringBuffer result = new StringBuffer(OPEN_NODE + label);
		while (rs.next()) {
			result.append(traversePreorder(treeID, rs.getInt(1), rs
					.getString(2)));
		}
		return result.append(CLOSE_NODE);
	}

	/**
	 * Get the childern number <code>first</code> to <code>last</code> of
	 * node <code>parentID</code> of tree <code>treeID</code> (ordered).
	 * 
	 * Can deal with values for <code>first</code> and <code>last</code> that do not exist.
	 * 
	 * Efficient, especially with index on (treeID, nodeID, sibPos).
	 * 
	 * @param treeID
	 * @param parentID
	 * @param first number of the first child returned (leftmost child has number 1)
	 * @param last last child returned
	 * @return resultset with schema (nodeID, label)
	 * @throws SQLException
	 */
	public ResultSet getChildren(int treeID, int parentID, int first, int last)
			throws SQLException {
		String query = "SELECT " + atbNodeID + "," + atbLabel + " FROM `"
				+ this.getTblName() + "` WHERE " + atbTreeID + "=" + treeID
				+ " AND " + atbParentID + "=" + parentID + " AND " + atbSibPos
				+ ">=" + first + " AND " + atbSibPos + "<=" + last
				+ " ORDER BY " + atbSibPos;
		ResultSet rs = 
			SQLTools.executeQuery(this.getStatement(), query,
					"Getting children " + first + " to " + last 
					+ " of node " + parentID + " in tree " + treeID);
		return rs;
	}

	/**
	 * Get the childern number <code>first</code> and all to the left of it, of
	 * node <code>parentID</code> of tree <code>treeID</code> (ordered).
	 * 
	 * Efficient, especially with index on (treeID, nodeID, sibPos).
	 * 
	 * @param treeID
	 * @param parentID
	 * @param first number of the first child returned (leftmost child has number 1)
	 * @return resultset with schema (nodeID, label)
	 * @throws SQLException
	 */
	public ResultSet getChildren(int treeID, int parentID, int first)
			throws SQLException {
		String query = "SELECT " + atbNodeID + "," + atbLabel + " FROM "
				+ this.getTblName() + " WHERE " + atbTreeID + "=" + treeID
				+ " AND " + atbParentID + "=" + parentID + " AND " + atbSibPos
				+ ">=" + first
				+ " ORDER BY " + atbSibPos;
		ResultSet rs = 
			SQLTools.executeQuery(this.getStatement(), query,
					"Getting all children starting with child number " + first 
					+ " of node " + parentID + " in tree " + treeID);
		return rs;
	}
	
	
	/**
	 * Get all children (ordered) of a node in a resultset.
	 * 
	 * @param treeID
	 * @param parentID
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getChildren(int treeID, int parentID) throws SQLException {
		return this.getChildren(treeID, parentID, 1);
	}

	/**
	 * Get number of children of a node.
	 * 
	 * @param treeID
	 * @param parentID
	 * @return number of children of node <code>parentID</code>
	 * @throws SQLException
	 */
	public int getChildCount(int treeID, int parentID) throws SQLException {
		String query = "SELECT COUNT(*) as cnt " + " FROM `" + this.getTblName()
				+ "` WHERE " + atbTreeID + "=" + treeID + " AND " + atbParentID
				+ "=" + parentID;
		ResultSet rs = 
			SQLTools.executeQuery(this.getStatement(), query,
					"Getting number of children...");
		rs.next();
		return rs.getInt("cnt");
	}

	/**
	 * Get row that represents a node in a result set. IntervalEncCursor is already set on the correct row (no rs.next() required...).
	 * 
	 * @param treeID
	 * @param nodeID
	 * @return
	 * @throws SQLException
	 */
	public AdjNode getNode(int treeID, int nodeID) throws SQLException {
		String query = "SELECT " + atbLabel + "," + atbValue + "," + atbSibPos + ","
				+ atbParentID + " FROM `" + this.getTblName() + "` WHERE "
				+ atbTreeID + "=" + treeID + " AND " + atbNodeID + "=" + nodeID;
		ResultSet rs = SQLTools.executeQuery(this.getStatement(), 
				query, "Looking up node " + nodeID + " in tree " + treeID);
		if (rs.next()) {
			return new AdjNode(treeID, nodeID,
					rs.getInt(this.atbSibPos),
					rs.getInt(this.atbParentID),
					rs.getString(this.atbLabel),
					rs.getString(this.atbValue)
			);
		} else {
			return null;
		}
	}

	/**
	 * Get the root node id for the tree <code>treeID</code>.
	 * 
	 * @param treeID
	 * @return
	 * @throws SQLException
	 */
	public AdjNode getRootNode(int treeID) throws SQLException {
		String query = "SELECT * FROM `" + this.getTblName() + "`"
				+ " WHERE " + atbTreeID + "=" + treeID + " AND " + atbParentID
				+ "=" + Node.NO_NODE;
		ResultSet rs = SQLTools.executeQuery(this.getStatement(), 
				query, "Looking up root node in tree " + treeID);
		if (rs.next()) {
			AdjNode res = new AdjNode(treeID, 
					rs.getInt(this.atbNodeID),
					rs.getInt(this.atbSibPos),
					rs.getInt(this.atbParentID),
					rs.getString(this.atbLabel),
					rs.getString(this.atbValue)
			);
			if (rs.next()) {
				throw new RuntimeException("Tree " + treeID + " has more than 1 root nodes!");
			}
			return res;
		} else {
			return null;
		}
	}

	/*
	 * Getters and Setters
	 */

	/**
	 * @return Returns the atbLabel.
	 */
	public String getAtbLabel() {
		return atbLabel;
	}

	/**
	 * @return Returns the atbNodeID.
	 */
	public String getAtbNodeID() {
		return atbNodeID;
	}

	/**
	 * @return Returns the atbParentID.
	 */
	public String getAtbParentID() {
		return atbParentID;
	}

	/**
	 * @return Returns the atbSibPos.
	 */
	public String getAtbSibPos() {
		return atbSibPos;
	}

	/**
	 * @return Returns the atbTreeID.
	 */
	public String getAtbTreeID() {
		return atbTreeID;
	}

		
	
	public boolean equals(AdjacencyEncForest f) throws SQLException {
		return this.equals(f, f.atbTreeID);
	}	
	
	/**
	 * Creates primary index on the attributes (treeID,nodeID).
	 */
	public void createPrimaryIndex() {
		try {
			String qry = 
				"ALTER TABLE `" + this.getTblName() + "` ADD PRIMARY KEY ("
				+ this.getAtbTreeID() + "," + this.getAtbNodeID() + ")";
			SQLTools.execute(this.getStatement(), qry, "Creating primary index on (treeID, nodeID)");
		} catch (SQLException e) {}
	}
	
	/**
	 * Drops primary index on the attributes (treeID,nodeID).
	 */
	public void dropPrimaryIndex() {
		try {
			String qry = 
				"ALTER TABLE `" + this.getTblName() + "` DROP PRIMARY KEY";
			SQLTools.execute(this.getStatement(), qry, "Dropping primary index on (treeID, nodeID)");
		} catch (SQLException e) {}

	}

	/**
	 * Creates unique index on the attributes (treeID,parentID,sibPos).
	 *
	 */
	public void createSibPosIndex()  {
		try {
			String qry =
				"CREATE UNIQUE INDEX sibPos ON " + this.getTblName() + " ("
				+ atbTreeID + "," + atbParentID + "," + atbSibPos + ")";
			SQLTools.execute(this.getStatement(), qry, "Creating unique index on (treeID, parentID, sibPos)");
		} catch (SQLException e) {}
	}	

	/**
	 * Drops unique index on the attributes (treeID,parentID,sibPos).
	 */
	public void dropSibPosIndex() {
		try {
			String qry =
				"DROP INDEX sibPos ON " + this.getTblName();
			SQLTools.execute(this.getStatement(), qry, "Drpping unique index on (treeID, parentID, sibPos)");
		} catch (SQLException e) {}
	}
	
	/**
	 * Calls {@see #createPrimaryIndex()} and {@see #createSibPosIndex()}.
	 *
	 */
	public void createIndices() {
		createPrimaryIndex();
		createSibPosIndex();
	}
	
	/**
	 * Calls {@see #dropPrimaryIndex()} and {@see #dropSibPosIndex()}.
	 *
	 */
	public void dropIndices() {
		dropPrimaryIndex();
		dropSibPosIndex();
	}

	/**
	 * Insert a single node (=tuple) into the table using the insert buffer. 
	 * No checks for duplicates or consistency are done.
	 *  
	 * @param treeID
	 * @param anchorID
	 * @param sibPos
	 * @param label
	 * @param value
	 * @throws SQLException
	 */
	public void storeNode(int treeID, int anchorID, int sibPos, int parentID,
			String label, String value) 
	throws SQLException {	
		if (label == null) {
			throw new RuntimeException("Label of node in interval encoding can not be 'null'.");
		} else {
			label = "'" + SQLTools.escapeSingleQuote(label) + "'";
		}
		if (value == null) {
			value = "NULL";
		} else {
			value = "'" + SQLTools.escapeSingleQuote(value) + "'";
		}
		String tuple = 
			"(" + treeID + "," + anchorID + "," + sibPos + "," + parentID + "," 
			+ label + "," + value + ")";
		getInsBuff().insert(tuple);		
	}
	
	public Iterator<LblValTree> forestIterator() throws SQLException {
		throw new RuntimeException("forestIterator() not implemetned in " + this.getClass() + "."); // TODO
	}

	public long getForestSize() throws SQLException {
		throw new RuntimeException("getForestSize() not implemetned in " + this.getClass() + "."); // TODO
	}

}