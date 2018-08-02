/*
 * Created on Sep 27, 2006
 */
package string;

import intervalenc.IntervalEncForest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import sqltools.InsertBuffer;
import sqltools.SQLTools;
import sqltools.TableWrapper;

/**
 * @author augsten
 */
public class StringTbl extends TableWrapper {

	/**
	 * default name of the tree index attribute
	 */
	public final String ATB_TREE_ID = "treeID";

	/**
	 * default name of the hashed pq-gram
	 */
	public final String ATB_LABEL = "name";

	private String atbTreeID = ATB_TREE_ID;

	private String atbLabel = ATB_LABEL;

	/**
	 * @param con
	 * @param streamCon
	 * @param insBuff
	 * @param tblName
	 */
	public StringTbl(Connection con, Connection streamCon,
			InsertBuffer insBuff, String tblName) {
		super(con, streamCon, insBuff, tblName);
	}

	/**
	 * @param con
	 * @param tblName
	 */
	public StringTbl(Connection con, String tblName) {
		super(con, tblName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	@Override
	public String getAtbList() {
		return this.getAtbTreeID() + "," + this.getAtbLabel();
	}

	public void loadRootLabels(IntervalEncForest f) throws SQLException {
		String qry = "INSERT INTO " + this.getTblName() + " SELECT "
				+ f.getAtbTreeID() + " AS " + this.getAtbTreeID() + ","
				+ f.getAtbLabel() + " AS " + this.getAtbLabel() + " FROM "
				+ f.getTblName() + " WHERE " + f.getAtbLft() + "=0";
		SQLTools.execute(this.getStatement(), qry, "Loading root labels from '"
				+ f.getTblName() + "' to '" + this.getTblName() + "'");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sqltools.TableWrapper#create()
	 */
	@Override
	public void create() throws SQLException {
		StringBuffer qryCreate = new StringBuffer("CREATE TABLE `"
				+ getTblName() + "` (");
		qryCreate.append(atbTreeID + " INT NOT NULL,");
		qryCreate.append(atbLabel + " TEXT)");
		this.getStatement().execute(qryCreate.toString());
	}

	/**
	 * @return Returns the atbPQGram.
	 */
	public String getAtbLabel() {
		return atbLabel;
	}

	/**
	 * @return Returns the atbTreeID.
	 */
	public String getAtbTreeID() {
		return atbTreeID;
	}

	public String getName(int treeID) throws SQLException {
		ResultSet rs = this.getStatement().executeQuery(
				"SELECT `" + this.atbLabel + "` FROM `" + this.getTblName()
						+ "` WHERE `" + this.atbTreeID + "`=" + treeID);
		if (rs.next()) {
			return rs.getString(1);
		} else {
			return null;
		}
	}

	public void setAtbLabel(String atbLabel) {
		this.atbLabel = atbLabel;
	}

	public void setAtbTreeID(String atbTreeID) {
		this.atbTreeID = atbTreeID;
	}

}
