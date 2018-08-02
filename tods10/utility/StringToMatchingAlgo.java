/*
 * Created on Feb 18, 2005
 */
package utility;

import matching.GlobalGreedy;
import matching.GlobalThreshold;
import matching.LocalGreedy;
import matching.MatchingAlgo;
import matching.StblMarriage;
import matching.SymNearestNeighbor;

/**
 * Some static methods to evaluate command line parameters...
 * 
 * @author augsten
 */
final public class StringToMatchingAlgo {

	public static boolean DEBUG = true;
	
	/**
	 * Only static methods...
	 */
	private StringToMatchingAlgo() {
		super();
	}
	
	public static String getSyntax() {
		String s = 
			"'gg': global greedy\n" +
			"'lg': local greedy\n" +
			"'sm': stable marriage\n" +
			"'nn': symmetric nearest neighbor\n" +
			"'tauX': global threshold (X is the theshold)";
		return s;
	}

	/**
	 * Depending on the argument, a different specialisation of a MatchingAlgo is returned.
	 * <ul>
	 * </ul>
	 * @param arg string argument from command line 
	 * @return MatchingAlgo of the type corresponding to the argument, or null, if no match...
	 */
	public static MatchingAlgo getMatchingAlgo(String arg) {
		MatchingAlgo matchingAlgo = null;
		if (arg.equals("gg")) {
			matchingAlgo = new GlobalGreedy();
		} else if (arg.equals("lg")) {
			matchingAlgo = new LocalGreedy();
		} else if (arg.equals("sm")) {
			matchingAlgo = new StblMarriage();
		} else if (arg.equals("nn")) {
			matchingAlgo = new SymNearestNeighbor();
		} else if (arg.substring(0, 3).equals("tau")) {
			double tau = Double.parseDouble(arg.substring(3));
			matchingAlgo = new GlobalThreshold(tau);
		} 	
		if (matchingAlgo != null) {
			System.out.println("# matching algorithm: " + matchingAlgo.toString());			
		}
		return matchingAlgo;
	}
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Please give a parameter:\n" + 
					getSyntax());			
		} else {
			MatchingAlgo matchingAlgo = getMatchingAlgo(args[0]);
			if (matchingAlgo == null) {
				System.out.println("Unknown parameter. Please use on of the following:\n" + 
						getSyntax());									
			} else {
				System.out.println(matchingAlgo);
			}
		}
	}
}
