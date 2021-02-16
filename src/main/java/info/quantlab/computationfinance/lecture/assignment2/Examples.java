package info.quantlab.computationfinance.lecture.assignment2;

import java.util.Random;

import info.quantlab.computationfinance.lecture.Assignment2;
import net.finmath.aadexperiments.randomvalue.RandomValue;
import net.finmath.aadexperiments.randomvalue.RandomValueFactory;
import net.finmath.aadexperiments.value.ConvertableToFloatingPoint;
import net.finmath.functions.AnalyticFormulas;

public class Examples {
	private static RandomValue getDigitalCapletDelta(RandomValueFactory randomValueFactory, double[] samples, double modelForwardRate, double modelPayoffUnit, double modelVolatility, double productStrike, double productMaturity, double productPeriodLength, Assignment2 solution) {
		RandomValue normal = randomValueFactory.fromArray(samples);
		RandomValue forwardRate = randomValueFactory.fromConstant(modelForwardRate);
		RandomValue payoffUnit = randomValueFactory.fromConstant(modelPayoffUnit);
		RandomValue volatility = randomValueFactory.fromConstant(modelVolatility);
		RandomValue strike = randomValueFactory.fromConstant(productStrike);
		RandomValue maturity = randomValueFactory.fromConstant(productMaturity);
		RandomValue periodLength = randomValueFactory.fromConstant(productPeriodLength);

		RandomValue brownianMotionUponMaturity = normal.mult(maturity.sqrt());

		RandomValue delta = solution.getMonteCarloBlackModelDeltaOfDigitalCaplet(forwardRate, payoffUnit, volatility, brownianMotionUponMaturity, strike, maturity, periodLength);

		return delta;
	}

	public static void testAssigmentDigitalCaplet(Assignment2 solution) {

		final double modelForwardRate = 0.05;
		final double modelPayoffUnit = 0.9;
		final double modelVolatility = 0.3;

		final double productStrike = 0.06;
		final double productMaturity = 2.0;
		final double productPeriodLength = 0.5;

		final int numberOfPaths = 100000;

		/*
		 * Create a normal distributed random sample vector
		 */
		// Create normal distributed random variable
		Random random = new Random(3413);

		double[] samples = new double[numberOfPaths];
		for(int pathIndex=0; pathIndex<numberOfPaths; pathIndex++) {
			samples[pathIndex] = random.nextGaussian();
		}

		RandomValue normal = solution.getRandomValueFromArray(samples);
		RandomValueFactory randomValueFactory = normal.getFactory();

		RandomValue forwardRate = randomValueFactory.fromConstant(modelForwardRate);
		RandomValue payoffUnit = randomValueFactory.fromConstant(modelPayoffUnit);
		RandomValue volatility = randomValueFactory.fromConstant(modelVolatility);
		RandomValue strike = randomValueFactory.fromConstant(productStrike);
		RandomValue maturity = randomValueFactory.fromConstant(productMaturity);
		RandomValue periodLength = randomValueFactory.fromConstant(productPeriodLength);

		RandomValue brownianMotionUponMaturity = normal.mult(maturity.sqrt());

		RandomValue value = solution.getMonteCarloBlackModelValueOfDigitalCaplet(forwardRate, payoffUnit, volatility, brownianMotionUponMaturity, strike, maturity, periodLength);

		double valueMonteCarlo = ((ConvertableToFloatingPoint)value).asFloatingPoint();
		double valueAnalytic = AnalyticFormulas.blackScholesDigitalOptionValue(modelForwardRate, 0.0, modelVolatility, productMaturity, productStrike) * modelPayoffUnit * productPeriodLength;


		boolean success = Math.abs(valueAnalytic-valueMonteCarlo) < 1E-3;
		String message = "Test of getMonteCarloBlackModelValueOfDigitalCaplet: ";
		if(success) message += "Congratulation! The valuation of the digital caplet appears to be correct.";
		else message += "Sorry, the valuation of the digital caplet appears to be not correct.";

		message += "  Expected: " + valueAnalytic + "\n";
		message += "  Actual..: " + valueMonteCarlo + " (using RandomValue) \n";
		message += "\n";

		System.out.println(message);
	}

