/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.11.2020
 */
package net.finmath.aadexperiments.value;

import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValueDoubleDifferentiableChooseTest {

	@Test
	void testDiscontinousFunction() {

		// The leaf nodes (input values)
		ValueDifferentiable k = new ValueDoubleDifferentiable(1.0);
		ValueDifferentiable a = new ValueDoubleDifferentiable(3.0);
		ValueDifferentiable b = new ValueDoubleDifferentiable(4.0);

		// This is the function x -> (x-k) >= 0 ? a x : b * x * x
		Function<Value,Value> function = x -> x.squared().sub(k).choose(a.mult(x),b.mult(x.squared()));

		// df/dx for x = 2
		ValueDifferentiable x1 = new ValueDoubleDifferentiable(2.0);
		ValueDifferentiable y1 = (ValueDifferentiable) function.apply(x1);
		Value derivativeAlgorithmic1 = y1.getDerivativeWithRespectTo(x1);
		Assertions.assertEquals(valueOf(a), valueOf(derivativeAlgorithmic1), 1E-15, "partial derivative");

		// df/dx for x = 1/2
		ValueDifferentiable x2 = new ValueDoubleDifferentiable(0.5);
		ValueDifferentiable y2 = (ValueDifferentiable) function.apply(x2);
		Value derivativeAlgorithmic2 = y2.getDerivativeWithRespectTo(x2);
		Assertions.assertEquals(valueOf(b.mult(x2.add(x2))), valueOf(derivativeAlgorithmic2), 1E-15, "partial derivative");

		// df/dx for x = k
		ValueDifferentiable x3 = (ValueDifferentiable)k.sqrt();
		ValueDifferentiable y3 = (ValueDifferentiable) function.apply(x3);
		Value derivativeAlgorithmic3 = y3.getDerivativeWithRespectTo(x3);
		Assertions.assertEquals(Double.NaN, valueOf(derivativeAlgorithmic3), 1E-15, "partial derivative");

		// df/da for x = k
		Value derivativeAlgorithmic4 = y3.getDerivativeWithRespectTo(a);
		Assertions.assertEquals(valueOf(x3), valueOf(derivativeAlgorithmic4), 1E-15, "partial derivative");
	}

	private static double valueOf(Value x) {
		return ((ConvertableToFloatingPoint)x).asFloatingPoint();
	}	
}
