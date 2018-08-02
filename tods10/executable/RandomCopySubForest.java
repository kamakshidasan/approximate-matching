/*
 * used in the following experiments:
 * - thresholdjoin
 */
package executable;

import intervalenc.IntervalEncForest;

import java.sql.Connection;
import random.RandomVal;
import sqltools.SQLTools;
import tree.LblValTree;
import utility.Configuration;

/**
 * @author naugsten
 *
 */
public class RandomCopySubForest {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("\nparams: sourceForest targetForest numTrees minTreeSize randomSeed");
			System.out.println("\n  Copy numTrees trees from sourceForest to targetForest "
					+ "\n  where the copied trees have at least minTreeSize nodes." 
					+ "\n  The random seed (e.g., 1) makes the tree choice reproducible." 
					+ "\n  The table targetForest is reset before the copy operation."
					+ "\n  The trees are chosen at random from all trees in the forest.\n");
			System.exit(-1);
		}
		SQLTools.DEBUG = false;
		Configuration config = new Configuration(Configuration.CONFIG_FILE);
		Configuration.loadDrivers();
		Connection[] con = config.getConnections(4);		
		
		IntervalEncForest source = new IntervalEncForest(con[0], con[1], Configuration.getInsertBuffer(), args[0]);
		IntervalEncForest target = new IntervalEncForest(con[2], con[3], Configuration.getInsertBuffer(), args[1]);
		target.reset();
		int numTrees = Integer.parseInt(args[2]);
		int minTreeSize = Integer.parseInt(args[3]);
		int randomSeed = Integer.parseInt(args[4]);
		int[] treeIDs = source.getTreeIDs();
		numTrees = Math.min(treeIDs.length, numTrees);
		RandomVal r = new RandomVal(randomSeed);
		int found = 0;  // number of copied trees 
		int missed = 0; // number of randomly selected trees that do not fit size requirement
		//System.out.print("Copying trees");
		while (found < numTrees && found + missed < treeIDs.length) {
			int randomID = Integer.MIN_VALUE;
			
			while (randomID == Integer.MIN_VALUE) {
				int randomPos = r.getInt(0, treeIDs.length - 1);
				randomID = treeIDs[randomPos];
				treeIDs[randomPos] = Integer.MIN_VALUE;				
			}			
			LblValTree tree = source.loadTree(randomID);
			int size = tree.getNodeCount();
			if (size >= minTreeSize ) {
				target.storeTree(tree);
				//System.out.print(".");
				found++;
			} else {
				missed++;
			}
		}		
		System.out.println("Copied " + found + " trees from '" + 
				source.getTblName() + "' to '" + target.getTblName() + "'" +
				" (minimum tree size: " + minTreeSize + 
				", random seed: " + randomSeed + ")");

	}

}
