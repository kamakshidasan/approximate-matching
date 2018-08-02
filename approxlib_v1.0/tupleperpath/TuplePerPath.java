/*
 * Created on Nov 3, 2006
 */
package tupleperpath;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import sqltools.InsertBuffer;
import sqltools.SQLTools;
import sqltools.TableWrapper;
import tree.Forest;
import tree.MMForest;
import tree.LblTree;
import tree.LblValTree;
import tree.Node;
import util.FormatUtilities;

/**
 * @author augsten
 */
public class TuplePerPath extends TableWrapper implements Forest {

	private String atbTreeID;
	private String[] atbLevel;
	private String[] sortType;   // used to cast values when sorting
	private String[] columnType;
	
	//////////////////
	// Constructors //
	//////////////////
	
	/**
	 * 
	 * @param con
	 * @param streamCon
	 * @param insBuff
	 * @param tblName
	 * @param atbTreeID
	 * @param atbLevel
	 */
	public TuplePerPath(Connection con, Connection streamCon,
			InsertBuffer insBuff, String tblName, String atbTreeID, String[] atbLevel) {
		super(con, streamCon, insBuff, tblName);
		this.atbTreeID = atbTreeID;
		this.atbLevel = atbLevel;
		this.sortType = new String[atbLevel.length];
		this.columnType = new String[atbLevel.length];
		for (int i = 0; i < this.columnType.length; i++) {
			columnType[i] = "VARCHAR(255)";
		}
	}

	/** 
	 * @param con
	 * @param tblName
	 * @param atbTreeID
	 * @param atbLevel
	 */
	public TuplePerPath(Connection con, String tblName, String atbTreeID, String[] atbLevel) {
		this(con, con, new InsertBuffer(), tblName, atbTreeID, atbLevel);
	}
	
	/////////////
	// Methods //
	/////////////
	
	public boolean hasTreeIDs() throws SQLException {
		ResultSetMetaData meta =
			SQLTools.getMetaData(this.getTblName(), this.getCon());
		for (int i = 1; i <= meta.getColumnCount(); i++) {
			if (meta.getColumnName(i).equals(this.atbTreeID)) {
				return true;
			}
		}
		return false;
	}
		
	public void insertTreeIDs() throws SQLException {
		SQLTools.createIndex("root", this, new String[] {this.atbLevel[0]});
		// get all root labels
		String qry =
			"SELECT DISTINCT `" + this.atbLevel[0] + "` FROM `" + this.getTblName() + "`";
		ResultSet rs =
			SQLTools.executeQuery(this.getStatement(), qry,
					"Reading all root labels from '" + this.getTblName() + "'");		
		// insert a treeID column
		qry = "ALTER TABLE `" + this.getTblName()  
			+ "` ADD `" + this.atbTreeID + "` INT UNSIGNED NOT NULL"; 
		SQLTools.execute(this.getStatement(), qry,
				"Inserting new column into '" + this.getTblName() + "'");					
		// set the treeID values
		int i = 0;
		while (rs.next()) {
			String root = rs.getString(1);
			qry = "UPDATE `" + this.getTblName() + "` SET `" + this.atbTreeID 
			 	+ "`=" + i + " WHERE `" + this.atbLevel[0] + "`='" 
				+ SQLTools.escapeSingleQuote(root) + "'";
			SQLTools.execute(this.getStatement(), qry, 
					"Setting treeID=" + i + " for tree with root '" + root + "'");
			i++;
		}
		this.createTreeIDIndex();
		SQLTools.dropIndex("root", this);
	}	
	
	/**
	 * Creates an index on the attribute (treeID).
	 *
	 */
	public void createTreeIDIndex()  {
		SQLTools.createIndex("treeID", this, new String[] {this.atbTreeID});
	}
	
	/**
	 * Creates an index on the attribute (treeID).
	 *
	 */
	public void droptreeIDIndex() {
		SQLTools.dropIndex("treeID", this);
	}
		
	///////////////////////
	// Inherited Methods //
	///////////////////////
	
	/**
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	@Override
	public String getAtbList() {
		return "`" + atbTreeID + "`," + FormatUtilities.commaSeparatedList(atbLevel, '`');
	}
	
	/**
	 * @see sqltools.TableWrapper#create()
	 */
	@Override
	public void create() throws SQLException {
		StringBuffer qryCreate = new StringBuffer("CREATE TABLE `" + getTblName() +
				"` (" + atbTreeID + " INT NOT NULL,");
		for (int i = 0; i < atbLevel.length; i++) {
			qryCreate.append(atbLevel[i] + " " + columnType[i]);
			if (i < atbLevel.length - 1) {
				qryCreate.append(",");
			}
		}
		qryCreate.append(")");
		SQLTools.execute(this.getStatement(), qryCreate.toString(),
				"Creating TuplePerPath table '" + this.getTblName() + "'");
	}

