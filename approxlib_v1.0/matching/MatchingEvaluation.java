/*
 * Created on Sep 24, 2008
 */
package matching;

import java.util.Arrays;

/**
 * Stores the result of a matching evaluation. The evaluation can be queried for 
 * recall, precision, true/false positives/negatives, etc.
 * 
 * @author naugsten
 */
public class MatchingEvaluation {

	private long truePositive;
	private long falsePositive;
	private long falseNegative;	

	/**
	 * Initialize with true positives, false positives and false negatives.
	 *  
	 * @param truePositive 
	 * @param falsePositive
	 * @param falseNegative
	 */
	public MatchingEvaluation(long truePositive, long falsePositive, long falseNegative) {
		this.truePositive = truePositive;
		this.falsePositive = falsePositive;
		this.falseNegative = falseNegative;
	}

	public MatchingEvaluation(Match[] computed, Match[] correct) {
		Arrays.sort(computed);
		Arrays.sort(correct);
		int i1 = 0, i2 = 0;
		long insec = 0;
		while ((i1 < computed.length) && (i2 < correct.length)) {
			int cmp = (computed[i1]).compareTo(correct[i2]);
			if (cmp == 0) {
				i1++;
				i2++;
				insec++;
			} else if (cmp < 0) {
				i1++;
			} else {
				i2++;
			}
		}		
		this.falsePositive = computed.length - insec;
		this.falseNegative = correct.length - insec;
		this.truePositive = insec;		
	}

	public double recall() {
		return this.truePositive() / (double)(this.truePositive() + this.falseNegative());
	}

	public double precision() {
		return this.truePositive() / (double)(this.truePositive() + this.falsePositive());
	}
	public double fMeasure() {
		return 2 * this.precision() * this.recall() / (this.precision() + this.recall());
	}

	public long falseNegative() {
		return falseNegative;
	}

	public long falsePositive() {
		return falsePositive;
	}

	public long truePositive() {
		return truePositive;
	}

	@Override
	public String toString() {
		char sep = '\t';
		String s = 
			"# " +
			"tPos" + sep + 
			"fPos" + sep +
			"fNeg" + sep +
			"recall" + sep +
			"precision" + sep +
			"fMeasure\n";
		s += 
			"" +  
			this.truePositive() + sep +
			this.falsePositive() + sep +
			this.falseNegative() + sep +
			this.recall() + sep +
			this.precision() + sep +
			this.fMeasure();
		return s;
	}



}
