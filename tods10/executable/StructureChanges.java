/*
 * used in the following experiments:
 * - structure
 */
package executable;

import java.util.*;

import tree.LblTree;
import tree.MMForest;
import distance.PQGramDist;
import distance.TreeDist;
import random.RandomVal;

public class StructureChanges {
	
	public static final int MAX_RUNS = 100;
	public static final String PARAMS = 
		"params: treefile {-l|-i|-b} number_of_changes p q";
	
	/** 
	 * Deletes a node and inserts the children of the deleted node between the left
	 * and the right sibling of that node.
	 *
	 * If the deleted node is a root node, one of the children of this node randomly is
	 * choosen to be the new root node. The children of the new root are inserted
	 * between its former left and richt siblings.
	 * If root is leaf, nothing happens.
	 * 
	 * (Internally the root node is renamed and one of the children is deleted!)
	 */
	
	public static void delete(LblTree tree) {
		if (tree.isRoot()) {
			if (tree.isLeaf()) {
				return;
			}
			int r = (new Random()).nextInt(tree.getChildCount());
			LblTree newRoot = (LblTree)tree.getChildAt(r);
			tree.setLabel(newRoot.getLabel());
			tree = newRoot;
		}
		LblTree parent = (LblTree)tree.getParent();
		int id = parent.getIndex(tree);
		int cc = tree.getChildCount();
		for (int i = 0; i < cc; i++) {
			parent.insert((LblTree)tree.getChildAt(0), id + 1 + i);
		}
		parent.remove(id);
	}

	
	/**
	 * From the tree <tt>T</tt> (given as a parameter) randomly a node <tt>v</tt> 
	 * (not the root node) is choosen and deleted. Depending on the parameters given, 
	 * <tt>v</tt> may be a leaf, a non-leaf, or any node. For various numbers of
	 * deletions a line is printed to std-out that gives <em>the sum</em> of the edit 
	 * distances (number of deletions) and the pq-gram distances (normalized and non-normalized) 
	 * for all runs. Further the number of runs is given in the same line. 
	 * The output should be self-explanatory.
	 * 
	 * This program takes the following parameters:
	 * <ul>
	 * <li><tt>treefile</tt>: A file containing a tree as a text string.
	 * <li><tt>{-l|-i|-b}</tt>: <tt>-l</tt> delete only leaves, 
	 *                          <tt>-i</tt> delete only non-leaf nodes
	 *                          <tt>-b</tt> delete both, leaf and non-leaf nodes
	 * <li><tt>number_of_changes</tt>: run experiment for <tt>1</tt> to <tt>number_of_changes</tt> changes
	 * <li><tt>p</tt>: p-value for pq-gram distance
	 * <li><tt>q</tt>: q-value for pq-gram distance
	 * </ul>
	 * 
	 * The experimental result is averaged over MAX_RUNS runs. 
	 * 
	 * @param args file with tree, p and q
	 * @throws Exception
	 */	
	public static void main(String[] args) throws Exception {
		
		if (args.length != 5) {
			System.err.println(PARAMS);
		} else {
			
			boolean onlyleaves = args[1].equals("-l");
			boolean onlyinternals = args[1].equals("-i");
			boolean both = args[1].equals("-b");
			if (!(onlyleaves || onlyinternals || both)) {
				System.err.println(PARAMS);
				return;
			}
			
			int numberOfChanges = Integer.parseInt(args[2]);
			int p = Integer.parseInt(args[3]);
			int q = Integer.parseInt(args[4]);
			
			String filename = args[0];
						
			MMForest forest = new MMForest(filename);
			LblTree noisyTree = forest.getTreeAt(0);
			LblTree original = LblTree.deepCopy(noisyTree);
			
			RandomVal r = new RandomVal();
				
			System.out.println("# tree `" + filename + "` with " + 
					noisyTree.getNodeCount() + " nodes and " + 
					noisyTree.getLeafCount() + " leaves, depth = " 
					+ noisyTree.getDepth());
			System.out.println("# p = " + p + ", q = " + q);
			System.out.println("# " + MAX_RUNS + " run average");
			if (onlyleaves) {
				System.out.println("# columns:\n# deleted leaves | pq-gram-distance | normalized pq-gram-distance");
			}
			if (onlyinternals) {
				System.out.println("# columns:\n# deleted internal nodes | pq-gram-distance | normalized pq-gram-distance");
			}
			if (both) {
				System.out.println("# columns:\n# deleted nodes | pq-gram-distance | normalized pq-gram-distance");
			}
			TreeDist normDist = new PQGramDist(p, q, true);
			TreeDist dist = new PQGramDist(p, q, false);
			//TreeDist normDist = new WeightedEditDist(new FanoutWeighting(1), true);
			//TreeDist dist = new WeightedEditDist(new FanoutWeighting(1), false);
						
			for (int changes = 1; changes <= numberOfChanges; changes++) {
				double distSum = 0;
				double normDistSum = 0;
				for (int runs = 0; runs < MAX_RUNS; runs++) {
					noisyTree = LblTree.deepCopy(original);		
					noisyTree.setTmpData(null);
					
					for (int i = 0; i < changes; i++) {		
						int nodeCount = noisyTree.getNodeCount();
						
						LblTree[] nodeArray = new LblTree[nodeCount];
						
						int cnt = 0;
						for (Enumeration e = noisyTree.breadthFirstEnumeration(); e.hasMoreElements();) {    
							nodeArray[cnt] = (LblTree)e.nextElement();		
							cnt++;
						}
						
						int modifiedNode = r.getInt(0, nodeCount - 1);
						if (both) {
							while (modifiedNode == 0) {
								modifiedNode = r.getInt(1, nodeCount - 1);
							}
						} else {			    
							if (onlyleaves) {
								while (!nodeArray[modifiedNode].isLeaf()) {
									modifiedNode = r.getInt(0, nodeCount - 1);
								}
							} else if (onlyinternals) {
								while ((nodeArray[modifiedNode].isLeaf()) || (modifiedNode == 0)) {
									modifiedNode = r.getInt(1, nodeCount - 1);
								}
							}
						}
						delete(nodeArray[modifiedNode]);
					}
					//original.prettyPrint();
					//noisyTree.prettyPrint();
					distSum += dist.treeDist(noisyTree, original);
					normDistSum += normDist.treeDist(noisyTree, original);
				}
				
				System.out.println(changes + " " + (distSum/MAX_RUNS) + " " + (normDistSum/MAX_RUNS));
				
			}
		}
		
	}
}
