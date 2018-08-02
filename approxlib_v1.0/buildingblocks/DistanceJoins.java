/**
 * 
 */
package buildingblocks;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;

import matching.GlobalThreshold;
import matching.Match;
import matching.MatchingAlgo;

import tree.Forest;
import tree.LblValTree;
import tree.MMForest;
import distance.TreeDist;
import distmat.DistMatrix;

/**
 * @author naugsten
 *
 */
public class DistanceJoins {
	
	/**
	 * Compute the distances between all pairs of trees of two forests.
	 * @param f1
	 * @param f2
	 * @param dist
	 * @return
	 */
	public static DistMatrix computeDistMatrix(Forest fRow, Forest fCol, TreeDist dist) throws SQLException {		
		DistMatrix distMatrix = new DistMatrix(fRow.getTreeIDs(), fCol.getTreeIDs());
		for (Iterator<LblValTree> itRow = fRow.forestIterator(); itRow.hasNext();) {
			LblValTree tRow = itRow.next();
			for (Iterator<LblValTree> itCol = fCol.forestIterator(); itCol.hasNext();) {
				LblValTree tCol = itCol.next();
				double d = dist.treeDist(tRow, tCol); 
				distMatrix.setDistAtId(tRow.getTreeID(), tCol.getTreeID(), d);
			}
		}
		return distMatrix;
	}
	
	/**
	 * Compute the distances between all pairs of trees of two forests.
	 * @param f1
	 * @param f2
	 * @param dist
	 * @return
	 */
	public static DistMatrix computeDistMatrix(MMForest fRow, MMForest fCol, TreeDist dist) throws SQLException {		
		DistMatrix distMatrix = new DistMatrix(fRow.getTreeIDs(), fCol.getTreeIDs());
		LblValTree tRow, tCol;
		for (int i = 0; i < fRow.getForestSize(); i++) {
			for (int j = 0; j < fCol.getForestSize(); j++) {
				tRow = fRow.getTreeAt(i);
				tCol = fCol.getTreeAt(j);
				double d = dist.treeDist(tRow, tCol); 
				distMatrix.setDistAtId(tRow.getTreeID(), tCol.getTreeID(), d);				
			}
		}
		return distMatrix;
	}
	
	public static Match[] computeMatches(Forest f1, Forest f2, TreeDist dist, MatchingAlgo matchingAlgo) throws SQLException {
		DistMatrix distMatrix = computeDistMatrix(f1, f2, dist);
		return matchingAlgo.match(distMatrix);
	}
	
	public static Match[] computeMatches(Forest fRow, Forest fCol, TreeDist dist, GlobalThreshold matchingAlgo) throws SQLException {
		LinkedList<Match> matchList = new LinkedList<Match>();
		for (Iterator<LblValTree> itRow = fRow.forestIterator(); itRow.hasNext();) {
			LblValTree tRow = itRow.next();
			for (Iterator<LblValTree> itCol = fCol.forestIterator(); itCol.hasNext();) {
				LblValTree tCol = itCol.next();
				double d = dist.treeDist(tRow, tCol);
				if (d <= matchingAlgo.getThreshold()) {
					matchList.add(new Match(tRow.getTreeID(), tCol.getTreeID()));
				}
			}
		}
		return matchList.toArray(new Match[matchList.size()]);		
	}

}
