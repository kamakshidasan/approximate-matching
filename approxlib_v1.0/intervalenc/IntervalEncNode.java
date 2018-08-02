/*
 * Created on 10-Apr-06
 */
package intervalenc;

public class IntervalEncNode {
	private int treeID;
	private int lft;
	private int rgt;
	private String label;
	private String value;
	
	/**
	 * 
	 * @param treeID
	 * @param label
	 * @param value
	 * @param lft
	 * @param rgt
	 */
	public IntervalEncNode(int treeID, String label, String value, int lft, int rgt) {
		this.treeID = treeID;
		this.lft = lft;
		this.rgt = rgt;
		this.label = label;
		this.value = value;
	}
	
	public String getLabel() {
		return label;
	}
	
	public boolean isLeaf() {	    
		return (rgt == lft + 1);
	}
	
	public boolean isAncestor(IntervalEncNode n) {
		if ((n == null) || (!sameTree(n))) {
			return false;
		} else {
			return (rgt > n.getRgt()) && (lft < n.getLft());
		}
	}

	public boolean isDescendant(IntervalEncNode n) {
		if ((n == null) || (!sameTree(n))) {
			return false;
		} else {
			return (rgt < n.getRgt()) && (lft > n.getLft());
		}
	}
	
	public int subtreeSize() {
		return (this.getRgt() - this.getLft() + 1) / 2;
	}
	
	public boolean sameTree(IntervalEncNode n) {
		if (n == null) {
			return false;
		} else {
			return (treeID == n.getTreeID());
		}
	}
	
	// only for nested-set-version
	public int getLft() {
		return lft;
	}
	
	public void setLft(int lft) {
		this.lft = lft;
	}
	
	public int getRgt() {
		return rgt;
	}
	
	public int getTreeID() {
		return treeID;
	}

	/**
	 * @return Returns the value.
	 */
	public String getValue() {
		return value;
	}
	
	

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg0) {
		IntervalEncNode n = (IntervalEncNode) arg0;
		boolean equal = 
			((n.getLabel() != null && n.getLabel().equals(this.getLabel())) || (n.getLabel() == null && this.getLabel() == null)) && 
			((n.getValue() != null && n.getValue().equals(this.getValue())) || (n.getValue() == null && this.getValue() == null)) && 
			this.treeID == n.treeID &&
			this.getLft() == n.getLft() &&
			this.getRgt() == n.getRgt();
		return equal;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return 
			"(" + this.getTreeID() + "," + this.getLabel() + "," 
			+ this.getValue() + "," + this.getLft() + "," + this.getRgt() + ")";
	}
	
	
	
}
