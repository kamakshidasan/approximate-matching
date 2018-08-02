/*
 * Created on 21/11/2006
 */
package index;

/**
 * QPart-Matrix with at least q rows, unless the anchor node of the q-part is a leaf. 
 * In this case rows = 1. (Note: rows = 1 also for q = 1.)
 *   
 * @author naugsten
 */
public class QMat {

	private String[][] m;
	private int rows;
	private int q;
	
	/**
	 * Qmat without rows. Use {@see #addRow(String)} to add rows.
	 *  
	 * @param q
	 */
	public QMat(int q) {
		m = new String[2 * (q - 1)][q];
		rows = 0;
		this.q = q;
	}
	
	/**
	 * Empty Qmat (all entries are null).
	 * 
	 * @param rows rows >= q. If q-part of a leaf, then rows = 1.
	 * @param q q >= 1
	 */
	public QMat(int rows, int q) {
		m = new String[rows][q];
		this.rows = rows;
		this.q = q;
	}

	/*
	 * Quadratic Qmat with null nodes and diagonal.
	 * 
	 */
	public QMat(int q, String nodeLabel, String dummyNode) {
		this(q, q);
		for (int i = 0; i < q; i++) {
			for (int j = 0; j < q; j++) {
				m[i][j] = (i == q - j - 1) ? nodeLabel : dummyNode;
			}
		}
	}
	
	
	/**
	 * 
	 * @param qpart hashed q-part of length q * hl (hl is the length of a hashed node label)
	 */
	public void addRow(String qpart) {
		rows++;
		int len = qpart.length() / q;
		for (int i = 0; i < q; i++) {
			m[rows - 1][i] = qpart.substring(len * i, len * (i + 1));
		}
	}
	
	public void setValue(int i, int j, String hashedLabel) {
		m[i][j] = hashedLabel;
	}

	public QMat replaceDiagonals(String hashedLabel) {
		int q = this.getQ();
		QMat qm = new QMat(q, q);
		
		// special case: q-part of a leaf node
		boolean isLeafQPart = (this.getRows() == 1) && (q != 1);
		String dummyLabel = isLeafQPart ? m[0][0] : null;
		
		for (int i = 0; i < q - 1; i++) {
			for (int j = 0; j < q - i - 1; j++) {
				String lbl = isLeafQPart ?  dummyLabel : m[i][j];
				qm.setValue(i, j, lbl);
			}
		}
		for (int i = 0; i < q; i++) {
			qm.setValue(i, q - 1 - i, hashedLabel);
		}
		int offset = this.getRows() - q + 1;
		for (int i = 0; i < q - 1; i++) {
			for (int j = 0; j <= i; j++) {
				String lbl = isLeafQPart ? dummyLabel : m[i + offset][q - 1 - j];
				qm.setValue(i + 1, q - 1 - j, lbl);
			}
		}
		return qm;
	}
	
	public QMat extractDiagonals(String dummyLabel) {
		int q = this.getQ();
		QMat qm = new QMat(this.getRows(), q);
		for (int i = 0; i < qm.rows; i++) {
			for (int j = 0; j < qm.getQ(); j++) {
				qm.setValue(i, j, m[i][j]);
			}
		}
		for (int i = 0; i < q - 1; i++) {
			for (int j = 0; j < q - i - 1; j++) {
				qm.setValue(i, j, dummyLabel);
			}
		}
		int offset = this.getRows() - q + 1;
		for (int i = 0; i < q - 1; i++) {
			for (int j = 0; j <= i; j++) {
				qm.setValue(i + offset, q - 1 - j, dummyLabel);
			}
		}
		return qm;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < this.getRows(); i++) {
			for (int j = 0; j < this.getQ(); j++) {
				sb.append(m[i][j] + " ");				
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public String getQPart(int row) {
		StringBuffer qpart = new StringBuffer();
		for (int j = 0; j < this.getQ(); j++) {
			qpart.append(m[row][j]);
		}
		return qpart.toString();
	}
	
	public QMat getHead(int numRows) {
		int minRows = Math.min(this.rows, numRows);
		QMat qm = new QMat(minRows, this.getQ());
		for (int i = 0; i < minRows; i++) {
			for (int j = 0; j < this.getQ(); j++) {
				qm.setValue(i, j, m[i][j]);
			}	
		}
		return qm;
	}

	public QMat getTail(int numRows) {
		int minRows = Math.min(this.rows, numRows);
		QMat qm = new QMat(minRows, this.getQ());
		for (int i = 0; i < minRows; i++) {
			for (int j = 0; j < this.getQ(); j++) {
				qm.setValue(i, j, m[rows - minRows + i][j]);
			}	
		}
		return qm;
	}
	
	public int getRows() {
		return rows;
	}
	
	public int getQ() {
		return q;
	}
	
}
