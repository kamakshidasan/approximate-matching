/*
 * Created on Jun 15, 2005
 */
package index;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;

import mysqltools.MySqlInsertBuffer;
import sqltools.TableWrapper;
import sqltools.SQLTools;

import adjacencyenc.AdjacencyEncForest;
import hash.FixedLengthHash;
import index.EditLog;
import java.util.ListIterator;

/**
 * @author augsten
 */
public class Index extends TableWrapper {
	
	public static final String ATB_TREE_ID = "treeID";
	public static final String ATB_PQGRAM = "pqgram";
	public static final String ATB_CNT = "cnt";
	
	private String atbTreeID = ATB_TREE_ID ;
	private String atbPQGram = ATB_PQGRAM;
	private String atbCnt = ATB_CNT;
	
	int p;
	int q;
	FixedLengthHash hf;
	
	public Index(String indexName, Connection con, int p, int q, FixedLengthHash hf) {
		super(con, indexName);
		this.p = p;
		this.q = q;
		this.hf = hf;
	}
	
	/**
	 * 
	 * @param f
	 * @throws Exception
	 */
	public void buildFromScratch(AdjacencyEncForest f) throws Exception {

		this.reset();
		this.addIndex();
		int[] treeIDs = f.getTreeIDs();
		for (int i = 0; i < treeIDs.length; i++) {
			DeltaProfile deltaProf = 
				new DeltaProfile(f, p, q, this.hf, 
						"Ptmp", "Qtmp", new MySqlInsertBuffer(), true);
			deltaProf.createProfile(treeIDs[i]);
			this.add(deltaProf);
			deltaProf.drop();
		}
	}
	
	
	
	/**
	 * @return Returns the hf.
	 */
	public FixedLengthHash getHf() {
		return hf;
	}
	/**
	 * Add the profile of a tree that is NOT yet in the index.
	 * 
	 * If you decide to drop the index with {@link #dropIndex()} for faster insert
	 * you should add it again using {@link #addIndex()}.   
	 * @param deltaProf
	 */
	public void buildFromScratch(DeltaProfile deltaProf) throws SQLException {

		PPartTbl pp = deltaProf.getPparts();
		QPartTbl qp = deltaProf.getQparts();
		String tblP = "`" + pp.getTblName() + "`";
		String tblQ = "`" +qp.getTblName() + "`";
		
		try {
			String qry = "DROP TABLE P_tmp_scratch";
			SQLTools.execute(this.getStatement(), qry, "Deleting table 'P_tmp_scratch'");
		} catch (Exception e) {}
		
		// join the p- and q-parts
		String qry = "CREATE TABLE P_tmp_scratch AS\nSELECT "
			+ tblP + "." + pp.getAtbTreeID() + " as treeID" + "," 
			+ "CONCAT(" + pp.getAtbPPart() + "," + qp.getAtbQPart() + ") AS pqgram\n"
			+ "FROM " + tblP + "," + tblQ + "\n" 
			+ "WHERE "
			+ tblP + "." + pp.getAtbTreeID() + "=" + tblQ + "." + qp.getAtbTreeID() + " AND\n"
			+ tblP + "." + pp.getAtbAnchorID() + "=" + tblQ + "." + qp.getAtbAnchorID();
		SQLTools.execute(this.getStatement(), qry,
				"Joining p-parts and q-parts of delta-profile (" + tblP + "," + tblQ + ")");
		
		// use an index on the pq-grams
		SQLTools.execute(this.getStatement(), 
				"ALTER TABLE P_tmp_scratch ADD INDEX (pqgram)",
				"Adding index to pq-grams before aggregating them.");
		
		// aggregate the pq-grams
		this.open();
		qry = 
			"INSERT INTO " + this.getTblName() + "\n"
			+ "SELECT treeID AS " + this.atbTreeID + ","
			+ "pqgram AS " + this.atbPQGram + ","
			+ "COUNT(*) AS " + this.atbCnt 
			+ " FROM P_tmp_scratch GROUP BY " + this.atbPQGram;
		SQLTools.execute(this.getStatement(), qry,
				"Aggregating pq-grams of delta-profile (" + tblP + "," + tblQ + ")");
		SQLTools.execute(this.getStatement(), 
				"DROP TABLE P_tmp_scratch", 
				"Drop table...");
		
		this.addIndex();		
	}
	
