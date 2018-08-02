/*
 * used in the following experiments:
 * - thresholdjoin
 */
package executable;

import intervalenc.IntervalEncForest;

import java.util.Iterator;

import matching.Match;
import matching.MatchingAlgo;
import matching.MatchingEvaluation;

import buildingblocks.DistanceJoins;
import buildingblocks.ForestTools;

import distance.TreeDist;
import distmat.DistMatrix;

import random.RandomVal;
import sqltools.SQLTools;
import tree.LblValTree;
import tree.MMForest;
import utility.Configuration;
import utility.StringToMatchingAlgo;
import utility.StringToTreeDist;

public class ThresholdJoin {

	public static final String[] NOISE_TYPE = new String[] {"del", "ren", "delren"}; 
	public static final String SEPARATOR = "\t"; 
	
	/**
	 * Average distance between matches.
	 * 
	 * @param correct matches
	 * @param dm distance matrix
	 * @return
	 */
	public static double averageDistToMatch(Match[] correct, DistMatrix dm) {
		double sum = 0;
		for (int i = 0; i < correct.length; i++) {
			sum += dm.distAtId(correct[i].getIdRow(), correct[i].getIdCol());
		}
		sum /= correct.length;
		return sum;
	}
	
	/**
	 * Average of the smallest distance of a row object to a non-matching column object.
	 * Assumes that each row object has at most one match in the column objects. 
	 * Only row objects that have a match are considered.
	 *  
	 * @param correct
	 * @param dm
	 * @return
	 */
	public static double averageDistToNonMatch(Match[] correct, DistMatrix dm) {
		double sum = 0;
		for (int i = 0; i < correct.length; i++) {
			int row = dm.getRow(correct[i].getIdRow());
			int col = dm.getCol(correct[i].getIdCol());
			double min = Double.MAX_VALUE;
			for (int k = 0; k < dm.getColNum(); k++) {
				if ((k != col) && (dm.distAt(row, k) < min)) {
					min = dm.distAt(row, k);
				}
			}
			sum += min;
		}
		sum /= correct.length;
		return sum;
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 6)  {
			System.err.print("params: origForest noisyForest treeDist machingAlgo steps {");
			for (int i = 0; i < NOISE_TYPE.length; i++) {
				System.err.print(NOISE_TYPE[i]);
				if (i < NOISE_TYPE.length - 1) {
					System.err.print("|");
				}
			}
			System.err.println("}");
			System.exit(-1);
		}			
		// set up database
		SQLTools.DEBUG = false;
		Configuration config = new Configuration(Configuration.CONFIG_FILE);
		Configuration.loadDrivers();
		
		// initialize forests
		IntervalEncForest f1 = new IntervalEncForest(config.getConnection(), 
				config.getConnection(), 
				Configuration.getInsertBuffer(), args[0]);
		IntervalEncForest f2 = new IntervalEncForest(config.getConnection(), 
				config.getConnection(), 
				Configuration.getInsertBuffer(), args[1]);
		
		// get other paremeters
		TreeDist treeDist = StringToTreeDist.getTreeDist(args[2]);
		MatchingAlgo matchingAlgo = StringToMatchingAlgo.getMatchingAlgo(args[3]); 
		int steps = Integer.parseInt(args[4]);
		String noiseType = args[5];
		
		
		// print out log information
		System.out.println("# original forest: " + f1.getTblName());
		System.out.println("# noise type: " + noiseType);
		System.out.println("# step size = " +  1.0 / steps);
		System.out.print("# " + 
				"1:step" + SEPARATOR +
				"2:noise [%]" + SEPARATOR +
				"3:true positives [%]" + SEPARATOR +
				"4:false positives [%]" + SEPARATOR +
				"5:false negative [%]" + SEPARATOR +
				"6:precision [%]" + SEPARATOR +
				"7:recall [%]" + SEPARATOR +
				"8:avDistMatch" + SEPARATOR +
				"9:avDistNonMatch" + "\n"
				);
		
		
		// noise type
		double deletions = 0;
		double renames = 0;
		if (noiseType.equals(NOISE_TYPE[0])) {
			deletions = 1;
		} else if (noiseType.equals(NOISE_TYPE[1])) {
			renames = 1;
		} else if (noiseType.equals(NOISE_TYPE[2])) {
			deletions = 0.5;
			renames = 0.5;			
		} else {
			System.err.println("Unknown noise type: " + noiseType);
			System.exit(-2);
		}
		
		// initialize correct matches
		Match[] correctMatches = new Match[(int)f1.getForestSize()];
		int pos = 0;
		for (Iterator<LblValTree> it = f1.forestIterator(); it.hasNext();) {
			int id = it.next().getTreeID();
			correctMatches[pos++] = new Match(id, id); 
		}
				
		for (int i = 0; i <= steps; i++) {
			// add noise 
			f2.reset();
			ForestTools.randomEditTrees(f1, f2, 
					deletions / steps * i, renames / steps * i,
					new RandomVal(4));
			double edits = (deletions + renames) / steps * i;
			
			// comptue matches
			MMForest mf1 = f1.loadForest();
			MMForest mf2 = f2.loadForest();
			for (int k = 0; k < mf1.size(); k++) {
				mf1.set(k, new LblValTree(mf1.elementAt(k).getLblTree(null)));
				mf2.set(k, new LblValTree(mf2.elementAt(k).getLblTree(null)));
			}						
			DistMatrix dm = DistanceJoins.computeDistMatrix(mf1, mf2, treeDist);
			Match[] matches = matchingAlgo.match(dm);
						
			// evalute matches
			MatchingEvaluation eval = new MatchingEvaluation(matches, correctMatches);
			double avDistToMatch = ThresholdJoin.averageDistToMatch(correctMatches, dm);
			double avDistToNonMatch = ThresholdJoin.averageDistToNonMatch(correctMatches, dm);

			// print results for one noise level
			System.out.print(i + SEPARATOR);
			System.out.print(edits + SEPARATOR);
			System.out.print(eval.truePositive() + SEPARATOR);
			System.out.print(eval.falsePositive() + SEPARATOR);
			System.out.print(eval.falseNegative() + SEPARATOR);
			System.out.print(eval.precision() + SEPARATOR);
			System.out.print(eval.recall()  + SEPARATOR);
			System.out.print(avDistToMatch  + SEPARATOR);
			System.out.print(avDistToNonMatch + "\n");
						
		}
				
	}

}
