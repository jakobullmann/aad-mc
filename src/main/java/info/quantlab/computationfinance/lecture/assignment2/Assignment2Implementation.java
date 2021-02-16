package info.quantlab.computationfinance.lecture.assignment2;

import info.quantlab.computationfinance.lecture.Assignment2;
import net.finmath.aadexperiments.randomvalue.RandomValue;
import net.finmath.aadexperiments.randomvalue.RandomValueDifferentiable;

/**
 * The solution of assignment 2 implements the following methods. See the interface definition for a documentation.
 */
public class Assignment2Implementation implements Assignment2 {

	@Override
	public RandomValue getRandomValueFromArray(double[] values) {
		return RandomVariable.factory().fromArray(values); // these are always differentiable
	}

	@Override
	public RandomValueDifferentiable getRandomDifferentiableValueFromArray(double[] values) {
		return RandomVariable.factory().fromArray(values);
	}

	@Override
	public RandomValue getMonteCarloBlackModelValueOfDigitalCaplet(RandomValue forwardRate, RandomValue payoffUnit,
			RandomValue volatility, RandomValue brownianMotionUponMaturity, RandomValue strike, RandomValue maturity,
			RandomValue periodLength) {

		RandomValue forwardRateAtMaturity = brownianMotionUponMaturity.mult(volatility)
				.sub(volatility.squared().mult(.5).mult(maturity))
				.exp().mult(forwardRate);
		RandomValue digitalCapletPayoff = payoffUnit.mult(periodLength);
		RandomValue condition  = forwardRateAtMaturity.sub(strike);

		return condition.choose(digitalCapletPayoff, RandomVariable.factory().zero()).expectation();
	}

	@Override
	public RandomValue getMonteCarloBlackModelDeltaOfDigitalCaplet(RandomValue forwardRate, RandomValue payoffUnit,
			RandomValue volatility, RandomValue brownianMotionUponMaturity, RandomValue strike, RandomValue maturity,
			RandomValue periodLength) {

		RandomValue value = getMonteCarloBlackModelValueOfDigitalCaplet(forwardRate, payoffUnit, volatility, brownianMotionUponMaturity,
		                                                                strike, maturity, periodLength);

		return ((RandomValueDifferentiable)value).getDerivativeWithRespectTo((RandomVariableDifferentiable)forwardRate);

	}

	@Override
	public RandomValue getMonteCarloBlackModelValueOfForwardRateInArrears(RandomValue forwardRate, RandomValue payoffUnit,
			RandomValue volatility, RandomValue brownianMotionUponMaturity, RandomValue maturity, RandomValue periodLength) {

		RandomValue forwardRateAtMaturity = brownianMotionUponMaturity.mult(volatility)
				.sub(volatility.squared().mult(.5).mult(maturity))
				.exp().mult(forwardRate);
		RandomValue equivalentPayoffAtT2 = forwardRateAtMaturity.mult(periodLength).mult(RandomVariable.factory().one().add(forwardRateAtMaturity.mult(periodLength)));
		RandomValue value = equivalentPayoffAtT2.mult(payoffUnit).expectation();

		return value;

	}

	@Override
	public 	RandomValue getMonteCarloBlackModelDeltaOfForwardRateInArrears(RandomValue forwardRate, RandomValue payoffUnit,
			RandomValue volatility, RandomValue brownianMotionUponMaturity, RandomValue maturity, RandomValue periodLength) {

		RandomValueDifferentiable value = (RandomValueDifferentiable) getMonteCarloBlackModelValueOfForwardRateInArrears(forwardRate,
				payoffUnit, volatility, brownianMotionUponMaturity, maturity, periodLength);

		return value.getDerivativeWithRespectTo((RandomValueDifferentiable) forwardRate);

	}
}
