package info.quantlab.computationfinance.lecture.assignment2;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.finmath.aadexperiments.randomvalue.RandomValue;
import net.finmath.aadexperiments.value.ConvertableToFloatingPoint;

/**
 * A class holding sample values of a random variable, with support for built-in, as well as custom, arithmetic operations of
 * one, two or three parameters, and AAD for those, with support for parallel computation.
 *
 * @author Jakob Ullmann
 *
 */
public class RandomVariable implements ConvertableToFloatingPoint, RandomValue, StatisticalObject {

	/**
	 * Controls the width of the call spread (2*h) when RandomVariable.choose() is called: h will be set to
	 * RandomVariable.hFactor * sd, where sd is the biased standard deviation of the calling instance. The returned
	 * RandomVariable object will hold this value of h.
	 */
	public static double hFactor = .005;

	/**
	 * Controls the tolerance within which sample values are considered equal and can trigger a simplification of a random
	 * variable to a deterministic value.
	 */
	public static double tolerance = 1e-8;

	/**
	 * If set to true, obj.writeDebug (where obj is an instance of RandomVariable) will write debug messages to System.out.
	 * This method is called when RandomVariable.choose() is applied, and it will print the number of samples within the
	 * call spread.
	 */
	private final static boolean debugMode = false;

	private static AtomicLong nextId = new AtomicLong();

	protected long id = nextId.incrementAndGet();

	/**
	 * Call spread parameter.
	 */
	protected double h = Double.NaN;

	/**
	 * locked = true indicates that this random variable is the result of a derivative operation on a RandomVariable constructed through
	 * RandomVariable.choose(), or a descendant thereof. It is not possible to construct RandomVariableDifferentiable objects by passing
	 * this RandomVariable object to RandomVariableDifferentiable's copy constructor in that case, and whenever any arithmetic operation
	 * is called on this object, or whenever this object is passed as an argument to an arithmetic operation called on another object of
	 * RandomVariable or RandomVariableDifferentiable, the resulting object will not implement RandomVariableDifferentiable.
	 */
	protected boolean undifferentiable = false;

	private RandomVariableFactory mFactory = null;

	/**
	 * Sample values.
	 */
	protected ArrayList<Double> values;

	/**
	 * Functional interface for function of three input variables and one output.
	 * @author Jakob Ullmann
	 *
	 * @param <T> input type 1
	 * @param <U> input type 2
	 * @param <V> input type 3
	 * @param <R> return type
	 */
	@FunctionalInterface
	public interface TriFunction<T, U, V, R> {
		public R apply(T t, U u, V v);
	}

	protected enum Operation {
		SQRT, EXP, LOG, SQR, ADD, SUB, MUL, DIV, CHO, CUSTOMUNI, CUSTOMBI, CUSTOMTRI, EXPECT
	}

	protected Function<Double, Double>                        customFunctionDerivative      = null;
	protected BiFunction<Double, Double, Double>              customBiFunctionDerivativeX   = null;
	protected BiFunction<Double, Double, Double>              customBiFunctionDerivativeY   = null;
	protected TriFunction<Double, Double, Double, Double>     customTriFunctionDerivativeX  = null;
	protected TriFunction<Double, Double, Double, Double>     customTriFunctionDerivativeY  = null;
	protected TriFunction<Double, Double, Double, Double>     customTriFunctionDerivativeZ  = null;

	/**
	 * Writes a debug message to System.out, along with the this.id and the this.operation, provided that RandomVariable.debugMode
	 * is set to true. Otherwise, nothing will happen.
	 * @param msg Debug message
	 */
	private void writeDebug(String msg) {
		if (debugMode)
			System.out.println("[ID: " + Long.toString(id) + "]  " + msg);
	}

	/**
	 * Indicates whether this.values contains precisely one element, or all elements are equal.
	 * Only for caching purposes, may be false-negative. For reliable information, use
	 * this.isDeterministic(), which will eventually update this.deterministic.
	 */
	protected boolean deterministic = false;

	/**
	 * A reference to an enum Operation, indicating what differentiable arithmetic operation led to this random variable.
	 * If this random variable was not constructed from other random variables through arithmetic operations, or if those
	 * operations are not differentiable, this.operation shall be null.
	 */
	protected Operation operation = null;

