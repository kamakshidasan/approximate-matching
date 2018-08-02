/**
 * 
 */
package buildingblocks;

import intervalenc.IntervalEncForest;
import intervalenc.IntervalEncNode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Iterator;

import random.RandomVal;
import sqltools.SQLTools;
import tree.TreeNoise;
import tree.Forest;
import tree.LblValTree;

/**
 * @author naugsten
 * 
 */
public class ForestTools {

	/**
	 * Copy a tree to an other forest and delete the root node of this tree.
	 * When the root node is deleted, the tree is split into subrees rooted by
	 * the children of the former root node.
	 * 
	 * The treeIDs of the new trees are chosen to be consecutive numbers
	 * starting with the largest treeID in the forest plus 1. If the forest is
	 * empty, the first treeID is 1.
	 * 
	 * @param source
	 *            source forest
	 * @param treeID
	 *            ID of a tree in the source forest
	 * @param destination
	 *            destination forest
	 * @return number of new trees in the forest
	 * @throws SQLException
	 */
	public static int deleteRootNode(Forest source, int treeID,
			Forest destination) throws SQLException {
		// find free treeIDs
		int[] treeIDs = destination.getTreeIDs();
		int maxID = 0;
		for (int i = 0; i < treeIDs.length; i++) {
			if (treeIDs[i] > maxID) {
				maxID = treeIDs[i];
			}
		}
		// all IDs larger than maxID are free

		// make children of the tree new trees
		LblValTree t = source.loadTree(treeID);
		int childCount = 0;
		if (t != null) {
			for (Enumeration<LblValTree> children = t.children(); children
					.hasMoreElements();) {
				LblValTree child = LblValTree.deepCopy(children.nextElement());
				child.setTreeID(++maxID);
				destination.storeTree(child);
				childCount++;
			}
		}
		return childCount;
	}

	/**
	 * An efficient implementation of
	 * {@link #deleteRootNode(Forest, int, Forest)} for an
	 * {@link intervalenc.IntervalEncForest}.
	 * 
	 * @param source
	 *            source forest
	 * @param treeID
	 *            ID of a tree in the source forest
	 * @param destination
	 *            destination forest
	 * @return number of new trees in the forest
	 * @throws SQLException
	 */
	public static int deleteRootNode(IntervalEncForest source, int treeID,
			IntervalEncForest destination) throws SQLException {
		IntervalEncNode root = source.getRootNode(treeID);
		if (root == null) {
			return 0;
		}
		String sql = "SELECT MAX(" + destination.getAtbTreeID()
				+ ") as maxID FROM `" + destination.getTblName() + "`";
		ResultSet rs = SQLTools.executeQuery(source.getStatement(), sql,
				"Retrieve the largest treeID in the forest.");
		int currentTreeID;
		rs.next();
		currentTreeID = rs.getInt("maxID") + 1; // maxID is NULL if the table is
												// empty. NULL is converted to
												// the int value 0.
		IntervalEncNode nd = source.getNode(treeID, root.getLft() + 1);
		Statement statement = destination.getStatement();
		while (nd != null) {
			String qry = "INSERT INTO " + destination.getTblName() + " SELECT "
					+ currentTreeID + " AS " + destination.getAtbTreeID() + ","
					+ source.getAtbLabel() + " AS " + destination.getAtbLabel()
					+ "," + source.getAtbValue() + " AS "
					+ destination.getAtbValue() + "," + source.getAtbLft()
					+ "-" + (nd.getLft()) + " AS " + destination.getAtbLft()
					+ "," + source.getAtbRgt() + "-" + (nd.getLft()) + " AS "
					+ destination.getAtbRgt() + " FROM " + source.getTblName()
					+ " WHERE " + source.getAtbLft() + ">=" + nd.getLft()
					+ " AND " + source.getAtbLft() + "<=" + nd.getRgt() +
					// using lft (instead of rgt) here speeds up the query a
					// lot!
					" AND " + source.getAtbTreeID() + "=" + treeID;
			int size = SQLTools.executeUpdate(statement, qry,
					"Splitting tree id=" + treeID + " into forest '"
							+ destination.getTblName() + "'.");
			if (SQLTools.DEBUG) {
				System.out.println("Inserted tree " + currentTreeID + " with "
						+ size + " nodes...");
			}
			currentTreeID++;
			nd = source.getNode(treeID, nd.getRgt() + 1);
		}
		return currentTreeID - 1;
	}

	/**
	 * Copy a specific number of trees which are between a minimum and maximum
	 * size from one forest to an other. Identical calls on the same forest may
	 * copy different subsets. There may not be enough trees in the forest that
	 * satisfy the size condition. In this case all trees that satisfy the
	 * condition will be copied.
	 * 
	 * @param forest
	 *            source forest
	 * @param subforest
	 *            destination forest
	 * @param numOfTrees
	 *            number of trees to be copied
	 * @param minSize
	 *            minimum tree size
	 * @param maxSize
	 *            maximum tree size
	 * @return number of trees that where actually copied to the subforest
	 */
	public static int copySubForst(Forest forest, Forest subforest,
			int numOfTrees, long minSize, long maxSize) throws SQLException {
		int i = 0;
		for (Iterator<LblValTree> it = forest.forestIterator(); it.hasNext()
				&& i < numOfTrees;) {
			LblValTree t = it.next();
			int nodeCount = t.getNodeCount();
			if ((nodeCount >= minSize) && (nodeCount <= maxSize)) {
				subforest.storeTree(t);
				i++;
			}
		}
		return i;
	}

	/**
	 * 
	 * @param source
	 * @param dest
	 * @param deletions
	 * @param renames
	 * @param random
	 * @throws SQLException
	 */
	public static void randomEditTrees(Forest source, Forest dest,
			int deletions, int renames, RandomVal random) throws SQLException {

		int[] tids = source.getTreeIDs();
		int numOfTrees = tids.length;

		for (int i = 0; i < numOfTrees; i++) {
			LblValTree t = source.loadTree(tids[i]);
			TreeNoise.randomDeleteNodes(t, deletions, random);
			TreeNoise.randomRenameNodes(t, renames, random);

			dest.storeTree(t);
		}
	}

	/**
	 * 
	 * @param source
	 * @param dest
	 * @param percentDeletions
	 * @param percentRenames
	 * @param random
	 * @throws SQLException
	 */
	public static void randomEditTrees(Forest source, Forest dest,
			double percentDeletions, double percentRenames, RandomVal random)
			throws SQLException {

		int[] tids = source.getTreeIDs();
		int numOfTrees = tids.length;

		for (int i = 0; i < numOfTrees; i++) {
			LblValTree t = source.loadTree(tids[i]);
			int n = t.getNodeCount();
			TreeNoise.randomDeleteNodes(t, (int) Math.round(n
					* percentDeletions), random);
			TreeNoise.randomRenameNodes(t,
					(int) Math.round(n * percentRenames), random);

			dest.storeTree(t);
		}
	}

}
