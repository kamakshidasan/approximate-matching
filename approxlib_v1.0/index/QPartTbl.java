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
 * Stores the q-parts of a pq-gram in a table with schema (treeID, anchorID, row, qpart). 
 * The q-parts are hashed, the hash values are concatenated and stored.
 * 
 * Note: Some DMBS remove trailing spaces for CHAR attributes. As all qparts have the same length, when loading them,
 * it is easy to add removed spaces again.
 * 
 * @author augsten
 */
public class QPartTbl extends TableWrapper {
	
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
	public final String ATB_ROW = "row";
	
	/**
	 * default name of the q-part attribute (q-part of the pq-gram)  
	 */
	public final String ATB_QPART= "qpart";

	private String atbTreeID = ATB_TREE_ID;
	private String atbAnchorID = ATB_ANCHOR_ID;
	private String atbRow = ATB_ROW;
	private String atbQPart = ATB_QPART;
	
	private FixedLengthHash hf;

	/**
	 * q-parameter of the p-part stored in the profile.
	 */
	private int q;
	
	/**
	 * counts all q-parts stored in this table. 
	 */
	private int size;

	//private boolean lonlyLeaf = false;
	
	/**
	 * @param con
	 * @param insBuff
	 * @param tblName
	 * @param hf hash function
	 * @param q p-parameter of the p-part stored in the profile
	 * 
	 */
	public QPartTbl(Connection con, InsertBuffer insBuff,
			String tblName, FixedLengthHash hf, int q) {
		super(con, con, insBuff, tblName);
		this.hf = hf;
		this.q = q;
		this.size = 0;
//		this.lonlyLeaf = false;
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
		return atbTreeID + "," + atbAnchorID + "," + atbRow + "," + atbQPart;
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
		qryCreate.append(atbRow + " INT NOT NULL,");
		qryCreate.append(atbQPart + " CHAR(" + (this.hf.getLength() * this.q) + ")");
		qryCreate.append(")");
		this.getStatement().execute(qryCreate.toString());
		this.size = 0;
	}
	
	
	/**
	 * Add a q-part.
	 * 
	 * @param treeID
	 * @param anchorID anchor node of the p-part
	 * @param row row of the q-part in the q-part matrix of the anchor <code>anchorID</code>
	 * @param sib q-part
	 * 
	 * @throws SQLException 
	 * @throws RuntimeException
	 */
	public void addQPart(int treeID, int anchorID, int row, LinkedList sib)  
	throws SQLException,RuntimeException {
		// if q-part is compatible...
		if (sib.size() == this.q) {
			String h = hf.concatLst(sib);
			String qpart = SQLTools.escapeSingleQuote(h);
			this.getInsBuff().insert("(" + treeID + "," + anchorID + "," + row + ",'" + qpart + "')");
			this.size++;
		} else {
			throw (new RuntimeException("Dimensions q-part (value 'q') not compatible with the q-parts already stored in the profile."));
		}
	}
	
	/**
	 * Add a p-part.
	 * 
	 * @param treeID
	 * @param anchorID anchor node of the p-part
	 * @param row row of the q-part in the q-part matrix of the anchor <code>anchorID</code>
	 * @param qpart q-part
	 * 
	 * @throws SQLException 
	 * @throws RuntimeException
	 */
	public void addQPart(int treeID, int anchorID, int row, String qpart)  
	throws SQLException,RuntimeException {
		// if q-part is compatible...
		if (qpart.length() == this.q * this.getHf().getLength()) {
			qpart = SQLTools.escapeSingleQuote(qpart);
			this.getInsBuff().insert("(" + treeID + "," + anchorID + "," + row + ",'" + qpart + "')");
			this.size++;
		} else {
			throw (new RuntimeException("Dimensions q-part (value 'q') not compatible with the q-parts already stored in the profile."));
		}
	}
	