	/**
	 * List of dependencies for AAD.
	 */
	protected ArrayList<RandomVariable> deps = new ArrayList<>(3);

	/**
	 * Basic constructor. Initializes this.values to be an empty list, allocates space for one element.
	 */
	protected RandomVariable() {
		values = new ArrayList<>(1);
	}

	/**
	 * Creates a random variable from a given ArrayList<Double> of values. Important: The ArrayList values will be assigned,
	 * not copied!
	 * @param values An ArrayList<Double> holding the values.
	 */
	protected RandomVariable(ArrayList<Double> values) {
		this();
		this.values = values;
		simplify();
	}

	/**
	 * Creates a random variable from a given ArrayList<Double> of values and sets operation.
	 * @param values An ArrayList<Double> containing the values.
	 * @param operation Arithmetic operation.
	 */
	protected RandomVariable(ArrayList<Double> values, Operation operation) {
		this(values);
		this.operation = operation;
	}

	@Override
	public Double asFloatingPoint() {
		if (isDeterministic())
			return values.get(0);
		else
			return Double.NaN;
	}

	@Override
	public String toString() {
		if (isDeterministic())
			return Double.toString(asFloatingPoint());
		else
			return getSummaryStatistics().toString();
	}

	/**
	 * Get summary statistics (mean, variance, standard error, sample number).
	 * The variance is normalized by n rather than n-1, which is a biased estimator, but consistent with the expectation
	 * that deterministic random variables have zero variance; similarly for the standard error.
	 * @return summary statistics
	 */
	@Override
	public SummaryStatistics getSummaryStatistics() {
		return new SummaryStatistics(calculateExpectation(), calculateVariance(), calculateStandardError(), values.size());
	}


	protected RandomVariable setCustomFunctionDerivative(Function<Double, Double> f) {
		this.customFunctionDerivative = f;
		return this;
	}

	protected RandomVariable setCustomBiFunctionDerivatives(BiFunction<Double, Double, Double> f, BiFunction<Double, Double, Double> g) {
		this.customBiFunctionDerivativeX = f;
		this.customBiFunctionDerivativeY = g;
		return this;
	}

	protected RandomVariable setCustomTriFunctionDerivatives(TriFunction<Double, Double, Double, Double> f, TriFunction<Double, Double, Double, Double> g, TriFunction<Double, Double, Double, Double> h) {
		this.customTriFunctionDerivativeX = f;
		this.customTriFunctionDerivativeY = g;
		this.customTriFunctionDerivativeZ = h;
		return this;
	}

	/**
	 * Fix the call spread parameter h.
	 * @param h Value to be fixed.
	 * @return this
	 */
	protected RandomVariable setH(double h) {
		this.h = h;
		return this;
	}

	/**
	 *
	 * @param values
	 * @param operation
	 * @param deps
	 */
	protected RandomVariable(ArrayList<Double> values, Operation operation, ArrayList<RandomVariable> deps) {
		this(values, operation);
		this.deps = deps;
	}

	public RandomVariable(RandomVariable rv) {
		this();
		this.values = rv.values;
		this.deps = rv.deps;
		this.operation = rv.operation;
		this.undifferentiable = rv.undifferentiable;
		this.h = rv.h;
		this.id = rv.id;
		this.deterministic = rv.deterministic;
		this.customFunctionDerivative = rv.customFunctionDerivative;
		this.customBiFunctionDerivativeX = rv.customBiFunctionDerivativeX;
		this.customBiFunctionDerivativeY = rv.customBiFunctionDerivativeY;
		this.customTriFunctionDerivativeX = rv.customTriFunctionDerivativeX;
		this.customTriFunctionDerivativeY = rv.customTriFunctionDerivativeY;
		this.customTriFunctionDerivativeZ = rv.customTriFunctionDerivativeZ;
		this.mExp = rv.mExp;
		this.mLog = rv.mLog;
		this.mSqrt = rv.mSqrt;
		this.mExpectation = rv.mExpectation;
		this.mStandardError = rv.mStandardError;
		this.mVariance = rv.mVariance;
	}

