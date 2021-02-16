package info.quantlab.computationfinance.lecture.assignment2;

/**
 * Holds mean, variance, standard error and sample number of a stochastic object.
 * @author Jakob Ullmann
 *
 */
public class SummaryStatistics {

	private final double expectation, variance, standardError;
	private final int numberOfSamples;

	public SummaryStatistics(double expectation, double variance, double standardError, int numberOfSamples) {
		this.expectation = expectation;
		this.variance = variance;
		this.standardError = standardError;
		this.numberOfSamples = numberOfSamples;
	}

	/**
	 * Obtain a string representation.
	 */
	@Override
	public String toString() {
		String result = "[ Mean=" + Double.toString(expectation)
			+ ", Variance=" + Double.toString(variance)
			+ ", SE=" + Double.toString(standardError)
			+ ", N=" + Integer.toString(numberOfSamples) + " ]";
		return result;
	}
}
