/*
 * used in the following experiments:
 * - scalability
 */
package executable;

import intervalenc.IntervalEncForest;
import tree.LblValTree;
import tree.MMForest;
import utility.Configuration;

public class LoadTreeFilesToTbl {
	
	public static void writeErrMsg() {
		System.err.println("\nparams: table_name file_1 ... file_n\n");
		System.exit(-1);
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			writeErrMsg();
			System.exit(-1);
		}
		Configuration config = 
			new Configuration(Configuration.CONFIG_FILE);
		Configuration.loadDrivers();
		
		String tblName = args[0];
		IntervalEncForest outForest = 
			new IntervalEncForest(config.getConnection(), tblName);
		outForest.reset();
		MMForest inForest;
		int treeID = 1;
		System.out.println("# Writing to table " + tblName);
		for (int i = 1; i < args.length; i++) {
			System.out.println("# Loading forest from file " + args[i]);
			inForest = new MMForest(args[i]);
			System.out.println("# tree ID, tree size");
			for (int j = 0; j < inForest.size(); j++) {
				LblValTree t = inForest.getTreeAt(j);
				System.out.println(treeID + "\t" + t.getNodeCount());
				t.setTreeID(treeID++);
				outForest.storeTree(t);
			}
		}
		System.out.println("Building index on (treeID, lft)...");
		outForest.close();
		outForest.createPreorderIndex();
	}

}