	protected RandomVariable addDependencies(RandomVariable... args) {
		for (var arg : args)
			deps.add(arg);
		return this;
	}

	protected RandomVariable setOperation(Operation operation) {
		this.operation = operation;
		return this;
	}

	protected RandomVariable removeDifferentiability() {
		this.undifferentiable = true;
		return this;
	}

	/**
	 * Will set this.locked to true if at least one of the arguments to this function
	 * are marked as locked.
	 * @param sources
	 * @return this.
	 */
	protected RandomVariable qualifyDifferentiability(RandomVariable... sources) {
		for (var source : sources)
			this.undifferentiable |= source.undifferentiable;
		return this;
	}

	/**
	 * Returns either result, or a new RandomVariableDifferentiable instance holding a copy of result (and having the same id),
	 * depending on result.locked and whether result is already an instance of RandomVariableDifferentiable.
	 * @param result
	 * @return Either result or a RandomVariableDifferentiable copy of result.
	 */
	private static RandomVariable doReturn(RandomVariable result) {
		if (!result.undifferentiable && !(result instanceof RandomVariableDifferentiable))
			return new RandomVariableDifferentiable(result);
		else
			return result;
	}

	private boolean allEqual(int... args) {
		Integer val = null;
		for (int arg : args) {
			if (val == null)
				val = arg;
			else if (arg != val)
				return false;
		}
		return true;
	}

	/**
	 * Checks whether the list passed hereto is deterministic within the tolerance range controlled
	 * by RandomVariable.tolerance.
	 * @param values
	 * @return
	 */
	protected static boolean areValuesDeterministic(List<Double> values) {
		int s = values.size();
		if (s == 0)
			return false;
		else if (s == 1)
			return true;
		else {
			double a = values.get(0);
			for (double b : values)
				if (Math.abs(b - a) > tolerance)
					return false;
			return true;
		}
	}

	/**
	 * Checks whether the this.values is deterministic within the tolerance range controlled
	 * by RandomVariable.tolerance, and triggers a simplification in case.
	 * @return true if deterministic, else false.
	 */
	protected boolean isDeterministic() {
		if (!deterministic)
			deterministic = areValuesDeterministic(values);
		if (deterministic)
			reduce();
		return deterministic;
	}

	/**
	 * Reduce this.values to its first element; Does not perform a check for determinacy.
	 */
	protected void reduce() {
		double a = values.get(0);
		values = new ArrayList<>(1);
		values.add(a);
		deterministic = true;
	}

	/**
	 * Same as this.isDeterministic() and throwing away the result.
	 */
	protected void simplify() {
		isDeterministic();
	}

	protected ArrayList<Double> applyArithmeticOperation(Function<Double, Double> func, double op) {
		ArrayList<Double> result = new ArrayList<>(1);
		result.add(func.apply(op));
		return result;
	}

	protected ArrayList<Double> applyArithmeticOperation(BiFunction<Double, Double, Double> func, List<Double> op1, double op2) {
		if (!op1.isEmpty())
			return new ArrayList<>(op1.parallelStream().map(o -> func.apply(o, op2)).collect(Collectors.toList()));
		else
			throw new UnsupportedOperationException("applyArithmeticOperation called with empty operand");
	}

	protected ArrayList<Double> applyArithmeticOperation(TriFunction<Double, Double, Double, Double> func, List<Double> op1, double op2, double op3) {
		if (!op1.isEmpty())
			return new ArrayList<>(op1.parallelStream().map(o -> func.apply(o, op2, op3)).collect(Collectors.toList()));
		else
			throw new UnsupportedOperationException("applyArithmeticOperation called with empty operand");
	}

	protected ArrayList<Double> applyArithmeticOperation(BiFunction<Double, Double, Double> func, double op1, List<Double> op2) {
		return applyArithmeticOperation((b, a) -> func.apply(a, b), op2, op1);
	}

