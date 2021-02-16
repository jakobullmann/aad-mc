/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.11.2020
 */
package net.finmath.aadexperiments.value;

public class ValueDouble implements Value, ConvertableToFloatingPoint {

	private Double value;

	public ValueDouble(Double value) {
		super();
		this.value = value;
	}

	@Override
	public Value squared() {
		return new ValueDouble(value * value);
	}

	@Override
	public Value sqrt() {
		return new ValueDouble(Math.sqrt(value));
	}

	@Override
	public Value exp() {
		return new ValueDouble(Math.exp(value));
	}

	@Override
	public Value log() {
		return new ValueDouble(Math.log(value));
	}

	@Override
	public Value add(double x) {
		return this.add(new ValueDouble(x));
	}

	@Override
	public Value add(Value x) {
		return new ValueDouble(this.value + valueOf(x));
	}

	@Override
	public Value sub(Value x) {
		return new ValueDouble(value - valueOf(x));
	}

	@Override
	public Value mult(double x) {
		return this.mult(new ValueDouble(x));
	}

	@Override
	public Value mult(Value x) {
		return new ValueDouble(value * valueOf(x));
	}

	@Override
	public Value div(Value x) {
		return new ValueDouble(value / valueOf(x));
	}

	@Override
	public Value choose(Value valueIfNonNegative, Value valueIfNegative) {
		return new ValueDouble(value >= 0 ? valueOf(valueIfNonNegative) : valueOf(valueIfNegative));
	}

	@Override
	public Double asFloatingPoint() {
		return value;
	}

	@Override
	public String toString() {
		return value.toString();
	}
	
	private static double valueOf(Value x) {
		return ((ConvertableToFloatingPoint)x).asFloatingPoint();
	}
}
