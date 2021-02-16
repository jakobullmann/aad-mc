package net.finmath.aadexperiments.value;

public interface ConvertableToValueArray {

	/**
	 * Returns the floating point array value of this object
	 *
	 * @return Floating point array value represented by this object
	 */
	Value[] asValueArray();

}