	protected ArrayList<Double> applyArithmeticOperation(BiFunction<Double, Double, Double> func, List<Double> op1, List<Double> op2) {
		int s1 = op1.size(); int s2 = op2.size();
		if (s1 == s2 && s1 > 0) {

			ArrayList<Double> result = new ArrayList<>(op1);

			IntStream intstr = IntStream.range(0, s1);
			intstr.parallel().forEach(i -> {
				result.set(i, func.apply(op1.get(i), op2.get(i))); // no structural modification, so thread-safe
			});

			return result;
		} else
			throw new UnsupportedOperationException(MessageFormat.format("applyArithmeticOperation called with different or zero operand dimensions: {0}, {1}.",
					                                 s1, s2));
	}

	protected ArrayList<Double> applyArithmeticOperation(TriFunction<Double, Double, Double, Double> func, List<Double> op1, List<Double> op2, List<Double> op3) {
		int s1 = op1.size(); int s2 = op2.size(); int s3 = op3.size();
		if (allEqual(s1, s2, s3) && s1 > 0) {
			ArrayList<Double> result = new ArrayList<>(op1);

			IntStream intstr = IntStream.range(0, s1);
			intstr.parallel().forEach(i -> {
				result.set(i, func.apply(op1.get(i), op2.get(i), op3.get(i)));
			});

			return result;
		} else
			throw new UnsupportedOperationException(MessageFormat.format("applyArithmeticOperation called with different or zero operand dimensions: {0}, {1}, {2}.",
					                                 s1, s2, s3));
	}

	protected ArrayList<Double> applyArithmeticOperation(TriFunction<Double, Double, Double, Double> func, List<Double> op1, List<Double> op2, double op3) {
		int s1 = op1.size(); int s2 = op2.size();
		if (s1 == s2 && s1 > 0) {
			ArrayList<Double> result = new ArrayList<>(op1);

			IntStream intstr = IntStream.range(0, s1);
			intstr.parallel().forEach(i -> {
				result.set(i, func.apply(op1.get(i), op2.get(i), op3));
			});

			return result;
		} else
			throw new UnsupportedOperationException(MessageFormat.format("applyArithmeticOperation called with different or zero operand dimensions: {0}, {1}.",
					                                 s1, s2));
	}

	protected ArrayList<Double> applyArithmeticOperation(TriFunction<Double, Double, Double, Double> func,
			double op1, double op2, List<Double> op3) {
		return applyArithmeticOperation((b, c, a) -> func.apply(a, b, c), op2, op3, op1);
	}

	protected ArrayList<Double> applyArithmeticOperation(TriFunction<Double, Double, Double, Double> func,
			double op1, List<Double> op2, double op3) {
		return applyArithmeticOperation((b, a, c) -> func.apply(a, b, c), op2, op1, op3);
	}

	protected ArrayList<Double> applyArithmeticOperation(TriFunction<Double, Double, Double, Double> func,
			double op1, List<Double> op2, List<Double> op3) {
		return applyArithmeticOperation((b, c, a) -> func.apply(a, b, c), op2, op3, op1);
	}

	protected ArrayList<Double> applyArithmeticOperation(TriFunction<Double, Double, Double, Double> func,
			List<Double> op1, double op2, List<Double> op3) {
		return applyArithmeticOperation((a, c, b) -> func.apply(a, b, c), op1, op3, op2);
	}

	/*
	 * Automatically extending the dimensionality of an object, i.e. when adding an M x N matrix
	 * and an M-dimensional vector, is called broadcasting in numpy. I took the name from there,
	 * because that is essentially what the bi- and trivariate overloadings of this method do.
	 */
	protected ArrayList<Double> applyArithmeticOperationBroadcast(Function<Double, Double> func, List<Double> op) {
		if (areValuesDeterministic(op))
			return applyArithmeticOperation(func, op.get(0));
		else {
			int s = op.size();
			ArrayList<Double> result = new ArrayList<>(op);

			IntStream intstr = IntStream.range(0, s);
			intstr.parallel().forEach(i -> {
				result.set(i, func.apply(op.get(i)));
			});

			return result;
		}
	}

	protected ArrayList<Double> applyArithmeticOperationBroadcast(BiFunction<Double, Double, Double> func, List<Double> op1, List<Double> op2) {
		boolean det1 = areValuesDeterministic(op1), det2 = areValuesDeterministic(op2);
		if (!(det1 ^ det2))
			return applyArithmeticOperation(func, op1, op2);
		else if (det1 && !det2)
			return applyArithmeticOperation(func, op1.get(0), op2);
		else // if (!det1 && det2)
			return applyArithmeticOperation(func, op1, op2.get(0));
	}

