package info.quantlab.computationfinance.lecture.assignment2;

import java.util.ArrayList;

import net.finmath.aadexperiments.randomvalue.RandomValueFactory;

public class RandomVariableFactory implements RandomValueFactory {

	public RandomVariableFactory() {
	}

	@Override
	public RandomVariableDifferentiable zero() {
		return fromConstant(0.);
	}

	@Override
	public RandomVariableDifferentiable one() {
		return fromConstant(1.);
	}

	@Override
	public RandomVariableDifferentiable fromConstant(double constant) {
		return fromArray(new double[] { constant });
	}

	@Override
	public RandomVariableDifferentiable fromArray(double[] values) {
		ArrayList<Double> tmp = new ArrayList<>(values.length); // allocate enough memory

		for (double val : values)
			tmp.add(val);

		return new RandomVariableDifferentiable(tmp);
	}

}
