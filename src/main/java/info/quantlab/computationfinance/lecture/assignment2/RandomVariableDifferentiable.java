package info.quantlab.computationfinance.lecture.assignment2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.finmath.aadexperiments.randomvalue.RandomValue;
import net.finmath.aadexperiments.randomvalue.RandomValueDifferentiable;

/**
 * A class representing instances of RandomVariable that were constructed from other random variables
 * through application of built-in or custom differentiable arithmetic operations, with respect to which
 * AAD can be performed by means of the method getDerivativeWithRespectTo().
 * @author Jakob Ullmann
 *
 */
public final class RandomVariableDifferentiable extends RandomVariable implements RandomValueDifferentiable {

	protected HashMap<Long, RandomVariable> derivatives = null;    // map holding computed derivatives

	/**
	 * Creates a shallow copy and implements RandomVariableDifferentiable, if possible; throws UnsupportedOperationException otherwise.
	 * @param rv source
	 */
	protected RandomVariableDifferentiable(RandomVariable rv) {
		super(rv);
		if (rv.undifferentiable) {
			throw new UnsupportedOperationException("Derivatives of indicator function are not differentiable.");
		}
	}

	/**
	 * Creates a RandomVariableDifferentiable instance with the ArrayList passed as argument `values'.
	 * Important: This does not clone values, but it copies the reference. Use RandomVariable.factory().fromArray() for that purpose.
	 * @param values
	 */
	protected RandomVariableDifferentiable(ArrayList<Double> values) {
		this.values = values;
		simplify();
	}

	private void computeDerivatives() {

		derivatives = new HashMap<>();

		NavigableSet<RandomVariable> nodesOutstanding = new TreeSet<>( (x, y) -> Long.signum(x.id - y.id) );
		RandomVariable root = this;

		derivatives.put(root.id, getFactory().one());
		nodesOutstanding.add(root);

		while (!nodesOutstanding.isEmpty()) {
			RandomVariable node = nodesOutstanding.pollLast();
			processNode(node);
			nodesOutstanding.addAll(node.deps);
		}
	}

	private RandomVariable lookupOrInitialize(RandomVariable key) {
		if (!derivatives.containsKey(key.id))
			derivatives.put(key.id, getFactory().zero());
		return derivatives.get(key.id);
	}

	private RandomVariable lastNode           = null; // Caching
	private RandomVariable lastNodeDerivative = null;

	private void pushDerivative(RandomVariable node, RandomVariable key, RandomVariable value) {
		if (lastNode != node) {                                                                 // Update cache
			lastNode = node;
			lastNodeDerivative = lookupOrInitialize(node);
		}

		derivatives.put(key.id, lookupOrInitialize(key).add(value.mult(lastNodeDerivative)));   // add new value
	}

	private void pushDerivative(RandomVariable node, RandomVariable key, double value) {
		pushDerivative(node, key, getFactory().fromConstant(value));
	}

	private void pushExpectation(RandomVariable node, RandomVariable key) {
		RandomVariable nodeDerivative = lookupOrInitialize(node);
		derivatives.put(key.id, lookupOrInitialize(key).add(nodeDerivative.expectation()));   // add new value
	}

	private static TriFunction<Double, Double, Double, Double> getIndicatorDerivativeX(double h) {
		return ((_x, _y, _z) -> {
			if      (_x <= -h)   return 0.;
			else if (_x <= h)    return (_y - _z)/(2.*h);
			else                 return 0.;
		});
	}

	private static TriFunction<Double, Double, Double, Double> getIndicatorDerivativeY(double h) {
		return ((_x, _y, _z) -> {
			if      (_x <= -h)   return 0.;
			else if (_x <= h)    return (h + _x)/(2.*h);
			else                 return 1.;
		});
	}

	private static TriFunction<Double, Double, Double, Double> getIndicatorDerivativeZ(double h) {
		return ((_x, _y, _z) -> {
			if      (_x <= -h)   return 1.;
			else if (_x <= h)    return (h - _x)/(2.*h);
			else                 return 0.;
		});
	}

