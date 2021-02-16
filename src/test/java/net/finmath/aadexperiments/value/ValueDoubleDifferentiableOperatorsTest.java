/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.11.2020
 */
package net.finmath.aadexperiments.value;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ValueDoubleDifferentiableOperatorsTest {

	@Test
	void testDivWrtFirstArgument() {
		
		ValueDifferentiable x0 = new ValueDoubleDifferentiable(6.0);
		ValueDifferentiable x1 = new ValueDoubleDifferentiable(2.0);
		
		Value y = x0.div(x1);
		
		Double derivativeAnalytic = 1.0/2.0;
		Value derivativeAlgorithmic = ((ValueDifferentiable) y).getDerivativeWithRespectTo(x0);

		System.out.println(derivativeAnalytic);
		System.out.println(derivativeAlgorithmic);
		
		assertEquals(derivativeAnalytic, valueOf(derivativeAlgorithmic), 1E-15);
		
	}
	
	private static double valueOf(Value x) {
		return ((ConvertableToFloatingPoint)x).asFloatingPoint();
	}	
}