	/**
	 * Add a single q-gram that is fillded with null nodes.
	 * 
	 * @param treeID
	 * @param anchorID
	 * @param row
	 * @throws SQLException
	 * @throws RuntimeException
	 */
	public void addLeafQPart(int treeID, int anchorID, int row) throws SQLException,RuntimeException {
		StringBuffer qpart = new StringBuffer();
		String strNullNode = this.getHf().getNullNode().toString();
		for (int i = 0; i < this.getQ(); i++) {
			qpart.append(strNullNode);
		}
		this.addQPart(treeID, anchorID, row, qpart.toString());
	}
	
	/**
	 * @return Returns the q.
	 */
	public int getQ() {
		return q;
	}
	/**
	 * @return hash function used to store the p-parts
	 */
	public FixedLengthHash getHf() {
		return hf;
	}
	
	/**
	 * Issues no SQL query.
	 * 
	 * @return tuples stored in the table since object was created 
	 * (counter is reset with table reset/creation, counter is not persistent).
	 */
	public long getSize() {
		return size;
	}
	
	
	/**
	 * @return Returns the atbAnchorID.
	 */
	public String getAtbAnchorID() {
		return atbAnchorID;
	}
	/**
	 * @param atbAnchorID The atbAnchorID to set.
	 */
	public void setAtbAnchorID(String atbAnchorID) {
		this.atbAnchorID = atbAnchorID;
	}
	/**
	 * @return Returns the atbQPart.
	 */
	public String getAtbQPart() {
		return atbQPart;
	}
	/**
	 * @param atbQPart The atbQPart to set.
	 */
	public void setAtbQPart(String atbQPart) {
		this.atbQPart = atbQPart;
	}
	/**
	 * @return Returns the atbRow.
	 */
	public String getAtbRow() {
		return atbRow;
	}
	/**
	 * @param atbRow The atbRow to set.
	 */
	public void setAtbRow(String atbRow) {
		this.atbRow = atbRow;
	}
	/**
	 * @return Returns the atbTreeID.
	 */
	public String getAtbTreeID() {
		return atbTreeID;
	}
	/**
	 * @param atbTreeID The atbTreeID to set.
	 */
	public void setAtbTreeID(String atbTreeID) {
		this.atbTreeID = atbTreeID;
	}
//	/**
//	 * @return Returns the lonlyLeaf.
//	 */
//	public boolean isLonelyLeaf() {
//		return lonlyLeaf;
//	}
//	/**
//	 * @param lonlyLeaf The lonlyLeaf to set.
//	 */
//	public void setLonelyLeaf(boolean lonlyLeaf) {
//		this.lonlyLeaf = lonlyLeaf;
//	}
	
