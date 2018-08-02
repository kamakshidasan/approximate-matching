/**
 * 
 */
package matching;

/**
 * @author naugsten
 *
 */
public class Match implements Comparable {

	private int idRow;
	private int idCol;
	
	public Match(int idRow, int idCol) {
		this.idRow = idRow;
		this.idCol = idCol;
	}

	public int compareTo(Object o) {
		Match match = (Match)o;
		if (this.getIdRow() < match.getIdRow()) {
			return -1;
		} else if (this.getIdRow() == match.getIdRow()) {
			if (this.getIdCol() < match.getIdCol()) {
				return -1;
			} else if (this.getIdCol() == match.getIdCol()) {
				return 0;
			} else {
				return 1;
			}
		} else {
			return 1;
		}
	}	
	
	@Override
	public boolean equals(Object obj) {
		return (this.compareTo(obj) == 0);
	}

	public int getIdRow() {
		return this.idRow;
	}

	public void setIdRow(int id1) {
		this.idRow = id1;
	}

	public int getIdCol() {
		return this.idCol;
	}

	public void setIdCol(int id2) {
		this.idCol = id2;
	}

	@Override
	public String toString() {
		return "(" + this.getIdRow() + "," + this.getIdCol() + ")"; 
	}
	
	
	
	
}