	//////////////////////////////////////////////////////
	// Implementation of the DatabaseAdapter interface  //
	//////////////////////////////////////////////////////
	
	/**
	 * Stores only the labels and ignores the values.
	 * 
	 * @see tree.Forest#storeTree(tree.LblValTree)
	 */
	public void storeTree(LblValTree t) throws SQLException {		
		LblTree[] leafs = t.getLeafs();
		for (int i = 0; i < leafs.length; i++) {
			String[] tuple = new String[atbLevel.length];
			Arrays.fill(tuple, ""); // patch short paths with empty values
			int j = 0;
			for (Enumeration path = leafs[i].pathFromAncestorEnumeration(t); path.hasMoreElements();) {
				tuple[j] = SQLTools.escapeSingleQuote( 
					((LblTree)path.nextElement()).getLabel());
				j++;
			}
			String s = "(" + t.getTreeID() + "," 
				+ FormatUtilities.commaSeparatedList(tuple, '\'')
				+ ")";
			this.getInsBuff().insert(s);
		}
	}		

	/**
	 * @see tree.Forest#storeForest(tree.MMForest)
	 */
	public void storeForest(MMForest f) throws SQLException {
		for (int i = 0; i < f.size(); i++) {
			storeTree(f.elementAt(i));
		}
	}

	/**
	 * NOTE: steamBufferSize will be ignored. No stream buffers supported in this implementation.
	 *
	 * @param streamBufferSize ignored!
	 * @see tree.Forest#loadTree(int, int)
	 */
	public LblValTree loadTree(int treeID) throws SQLException {

		String qry = "SELECT DISTINCT " + this.getAtbList()   
			+ " FROM `" + getTblName()  + "`"
			+ " WHERE " + atbTreeID + "=" + treeID  
			+ " ORDER BY " + this.getSortList();

		ResultSet rs =
			SQLTools.executeQuery(
					this.getStatement(), // stream buffers not supported, because they are TYPE_FORWARD_ONLY 
					qry, "Loading tree with id=" + treeID 
					+ "' from TuplePerPath table '" + this.getTblName());
		
		MMForest forest = loadForest(rs);
		
		if (forest.size() == 1) {
			return forest.elementAt(0);
		} else {
			return null;
		}
	}
	
	
	/**
	 * @see tree.Forest#loadForest(int)
	 */
	public MMForest loadForest() throws SQLException {
		
		String qry = "SELECT DISTINCT " + this.getAtbList()   
			+ " FROM " + getTblName() 
			+ " ORDER BY " + this.getSortList();
		
		ResultSet rs =
			SQLTools.executeQuery(
					this.getStatement(), // stream buffers not supported, because they are TYPE_FORWARD_ONLY
					qry, "Loading forest from TuplePerPath table '" 
					+ this.getTblName() + "'");
		
		MMForest forest = loadForest(rs);
		
		return forest;
	}

