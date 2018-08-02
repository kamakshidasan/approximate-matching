/*
 * Created on Aug 10, 2007
 */
package matching;

import java.util.LinkedList;

import distmat.DistMatrix;

public class GlobalThreshold extends MatchingAlgo {
	
	private double threshold;
	
	public GlobalThreshold(double threshold) {
		this.threshold = threshold;
	}

	/**
	 * Match all pairs of objects that are within the distance threshold (<=).
	 */
	@Override
	public Match[] match(DistMatrix dm) {
		// compute matching and store it into a list
		LinkedList<Match> matchingList = new LinkedList<Match>();
		for (int row = 0; row < dm.getRowNum(); row++) {
			for (int col = 0; col < dm.getColNum(); col++){
				if (dm.distAt(row, col) <= threshold) {
					matchingList.add(new Match(dm.getIdRow(row), dm.getIdCol(col)));
				}
			}
		}
		// copy matching from list to array
		Match[] matching = new Match[matchingList.size()];
		java.util.Iterator<Match> it = matchingList.iterator();
		int i = 0;
		while (it.hasNext()) {
			matching[i++] = it.next();
		}		
		return matching;
	}

	public double getThreshold() {
		return this.threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	@Override
	public String toString() {
		return super.toString() + "[threshold=" + this.getThreshold() + "]";
	}	
	
}
