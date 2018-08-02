/*
 * Created on Aug 2, 2005
 */
package index;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;


import sqltools.InsertBuffer;
import sqltools.TableWrapper;
import sqltools.SQLTools;

import java.util.LinkedList;
import hash.FixedLengthHash;

/**
 * Stores the p-parts of a pq-gram in a table with schema (treeID, anchorID, sibPos, parentID, ppart). 
 * The p-parts are hashed. The hash values are concatenated and stored. 
 * 
 * Note: Some DMBS remove trailing spaces for CHAR attributes. As all pparts have the same length, when loading them,
 * it is easy to add removed spaces again.
 * 
 * @author augsten
 */
public class PPartTbl extends TableWrapper {
	
	/**
	 * default name of the tree index attribute  
	 */
	public final String ATB_TREE_ID = "treeID";
	
	/**
	 * default name of the anchor node index attribute (the anchor node of the p-part)   
	 */
	public final String ATB_ANCHOR_ID= "anchorID";
	
	/**
	 * default name of the sibling position attribute (the anchor node is child number <code>ATB_SIB_POS</code>)  
	 */
	public final String ATB_SIB_POS = "sibPos";
	
	/**
	 * default name of the parent node id attribute (  
	 */
	public final String ATB_PARENT_ID= "parentID";
	
	/**
	 * default name of the p-part attribute (p-part of the pq-gram)  
	 */
	public final String ATB_PPART= "ppart";

