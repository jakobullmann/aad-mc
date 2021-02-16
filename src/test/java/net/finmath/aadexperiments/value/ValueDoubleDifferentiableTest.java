/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.11.2020
 */
package net.finmath.aadexperiments.value;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class ValueDoubleDifferentiableTest {

	@Test
	void testHypotenuse1() {
		BiFunction<Value,Value,Value> hypotenuse = (a,b) -> (a.squared().add(b.squared())).sqrt();

		ValueDifferentiable a = new ValueDoubleDifferentiable(3.0);
		ValueDifferentiable b = new ValueDoubleDifferentiable(4.0);

		ValueDifferentiable c = (ValueDifferentiable) hypotenuse.apply(a, b);

		Value derivativeAnalytic1 = a.div(c);
		Value derivativeAlgorithmic1 = c.getDerivativeWithRespectTo(a);
		assertEquals(valueOf(derivativeAnalytic1), valueOf(derivativeAlgorithmic1), 1E-15, "partial derivative dz/dx");

		Value derivativeAnalytic2 = b.div(c);
		Value derivativeAlgorithmic2 = c.getDerivativeWithRespectTo(b);
		assertEquals(valueOf(derivativeAnalytic2), valueOf(derivativeAlgorithmic2), 1E-15, "partial derivative dz/dx");
	}

	@Test
	void testHypotenuse2() {
		BiFunction<Value,Value,Value> hypotenuse = (a,b) -> (a.squared().add(b.squared())).sqrt();

		ValueDifferentiable a = new ValueDoubleDifferentiable(4.0);
		ValueDifferentiable b = new ValueDoubleDifferentiable(7.0);

		ValueDifferentiable c = (ValueDifferentiable) hypotenuse.apply(a, b);

		Value derivativeAnalytic1 = a.div(c);
		Value derivativeAlgorithmic1 = c.getDerivativeWithRespectTo(a);
		assertEquals(valueOf(derivativeAnalytic1), valueOf(derivativeAlgorithmic1), 1E-15, "partial derivative dz/dx");

		Value derivativeAnalytic2 = b.div(c);
		Value derivativeAlgorithmic2 = c.getDerivativeWithRespectTo(b);
		assertEquals(valueOf(derivativeAnalytic2), valueOf(derivativeAlgorithmic2), 1E-15, "partial derivative dz/dx");
	}

	/**
	 * This test is sensitive to the fact that the tree is processed in the right order (descending by id).
	 */
	@Test
	void testOder() {
		Function<Value, Value> function = x -> {
			Value x1 = x.mult(new ValueDoubleDifferentiable(2.0));
			Value x2 = x1.mult(new ValueDoubleDifferentiable(4.0));
			Value x3 = x1.add(x2);

			return x3;
		};

		ValueDifferentiable x = new ValueDoubleDifferentiable(1.0);
		ValueDifferentiable y = (ValueDifferentiable) function.apply(x);

		Double derivativeAnalytic = 10.0;
		Value derivativeAlgorithmic = y.getDerivativeWithRespectTo(x);
		assertEquals(derivativeAnalytic, valueOf(derivativeAlgorithmic), 1E-15, "partial derivative dy/dx");
	}

	/**
	 * This test checks the implementation of div;
	 */
	@Test
	void testDivFirstArg() {
		ValueDifferentiable x0 = new ValueDoubleDifferentiable(6.0);
		ValueDifferentiable x1 = new ValueDoubleDifferentiable(2.0);

		Value y = x0.div(x1);

		Double derivativeAnalytic = 1.0 / 2.0;
		Value derivativeAlgorithmic = ((ValueDifferentiable)y).getDerivativeWithRespectTo(x0);
		assertEquals(derivativeAnalytic, valueOf(derivativeAlgorithmic), 1E-15, "partial derivative dy/dx");
	}

	/**
	 * This test checks the implementation of div;
	 */
	@Test
	void testDivSecondArg() {
		ValueDifferentiable x0 = new ValueDoubleDifferentiable(6.0);
		ValueDifferentiable x1 = new ValueDoubleDifferentiable(2.0);

		Value y = x0.div(x1);

		Double derivativeAnalytic = -6.0 / 4.0;
		Value derivativeAlgorithmic = ((ValueDifferentiable)y).getDerivativeWithRespectTo(x1);
		assertEquals(derivativeAnalytic, valueOf(derivativeAlgorithmic), 1E-15, "partial derivative dy/dx");
	}

	/**
	 * This test checks the implementation of div;
	 */
	@Test
	void testDiv() {
		ValueDifferentiable x0 = new ValueDoubleDifferentiable(6.0);

		Value y = x0.div(x0);

		Double derivativeAnalytic = 0.0;
		Value derivativeAlgorithmic = ((ValueDifferentiable)y).getDerivativeWithRespectTo(x0);
		assertEquals(derivativeAnalytic, valueOf(derivativeAlgorithmic), 1E-15, "partial derivative dy/dx");
	}

	private static double valueOf(Value x) {
		return ((ConvertableToFloatingPoint)x).asFloatingPoint();
	}	
}
