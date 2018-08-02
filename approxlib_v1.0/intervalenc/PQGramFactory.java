/*
 * Created on 10-Apr-06
 */
package intervalenc;

import hash.HashValue;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;

import join.PQGramTbl;
import join.ProfileSizeTbl;

public class PQGramFactory {
	
	public final static int ORDERED = 0;
	public final static int UNORDERED = 1;
	
	private PQGramFactory() {
	}

	/**
	 * Computes pq-grams for a forest given in preorder.
	 * None of the tables is indexed. Index the profile size table before you use it.
	 * 
	 * @param preorder
	 * @param pqg
	 * @param ps profile size table
	 * @param p
	 * @param q
	 * @param type
	 * @throws Exception
	 */
	public static void getPQGrams(Cursor preorder, PQGramTbl pqg, ProfileSizeTbl ps, 
			int p, int q) throws Exception {
		// Start creating pq-grams
		preorder.next();
		while (!preorder.isAfterLast()) {
			IntervalEncNode root = preorder.fetchNode();
			int treeID = root.getTreeID();
			pqg.resetInsertCnt();
			getOrderedPQGrams(preorder, pqg, pqg.getHf().getEmptyRegister(p), q);
			ps.insertTree(treeID, pqg.getInsertCnt());
		}
		pqg.setPsTbl(ps);
		pqg.close();
		ps.close();
		
	}

	/**
	 * Computes pq-grams for forest, given in preorder.
	 * None of the tables is indexed. Index the profile size table before you use it.
	 * 
	 * @param preorder
	 * @param pqg
	 * @param ps profile size table
	 * @param p
	 * @param q
	 * @param w
	 * @throws Exception
	 */
	public static void getUnorderedPQGrams(Cursor preorder, PQGramTbl pqg, ProfileSizeTbl ps, 
			int p, int q, int w) throws Exception {
		// Start creating pq-grams
		preorder.next();
		while (!preorder.isAfterLast()) {
			IntervalEncNode root = preorder.fetchNode();
			int treeID = root.getTreeID();
			pqg.resetInsertCnt();
			getUnorderedPQGrams(preorder, pqg, pqg.getHf().getEmptyRegister(p), q, w);
			ps.insertTree(treeID, pqg.getInsertCnt());
		}
		pqg.setPsTbl(ps);
		pqg.close();
		ps.close();
		
	}
	
	
	/**
	 * Calculates all pq-grams of the subtree rooted in the node n at
	 * the current position in "preorder" It has some side-effects:
	 *
	 * <ul>
	 * 
	 * <li>cursor in preorder is set to the next node that is not a
	 * descendant of n (or to the root of the next tree, if n is the
	 * root node of a tree)
	 *
	 * </ul>
	 *
	 * @param anc next p ancestors of n
	 * @param sib next q left siblings of n
	 * @return next node in preorder
	 *
	 */
	private static IntervalEncNode getOrderedPQGrams(Cursor preorder, PQGramTbl pqg, LinkedList anc_in, int q) throws SQLException {
		
		// copy input shift register (so it will not be changed)
		LinkedList anc = (LinkedList)anc_in.clone();
		
		// get current node
		IntervalEncNode root = preorder.fetchNode();
		
		
		// get the next node
		IntervalEncNode next;
		if (preorder.next()) {
			next = preorder.fetchNode(); 
		} else {
			next = null;
		}
		
		// new values for anc and sib
		anc.removeLast(); 
		anc.addFirst(pqg.getHf().h(root.getLabel(), root.getValue()));
		LinkedList sib = pqg.getHf().getEmptyRegister(q);
		
		// if you are a leaf, produce a leafs pq-grams 
		if (root.isLeaf()) {
			pqg.addPQGram(root.getTreeID(), anc, sib);
		} else {
			// while the next nodes are your descendants 
			// tell them to produce their pq-grams
			while (root.isAncestor(next)) {
				sib.removeLast(); 
				sib.addFirst(pqg.getHf().h(next.getLabel(), next.getValue()));
				pqg.addPQGram(root.getTreeID(), anc, sib);
				next = getOrderedPQGrams(preorder, pqg, anc, q);
			}
			for (int i = 0; i < q - 1; i++) {
				sib.removeLast();
				sib.addFirst(pqg.getHf().getNullNode());
				pqg.addPQGram(root.getTreeID(), anc, sib);
			}
		}
		
		// return next
		return next;	
	}
	