	public static void testAssigmentDigitalCapletDelta(Assignment2 solution) {

		final double modelForwardRate = 0.05;
		final double modelPayoffUnit = 0.9;
		final double modelVolatility = 0.3;

		final double productStrike = 0.06;
		final double productMaturity = 2.0;
		final double productPeriodLength = 0.5;

		final int numberOfPaths = 100000;

		/*
		 * Create a normal distributed random sample vector
		 */
		// Create normal distributed random variable
		Random random = new Random(3413);

		double[] samples = new double[numberOfPaths];
		for(int pathIndex=0; pathIndex<numberOfPaths; pathIndex++) {
			samples[pathIndex] = random.nextGaussian();
		}

		double deltaAnalytic = AnalyticFormulas.blackModelDigitalCapletDelta(modelForwardRate, modelVolatility, productPeriodLength, modelPayoffUnit, productMaturity, productStrike);

		/*
		 * Try to call the function with a differentiable - see what is happening
		 */
		boolean successWithRandomValueDifferentiable = false;
		double deltaMonteCarloWithDifferentiable = Double.NaN;
		try {
			RandomValueFactory randomValueFactory = solution.getRandomDifferentiableValueFromArray(samples).getFactory();
			RandomValue delta = getDigitalCapletDelta(randomValueFactory, samples, modelForwardRate, modelPayoffUnit, modelVolatility, productStrike, productMaturity, productPeriodLength, solution);

			deltaMonteCarloWithDifferentiable = ((ConvertableToFloatingPoint)delta).asFloatingPoint();

			successWithRandomValueDifferentiable = Math.abs(deltaAnalytic-deltaMonteCarloWithDifferentiable) < 1E-1;
		}
		catch(Exception e) {}

		/*
		 * Try to call the function with a RandomVaue - see what is happening
		 */
		boolean successWithRandomValue = false;
		double deltaMonteCarlo = Double.NaN;
		try {
			RandomValueFactory randomValueFactory = solution.getRandomValueFromArray(samples).getFactory();
			RandomValue delta = getDigitalCapletDelta(randomValueFactory, samples, modelForwardRate, modelPayoffUnit, modelVolatility, productStrike, productMaturity, productPeriodLength, solution);

			deltaMonteCarlo = ((ConvertableToFloatingPoint)delta).asFloatingPoint();

			successWithRandomValue = Math.abs(deltaAnalytic-deltaMonteCarlo) < 1E-1;
		}
		catch(Exception e) {}

		boolean success = successWithRandomValue || successWithRandomValueDifferentiable;

		String message = "Test of getMonteCarloBlackModelDeltaOfDigitalCaplet: ";
		if(success) message += "Congratulation! The delta of the digital caplet appears to be correct.\n";
		else message += "Sorry, the delta of the digital caplet appears to be not correct.\n";

		message += "  Expected: " + deltaAnalytic + "\n";
		message += "  Actual..: " + deltaMonteCarlo + " (using RandomValue) \n";
		message += "  Actual..: " + deltaMonteCarloWithDifferentiable + " (using RandomValueDifferentiable)\n";

		message += "\n";

		System.out.println(message);
	}

	public static void main(String[] args) {

		Assignment2 assigment2solution = new Assignment2Implementation();

		/*
		 * Create a normal distributed random sample vector
		 */
		// Create normal distributed random variable
		Random random = new Random(3413);
		int numberOfPath = 100000;
		double[] samples = new double[numberOfPath];
		for(int pathIndex=0; pathIndex<numberOfPath; pathIndex++) {
			samples[pathIndex] = random.nextGaussian();
		}

		RandomValue normal = assigment2solution.getRandomValueFromArray(samples);
		RandomValueFactory randomValueFactory = normal.getFactory();

		double optionMaturity = 2.0;
		double optionStrike = 0.056;
		double periodLength = 0.5;

		double forward = 0.05;
		double sigma = 0.30;
		double payoffUnit = Math.exp(- 0.05 * (optionMaturity+periodLength)); // Value of the zero bond at payment

		RandomValue brownianIncrement = normal.mult(Math.sqrt(optionMaturity));

		// Value of Caplet
		RandomValue L0 = randomValueFactory.fromConstant(forward);
		RandomValue strike = randomValueFactory.fromConstant(optionStrike);
/*
		// Black model     (I modified this code to facilitate debugging for me)
		RandomValue multiplier = brownianIncrement.mult(sigma)
				.add(- 0.5 * sigma * sigma * optionMaturity)
				.exp();
		RandomValue LT = L0.mult(
				multiplier
				);


		RandomValue exerciseValue = LT.sub(strike);

		// A way to calculate max(L - K,0) * periodLength
		RandomValue payoff = exerciseValue.choose(exerciseValue, randomValueFactory.zero()).mult(periodLength);

		// Discount value
		RandomValue value = payoff.mult(payoffUnit).expectation();

		double valueMonteCarlo = ((ConvertableToFloatingPoint)value).asFloatingPoint();
		double valueAnalytic = AnalyticFormulas.blackModelCapletValue(forward, sigma, optionMaturity, optionStrike, periodLength, payoffUnit);

		System.out.println(String.format("Monte-Carlo...: %7.5f", valueAnalytic));
		System.out.println(String.format("Analytic......: %7.5f", valueMonteCarlo));
*/
		//RandomValue derivative = ((RandomVariableDifferentiable)value).getDerivativeWithRespectTo(x);

		testAssigmentDigitalCaplet(assigment2solution);
		testAssigmentDigitalCapletDelta(assigment2solution);

		RandomValue myres = assigment2solution.getMonteCarloBlackModelValueOfDigitalCaplet(L0, RandomVariable.factory().one(),
				RandomVariable.factory().fromConstant(sigma),
				brownianIncrement,
				strike,
				RandomVariable.factory().fromConstant(optionMaturity),
				RandomVariable.factory().fromConstant(periodLength));
		System.out.println(((ConvertableToFloatingPoint)myres).asFloatingPoint());
		RandomValue mydelta = assigment2solution.getMonteCarloBlackModelDeltaOfDigitalCaplet(L0, RandomVariable.factory().one(),
				RandomVariable.factory().fromConstant(sigma),
				brownianIncrement,
				strike,
				RandomVariable.factory().fromConstant(optionMaturity),
				RandomVariable.factory().fromConstant(periodLength));
		System.out.println(((ConvertableToFloatingPoint)mydelta).asFloatingPoint());
	}
}
