/*
 * used in the following experiments:
 * - scalability
 */
package executable;

import tree.LblValTree;
import tree.MMForest;
import utility.StringToTreeDist;
import distance.TreeDist;

public class MatchTrees {
	
	public static void main(String[] args) throws Exception {
		if (args.length != 5) {
			System.out.println("\nparams: file1 file2 startNum stopNum dist");	
			System.out.println("\nWill calculated following distances:" +
					"\n  dist(file1_startNum, file2_startNum)" +
					"\n                    ..." +
					"\n  dist(file1_stopNum, file2_stopNum)");
			System.out.println("\n'dist' is on of the following:\n" + 
					StringToTreeDist.getSyntax());
			
		} else {
			// get command line arguments
			String file1 = args[0];
			String file2 = args[1];
			int startNum = Integer.parseInt(args[2]);
			int stopNum = Integer.parseInt(args[3]);
			TreeDist dist = StringToTreeDist.getTreeDist(args[4]); 
			
			System.out.println("# number of nodes, distance computation time");
			for (int fileNum = startNum; fileNum <= stopNum; fileNum++) {
				
				// construct file names for input files
				String fileName1 = file1 + "_";
				if (fileNum < 10) {
					fileName1 += "0";
				}
				fileName1 += fileNum;
				
				String fileName2 = file2 + "_";
				if (fileNum < 10) {
					fileName2 += "0";
				}
				fileName2 += fileNum;
				
				MMForest f1;
				MMForest f2;
				LblValTree tree1;
				LblValTree tree2;
				
				// preload first pair of tree to avoid offset for first disk access ...
				
				if (startNum == fileNum) {
					f1 = new MMForest(fileName1);
					f2 = new MMForest(fileName2);
					tree1 = f1.elementAt(0);
					tree2 = f2.elementAt(0);
					dist.treeDist(tree1, tree2);
				}
				
				// load trees
				
				System.err.print("Loading trees...");
				long start = System.currentTimeMillis();
				long overallStart = start;
				
				f1 = new MMForest(fileName1);
				f2 = new MMForest(fileName2);
				
				tree1 = f1.elementAt(0);
				tree2 = f2.elementAt(0);
				
				// compute pq-gram distance
				
				long stop = System.currentTimeMillis();
				System.err.println((stop - start) + "ms");
				System.err.print("Computing distance (" + dist + ")...");
				start = stop;
				
				double d = dist.treeDist(tree1, tree2); 
				
				
				stop = System.currentTimeMillis();
				System.err.println((stop - start) + "ms");
				System.err.println("dist(" + fileName1 + "," + fileName2 + ")=" + d);
				System.err.println("Time elapsed: " + (stop - overallStart));
				
				// this can be piped to a file
				System.out.println((int)(Math.pow(2, fileNum)*2-1) + " " + ((stop - overallStart)/1000.0));
			}	    	    
		}
	}
}
