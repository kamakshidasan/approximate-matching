/*
 * used in the following experiments:
 * - scalability
 */

package executable;

import hash.FixedLengthHash;
import hash.StringHash;
import intervalenc.Cursor;
import intervalenc.IntervalEncForest;
import intervalenc.PQGramFactory;

import java.sql.Connection;

import join.AggrProf;
import join.Intersect;
import join.PQGramTbl;
import join.ProfileSizeTbl;

import mysqltools.MySqlInsertBuffer;

import utility.Configuration;
import utility.WallClock;

public class MatchTblTrees {
	
	public static void main(String[] args) throws Exception {
		if (args.length != 6) {
			System.out.println("\nparams: tbl1 tbl2 startID stopID p q");	
			System.out.println("tbl1, tbl2 ... interval encoded forests");	
			System.out.println("\nWill calculated the following pq-gram distances:" +
					"\n  dist(tbl1.startID, tbl2.startID)" +
					"\n                    ..." +
					"\n  dist(tbl1.stopID, tbl2.stopID)");
			System.out.println("The disk-based algorithm is used.");				
		} else {
			// get command line arguments
			String tbl1 = args[0];
			String tbl2 = args[1];
			int startNum = Integer.parseInt(args[2]);
			int stopNum = Integer.parseInt(args[3]);
			int p = Integer.parseInt(args[4]);
			int q = Integer.parseInt(args[5]);
			
			Configuration.loadDrivers();
			Configuration config = new Configuration(Configuration.CONFIG_FILE);
			Connection[] cons = config.getConnections(5);
			
			IntervalEncForest f1 = new IntervalEncForest(cons[0], cons[1], 
					new MySqlInsertBuffer(), tbl1);
			IntervalEncForest f2 = new IntervalEncForest(cons[2], cons[3], 
					new MySqlInsertBuffer(), tbl2);
			
			PQGramJoin pqGramJoin = new PQGramJoin(cons[4], new StringHash(5), p, q);
			
			System.out.println("# number of nodes, distance computation time");
			for (int treeID = startNum; treeID <= stopNum; treeID++) {

				WallClock wc = new WallClock(System.err);
				wc.start("Computing pq-gram distance between trees with ID "
						+ treeID + " in tables " + tbl1 + " and " + tbl2 + "...");
			
				double d = pqGramJoin.joinTrees(f1, treeID, f2, treeID);
				long time = wc.getTime();
				wc.printStop();
	
				System.err.println("dist(" + tbl1 + "." + treeID + "," + 
						tbl2 + "." + treeID + ")=" + d);
								
				// this can be piped to a file
				System.out.println((int)(Math.pow(2, treeID)*2-1) + " " + time / 1000.0);
			}	    	    
		}
	}
}

class PQGramJoin {
	
	public static String PROF_SIZE_NAME = "ps_";
	public static String IDX_NAME = "idx_";
	public static String DIST_MAT_NAME = "distmat";
	public static String PROFILE_NAME = "profile";

	String ps1Str = PROF_SIZE_NAME  + "1";
	String ps2Str = PROF_SIZE_NAME + "2";
	String idxStr1 = IDX_NAME + "1";
	String idxStr2 = IDX_NAME + "2";
	String distMatStr = DIST_MAT_NAME;
	String profileStr = PROFILE_NAME;
	int p, q;
	FixedLengthHash hf;
	PQGramTbl pqg;
	ProfileSizeTbl ps1, ps2;
	Connection con;
	
	public PQGramJoin(Connection con, FixedLengthHash hf, int p, int q) throws Exception {
		this.p = p;
		this.q = q;
		this.hf = hf;
		this.con = con;
	
		pqg = new PQGramTbl(con,
				Configuration.getInsertBuffer(), profileStr, 
				hf, p, q);			
	
		ps1 = new ProfileSizeTbl(con, con,  
				Configuration.getInsertBuffer(), ps1Str);
		ps2 = new ProfileSizeTbl(con, con,
				Configuration.getInsertBuffer(), ps2Str);
	}
	
	/**
	 * Creates a pq-gram index if no index with the given name exists. This means, 
	 * you have to drop the old index if you want to rebuild it.
	 * 
	 * @param preorder
	 * @param idxName
	 * @param ps
	 * @return
	 * @throws Exception
	 */
	private AggrProf getIndex(Cursor preorder, 
			String idxName, ProfileSizeTbl ps) throws Exception {

		AggrProf idx = new AggrProf(con, idxName, pqg);

		ps.reset();
		pqg.reset();
		
		PQGramFactory.getPQGrams(preorder, pqg, ps, p, q);
			
		ps.buildIndex();
		
		idx.reset();
		idx.loadPQGrams();
		return idx;
	}

	/**
	 * 
	 * @param f1
	 * @param f2
	 * @return
	 * @throws Exception
	 */
	public Intersect joinForests(IntervalEncForest f1, 
			IntervalEncForest f2) throws Exception {

		
		AggrProf idx1 = getIndex(f1.forestInPreorder(), idxStr1, ps1);
		AggrProf idx2 = getIndex(f2.forestInPreorder(), idxStr2, ps2);
		
		Intersect is = new Intersect(f1.getCon(), distMatStr);
		is.reset();
		is.intersect(idx1, idx2);
		is.computeDist(ps1, ps2);
		
		return is;		
	}

	/**
	 * Compute and return distance between trees.
	 * @param f1
	 * @param treeID1
	 * @param f2
	 * @param treeID2
	 * @return
	 * @throws Exception
	 */
	public double joinTrees(IntervalEncForest f1, int treeID1, 
			IntervalEncForest f2, int treeID2) throws Exception {
		
		AggrProf idx1 = 
			this.getIndex(f1.treeInPreorder(treeID1), idxStr1, ps1);
		AggrProf idx2 = 
			this.getIndex(f2.treeInPreorder(treeID2), idxStr2, ps2);
		
		Intersect is = new Intersect(f1.getCon(), distMatStr);
		is.reset();
		is.intersect(idx1, idx2);
		is.computeDist(ps1, ps2);
		
		return is.getDist(treeID1, treeID2);
	}
	
}