	public void deleteNode(int treeID, int nodeID, int k, int parentID) throws SQLException {		
		String Q = this.getTblName();
		String row = this.getAtbRow();
		String qpart = this.getAtbQPart();
		
		int chCnt = this.getChildCount(treeID, nodeID);
		
		if (chCnt == -1) {
			throw new RuntimeException("Can not delete node " + nodeID + ", because it does not exist!");
		} else {
			
			if (chCnt == 0) {
				LinkedList sib = this.getHf().getEmptyRegister(this.getQ());
				this.addQPart(treeID, nodeID, 1, sib);				
				this.getInsBuff().flush();
			}
		
			// get changing rows of Q(parentID) as temporary tables
			
			try {
				this.getStatement().execute("DROP TABLE QL_tmp");
				this.getStatement().execute("DROP TABLE QR_tmp");
				this.getStatement().execute("DROP TABLE Q_tmp_L");
				this.getStatement().execute("DROP TABLE Q_tmp_R");
			} catch (Exception e) {}
			
			String qry =
				"CREATE TABLE QL_tmp AS " +
				"SELECT * FROM " + Q + 
				" WHERE " + 
				this.getAtbTreeID() + "=" + treeID + " AND " +
				this.getAtbAnchorID() + "=" + parentID + " AND " +
				row + ">=" + (k - 1) + " AND " + 
				row + "<" + ((k - 1) + (q - 1));
			SQLTools.execute(this.getStatement(), qry,
					"Creating temporary table " + "QL_tmp");
			qry =
				"CREATE TABLE QR_tmp AS " +
				"SELECT * FROM " + Q + 
				" WHERE " + 
				this.getAtbTreeID() + "=" + treeID + " AND " +
				this.getAtbAnchorID() + "=" + parentID + " AND " +
				row + ">=" + (k) + " AND " + 
				row + "<" + (k + (q - 1));
			SQLTools.execute(this.getStatement(), qry,
					"Creating temporary table " + "QR_tmp");
			
			// update qparts in Q(nodeID)				
			
			int hlen = this.getHf().getLength();
			
			// LEFT
			
			qry = "CREATE TABLE Q_tmp_L AS\n" + 
			"SELECT L." + qpart + " AS qpartL, M." + row + "+1 AS i," + " M.*\n" +
			"FROM " + Q + " AS M, QL_tmp AS L \n" +
			"WHERE " +  
			"M." + this.getAtbTreeID() + "=" + treeID + " AND " +
			"M." + this.getAtbAnchorID() + "=" + nodeID + " AND " +
			"L." + row + "-" + (k - 1) + "=M." + row + " ";
			
			SQLTools.execute(this.getStatement(), 
					qry, "Creating Q_tmp_L: Joining tables QL_tmp and QR_tmp on " + row + " attribute");
			
			qry = "DELETE FROM " + Q + " WHERE " + 
			this.getAtbTreeID() + "=" + treeID + " AND " +
			this.getAtbAnchorID() + "=" + nodeID + " AND " +
			row + "<" + this.q + "-1";
			SQLTools.execute(this.getStatement(), qry, "deleting...");
			
			qry = "INSERT INTO " + Q + "\n" + 
			"SELECT " +
			this.getAtbTreeID() + "," +
			this.getAtbAnchorID() + "," +
			this.getAtbRow() + "," +
			"CONCAT(SUBSTRING(qpartL FROM 1 FOR (" + q + "-i)*" + hlen + ")," +
			"SUBSTRING(" + qpart + " FROM 1+(" + q + "-i)*" + hlen + " FOR " + q * hlen + "))" +
			" FROM " + "Q_tmp_L";
			SQLTools.execute(this.getStatement(), 
					qry, "Substitute at beginning...");
			
			// RECHTS		
			qry = "CREATE TABLE Q_tmp_R AS\n" + 
			"SELECT L." + qpart + " AS qpartL, M." + row + "-" + chCnt + "+1 AS i," + " M.*\n" +
			"FROM " + Q + " AS M, QR_tmp AS L \n" +
			"WHERE " +  
			"M." + this.getAtbTreeID() + "=" + treeID + " AND " +
			"M." + this.getAtbAnchorID() + "=" + nodeID + " AND " +
			"L." + row + "-" + k + "=M." + row + "-" + chCnt;		
			SQLTools.execute(this.getStatement(), 
					qry, "Creating Q_tmp_R: Joining tables QL_tmp and QR_tmp on " + row + " attribute");
			
			qry = "DELETE FROM " + Q + " WHERE " + 
			this.getAtbTreeID() + "=" + treeID + " AND " +
			this.getAtbAnchorID() + "=" + nodeID + " AND " +
			row + ">" + (chCnt - 1);
			SQLTools.execute(this.getStatement(), qry, "deleting...");
			
			qry = "INSERT IGNORE INTO " + Q + "\n" + 
			"SELECT " +
			this.getAtbTreeID() + "," +
			this.getAtbAnchorID() + "," +
			this.getAtbRow() + "," +
			"CONCAT("  + 
			"SUBSTRING(" + qpart + " FROM 1 FOR (" + q + "-i)*" + hlen + ")," +
			"SUBSTRING(qpartL FROM 1+(" + q + "-i)*" + hlen + " FOR i*" + hlen + "))" +
			" FROM " + "Q_tmp_R";
			SQLTools.execute(this.getStatement(), 
					qry, "Substitute at end...");
			
			// delete substituted rows in Q(parentID)
			qry = "DELETE FROM " + Q + " WHERE " + 
				this.getAtbTreeID() + "=" + treeID + " AND " +
				this.getAtbAnchorID() + "=" + parentID + " AND " +
				row + ">=" + (k - 1) + " AND " + 
				row + "<" + ((k - 1) + q);
			SQLTools.execute(this.getStatement(), 
					qry, "Deleting subsituted part in Q(parentID)...");

			// update structure in Q(parentID)
			if (chCnt != 1) {
				qry = "UPDATE " + Q +
					" SET " + row + "=" + row + "+" + (chCnt-1) +
					" WHERE " +
					this.getAtbTreeID() + "=" + treeID + " AND " +
					this.getAtbAnchorID() + "=" + parentID + " AND " +
					row + ">=" + ((k - 1) + q);
				if (chCnt == 0) {
					qry += " ORDER BY " + row + " ASC";
				} else {
					qry += " ORDER BY " + row + " DESC";					
				}
			}
			SQLTools.execute(this.getStatement(), 
					qry, "updating rows in Q(parentID)...");
			
			// update structure and anchorID in Q(nodeID)
			qry = "UPDATE " + Q + 
			" SET " + row + "=" + row + "+" + (k - 1) + "," +
			this.getAtbAnchorID() + "=" + parentID +
			" WHERE " +
			this.getAtbTreeID() + "=" + treeID + " AND " +
			this.getAtbAnchorID() + "=" + nodeID; 
			SQLTools.execute(this.getStatement(), 
					qry, "updating rows and anchor nodes in Q(nodeID)...");
		}
		
	}
	
