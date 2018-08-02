/*
 * Created on Feb 18, 2005
 */
package utility;

import distance.ButtomUpDist;
import distance.EditDist;
import distance.FanoutWeighting;
import distance.ProfileDist;
import distance.TreeEmbedDist;
import distance.PQGramDist;
import distance.TreeDist;
import distance.HistoDist;
import distance.MergeDist;
import distance.BinaryBranchDist;
import distance.WeightedEditDist;
import distance.WinPQGramDist;

/**
 * Some static methods to evaluate command line parameters...
 * 
 * @author augsten
 */
final public class StringToTreeDist {

	public static boolean DEBUG = true;
	
	/**
	 * Only static methods...
	 */
	private StringToTreeDist() {
		super();
	}
	
	public static String getSyntax() {
		String s = 
			"   e     edit distance (Zhang & Shasha, SICOMP 1989)\n" +
			"   fweX  fanout weighted edit distance with leaf weight X\n" +
			"   b     bottom-up distance (Valiente, SPIRE 2001)\n" +
			"   pq    pq-gram distance, p=2, q=3 (Augsten et al., VLDB 2005)\n" +
			"   pXqY  pq-gram distance, p = X, q = Y (Augsten et al., VLDB 2005)\n" +
			"   wpq   windowed pq-gram distance, w=3, p=2, q=2 (Augsten et al., ICDE 2008)\n" +
			"   wXpYq windowed pq-gram distance, w=X, p=Y, q=2 (Augsten et al., ICDE 2008)\n" +
			"   bb    binary branch distance (Yang et al., SIGMOD 2005)\n" + 
			"   gk    edit distance embedding (Garofalakis & Kumar, PODS 2003)\n" +
			"   h     histogram distance (1,0-gram distance)\n" +
			"   m     merge distance\n" +
//			"'svenProfile': 2,3-grams with cross parsing\n" +
//			"'svenPQGrams': 2,3-gram with Svens version\n" +
			"\n   => prefixing 'N' means 'normalize', e.g., Ne, Np1q3, Nm\n" +
			"      For profile distances (pq, wpq, bb, gk, h) the metric bag norm is used.\n" +
			"   => profile distances only: prefixing 'D' means 'normalize with dice norm'\n"
			;	
		return s;
	}

	/**
	 * Depending on the argument, a different specialisation of a TreeDist Object is returned.
	 * <ul>
	 * </ul>
	 * @param strDist string argument from command line 
	 * @return TreeDist of the type corresponding to the argument, or null, if no match...
	 */
	public static TreeDist getTreeDist(String strDist) {
		TreeDist dist = null;
		boolean normalized = false;
		int typeOfNormalization = ProfileDist.BAG_NORM;
		if (strDist.charAt(0) == 'N') {
			normalized = true;
			strDist = strDist.substring(1);
		} else if (strDist.charAt(0) == 'D') {
			normalized = true;
			typeOfNormalization = ProfileDist.DICE_NORM;
			strDist = strDist.substring(1);
		}		
		
		if (strDist.equals("e")) {
			dist = new EditDist(normalized);			
		} else if (strDist.substring(0, Math.min(strDist.length(), 3)).equals("fwe")) {
			int leafWeight = Integer.parseInt(strDist.substring(3));
			dist = new WeightedEditDist(new FanoutWeighting(leafWeight), normalized);
		} else if (strDist.equals("b")) {
			dist = new ButtomUpDist(normalized);
		} else if (strDist.equals("m")) {
			dist = new MergeDist(normalized);
		} else if (strDist.equals("gk")) {
			dist = new TreeEmbedDist(normalized);
			((ProfileDist)dist).setTypeOfNormalization(typeOfNormalization);
		} else if (strDist.equals("pq")) {
			dist = new PQGramDist(2, 3, null, normalized);
			((ProfileDist)dist).setTypeOfNormalization(typeOfNormalization);
		} else if (strDist.equals("h")) {
			dist = new HistoDist(normalized);
			((ProfileDist)dist).setTypeOfNormalization(typeOfNormalization);
		} else if (strDist.equals("bb")) {
			dist = new BinaryBranchDist(normalized);
			((ProfileDist)dist).setTypeOfNormalization(typeOfNormalization);
		} else if (strDist.equals("wpq")) {
			dist = new distance.WinPQGramDist(2, 3, normalized);
			((ProfileDist)dist).setTypeOfNormalization(typeOfNormalization);
//		} else if (strDist.equals("svenPQGrams")) {
//			StringWriter sw1 = new StringWriter();
//			StringWriter sw2 = new StringWriter();
//			dist = new SvenDist(new SvenPQGrams(sw1, 2, 3),	
//					new SvenPQGrams(sw2, 2, 3), normalized); 
//		} else if (strDist.equals("svenProfile")) {
//			dist = new SvenDistFromProfile(2, 3, normalized); 
		} else if (strDist.charAt(0) == 'p') {
			int qPos = strDist.indexOf('q');
			if ((qPos >= 2) && (qPos < strDist.length())) {
				String pStr = strDist.substring(1, qPos);
				String qStr = strDist.substring(qPos + 1, strDist.length());
				int p = Integer.parseInt(pStr);
				int q = Integer.parseInt(qStr);
				dist = new PQGramDist(p, q, null, normalized);
				((ProfileDist)dist).setTypeOfNormalization(typeOfNormalization);
			}
		} else if (strDist.charAt(0) == 'w') {
			int pPos = strDist.indexOf('p');
			if ((pPos >= 2) && (pPos < strDist.length())) {
				String wStr = strDist.substring(1, pPos);
				String pStr = strDist.substring(pPos + 1, strDist.length() - 1);
				int w = Integer.parseInt(wStr);
				int p = Integer.parseInt(pStr);
				dist = new WinPQGramDist(p, w, null, normalized);
				((ProfileDist)dist).setTypeOfNormalization(typeOfNormalization);
			}
		}
//		if (dist != null) {
//			System.out.println("# distance: " + dist);			
//		}
		return dist;
	}
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Please give a parameter:\n" + 
					getSyntax());			
		} else {
			TreeDist dist = getTreeDist(args[0]);
			if (dist == null) {
				System.out.println("Unknown distance parameter. Please use on of the following:\n" + 
						getSyntax());									
			} else {
				System.out.println(dist);
			}
		}
	}
}