	/**
	 * @return Returns the atbAnchorID.
	 */
	public String getAtbAnchorID() {
		return atbAnchorID;
	}
	/**
	 * @return Returns the atbParentID.
	 */
	public String getAtbParentID() {
		return atbParentID;
	}
	/**
	 * @return Returns the atbPPart.
	 */
	public String getAtbPPart() {
		return atbPPart;
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
	private String atbTreeID = ATB_TREE_ID;
	private String atbAnchorID = ATB_ANCHOR_ID;
	private String atbSibPos= ATB_SIB_POS;
	private String atbParentID = ATB_PARENT_ID;
	private String atbPPart = ATB_PPART;
	
	private FixedLengthHash hf;

	/**
	 * p-parameter of the p-part stored in the profile.
	 */
	private int p;

	/**
	 * @param con
	 * @param insBuff
	 * @param tblName
	 * @param hf hash function
	 * @param p p-parameter of the p-part stored in the profile
	 * 
	 */
	public PPartTbl(Connection con, InsertBuffer insBuff,
			String tblName, FixedLengthHash hf, int p) {
		super(con, con, insBuff, tblName);
		this.hf = hf;
		this.p = p;
	}

	/**
	 * Returns a comma-separated list of the attribute names in the following order: 
	 * treeID, anchorID, sibPos, parentID, ppart.
	 * 
	 * Overridden method.
	 *
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	public String getAtbList() {		
		return atbTreeID + "," + atbAnchorID + "," + atbSibPos + "," + atbParentID + "," + atbPPart;
	}

	/**
	 * Overridden method.
	 *
	 * @see sqltools.TableWrapper#create()
	 */
	public void create() throws SQLException {
		StringBuffer qryCreate = new StringBuffer("CREATE TABLE `" + getTblName() + "` (");
		qryCreate.append(atbTreeID + " INT NOT NULL,");
		qryCreate.append(atbAnchorID + " INT NOT NULL,");
		qryCreate.append(atbSibPos + " INT NOT NULL,");
		qryCreate.append(atbParentID + " INT NOT NULL,");
		qryCreate.append(atbPPart + " CHAR(" + (this.hf.getLength() * p) + ")");
		qryCreate.append(")");
		this.getStatement().execute(qryCreate.toString());	
	}
	
	
	/**
	 * Add a p-part.
	 * 
	 * @param treeID
	 * @param anchorID anchor node of the p-part
	 * @param sibPos node <code>anchorID</code> is child number <code>sibPos</code> of node <code>parentID</code>
	 * @param parentID parent of node <code>anchorID</code>
	 * @param anc p-part
	 * 
	 * @throws SQLException 
	 * @throws RuntimeException
	 */
	public void addPPart(int treeID, int anchorID, int sibPos, int parentID, LinkedList anc)  
	throws SQLException,RuntimeException {
		// if p-part is compatible...
		if (anc.size() == this.p) {
			String h = hf.concatLst(anc);
			String ppart = SQLTools.escapeSingleQuote(h);
			this.getInsBuff().insert("(" + treeID + "," + anchorID + "," + sibPos + "," + parentID + ",'" + ppart + "')");
		} else {
			throw (new RuntimeException("Dimensions p-part (value 'p') not compatible with the p-parts already stored in the profile."));
		}
	}
	
	/**
	 * Add a p-part.
	 * 
	 * @param treeID
	 * @param anchorID anchor node of the p-part
	 * @param sibPos node <code>anchorID</code> is child number <code>sibPos</code> of node <code>parentID</code>
	 * @param parentID parent of node <code>anchorID</code>
	 * @param ppart p-part of parent as a string
	 * @param label hashed label of this node
	 * 
	 * @throws SQLException 
	 * @throws RuntimeException
	 */
	public void addPPart(int treeID, int anchorID, int sibPos, int parentID, String ppart, String label)  
	throws SQLException,RuntimeException {
		// if p-part is compatible...
		if (ppart.length() == this.p * this.hf.getLength()) {
			ppart = SQLTools.escapeSingleQuote(ppart.substring(hf.getLength(), ppart.length()) + label);
			String tuple = "(" + treeID + "," + anchorID + "," + sibPos + "," + parentID + ",'" + ppart + "')";
			this.getInsBuff().insert(tuple);
		} else {
			throw (new RuntimeException("Dimensions p-part (value 'p') not compatible with the p-parts already stored in the profile."));
		}
	}
	
	/**
	 * Get children at sibpos k to m of parentID (first sibpos is 1).
	 * 
	 * @param treeID
	 * @param parentID
	 * @param k
	 * @param m
	 * @return
	 */
	public int[] getChildren(int treeID, int parentID, int k, int m) throws SQLException {
		String qry =
			"SELECT " + this.getAtbAnchorID() + " FROM " + this.getTblName() +
			" WHERE " + this.getAtbParentID() + "=" + parentID +
			" AND " + this.getAtbSibPos() + ">=" + k +
			" AND " + this.getAtbSibPos() + "<=" + m;
		ResultSet rs = 
			SQLTools.executeQuery(this.getStatement(), qry, 
					"Loading children " + k + " to " + m + " of " + parentID);
		rs.last();
		int[] res = new int[rs.getRow()];
		rs.beforeFirst();
		int i = 0;
		while (rs.next()) {
			res[i] = rs.getInt(this.getAtbAnchorID());
			i++;
		}
		return res;
	}
	
	/**
	 * Get children at sibpos k to m of parentID (first sibpos is 1).
	 * 
	 * @param treeID
	 * @param parentID
	 * @param k
	 * @param m
	 * @return
	 */
	public String[] getHashedLabels(int treeID, int parentID, int k, int m) throws SQLException {
		String qry =
			"SELECT " + this.getAtbPPart() + " FROM " + this.getTblName() +
			" WHERE " + this.getAtbParentID() + "=" + parentID +
			" AND " + this.getAtbSibPos() + ">=" + k +
			" AND " + this.getAtbSibPos() + "<=" + m;
		ResultSet rs = 
			SQLTools.executeQuery(this.getStatement(), qry, 
					"Loading children " + k + " to " + m + " of " + parentID);
		rs.last();
		String[] res = new String[rs.getRow()];
		rs.beforeFirst();
		int i = 0;
		int len = this.getHf().getLength();
		int p = this.getP();
		while (rs.next()) {
			res[i] = rs.getString(this.getAtbPPart()).substring(len * (p - 1), len * p);
			i++;
		}
		return res;
	}	
	
	/**
	 * @return hash function used to store the p-parts
	 */
	public FixedLengthHash getHf() {
		return hf;
	}
	
	/**
	 * @return Returns the p.
	 */
	public int getP() {
		return p;
	}
	
	public void union(PPartTbl p) throws SQLException {
		String qry = "INSERT IGNORE INTO " + this.getTblName() + 
			" SELECT * FROM " + p.getTblName();
		SQLTools.execute(this.getStatement(), qry, 
				"Doing UNION of " + this.getTblName() + " and " + 
				p.getTblName());
	}
	
	public ResultSet getPPart(int treeID, int anchorID) throws SQLException {
		String qry =
			"SELECT * FROM " + this.getTblName() + " WHERE " +
			this.getAtbTreeID() + "=" + treeID + " AND " +
			this.getAtbAnchorID() + "=" + anchorID;
		return SQLTools.executeQuery(this.getStatement(), qry, 
				"Selecting ppart with anchor node " + anchorID + ".");
			
	}

	public String getPPartStr(int treeID, int anchorID) throws SQLException {
		String qry =
			"SELECT * FROM " + this.getTblName() + " WHERE " +
			this.getAtbTreeID() + "=" + treeID + " AND " +
			this.getAtbAnchorID() + "=" + anchorID;
		ResultSet rs =
			SQLTools.executeQuery(this.getStatement(), qry, 
				"Selecting ppart with anchor node " + anchorID + ".");
		rs.next();
		return rs.getString(this.getAtbPPart());
			
	}
	
	
	public void deletePPart(int treeID, int anchorID) throws SQLException {
		String qry =
			"DELETE FROM " + this.getTblName() + " WHERE " +
			this.getAtbTreeID() + "=" + treeID + " AND " +
			this.getAtbAnchorID() + "=" + anchorID;
		SQLTools.execute(this.getStatement(), qry, 
				"Deleting ppart with anchor node " + anchorID + ".");
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
		String query = "SELECT COUNT(*) as cnt " + " FROM " + this.getTblName()
				+ " WHERE " + atbTreeID + "=" + treeID + " AND " + atbParentID
				+ "=" + parentID;
		ResultSet rs = this.getStatement().executeQuery(query);
		rs.next();
		return rs.getInt("cnt");
	}
	
	/**
	 * Creates unique index on the attribute (anchorID).
	 */
	public void createAnchorIDIndex()  {
		try {
			String qry =
				"CREATE UNIQUE INDEX anchorID ON " + this.getTblName() + " ("
				+ atbAnchorID + ")";
			SQLTools.execute(this.getStatement(), qry, "Creating unique index on (anchorID)");
		} catch (SQLException e) {}
	}	

	/**
	 * Drops unique index on the attribute (anchorID).
	 */
	public void dropAnchorIDIndex() {
		try {
			String qry =
				"DROP INDEX anchorID ON " + this.getTblName();
			SQLTools.execute(this.getStatement(), qry, "Drpping unique index on (anchorID)");
		} catch (SQLException e) {}
	}
	
	/**
	 * Increment may be also negative.
	 * @param treeID
	 * @param parentID
	 * @param firstSib
	 * @param increment
	 */
	public void updateSibPos(int treeID, int parentID, int firstSib, int increment) throws SQLException {
		if (increment == 0) {
			return;
		}
		String qry = 
			"UPDATE " + this.getTblName()
			+ " SET " + this.getAtbSibPos() + "=" + this.getAtbSibPos()
			+ "+(" + increment + ")" 
			+ " WHERE " + this.getAtbTreeID() + "=" + treeID 
			+ " AND " + this.getAtbParentID() + "=" + parentID 
			+ " AND " + this.getAtbSibPos() + ">=" + firstSib;
		// avoid duplicate keys
		if (increment < 0) {
			qry += " ORDER BY " + this.getAtbSibPos() + " ASC";
		} else {
			qry += " ORDER BY " + this.getAtbSibPos() + " DESC";					
		}
		SQLTools.execute(this.getStatement(), qry, 
				"Increasing positions of children >= " + firstSib + " of node " + parentID 
				+ ", treeID=" + treeID + ", by "
				+ increment + ".");
	}
	
	/**
	 * Change parentID and increment sibling pos. Increment may be negative.
	 * 
	 * @param treeID
	 * @param oldParentID
	 * @param newParentID
	 * @param increment
	 */
	public void updateParentID(int treeID, int oldParentID, int newParentID, int increment) 
	throws SQLException { 
		updateParentID(treeID, oldParentID, newParentID, 0, Integer.MAX_VALUE, increment);
	}
	
	/**
	 * Change parentID and increment sibling pos of selected siblings. Increment may be negative.
	 * 
	 * @param treeID
	 * @param oldParentID
	 * @param newParentID
	 * @param increment
	 */
	public void updateParentID(int treeID, int oldParentID, int newParentID, int firstSib, int lastSib, int increment) 
	throws SQLException { 
		String qry =
			"UPDATE " + this.getTblName()
			+ " SET " + this.getAtbSibPos() + "=" + this.getAtbSibPos()
			+ "+(" + increment + ")," 
			+ this.getAtbParentID() + "=" + newParentID
			+ " WHERE " + this.getAtbTreeID() + "=" + treeID 
			+ " AND " + this.getAtbParentID() + "=" + oldParentID  
			+ " AND " + this.getAtbSibPos() + ">=" + firstSib 
			+ " AND " + this.getAtbSibPos() + "<=" + lastSib;
		// avoid duplicate keys
		if (increment < 0) {
			qry += " ORDER BY " + this.getAtbSibPos() + " ASC";
		} else {
			qry += " ORDER BY " + this.getAtbSibPos() + " DESC";					
		}			
		SQLTools.execute(this.getStatement(), qry, 
				"Move children of node " + oldParentID + " to node " + newParentID 
				+ ", treeID=" + treeID + ", incresing sibling positions by "
				+ increment + ".");
	}


	
}