	/**
	 * Carefull! Assumes, that all childrenof nodeID are in the QPartTbl. Almost never true! 
	 * 
	 * @param treeID
	 * @param nodeID
	 * @return -1 if node does not exist, 0 for leaf node, number of children for other nodes 
	 * @throws SQLException
	 */
	public int getChildCount(int treeID, int nodeID) throws SQLException {
		String qry = "SELECT COUNT(*) AS cnt FROM " + this.getTblName() + " WHERE " +
			this.getAtbTreeID() + "=" + treeID + " AND " +
			this.getAtbAnchorID() + "=" + nodeID;
		ResultSet rs = 
			SQLTools.executeQuery(this.getStatement(), qry, 
			"Computing fanout of node " + nodeID + ".");
		rs.next();
		int m = rs.getInt("cnt");
		if (m == 0) { // node does not exist
			return -1;
		} else if (m == 1) { // leaf
			return 0;			
		} else {
			m = rs.getInt("cnt") - (this.getQ() - 1);
		}
		return m;
	}
	
	public void deleteQPart(int treeID, int anchorID, int row) throws SQLException {
		String qry =
			"DELETE FROM `" + this.getTblName() + "`  " + 
			"WHERE " 
			+ this.getAtbTreeID() + "=" + treeID + " AND "
			+ this.getAtbAnchorID() + "=" + anchorID + " AND "
			+ this.getAtbRow() + "=" + row;
		SQLTools.execute(this.getStatement(), qry,
				"Deleting row " + row + ", anchorID=" + anchorID + 
				", of tree " + treeID + ".");
	}
	
	/**
	 * Loading the rows containing the k-th and the m-th diagonal of 
	 * q-part of node (treeID, anchorID).
	 * 
	 * @param treeID
	 * @param anchorID
	 * @param k 
	 * @param m
	 * @return
	 * @throws SQLException
	 */
	public QMat loadQMat(int treeID, int anchorID, int k, int m) throws SQLException {
		String qry =
			"SELECT " + this.atbQPart + " FROM " + this.getTblName() 
			+ " WHERE " + this.getAtbTreeID() + "=" + treeID 
			+ " AND " + this.getAtbAnchorID() + "=" + anchorID 
			+ " AND (((" 
			+ this.atbRow + ">=" + k + ") AND (" 
			+ this.atbRow + "<=" + (k + q - 2) + ")) OR (("
			+ this.atbRow + ">=" + (m + 1) + ") AND (" 
			+ this.atbRow + "<=" + (m + q - 1) + ")))";
		ResultSet rs = 
			SQLTools.executeQuery(this.getStatement(), qry, 
					"Loading Q^km(" + anchorID + ") for k=" 
					+ k + " and m=" + m + " treeID=" + treeID + ".");
		QMat qm = new QMat(this.getQ());
		while (rs.next()) {
			qm.addRow(rs.getString(this.atbQPart));
		}
		return qm;
	}
	