	protected ArrayList<Double> applyArithmeticOperationBroadcast(TriFunction<Double, Double, Double, Double> func, List<Double> op1, List<Double> op2, List<Double> op3) {
		boolean det1 = areValuesDeterministic(op1), det2 = areValuesDeterministic(op2), det3 = areValuesDeterministic(op3);
		if ((det1 && det2 && det3) || (!det1 && !det2 && !det3))
			return applyArithmeticOperation(func, op1, op2, op3);
		else if (!det1 && !det2 && det3)
			return applyArithmeticOperation(func, op1, op2, op3.get(0));
		else if (!det1 && det2 && !det3)
			return applyArithmeticOperation(func, op1, op2.get(0), op3);
		else if (det1 && !det2 && !det3)
			return applyArithmeticOperation(func, op1.get(0), op2, op3);
		else if (det1 && det2 && !det3)
			return applyArithmeticOperation(func, op1.get(0), op2.get(0), op3);
		else if (det1 && !det2 && det3)
			return applyArithmeticOperation(func, op1.get(0), op2, op3.get(0));
		else // if (!det1 && det2 && det3)
			return applyArithmeticOperation(func, op1, op2.get(0), op3.get(0));
	}

	@Override
	public RandomVariableFactory getFactory() {
		if (mFactory == null)
			mFactory = factory();
		return mFactory;
	}


	public static RandomVariableFactory factory() {
		return new RandomVariableFactory();
	}

	protected RandomVariable mExpectation = null;
	protected RandomVariable mVariance = null;
	protected RandomVariable mStandardError = null;

	private double calculateExpectation() {
		if (mExpectation == null)
			mExpectation = getFactory().fromConstant(values.parallelStream().mapToDouble(a -> a).sum() / values.size())
					.addDependencies(this).setOperation(Operation.EXPECT);
		return mExpectation.asFloatingPoint();
	}

	private double calculateVariance() {
		if (mVariance == null)
			mVariance = this.sub(this.expectation()).squared().expectation();
		return mVariance.asFloatingPoint();
	}

	private double calculateStandardError() {
		if (mStandardError == null)
			mStandardError = variance().sqrt().div(this.values.size());
		return mStandardError.asFloatingPoint();
	}


	@Override
	public RandomVariable expectation() {
		calculateExpectation();
		return mExpectation;
	}

	@Override
	public RandomVariable variance() {
		calculateVariance();
		return mVariance;
	}

	@Override
	public RandomVariable sampleError() {
		calculateStandardError();
		return mStandardError;
	}

	@Override
	public RandomVariable squared() {
		return doReturn( new RandomVariable(applyArithmeticOperationBroadcast(a -> a * a, values)).addDependencies(this).setOperation(Operation.SQR)
				.qualifyDifferentiability(this) );
	}

	private RandomVariable mSqrt = null;

	@Override
	public RandomVariable sqrt() {
		if (mSqrt == null)
			mSqrt = doReturn( new RandomVariable(applyArithmeticOperationBroadcast(a -> Math.sqrt(a), values)).addDependencies(this).setOperation(Operation.SQRT)
					.qualifyDifferentiability(this) );
		return mSqrt;
	}

	private RandomVariable mExp = null;

	@Override
	public RandomVariable exp() {
		if (mExp == null)
			mExp = doReturn( new RandomVariable(applyArithmeticOperationBroadcast(a -> Math.exp(a), values)).addDependencies(this).setOperation(Operation.EXP)
					.qualifyDifferentiability(this) );
		return mExp;

	}

	private RandomVariable mLog = null;

	@Override
	public RandomVariable log() {
		if (mLog == null)
			mLog = doReturn(  new RandomVariable(applyArithmeticOperationBroadcast(a -> a >  0 ? Math.log(a) :
		                                                                                a == 0 ? Double.NEGATIVE_INFINITY
		                                                                                       : Double.NaN,
		                                                                           values))
		                      .addDependencies(this).setOperation(Operation.LOG)
		                      .qualifyDifferentiability(this) );
		return mLog;
	}

