package matching;

import distmat.DistMatrix;

public class LocalGreedy extends MatchingAlgo {
	
	public LocalGreedy() {
		super();
	}



	/**
	 * If dm is quadratic or has less rows than columns, then the matching is done row-wise.
	 * When a row (column) object is at the same distance to two column (row) objects, then 
	 * the object with the smaller column (row) number is matched.  
	 * 
	 * @see matching.MatchingAlgo#match(distmat.DistMatrix)
	 */
	@Override
	public Match[] match(DistMatrix dm) {
		int rows = dm.getRowNum();
		int cols = dm.getColNum();
		Match[] matching = new Match[Math.min(rows, cols)];
		if (rows <= cols) {
			boolean[] visitedCol = new boolean[cols];
			for (int row = 0; row < matching.length; row++) {
				double distMin = Double.MAX_VALUE;
				int colMin = -1;
				for (int col = 0; col < cols; col++) {
					if ((!visitedCol[col]) && (dm.distAt(row,col) < distMin)) {
						distMin = dm.distAt(row,col);
						colMin = col;
					}
				}
				visitedCol[colMin] = true;
				matching[row] = new Match(dm.getIdRow(row), dm.getIdCol(colMin));
			}			
		} else {
			boolean[] visitedRow = new boolean[rows];
			for (int col = 0; col < matching.length; col++) {
				double distMin = Double.MAX_VALUE;
				int rowMin = -1;
				for (int row = 0; row < rows; row++) {
					if ((!visitedRow[row]) && (dm.distAt(row,col) < distMin)) {
						distMin = dm.distAt(row,col);
						rowMin = row;
					}
				}
				visitedRow[rowMin] = true;
				matching[col] = new Match(dm.getIdRow(rowMin), dm.getIdCol(col));
			}						
		}
		return matching;
	}

}