	/**
	 * @see tree.Forest#getTreeIDs()
	 */
	public int[] getTreeIDs() throws SQLException {
		String tmpTbl = this.getTblName() + "_treeIDs";
		String qry = 
			"SELECT DISTINCT " + atbTreeID 
			+ " FROM `" + this.getTblName() + "` ORDER BY " + this.atbTreeID;
		ResultSet rs = 
			SQLTools.executeQuery(this.getStatement(), qry,
					"Loading tree-IDs from TuplePerPath table '" 
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
	 * Loads trees from a resultset. 
	 * @param rs resultset containing tuples. Column 1 must be treeID, column 2 the root level, etc. 
	 *           The resultset must be ordered by column 1, column 2, etc.
	 * @return the forest contained in the resultset. Returns null, if not enough columns and empty vector, 
	 *          if resultset is empty.
	 */
	private MMForest loadForest(ResultSet rs) throws SQLException {
		if (atbLevel.length < 1) {   // is there at least 1 level?
			return new MMForest(0, 0);
		}			
		MMForest forest = new MMForest(100, 100);			
		if (rs.next()) {								
			rs.first();
			int row = 1;
			int oldTreeID = rs.getInt(this.atbTreeID); // the treeID
			String rootLabel = rs.getString(this.atbLevel[0]);  // the root label
			int markLabelChange = 1;       // mark the last occurence of a label change
			
			while (rs.next()) {
				row++;
				int newTreeID = rs.getInt(atbTreeID);      // the treeID of path in row of resultset
				if (newTreeID != oldTreeID) {
					LblValTree n = loadTree(rs, rootLabel, markLabelChange, row - 1, 1, atbLevel.length - 1);
					n.setTreeID(oldTreeID);
					forest.add(n);
					rs.absolute(row);
					oldTreeID = newTreeID;
					rootLabel = rs.getString(this.atbLevel[0]);
					markLabelChange = row;
				}
			}
			row++;
			LblValTree n = loadTree(rs, rootLabel, markLabelChange, row - 1, 1, atbLevel.length - 1);
			n.setTreeID(oldTreeID);
			forest.add(n);
		}
		return forest;
	}
	
	/**
	 * Used by loadForest.
	 *
	 * @param rs resultset
	 * @param nodeLabel label of new node
	 * @param firstRow row in rs, where you should continue
	 * @param lastRow row in rs, where you should stop
	 * @param level col in rs, where to start with subtree
	 * @param maxLevel number of last column in rs
	 */
	private LblValTree loadTree(ResultSet rs, String nodeLabel, int firstRow, int lastRow, 
			int level, int maxLevel) throws SQLException {

		LblValTree node = new LblValTree(nodeLabel, null, Node.NO_TREE_ID);
		
		rs.absolute(firstRow); 
		String oldLabel = rs.getString(this.atbLevel[level]);  // the first label
		int markLabelChange = firstRow;       // mark the last occurence of a label change
		
		for (int row = firstRow + 1; row <= lastRow; row++) {
			rs.absolute(row);
			String newLabel = rs.getString(this.atbLevel[level]);
			if (!newLabel.equals(oldLabel)) {
				if (level < maxLevel) {
					node.add(loadTree(rs, oldLabel, markLabelChange, row - 1, level + 1, maxLevel));
				} else {
					node.add(new LblValTree(oldLabel, null, Node.NO_TREE_ID));		    
				}
				oldLabel = newLabel;
				markLabelChange = row;
			}
		}
		if (level < maxLevel) {
			node.add(loadTree(rs, oldLabel, markLabelChange, lastRow, level + 1, maxLevel));
		} else {
			node.add(new LblValTree(oldLabel, null, Node.NO_TREE_ID));
		}       
		return node;
	}	
	
	/**
	 * Return the sort list for the ORDER BY sql-statement. 
	 *  
	 * @return
	 */
	private String getSortList() {
		String sortList = atbTreeID;
		for (int i = 0; i < atbLevel.length; i++) {
			if (sortType[i] == null) {
				sortList += ",`" + atbLevel[i] + "`";
			} else {
				sortList += ",CAST(`" + atbLevel[i] + "` AS " 
					+ sortType[i] + ")";			
			}	    
		}
		return sortList;
	}
	
	/////////////////////////
	// Getters and Setters //
	/////////////////////////

	
	
	/**
	 * <p>Set the SQL column type of the tree levels for creating the table.
	 * <code>columnType[i]</code> is the type of the i-th level, where the root
	 * level has <code>i=0</code>. 
	 * <p>
	 * Example:<br>
	 * columnType[0] = "VARCHAR(10)" ==> the root level (0) has column type VARCHAR(10)  
	 * 
	 * @param columnType SQL column type for each tree level 
	 */
	public void setColumnType(String[] columnType) {
		this.columnType = columnType;
	}
	/**
	 * <p>
	 * When building trees with {@see #loadTree(int, int)} or {@see #loadForest(int)} 
	 * the columns are sorted accoring to the SQL type specified here 
	 * (using CAST AS in the ORDER BY clause).
	 * <p>
	 * Example:<br>
	 * sortType[0] = "UNSIGNED" ==> the root level (0) is sorted as an unsigned integer.
	 * 
	 * @param sortType 
	 */
	public void setSortType(String[] sortType) {
		this.sortType = sortType;
	}
	
	public Iterator<LblValTree> forestIterator() throws SQLException {
		throw new RuntimeException("forestIterator() not implemetned in " + this.getClass() + "."); // TODO
	}

	public long getForestSize() throws SQLException {
		throw new RuntimeException("getForestSize() not implemetned in " + this.getClass() + "."); // TODO
	}
	
	
	
}
