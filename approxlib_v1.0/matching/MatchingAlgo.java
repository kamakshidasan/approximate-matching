/*
 * Created on Aug 10, 2007
 */
package matching;

import distmat.DistMatrix;

public abstract class MatchingAlgo {

	/**
	 * Compute matches for a given distance matrix.
	 * 
	 * @param dm distance matrix
	 * @return array of matches, where match is a pair of object IDs (rowId, colId)
	 */
	public abstract Match[] match(DistMatrix dm);

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