	/**
	 * Deleting the rows containing the k-th and the m-th diagonal of 
	 * q-part of node (treeID, anchorID).
	 * 
	 * @param treeID
	 * @param anchorID
	 * @param k
	 * @param m
	 * @throws SQLException
	 */
	public void deleteQMat(int treeID, int anchorID, int k, int m) throws SQLException {
		String qry =
			"DELETE FROM " + this.getTblName() 
			+ " WHERE " + this.getAtbTreeID() + "=" + treeID 
			+ " AND " + this.getAtbAnchorID() + "=" + anchorID 
			+ " AND (((" 
			+ this.atbRow + ">=" + k + ") AND (" 
			+ this.atbRow + "<=" + (k + q - 2) + ")) OR (("
			+ this.atbRow + ">=" + (m + 1) + ") AND (" 
			+ this.atbRow + "<=" + (m + q - 1) + ")))";
			SQLTools.execute(this.getStatement(), qry, 
					"Deleting Q^km(" + anchorID + ") for k=" 
					+ k + " and m=" + m + " treeID=" + treeID + ".");		
	}
	
	public void storeQMat(int treeID, int anchorID, int firstRow, QMat qm) throws SQLException {
		for (int i = 0; i < qm.getRows(); i++) {
			this.addQPart(treeID, anchorID, firstRow + i, qm.getQPart(i));
		}
		this.flush();
	}
	
	/**
	 * Increment may be negative
	 * @param treeID
	 * @param anchorID
	 * @param firstRow
	 * @param increment
	 * @throws SQLException
	 */
	public void updateRows(int treeID, int anchorID, int firstRow, int increment) 
	throws SQLException {
		if (increment == 0) {
			return;
		}
		String qry =
			"UPDATE " + this.getTblName()
			+ " SET " + this.getAtbRow() + "=" + this.getAtbRow()
			+ "+(" + increment + ")" 
			+ " WHERE " + this.getAtbTreeID() + "=" + treeID 
			+ " AND " + this.getAtbAnchorID() + "=" + anchorID 
			+ " AND " + this.getAtbRow() + ">=" + firstRow;
		// avoid duplicate keys
		if (increment < 0) {
			qry += " ORDER BY " + this.getAtbRow() + " ASC";
		} else {
			qry += " ORDER BY " + this.getAtbRow() + " DESC";					
		}
		SQLTools.execute(this.getStatement(), qry, 
				"Increasing rows>=" + firstRow + " of Q(" + anchorID 
				+ "), treeID=" + treeID + ", by "
				+ increment + ".");
		
	}

	
	/**
	 * Rows start with 0.
	 * 
	 * @param treeID
	 * @param anchorID
	 * @param firstRow 
	 * @param increment
	 * @throws SQLException
	 */
	public void updateAnchorIDs(int treeID, int oldAnchorID, int newAnchorID, int k, int m) 
	throws SQLException {
		
		String qry =
			"UPDATE " + this.getTblName()
			+ " SET " + this.getAtbAnchorID() + "=" + newAnchorID
			+ " WHERE " + this.getAtbTreeID() + "=" + treeID 
			+ " AND " + this.getAtbAnchorID() + "=" + oldAnchorID 
			+ " AND " + this.getAtbRow() + ">=" + k
			+ " AND " + this.getAtbRow() + "<=" + m;
		SQLTools.execute(this.getStatement(), qry, 
				"Changing anchorID of " + k + " <= rows <= " + m + " of Q(" + oldAnchorID 
				+ "), treeID=" + treeID + ", to anchorID="
				+ newAnchorID + ".");
		
	}
	
	
}
