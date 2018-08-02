/*
 * used in the following experiments:
 * - streetmatching
 */
package executable;
/*
 * created 2008-10-08
 */

import buildingblocks.DistanceJoins;
import matching.Match;
import matching.MatchingAlgo;
import matching.MatchingEvaluation;
import tree.MMForest;
import utility.StringToMatchingAlgo;
import utility.StringToTreeDist;
import utility.WallClock;
import distance.TreeDist;
import distmat.DistMatrix;
import distmat.DistMatrixIO;

/**
 * @author augsten
 * @see TreeDist
 */
public class MatchForests {
	
	public static final boolean normalize = true;
	
	public static void main(String[] args) throws Exception {
		
		// read command line parameters
		if (args.length != 6) {
			System.err.println("params: forest1 forest2 solution distmatFile treeDist matchingAlgo\n\n" + 
					"forest1,forest2: text files containing forests\n" +
					"distmatFile: filename of the output distance matrix\n" + 			
					"solution: file with correct matches (format of each row: idRow|idCol)\n" +
					"=== treeDist: ===\n" + StringToTreeDist.getSyntax() + "\n" +
					"=== matchingAlgo: ===\n" + StringToMatchingAlgo.getSyntax()			
			);
			System.exit(-1);
		}
		TreeDist treeDist = StringToTreeDist.getTreeDist(args[4]);
		if (treeDist == null) {
			System.err.println("Unknown tree distance. Here is the correct synatx:");
			System.err.println(StringToTreeDist.getSyntax());
			System.exit(-2);
		}
		MatchingAlgo matchingAlgo = StringToMatchingAlgo.getMatchingAlgo(args[5]);
		if (matchingAlgo == null) {
			System.err.println("Unknown matching algorithm. Here is the correct synatx:");
			System.err.println(StringToMatchingAlgo.getSyntax());
			System.exit(-3);
		}
		MMForest forest1 = new MMForest(args[0]);
		MMForest forest2 = new MMForest(args[1]);
		Match[] correct = matching.MatchIO.fromFile(args[2], '|');
		String distMatrixFile = args[3];
		
		// status-message
		System.out.println("# " + args[0] + ": " + forest1.size() + " trees, " + forest1.getNodeCount() + " nodes");
		System.out.println("# " + args[1] + ": " + forest2.size() + " trees, " + forest2.getNodeCount() + " nodes");
		
		// compute distances
		WallClock wallClock = new WallClock(System.out);
		wallClock.start("# Computation time: ");
		DistMatrix distMatrix = DistanceJoins.computeDistMatrix(forest1, forest2, treeDist);
		wallClock.printStop();
		System.out.println("# Writing distance matrix to file '" +
				distMatrixFile + "'");
		DistMatrixIO.toFile(distMatrix, distMatrixFile, '|');
		
		// compute matches		
		Match[] computed = matchingAlgo.match(distMatrix);
//		for (int i = 0; i< computed.length; i++) {
//			System.out.println(((Match)computed[i]).getIdRow() + "|" + ((Match)computed[i]).getIdCol()); 
//		}
		MatchingEvaluation matchingEvaluation = new MatchingEvaluation(computed, correct);
		
		System.out.println(matchingEvaluation);
		
//		System.out.println("true positive: " + matchingEvaluation.truePositive());
//		System.out.println("false postive: " + matchingEvaluation.falsePositive());
//		System.out.println("false negative: " + matchingEvaluation.falseNegative());
//		System.out.println("precision: " + matchingEvaluation.precision());
//		System.out.println("recall: " + matchingEvaluation.recall());
		
	}
	
}