	/**
	 * Add an index on (pqgram,treeID).
	 * @throws SQLException
	 */
	public void addIndex() throws SQLException {
		SQLTools.execute(this.getStatement(),
				"ALTER TABLE `" + this.getTblName() + "`" 
				+ " ADD UNIQUE idx (" + this.atbPQGram + "," + this.atbTreeID + ")",
				"Adding index to " + this.getTblName() + ".");
	}
	
	/**
	 * Drop the index on (qpgram,treeID).
	 * @throws SQLException
	 */
	public void dropIndex() throws SQLException {
		SQLTools.execute(this.getStatement(),
				"DROP INDEX idx ON `" + this.getTblName() + "`",
				"Dropping index from " + this.getTblName() + ".");
	}

	
	/**
	 * Uses NON-NORMALIZED pq-gram distance!
	 * 
	 * @param idx
	 * @param tau
	 * @return Streaming ResultSet with scema (this.getAtbTreeID,cnt), where cnt is the number
	 * of shared pqgrams of idx and the tree in this.getAtbTreeID, and it is less than tau. 
	 */
	public ResultSet lookup(Index idx, double tau) throws SQLException {
		String A = "`" + this.getTblName() + "`";
		String B = "`" + idx.getTblName() + "`";
		String qry =
			"SELECT A." + this.atbTreeID 
			+ ", COUNT(LEAST(A." + this.atbCnt + "," 
			+ "B." + idx.atbCnt + ")) AS cnt\n"
			+ "FROM " + A + " AS A JOIN " + B + " AS B ON "
			+ "A." + this.atbPQGram + "=B." + idx.atbPQGram + "\n"
			+ "GROUP BY A." + this.atbTreeID + " "
			+ "HAVING (cnt >= " + tau + ")";
		return SQLTools.executeQuery(this.getStreamStatement(), qry,
				"Looking up " + A + " in index " + B);
	}
	
	public void add(DeltaProfile deltaProf) throws SQLException {
		Index idx = new Index(this.getTblName() + "_plus", this.getCon(), p, q, new hash.PrefixHash(4));
		idx.reset();
		idx.buildFromScratch(deltaProf);
		this.add(idx);
		idx.drop();
	}
	
	public void remove(DeltaProfile deltaProf) throws SQLException {
		Index idx = new Index(this.getTblName() + "_minus", this.getCon(), p, q, new hash.PrefixHash(4));
		idx.reset();
		idx.buildFromScratch(deltaProf);
		this.remove(idx);
		idx.drop();
	}
	
	
	/**
	 * Add the index <code>idx</code> to the existing index (bag union).
	 * 
	 * @param idx
	 * @throws SQLException
	 */
	public void add(Index idx) throws SQLException {

		String qry =
			"INSERT IGNORE INTO " + this.getTblName() 
			+ " SELECT "
			+ idx.getTblName() + "." + idx.atbTreeID + ","
			+ idx.getTblName() + "." + idx.atbPQGram + ","
			+ "0 AS " + idx.atbCnt + " "
			+"FROM " + idx.getTblName();
		SQLTools.executeUpdate(this.getStatement(), qry, 
				"Adding index " + idx.getTblName() + " to " +
				this.getTblName() + ": inserting new pq-grams");
		
		qry = 
			"UPDATE " + this.getTblName() + " as A JOIN " 
			+ idx.getTblName() + " AS B ON " 
			+ "A." + this.atbPQGram + "=B." + idx.atbPQGram + " AND " 
			+ "A." + this.atbTreeID + "=B." + idx.atbTreeID + " "
			+ "SET A." + this.atbCnt + "=A." + atbCnt + "+B." + idx.atbCnt;
		SQLTools.executeUpdate(this.getStatement(), qry, 
				"Adding index " + idx.getTblName() + " to " +
				this.getTblName() + ": updating count for existing or newly inserted pq-grams.");
	}

	
	
