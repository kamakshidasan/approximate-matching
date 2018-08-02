/*
 * Created on 10-Apr-06
 */
package string;

import java.sql.ResultSet;
import sqltools.SQLTools;

import join.PQGramTbl;
import join.ProfileSizeTbl;

public class QGramFactory {

	public QGramFactory() {
		super();
	}

	public static String[] getQGrams(String s, int q) {
		String[] qgrams = new String[s.length() + (q - 1)];
		StringBuffer gram = new StringBuffer(q);
		for (int i = 0; i < q; i++) {
			gram.append('#');
		}
		for (int i = 0; i < s.length(); i++) {
			gram.deleteCharAt(0);
			gram.append(s.charAt(i));
			qgrams[i] = gram.toString();
		}
		for (int i = 0; i < q - 1; i++) {
			gram.deleteCharAt(0);
			gram.append("#");
			qgrams[s.length() + i] = gram.toString();
		}
		return qgrams;
	}

	/**
	 * Computes q-grams strings, given in preorder. None of the tables is
	 * indexed. Index the profile size table before you use it.
	 * 
	 * @param preorder
	 * @param pqg
	 * @param ps
	 *            profile size table
	 * @param p
	 * @param q
	 * @param type
	 * @throws Exception
	 */
	public static void getQGrams(StringTbl str, PQGramTbl pqg,
			ProfileSizeTbl ps, int q) throws Exception {
		pqg.open();
		ps.open();
		// Start creating q-grams
		String qry = "SELECT " + str.getAtbTreeID() + "," + str.getAtbLabel()
				+ " FROM " + str.getTblName();
		ResultSet rs = SQLTools.executeQuery(str.getStatement(), qry,
				"Loading strings for pq-gram computation from "
						+ str.getTblName());
		while (rs.next()) {
			int treeID = rs.getInt(str.getAtbTreeID());
			String label = rs.getString(str.getAtbLabel());
			String[] qgrams = getQGrams(label, q);
			ps.insertTree(treeID, qgrams.length);
			for (int i = 0; i < qgrams.length; i++) {
				pqg.getInsBuff().insert(
						"(" + treeID + ",'"
								+ SQLTools.escapeSingleQuote(qgrams[i]) + "')");
			}
		}
		ps.close();
		pqg.setPsTbl(ps);
		pqg.close();
	}

//	public static void main(String[] args) throws Exception {
//		// establish DB connection
//		mysqltools.MySqlTools.loadDriver();
//		Connection out = Configuration.getConnection();
//		Connection in = Configuration.getConnection();
//
//		// get parameters
//		int q = 3;
//		long time;
//
//		// // creating index on magic table
//		// System.out.print("Computing orderd pq-gram index on magic table...");
//		ProfileSizeTbl ps_str = new ProfileSizeTbl(out, "ps_str2");
//
//		PQGramTbl pqg_str = new PQGramTbl(out, new MySqlInsertBuffer(),
//				"pqg_in", new PrefixHash(q), 0, q);
//		StringTbl labels = new StringTbl(out, "namesAN");
//		labels.open();
//		pqg_str.reset();
//		ps_str.reset();
//		time = System.currentTimeMillis();
//		QGramFactory qgFac = new QGramFactory();
//		qgFac.getQGrams(labels, pqg_str, ps_str, q);
//		System.out.println((System.currentTimeMillis() - time) + "ms");
//		pqg_str.close();
//		//
//		// // creating index on input table
//		// IntervalEncForest f_in = new IntervalEncForest(out, in,
//		// new MySqlInsertBuffer(), "tblANlc_ie");
//		// f_in.open();
//		// System.out.print(" Creating preorder index on input table...");
//		// time = System.currentTimeMillis();
//
//	}

}
