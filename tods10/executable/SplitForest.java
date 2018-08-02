/*
 * used in the following experiments:
 * - thresholdjoin
 */
/*
 * Created on 20-Apr-06
 */
package executable;

import java.sql.Connection;

import buildingblocks.ForestTools;

import sqltools.SQLTools;
import utility.Configuration;

import intervalenc.IntervalEncForest;

/**
 * @author augsten
 */
public class SplitForest {

	/**
	 * 
	 */
	public SplitForest() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		SQLTools.DEBUG = false;
		Configuration config = new Configuration(Configuration.CONFIG_FILE);
		Configuration.loadDrivers();
		if (args.length != 3) {
			System.out.println("\nparams: sourceForest sourceTreeID targetForest");
			System.out.println("\n  Deletes root of sourceTreeID in sourceForest and "
					+ "\n  stores the result in targetForest." 
					+ "\n  If the target forest does not exist, it is created." 
					+ "\n  Otherwise the new trees are appended.");
			System.exit(-1);
		}
		Connection con = config.getConnection();
		
		// connect to forests
		IntervalEncForest sf = new IntervalEncForest(
				con, args[0]);
		IntervalEncForest tf = new IntervalEncForest(
				con, con, 
				Configuration.getInsertBuffer(), args[2]);
		tf.reset();
		int n = ForestTools.deleteRootNode(sf, Integer.parseInt(args[1]), tf);
		System.out.println("Created new table '" + tf.getTblName() + "' with " 
				+ n + " trees.");
		System.out.print("Building preorder index...");
		tf.createPreorderIndex();
		System.out.println("done");
	}

}