	/**
	 * Delete the index <code>idx</code> from the existing index (bag minus).
	 * 
	 * @param idx
	 * @throws SQLException
	 */
	public void remove(Index idx) throws SQLException {
		String qry = 
			"UPDATE " + this.getTblName() + " as A JOIN " 
			+ idx.getTblName() + " AS B ON " 
			+ "A." + this.atbPQGram + "=B." + idx.atbPQGram + " AND " 
			+ "A." + this.atbTreeID + "=B." + idx.atbTreeID + " "
			+ "SET A." + this.atbCnt + "=A." + atbCnt + "-B." + idx.atbCnt;
		SQLTools.executeUpdate(this.getStatement(), qry, 
				"Subtracting index " + idx.getTblName() + " from " +
				this.getTblName() + ".");
		qry = 
			"DELETE FROM " + this.getTblName() + " WHERE "
			+ this.atbCnt + "<=" + 0;
		SQLTools.executeUpdate(this.getStatement(), qry, 
				"Deleting zero-count pq-grams from " +
				this.getTblName() + ".");
	}
	
	/**
	 * Update the index for a single edit operation on f.
	 * 
	 * @param f
	 * @param treeID
	 * @param nodeID
	 * @param label
	 * @throws Exception
	 */
	public void renameNode(AdjacencyEncForest f, int treeID, int nodeID, String label) throws Exception {
		DeltaProfile deltaProf = new DeltaProfile(f, p, q, this.hf, 
				this.getTblName() + "_" + "deltaP", 
				this.getTblName() + "_" + "deltaQ", new MySqlInsertBuffer(), true);
		deltaProf.computeDeltaRename(treeID, nodeID);
		this.remove(deltaProf);
		deltaProf.updateDeltaRename(treeID, nodeID, label);
		this.add(deltaProf);
	}
	
	/**
	 * Update the index for a single edit operation on f.
	 * 
	 * @param f
	 * @param treeID
	 * @param nodeID
	 * @throws Exception
	 */
	public void deleteNode(AdjacencyEncForest f, int treeID, int nodeID) throws Exception  {
		DeltaProfile deltaProf = new DeltaProfile(f, p, q, new hash.PrefixHash(4), 
				this.getTblName() + "_" + "deltaP", 
				this.getTblName() + "_" + "deltaQ", new MySqlInsertBuffer(), true);
		deltaProf.computeDeltaDelete(treeID, nodeID);
		this.remove(deltaProf);
		deltaProf.updateDeltaDelete(treeID, nodeID);
		this.add(deltaProf);
		
	}
	
	/**
	 * Update the index for a single edit operation on f.
	 * 
	 * @param treeID
	 * @param nodeID
	 * @param parentID
	 * @param k
	 * @param n
	 */
	public void insertNode(int treeID, int nodeID, int parentID, int k, int n) {
		throw new RuntimeException("Insert is not implemented.");
	}
	
	/**
	 * Overridden method.
	 *
	 * @see sqltools.TableWrapper#create()
	 */
	public void create() throws SQLException {
		String qry =
			"CREATE TABLE `" + this.getTblName() + "` ("
			+ this.atbTreeID + " INT NOT NULL,"
			+ this.atbPQGram + " CHAR(" + ((p+q) * hf.getLength()) + "),"   
			+ this.atbCnt + " INT NOT NULL)";
		this.getStatement().execute(qry);
	}

	/**
	 * Overridden method.
	 *
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	public String getAtbList() {
		return this.atbTreeID + "," + this.atbPQGram + "," + this.atbCnt;
	}
	
	
	/**
	 * 
	 * @param idx
	 * @return true, if both indices contain exactly the same pq-grams
	 * @throws SQLException
	 */
	public boolean equals(Index idx) throws SQLException {
		return this.equals(idx, idx.atbTreeID);
	}
	