	@Override
	public RandomVariable add(double x) {
		RandomVariable constant = getFactory().fromConstant(x);
		return add(constant);
	}

	@Override
	public RandomVariable add(RandomValue x) {
		return doReturn( new RandomVariable(applyArithmeticOperationBroadcast( (a,b) -> a + b, values, ((RandomVariable)x).values))
				.addDependencies(this, (RandomVariable)x).setOperation(Operation.ADD)
				.qualifyDifferentiability(this, (RandomVariable)x) );
	}

	@Override
	public RandomVariable sub(RandomValue x) {
		return doReturn( new RandomVariable(applyArithmeticOperationBroadcast( (a,b) -> a - b, values, ((RandomVariable)x).values))
				.addDependencies(this, (RandomVariable)x).setOperation(Operation.SUB)
				.qualifyDifferentiability(this, (RandomVariable)x) );
	}

	@Override
	public RandomVariable mult(double x) {
		RandomVariable constant = getFactory().fromConstant(x);
		return mult(constant);
	}

	@Override
	public RandomVariable mult(RandomValue x) {
		return doReturn( new RandomVariable(applyArithmeticOperationBroadcast( (a,b) -> a * b, values, ((RandomVariable)x).values))
				.addDependencies(this, (RandomVariable)x).setOperation(Operation.MUL)
				.qualifyDifferentiability(this, (RandomVariable)x) );
	}

	@Override
	public RandomVariable div(RandomValue x) {
		return doReturn( new RandomVariable(applyArithmeticOperationBroadcast( (a,b) -> a / b, values, ((RandomVariable)x).values))
				.addDependencies(this, (RandomVariable)x).setOperation(Operation.DIV)
				.qualifyDifferentiability(this, (RandomVariable)x) );
	}

	/**
	 * Same as this.div(this.getFactory().fromConstant(x))
	 * @param x: divisor
	 * @return New RandomVariable object holding the result.
	 */
	public RandomVariable div(double x) {
		RandomVariable constant = getFactory().fromConstant(x);
		return div(constant);
	}

	/**
	 * Determines and returns the call spread for a .choose() operation. The size of the call spread will be choosen as
	 * RandomValue.hFactor * sd, where sd is the (biased) standard deviation of this.values. Does not fix the determined value.
	 * @return The determined parameter h.
	 */
	protected double getH() {
		return Math.sqrt(this.variance().values.get(0)) * hFactor;
	}

	/**
	 * Get the indicator function with call spread of size h.
	 * @param h
	 * @return indicator function
	 */
	protected static TriFunction<Double, Double, Double, Double> getIndicatorFunction(double h) {
		return ((x, y, z) -> {
			double ratio = x/h;
			return (ratio <= -1) ? z :
			       (ratio >  1)  ? y
			                     : ratio * (y - z) + .5 * (z + y);
		});
	}

	@Override
	public RandomVariable choose(RandomValue valueIfNonNegative, RandomValue valueIfNegative) {

		if (valueIfNonNegative instanceof RandomVariable && valueIfNegative instanceof RandomVariable) {

			double h = getH();

			if (debugMode) {
				// Output how many sample points are within the call spread
				long number = values.parallelStream().filter(o -> (Math.abs(o) <= h)).count();
				writeDebug("samples within call spread: " + Long.toString(number));
			}

			return doReturn( new RandomVariable(applyArithmeticOperationBroadcast(getIndicatorFunction(h), values,
				                                                                  ((RandomVariable)valueIfNonNegative).values,
				                                                                  ((RandomVariable)valueIfNegative).values))
				             .addDependencies(this, (RandomVariable)valueIfNonNegative, (RandomVariable)valueIfNegative)
				             .setOperation(Operation.CHO)
				             .setH(h)
				             .qualifyDifferentiability(this, (RandomVariable)valueIfNonNegative, (RandomVariable)valueIfNegative) );


		} else {
			System.out.println("Error: Arguments must be instanceof RandomVariable.");
			System.exit(1);
		}
		return null;
	}

