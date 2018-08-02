package matching;

import distmat.DistMatrix;

class Elem implements Comparable {
	double dist;
	int row;
	int col;
	Elem(double dist, int row, int col) {
		this.dist = dist;
		this.row = row;
		this.col = col;
	}
	public int compareTo(Object arg0) {		
		return Double.compare(dist, ((Elem)arg0).dist);
	}

}

public class GlobalGreedy extends MatchingAlgo {
	
	public GlobalGreedy() {
		super();
	}
	
	@Override
	public Match[] match(DistMatrix dm) {
		int M = dm.getRowNum();
		int N = dm.getColNum();
		Elem[] S = new Elem[M * N];
		boolean[] vistedRow = new boolean[M];
		boolean[] visitedCol = new boolean[N];
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				Elem s = new Elem(dm.distAt(i, j), i, j);
				S[i * N + j] = s;
			}
		}
		java.util.Arrays.sort(S);
		Match[] matching = new Match[Math.min(M, N)];
		int i = 0;
		int k = 0;
		while (k < matching.length) {	
			int a = S[i].row;
			int b = S[i].col;
			//double dist = S[i].dist;
			if (!vistedRow[a] && !visitedCol[b]) {
				matching[k] = new Match(
						dm.getIdRow(S[i].row), 
						dm.getIdCol(S[i].col));
				k++;
				vistedRow[a] = true;
				visitedCol[b] = true;
			}
			i++;
		}
		return matching;
	}

	
	
}