	/**
	 * Calculates all pq-grams of the subtree rooted in the node n at
	 * the current position in "preorder" It has some side-effects:
	 *
	 * <ul>
	 * 
	 * <li>cursor in preorder is set to the nesuper.toString();xt node that is not a
	 * descendant of n (or to the root of the next tree, if n is the
	 * root node of a tree)
	 *
	 * </ul>
	 *
	 * @param anc next p ancestors of n
	 * @param sib next q left siblings of n
	 * @return next node in preorder
	 *
	 */
	private static IntervalEncNode getUnorderedPQGrams(Cursor preorder, PQGramTbl pqg, LinkedList<HashValue> stem_in, int q, int w) 
		throws SQLException {
		

		// copy input shift register (so it will not be changed)
		LinkedList<HashValue> stem = (LinkedList)stem_in.clone();
		
		// get current node
		IntervalEncNode root = preorder.fetchNode();		
		
		// get the next node
		IntervalEncNode next;
		if (preorder.next()) {
			next = preorder.fetchNode(); 
		} else {
			next = null;
		}
		
		// new values for anc and sib
		stem.removeLast(); 
		stem.addFirst(pqg.getHf().h(root.getLabel(), root.getValue()));
		
		// if you are a leaf, produce a leafs pq-grams 
		if (root.isLeaf()) {
			LinkedList<HashValue> base;
			base = pqg.getHf().getEmptyRegister(q);
			pqg.addPQGram(root.getTreeID(), stem, base);					
		} else {
			LinkedList<HashValue> children = new LinkedList<HashValue>();
			
			// while the next nodes are your descendants
			// - store the children
			// - tell them to produce their pq-grams
			while (root.isAncestor(next)) {
				children.add(pqg.getHf().h(next.getLabel(), next.getValue()));
				next = getUnorderedPQGrams(preorder, pqg, stem, q, w);
			}
			
			// copy children to array, add null nodes and sort
			int f = children.size();
			int dummies = Math.max(0, w - f);
			HashValue[] chArr = new HashValue[f + dummies];
			children.toArray(chArr);
			for (int i = 0; i < dummies; i++) {
				chArr[i + f] = HashValue.maxValue(pqg.getHf().getLength());
			}
			Arrays.sort(chArr);
			for (int i = 0; i < dummies; i++) {
				chArr[i + f] = pqg.getHf().getNullNode();
			}

			
			
			// produce leaf-parts of pq-grams
			for (int i = 0; i < chArr.length; i++) {
				LinkedList<HashValue> base = new LinkedList<HashValue>();
				if (q == 2) {
					base.add(chArr[i]);	  // starter node (first node in q-window)
					for (int j = i + 1; j < i + w; j++) {
						base.addFirst(chArr[j % chArr.length]);            // node in q-window other then starter node 
						pqg.addPQGram(root.getTreeID(), stem, base);
						base.removeFirst();
					}
				} else if (q == w) {
					for (int j = i; j < i + q; j++) {						
						base.addFirst(chArr[j % chArr.length]);  // node in q-window other then starter node
					}					
					pqg.addPQGram(root.getTreeID(), stem, base);					
				} else {
					throw new RuntimeException("Implementation incomplete. Works only for (q = 2, w >= 2) or (q > 2, w = q) -- SORRY.");
				}
			}
		}
		
		// return next
		return next;	
	}
	
}
