/*
 * Created on Jan 18, 2005
 */
package intervalenc;


import index.Editable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import sqltools.SQLTools;
import sqltools.InsertBuffer;
import sqltools.TableWrapper;
import tree.Forest;
import tree.MMForest;
import tree.LblValTree;


/**
 * Wrapper for a forset in interval encoding in a database table.
 * 
 * @author augsten
 */
public class IntervalEncForest extends TableWrapper
		implements Forest, Editable {
	
	public final String ATB_TREEID = "treeID";
	public final String ATB_LABEL = "label";
	public final String ATB_VALUE = "value";
	public final String ATB_LFT = "lft";
	public final String ATB_RGT = "rgt";	
	
	private String atbTreeID = ATB_TREEID; // treeID of this node's tree
	private String atbLabel = ATB_LABEL;  // node label
	private String atbValue = ATB_VALUE; // node value
	private String atbLft = ATB_LFT; // left interval entpoint
	private String atbRgt = ATB_RGT; // right interval endpoint	
	
	
	/**
	 * OldIntervalEncForest that uses different connections for read and write.
	 * 
	 * @param con connection for write
	 * @param streamCon connection for read
	 * @param insBuff insert buffer for fast insert of many tuples
	 * @param tblName name of the table storing the forest
	 */
	public IntervalEncForest(Connection con, Connection streamCon,
			InsertBuffer insBuff, String tblName) {
		super(con, streamCon, insBuff, tblName);
		this.setTblName(tblName);
	}
	
	/**
	 * OldIntervalEncForest table that uses the same connection for read and write.
	 *	
	 * 	 * @param con
	 * @param tblName
	 */
	public IntervalEncForest(Connection con, String tblName) {
		this(con, con, new InsertBuffer(), tblName);
	}

	
	/* (non-Javadoc)
	 * @see sqltools.TableWrapper#create()
	 */
	public void create() throws SQLException {
		StringBuffer qryCreate = new StringBuffer("CREATE TABLE `" + getTblName() + "` (");
		qryCreate.append(atbTreeID + " INT NOT NULL,");
		//qryCreate.append(atbLabel + " VARCHAR(" + MAX_LABEL_LENGTH + "),");
		qryCreate.append(atbLabel + " TEXT NOT NULL,");
		qryCreate.append(atbValue + " TEXT,");
		qryCreate.append(atbLft + " INT NOT NULL,");
		qryCreate.append(atbRgt + " INT NOT NULL");
		qryCreate.append(")");
		this.getStatement().execute(qryCreate.toString());	
	}		
	
	/* (non-Javadoc)
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	public String getAtbList() {
		return atbTreeID + "," + atbLabel + "," + atbValue + "," +	atbLft + "," + atbRgt;
	}	
	
	
	/**
	 * Used by {@link #storeTree(LblTree, int)} to store a node using an insert buffer.
	 * 
	 * Must be public (used also by other classes...).
	 * 
	 */
	public void storeNode(int treeID, String label, String value, int lft, int rgt) 
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
			"(" + treeID + "," + label +"," + value + "," +
			lft + "," + rgt + ")";
		getInsBuff().insert(tuple);		
	}
	
	
	
	/**
	 * @see index.Editable#deleteNode(java.lang.Object, java.lang.Object)
	 *  
	 * tree id of the tree as an Integer object
	 * nodeOfTree lft-value of the node as an Integer object
	 * 
	 */
	public void deleteNode(Object tree, Object nodeOfTree) throws SQLException {
		int treeID = ((Integer)tree).intValue();
		int lft = ((Integer)nodeOfTree).intValue();
		
		String atbLft = this.atbLft;
		String atbRgt = this.atbRgt;
		
		Statement s = this.getCon().createStatement();
		
		// get rgt-value of nodeOfTree
		String qry = 
			"SELECT " + atbRgt + " FROM " + this.getTblName() + 
			" WHERE " + atbLft + "=" + lft;
		ResultSet rs = s.executeQuery(qry);
		int rgt;
		if (rs.next()) {
		   rgt = rs.getInt(1);
		} else {
			rgt = 0;
			throw new RuntimeException("Node does with lft=" + lft + " not exist.");
		}
		// delete nodeOfTree
		qry =
			"DELETE FROM " + this.getTblName() + " WHERE " + 
			atbLft + "=" + lft;
		s.executeUpdate(qry);
		
		// update descendants of nodeOfTree
		qry = 
			"UPDATE " + this.getTblName() + 
			" SET " + atbLft + "=" + atbLft + "-1," +
			atbRgt + "=" + atbRgt + "-1" +	
			" WHERE " + atbLft + ">" + lft + " AND " +
			atbRgt + "<" + rgt;
		s.executeUpdate(qry);
		
		// update ancestors
		qry = 
			"UPDATE " + this.getTblName() + 
			" SET " + atbRgt + "=" + atbRgt + "-2" +	
			" WHERE " + atbLft + "<" + lft + " AND " +
			atbRgt + ">" + rgt;
		s.executeUpdate(qry);
		
		// update all nodes after nodeOfTree in preorder
		qry = 
			"UPDATE " + this.getTblName() + 
			" SET " + atbLft + "=" + atbLft + "-2," +
			atbRgt + "=" + atbRgt + "-2" +	
			" WHERE " + atbLft + ">" + rgt;
		s.executeUpdate(qry);				
	}
	
	public void deleteNode(int treeID, int lft) throws SQLException {
		deleteNode(new Integer(treeID), new Integer(lft));
	}
	
	/**
	 * @see index.Editable#insertNode(java.lang.Object, java.lang.Object, java.lang.String, java.lang.Object, int, int)
	 *
	 * tree id of the tree as an Integer object
	 * newNode label of the newly inserted node as a String object
	 * parentNode lft-value of the node as an Integer object
	 */
	public void insertNode(Object tree, Object newNode, String label, Object parentNode,
			int k, int n) throws SQLException {
		throw (new RuntimeException("Method OldIntervalEncForest.insertNode() is not implemented."));
	}
	
	/** 
	 * @see index.Editable#renameNode(java.lang.Object, java.lang.Object, java.lang.String)
	 * 
	 * tree id of the tree as an Integer object
	 * nodeOfTree lft-value of the node as an Integer object
	 * label new label for the relabeled node
	 */
	public void renameNode(Object tree, Object nodeOfTree, String label) 
	throws SQLException {
		int treeID = ((Integer)tree).intValue();
		int lft = ((Integer)nodeOfTree).intValue();
		renameNode(treeID, lft, label);
	}
	
	/**
	 * Rename node <tt>lft</tt> of tree <tt>treeID</tt> to <tt>label</tt>.
	 * @param treeID
	 * @param lft
	 * @param label
	 * @throws SQLException
	 */
	public void renameNode(int treeID, int lft, String label) throws SQLException {
		String query =
			"UPDATE " + this.getTblName() + 
			" SET " + this.atbLabel + "='" + SQLTools.escapeSingleQuote(label) + "'" +
			" WHERE " + this.atbTreeID + "=" + treeID + 
			" AND "+ this.atbLft + "=" + lft;
		SQLTools.executeUpdate(this.getStatement(), query, 
				"Renaming node " + lft + " of tree " + treeID + " to '" + label + "'.");
	}
		
	public Cursor forestInPreorder() throws SQLException {
		Statement s = this.getStreamStatement();
		
		String qry = 
			"SELECT " + atbTreeID + "," + atbLabel + ", " + atbValue + "," 
				+ atbLft + "," + atbRgt +  
			" FROM " + this.getTblName() + 
			" ORDER BY " + atbTreeID + "," + atbLft;
		return new IntervalEncCursor(s.executeQuery(qry), this);
	}
	
	public Cursor treeInPreorder(int treeID) throws SQLException {
		Statement s = this.getStreamStatement();
		
		String qry = 
			"SELECT " + atbTreeID + "," + atbLabel + ", " + atbValue + "," 
				+ atbLft + "," + atbRgt 
				+ " FROM " + this.getTblName() 
				+ " WHERE " + atbTreeID + "=" + treeID  
				+ " ORDER BY " + atbTreeID + "," + atbLft;
		return new IntervalEncCursor(s.executeQuery(qry), this);
	}	

	
	/**
	 * Creates an index on the attributes (treeID, lft).
	 *
	 */
	public void createPreorderIndex()  {
		try {
			String qry =
				"CREATE UNIQUE INDEX preorder ON " + this.getTblName() + " ("
				+ atbTreeID + "," + atbLft + ")";
			this.getStatement().execute(qry);
		} catch (SQLException e) {}
	}

	/**
	 * Creates an index on the attributes (treeID, lft).
	 *
	 */
	public void dropPreorderIndex() {
		try {
			String qry =
				"DROP INDEX preorder ON " + this.getTblName();
			this.getStatement().execute(qry);
		} catch (SQLException e) {}
	}
		
	public long getTreeSize(int treeID) throws SQLException {
		String qry =
			"SELECT count(*) FROM " + this.getTblName() 
			+ " WHERE " + atbTreeID + "=" + treeID;
		ResultSet rs = this.getStatement().executeQuery(qry);
		rs.next();
		return rs.getLong(1);
	}
	
	
	/**
	 * Get node with given lft-value from tree with treeID (null if node not there...)
	 * @param treeID
	 * @param lft
	 * @return
	 * @throws SQLException
	 */
	public IntervalEncNode getNode(int treeID, int lft) throws SQLException {
		String qry = 
			"SELECT * FROM " + this.getTblName() + " WHERE " +
			this.getAtbTreeID() + "=" + treeID + " AND " +
			this.getAtbLft() + "=" + lft;
		ResultSet rs = SQLTools.executeQuery(this.getStatement(), qry, 
					"Getting node (lft=" + lft + ") of tree with id=" + treeID + ".");
		if (rs.next()) {
			return new IntervalEncNode(
					rs.getInt(this.getAtbTreeID()),
					rs.getString(this.getAtbLabel()),
					rs.getString(this.getAtbValue()),
					rs.getInt(this.getAtbLft()),
					rs.getInt(this.getAtbRgt()));
		} else {
			return null;
		}
	}

	public IntervalEncNode getRootNode(int treeID) throws SQLException {
		String qry = "SELECT MIN(" + atbLft + ") as lft FROM " 
		    + this.getTblName() + " WHERE " + atbTreeID + "=" + treeID;
		ResultSet rs = this.getStatement().executeQuery(qry); 
		if (rs.next()) {
			return this.getNode(treeID, rs.getInt("lft"));
		} else {
			return null;
		}
	}
	
	
	/**
	 * @return Returns the atbLabel.
	 */
	public String getAtbLabel() {
		return atbLabel;
	}

	/**
	 * @return Returns the atbValue.
	 */
	public String getAtbValue() {
		return atbValue;
	}

	/**
	 * @return Returns the atbLft.
	 */
	public String getAtbLft() {
		return atbLft;
	}

	/**
	 * @return Returns the atbRgt.
	 */
	public String getAtbRgt() {
		return atbRgt;
	}

	/**
	 * @return Returns the atbTreeID.
	 */
	public String getAtbTreeID() {
		return atbTreeID;
	}
		
	//////////////////////////////////////////////
	// Implementation of the Forest interface  //
	//////////////////////////////////////////////

	/*
	 * @see tree.Forest#getTreeIDs()
	 */
	public int[] getTreeIDs() throws SQLException {
		String tmpTbl = this.getTblName() + "_treeIDs";
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
		
	/*
	 * @see tree.Forest#storeTree(tree.LblValTree)
	 */
	public void storeTree(LblValTree t) throws SQLException {
		storeTree(t, 0);
		getInsBuff().flush();
	}
		
	/*
	 * @see tree.Forest#storeForest(tree.MMForest)
	 */
	public void storeForest(MMForest f) throws SQLException {
		for (int i = 0; i < f.size(); i++) {
			this.storeTree((LblValTree)f.elementAt(i));
		}
	}
	
	/*
	 * @see tree.Forest#loadTree(int)
	 */
	public LblValTree loadTree(int treeID) 
		throws SQLException {
		
		Cursor cursor = this.treeInPreorder(treeID);
		if (cursor.next()) {
			IntervalEncNode n = cursor.fetchNode();
			LblValTree t = new LblValTree(n.getLabel(), n.getValue(), n.getTreeID()); 
			loadTree(cursor, t, n.getRgt(), n.getTreeID()); 
			return t;
		} else {
			return null;
		}		
	}
	
	/*
	 * @see tree.Forest#loadForest(int)
	 */
	public MMForest loadForest() throws SQLException {
		MMForest f = new MMForest(500, 100);
		Cursor cursor = this.forestInPreorder();
		IntervalEncNode n = null;
		while (!cursor.isAfterLast()) {
			if (n == null) {
				cursor.next();
				n = cursor.fetchNode();
			}
			LblValTree t = new LblValTree(n.getLabel(), n.getValue(), n.getTreeID());
			n = loadTree(cursor, t, n.getRgt(), n.getTreeID());
			f.add(t);
		} 
		return f;
	}
	
	/**
	 * Recursive method used by {@link #storeTree(LblTree)}.
	 * 
	 * @param n the root node of the tree to store
	 * @param lft the left number of n
	 * @return the right number of n
	 */
	private int storeTree(LblValTree n, int lft) throws SQLException {
		int lftNum = lft;
		for (Enumeration e = n.children(); e.hasMoreElements();) {
			lft = storeTree((LblValTree)e.nextElement(), lft + 1);
		}
		int rgtNum = lft + 1;
		storeNode(n.getTreeID(), n.getLabel(), n.getValue(), lftNum, rgtNum);
		return rgtNum;
	}	
	
	/**
	 * Recursive method used by {@link #loadTree(int,int)}.
	 *
	 * @param rs resultset containing remaining tuples that represent the tree
	 * @param n current node in the tree
	 * @return the next node in the resultset that is not a descendant of n, 
	 *         null if no nodes are left.
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

	/*
	 * @see tree.Forest#forestIterator(tree.MMForest)
	 */
	public Iterator<LblValTree> forestIterator() throws SQLException {
		return new IEForestIterator(this);
	}

	public long getForestSize() throws SQLException {
		String qry =
			"SELECT COUNT(*) AS cnt FROM (SELECT DISTINCT " + 
			this.atbTreeID + " FROM " + this.getTblName() + ") AS X"; 
		ResultSet rs = this.getStatement().executeQuery(qry);
		rs.next();
		return rs.getLong("cnt");
	}
	
	

}