	private void processNode(RandomVariable node) {
		if (node.operation == null) return;

		RandomVariable x, y, z;  int s = node.deps.size();
		x = s > 0 ? node.deps.get(0) : null;
		y = s > 1 ? node.deps.get(1) : null;
		z = s > 2 ? node.deps.get(2) : null;

		Function<Double, Double>                     derivativeX = null;
		BiFunction<Double, Double, Double>           biDerivativeX = null,  biDerivativeY = null;
		TriFunction<Double, Double, Double, Double>  triDerivativeX = null, triDerivativeY = null, triDerivativeZ = null;

		switch (node.operation) {

			case ADD:
				pushDerivative(node, x, 1.);
				pushDerivative(node, y, 1.);
				break;
			case SUB:
				pushDerivative(node, x, 1.);
				pushDerivative(node, y, -1.);
				break;
			case MUL:
				pushDerivative(node, x, y);
				pushDerivative(node, y, x);
				break;
			case DIV:
				pushDerivative(node, x, getFactory().one().div(y));
				pushDerivative(node, y, x.div(y.squared()).mult(-1.));
				break;
			case SQR:
				pushDerivative(node, x, x.mult(2.));
				break;
			case SQRT:
				pushDerivative(node, x, getFactory().fromConstant(.5).div(node));
				break;
			case EXP:
				pushDerivative(node, x, node);
				break;
			case LOG:
				pushDerivative(node, x, getFactory().one().div(x));
				break;
			case EXPECT:
				pushExpectation(node, x);
				break;
			case CHO:
				double h = node.h;
				triDerivativeX = getIndicatorDerivativeX(h);
				triDerivativeY = getIndicatorDerivativeY(h);
				triDerivativeZ = getIndicatorDerivativeZ(h);
				pushDerivative(node, x, new RandomVariable(applyArithmeticOperationBroadcast(triDerivativeX,
						                                                            x.values, y.values, z.values))
						                    .removeDifferentiability());
				pushDerivative(node, y, new RandomVariable(applyArithmeticOperationBroadcast(triDerivativeY,
						                                                            x.values, y.values, z.values))
						                    .removeDifferentiability());
				pushDerivative(node, z, new RandomVariable(applyArithmeticOperationBroadcast(triDerivativeZ,
						                                                            x.values, y.values, z.values))
						                    .removeDifferentiability());
				break;
			case CUSTOMUNI:
				derivativeX = node.customFunctionDerivative;
				pushDerivative(node, x, new RandomVariable(applyArithmeticOperationBroadcast(derivativeX,
						                                                            x.values))
						                    .removeDifferentiability());
				break;
			case CUSTOMBI:
				biDerivativeX = node.customBiFunctionDerivativeX;
				biDerivativeY = node.customBiFunctionDerivativeY;
				pushDerivative(node, x, new RandomVariable(applyArithmeticOperationBroadcast(biDerivativeX,
						                                                            x.values, y.values))
						                    .removeDifferentiability());
				pushDerivative(node, y, new RandomVariable(applyArithmeticOperationBroadcast(biDerivativeY,
						                                                            x.values, y.values))
						                    .removeDifferentiability());
				break;
			case CUSTOMTRI:
				triDerivativeX = node.customTriFunctionDerivativeX;
				triDerivativeY = node.customTriFunctionDerivativeY;
				triDerivativeZ = node.customTriFunctionDerivativeZ;
				pushDerivative(node, x, new RandomVariable(applyArithmeticOperationBroadcast(triDerivativeX,
						                                                            x.values, y.values, z.values))
						                    .removeDifferentiability());
				pushDerivative(node, y, new RandomVariable(applyArithmeticOperationBroadcast(triDerivativeY,
						                                                            x.values, y.values, z.values))
						                    .removeDifferentiability());
				pushDerivative(node, z, new RandomVariable(applyArithmeticOperationBroadcast(triDerivativeZ,
						                                                            x.values, y.values, z.values))
						                    .removeDifferentiability());

		}
	}

	/*
	 * If both x and this are deterministic, this returns the ordinary derivative,
	 * otherwise it returns the Frechet derivative.
	 */
	@Override
	public RandomValue getDerivativeWithRespectTo(RandomValueDifferentiable x) {
		if (!undifferentiable && x instanceof RandomVariable) {
			if (derivatives == null)
				computeDerivatives();
			if (((RandomVariable)x).isDeterministic() && this.isDeterministic())
				return lookupOrInitialize((RandomVariable)x).expectation();
			else
				return lookupOrInitialize((RandomVariable)x);
		} else {
			if (!(x instanceof RandomVariable))
				return getFactory().zero();       // it is impossible to have this x as a dependency by design, therefore we can return zero.
			else
				throw new UnsupportedOperationException("Derivatives of indicator function are not differentiable.");
		}
	}

}
