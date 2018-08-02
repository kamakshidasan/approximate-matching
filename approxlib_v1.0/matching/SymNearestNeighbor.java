/**
 * 
 */
package matching;

import java.util.LinkedList;

import distmat.DistMatrix;

/**
 * @author naugsten
 *
 */
public class SymNearestNeighbor extends MatchingAlgo {

	/* (non-Javadoc)
	 * @see matching.MatchingAlgo#match(distmat.DistMatrix)
	 */
	@Override
	public Match[] match(DistMatrix dm) {
		LinkedList<Match> matches = new LinkedList();
		for (int row = 0; row < dm.getRowNum(); row++) {
			double valMin = Double.MAX_VALUE;
			int colMin = -1;
			boolean foundMatch = false;
			for (int col = 0; col < dm.getColNum(); col++) {
				if (dm.distAt(row, col) < valMin) {
					valMin = dm.distAt(row, col);
					colMin = col;
					foundMatch = true;
				} else if (dm.distAt(row, col) == valMin) {
					foundMatch = false;
				}
			}
			if (foundMatch) {
				for (int i = 0; i < dm.getRowNum(); i++) {
					if ((dm.distAt(i, colMin) <= valMin) && (i != row)) {
						foundMatch = false;
						break;
					}
				}
			}
			if (foundMatch) {
				matches.add(new Match(dm.getIdRow(row), dm.getIdCol(colMin)));
			}
		}
		return matches.toArray(new Match[matches.size()]);
	}

}