	/**
	 * 
	 * @param f forest containing an edit log: <code>(e<sub>1<sub>, ..., e<sub>n</sub>)</code>
	 * @return <code>dP(T<sub>n</sub>, e<sub>1</sub><sup>-1</sup>) &cup; ... &cup dP(T<sub>n</sub>, e<sub>n</sub><sup>-1</sup>) 
	 */
	public DeltaProfile getDeltaPlus(AdjacencyEncForest f) throws SQLException {
		DeltaProfile deltaPlus = new DeltaProfile(f, p, q, hf, 
				this.getTblName() + "_deltaPlus", this.getTblName() + "_Q", 
				new MySqlInsertBuffer(), true);
		deltaPlus.setUpdateMode(true, false);

		for (ListIterator it = f.getEditLog().listStartFirst(); it.hasNext();) {
			EditOperation e = (EditOperation)it.next();
			System.err.println("e:" + e);
			System.err.println("e-1:" + e.reverseEditOp());
			e.reverseEditOp().applyTo(deltaPlus);
		}
		return deltaPlus;
	}
	
	/**
	 * 
	 * @param deltaPlus will be converted to deltaMinus
	 * @return
	 * @throws SQLException
	 */
	public DeltaProfile getDeltaMinus(DeltaProfile deltaPlus, EditLog editLog) throws SQLException {
		deltaPlus.setUpdateMode(false, true);
		for (ListIterator it = editLog.listStartLast(); it.hasPrevious();) {
			EditOperation e = (EditOperation) it.previous();
			e.reverseEditOp().applyTo(deltaPlus);
		}
		return deltaPlus;
	}
	
	/**
	 * Update the index for a number of edit operations. 
	 * Assumes that f has stored a log of all edit operations 
	 * since the last update. Does not change the log.
	 * 
	 * @param f
	 * @throws SQLException
	 */
	public void update(AdjacencyEncForest f) throws SQLException {
		
		long start = System.currentTimeMillis();
		System.err.println("  Computing I+...");
		// getting deltaPlus in index format
		System.err.println("    -->getting deltaProf+");
		DeltaProfile deltaPlus = this.getDeltaPlus(f);
		System.err.println("    ...took me " + (System.currentTimeMillis() - start) + "ms.");
		start = System.currentTimeMillis();
		
		System.err.println("    -->creating deltaI+ form deltProf+");		
		Index idxPlus = new Index(this.getTblName() + "_plus", this.getCon(), p, q, new hash.PrefixHash(4));
		idxPlus.reset();
		idxPlus.buildFromScratch(deltaPlus);
		System.err.println("    ...took me " + (System.currentTimeMillis() - start) + "ms.");
		
		start = System.currentTimeMillis();
		System.err.println("  Computing I-...");
		// getting deltaMinus in index foramt
		System.err.println("    -->getting deltaProf-");		
		this.getDeltaMinus(deltaPlus, f.getEditLog()); // changes deltaPlus to deltaMinus
		System.err.println("    ...took me " + (System.currentTimeMillis() - start) + "ms.");
		start = System.currentTimeMillis();

		// get I- from deltaMinus (variable deltaPlus!)
		System.err.println("    -->creating deltaI- form deltProf-");		
		Index idxMinus = new Index(this.getTblName() + "_minus", this.getCon(), p, q, new hash.PrefixHash(4));
		idxMinus.reset();
		idxMinus.buildFromScratch(deltaPlus);
		System.err.println("    ...took me " + (System.currentTimeMillis() - start) + "ms.");
		start = System.currentTimeMillis();

		System.err.println("  Updating index with I+ and I-");
		// I_n = I_0 \ I- \cup I+
		this.remove(idxMinus);  
		this.add(idxPlus);
		idxPlus.drop();
		idxMinus.drop();
		deltaPlus.drop();
		System.err.println("  ...took me " + (System.currentTimeMillis() - start) + "ms.");
			
	}

}
