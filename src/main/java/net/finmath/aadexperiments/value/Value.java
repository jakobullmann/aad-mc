/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.11.2020
 */
package net.finmath.aadexperiments.value;

public interface Value {

	/**
	 * Applies x*x to this object x.
	 *
	 * @return New object representing the result.
	 */
	Value squared();

	/**
	 * Applies sqrt(x) to this object x.
	 *
	 * @return New object representing the result.
	 */
	Value sqrt();

	/**
	 * Applies exp(x) to this object x.
	 *
	 * @return New object representing the result.
	 */
	Value exp();

	/**
	 * Applies log(x) to this object x.
	 *
	 * @return New object representing the result.
	 */
	Value log();

	/**
	 * Applies a+x to this object a.
	 *
	 * @param x The value x - the value to add.
	 * @return New object representing the result.
	 */
	Value add(double x);

	/**
	 * Applies a+x to this object a.
	 *
	 * @param x The value x - the value to add.
	 * @return New object representing the result.
	 */
	Value add(Value x);

	/**
	 * Applies a-x to this object a.
	 *
	 * @param x The value x - the value to substract.
	 * @return New object representing the result.
	 */
	Value sub(Value x);

	/**
	 * Applies a*x to this object a.
	 *
	 * @param x The value x - the factor of this multiplication.
	 * @return New object representing the result.
	 */
	Value mult(double x);

	/**
	 * Applies a*x to this object a.
	 *
	 * @param x The value x - the factor of this multiplication.
	 * @return New object representing the result.
	 */
	Value mult(Value x);

	/**
	 * Applies a/x to this object a.
	 *
	 * @param x The value x - the denominator of the division.
	 * @return New object representing the result.
	 */
	Value div(Value x);

	/**
	 * Applies trigger >= 0 ? valueIfNonNegative : valueIfNegative to this object, where trigger = this object.
	 * 
	 * @param valueIfNonNegative The value if this value is positive or zero.
	 * @param valueIfNegative The vlaue is this value is negative.
	 * @return New object representing the result.
	 */
	Value choose(Value valueIfNonNegative, Value valueIfNegative);
}