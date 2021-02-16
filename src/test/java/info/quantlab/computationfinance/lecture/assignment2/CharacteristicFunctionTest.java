package info.quantlab.computationfinance.lecture.assignment2;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.finmath.aadexperiments.randomvalue.RandomValue;
import net.finmath.aadexperiments.randomvalue.RandomValueDifferentiable;
import net.finmath.aadexperiments.value.ConvertableToFloatingPoint;

class CharacteristicFunctionTest {

	static RandomValue X, Y;

	@BeforeAll
	static void setup() {
		Random random = new Random(3413);
		int numberOfPath = 100000;
		double[] samplesX = new double[numberOfPath], samplesY = new double[numberOfPath];
		for(int pathIndex=0; pathIndex<numberOfPath; pathIndex++) {
			samplesX[pathIndex] = random.nextGaussian();
			samplesY[pathIndex] = random.nextGaussian();
		}

		X = RandomVariable.factory().fromArray(samplesX); // X ~ N(0, 1)
		Y = RandomVariable.factory().fromArray(samplesY); // Y ~ N(0, 1)
	}

	/*
	 * This is supposed to test the AAD implementation in a situation where expectations
	 * occur at an inner position.
	 */
	@Test
	void testCharacteristicFunction() {

		double valueLambda = .5;
		RandomValue lambda = RandomVariable.factory().fromConstant(valueLambda); // lambda = 1

		/*
		 * We use that the characteristic function of N(0, 1) is exp(-1/2*k^2) [so E[exp(k*X)] = exp(+1/2*k^2)]
		 *
		 */
		RandomValue charf = X.div(X.squared().div(lambda.squared()).expectation().sqrt() ).exp().expectation();
		RandomValue derivative = ((RandomVariableDifferentiable)charf).getDerivativeWithRespectTo((RandomVariableDifferentiable)lambda);
		double charfValue = ((ConvertableToFloatingPoint)charf).asFloatingPoint();
		double derivativeValue = ((ConvertableToFloatingPoint)derivative).asFloatingPoint();
		double charfAnalytic = Math.exp( 0.5 * valueLambda * valueLambda );
		double derivativeAnalytic = valueLambda * charfAnalytic;

		System.out.println("Analytic characteristic function:  " + Double.toString(charfValue));
		System.out.println("Simulated characteristic function: " + Double.toString(charfAnalytic));

		assertTrue( Math.abs( charfValue - charfAnalytic ) < 0.01 );
		assertTrue( Math.abs( derivativeValue - derivativeAnalytic ) < 0.01 );

	}

	/*
	 * Testing custom functions
	 */
	@Test
	void testCustomFunction() {
		Function<Double, Double> cosine = (x -> Math.cos(x));
		Function<Double, Double> minusSine = (x -> -Math.sin(x));

		RandomValueDifferentiable cosX = (RandomVariableDifferentiable) ((RandomVariableDifferentiable) X).customOperation(cosine, minusSine).expectation();

		RandomValue derivative = cosX.getDerivativeWithRespectTo((RandomValueDifferentiable)X).expectation(); // E[sin(X)] = 0 by symmetry
		double derivativeValue = ((ConvertableToFloatingPoint)derivative).asFloatingPoint();

		System.out.println("Simulated E[sin(X)]: " + Double.toString(derivativeValue));

		assertTrue( Math.abs(derivativeValue) < 0.01 );

	}
	/*
	 * Testing custom functions
	 */
	@Test
	void testCustomFunction2() {
		BiFunction<Double, Double, Double> F    = ((x,y) -> Math.cos(x-y));
		BiFunction<Double, Double, Double> dFdX = ((x,y) -> -Math.sin(x-y));
		BiFunction<Double, Double, Double> dFdY = ((x,y) -> Math.sin(x-y));

		Random random = new Random(3413);
		int numberOfPath = 100000;
		double[] samplesX = new double[numberOfPath];
		double[] samplesY = new double[numberOfPath];
		for(int pathIndex=0; pathIndex<numberOfPath; pathIndex++) {
			samplesX[pathIndex] = random.nextGaussian();
			samplesY[pathIndex] = random.nextGaussian();
		}

		RandomVariableDifferentiable cosXminusY = (RandomVariableDifferentiable) ((RandomVariable)X).customOperation(F, (RandomVariable)Y, dFdX, dFdY);

		RandomValue derivativeX = cosXminusY.getDerivativeWithRespectTo((RandomValueDifferentiable)X).expectation(); // E[-sin(X-Y)] = 0
		RandomValue derivativeY = cosXminusY.getDerivativeWithRespectTo((RandomValueDifferentiable)Y).expectation(); // E[sin(X-Y)] = 0
		double derivativeValueX = ((ConvertableToFloatingPoint)derivativeX).asFloatingPoint();
		double derivativeValueY = ((ConvertableToFloatingPoint)derivativeY).asFloatingPoint();

		System.out.println("Simulated E[sin(X-Y)]:          " + Double.toString(derivativeValueY));
		System.out.println("Simulated E[sin(X-Y)-sin(X-Y)]: " + Double.toString(derivativeValueX + derivativeValueY));

		assertTrue( Math.abs(derivativeValueX) < 0.01 );
		assertTrue( Math.abs(derivativeValueX + derivativeValueY) < 0.00001 );

	}


}
