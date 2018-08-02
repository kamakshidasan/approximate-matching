/*
 * Created on Sep 29, 2005
 */
package adjacencyenc;

/**
 * @author augsten
 */
public class AdjNode {

	private int treeID;
	private int nodeID;
	private int parentID;
	private int sibPos;
	private String label;
	private String value;

	/**
	 * @param treeID
	 * @param anchorID
	 * @param parentID
	 * @param sibPos
	 * @param label
	 */
	public AdjNode(int treeID, int anchorID, int sibPos, int parentID, String label, String value) {
		this.treeID = treeID;
		this.nodeID = anchorID;
		this.parentID = parentID;
		this.sibPos = sibPos;
		this.label = label;
		this.value = value;
	}
	
	
	
	/**
	 * @return Returns the anchorID.
	 */
	public int getNodeID() {
		return nodeID;
	}
	/**
	 * @return Returns the label.
	 */
	public String getLabel() {
		return label;
	}
	/**
	 * @return Returns the value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @return Returns the parentID.
	 */
	public int getParentID() {
		return parentID;
	}
	/**
	 * @return Returns the sibPos.
	 */
	public int getSibPos() {
		return sibPos;
	}
	/**
	 * @return Returns the treeID.
	 */
	public int getTreeID() {
		return treeID;
	}
}