	/**
	 * Apply a custom function.
	 * @param func A function of one Double argument with Double return value
	 * @return A RandomVariable instance holding the result, not implementing RandomVariableDifferentiable.
	 */
	public RandomVariable customOperation(Function<Double, Double> func) {
		return doReturn( new RandomVariable( this.applyArithmeticOperationBroadcast(func, this.values) )
				         .removeDifferentiability() );
	}

	/**
	 * Apply a custom differentiable function.
	 * @param func A function of one Double argument with Double return value
	 * @param derivative That function's analytic derivative.
	 * @return A RandomVariable instance holding the result.
	 */
	public RandomVariable customOperation(Function<Double, Double> func, Function<Double, Double> derivative) {
		return doReturn( new RandomVariable( this.applyArithmeticOperationBroadcast(func, this.values) )
				         .setCustomFunctionDerivative(derivative)
				         .addDependencies(this).setOperation(Operation.CUSTOMUNI).qualifyDifferentiability(this) );
	}

	/**
	 * Apply a custom bivariate function.
	 * @param func A function of two Double arguments with Double return value
	 * @param Y The second argument to the function
	 * @return A RandomVariable instance holding the result, not implementing RandomVariableDifferentiable.
	 */
	public RandomVariable customOperation(BiFunction<Double, Double, Double> func, RandomVariable Y) {
		return doReturn( new RandomVariable( this.applyArithmeticOperationBroadcast(func, this.values, Y.values) )
				         .removeDifferentiability() );
	}

	/**
	 * Apply a custom bivariate function.
	 * @param func A function of two Double arguments with Double return value
	 * @param derivativeX That function's analytic partial derivative with respect to the first argument.
	 * @param derivativeY That function's analytic partial derivative with respect to the second argument.
	 * @param Y The second argument to the function
	 * @return A RandomVariable instance holding the result.
	 */
	public RandomVariable customOperation(BiFunction<Double, Double, Double> func, RandomVariable Y, BiFunction<Double, Double, Double> derivativeX,
			BiFunction<Double, Double, Double> derivativeY) {
		return doReturn( new RandomVariable( this.applyArithmeticOperationBroadcast(func, this.values, Y.values) )
				         .setCustomBiFunctionDerivatives(derivativeX, derivativeY)
				         .addDependencies(this, Y).setOperation(Operation.CUSTOMBI).qualifyDifferentiability(this, Y) );
	}

	/**
	 * Apply a custom trivariate function.
	 * @param func A function of three Double arguments with Double return value
	 * @param Y The second argument to the function
	 * @param Z The third argument to the function
	 * @return A RandomVariable instance holding the result, not implementing RandomVariableDifferentiable.
	 */
	public RandomVariable customOperation(TriFunction<Double, Double, Double, Double> func, RandomVariable Y, RandomVariable Z) {
		return doReturn( new RandomVariable( this.applyArithmeticOperationBroadcast(func, this.values, Y.values, Z.values) )
				         .removeDifferentiability() );
	}

	/**
	 * Apply a custom trivariate function.
	 * @param func A function of three Double arguments with Double return value
	 * @param derivativeX That function's analytic partial derivative with respect to the first argument.
	 * @param derivativeY That function's analytic partial derivative with respect to the second argument.
	 * @param derivativeZ That function's analytic partial derivative with respect to the third argument.
	 * @param Y The second argument to the function
	 * @param Z The third argument to the function
	 * @return A RandomVariable instance holding the result.
	 */
	public RandomVariable customOperation(TriFunction<Double, Double, Double, Double> func, RandomVariable Y, RandomVariable Z,
			TriFunction<Double, Double, Double, Double> derivativeX, TriFunction<Double, Double, Double, Double> derivativeY,
			TriFunction<Double, Double, Double, Double> derivativeZ) {
		return doReturn( new RandomVariable( this.applyArithmeticOperationBroadcast(func, this.values, Y.values, Z.values) )
				         .setCustomTriFunctionDerivatives(derivativeX, derivativeY, derivativeZ)
				         .addDependencies(this, Y, Z).setOperation(Operation.CUSTOMTRI).qualifyDifferentiability(this, Y) );
	}


}

