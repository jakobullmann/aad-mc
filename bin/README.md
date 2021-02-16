# Computational Finance Lecture - Algorithmic Differentiation for Random Variable and Interest Rate Models -

This repository contains the code and stub code for the second assignment of the lecture *Applied Mathematical Finance* / *Computational Finance 2* (Winter 2020/2021).

## Assignment 2: Algorithmic Differentiation for Random Variable and Interest Rate Models

**Note**: The interface mentioned below are in a separate project, which is referenced automatically via Maven. You can find its implementation at

https://github.com/qntlb/computational-finance-lecture/blob/master/src/main/java/net/finmath/aadexperiments/value/ConvertableToFloatingPoint.java
https://github.com/qntlb/computational-finance-lecture/blob/master/src/main/java/net/finmath/aadexperiments/randomvalue/RandomValue.java
https://github.com/qntlb/computational-finance-lecture/blob/master/src/main/java/net/finmath/aadexperiments/randomvalue/RandomValueDifferentiable.java
https://github.com/qntlb/computational-finance-lecture/blob/master/src/main/java/net/finmath/aadexperiments/randomvalue/RandomValueFactory.java
https://github.com/qntlb/computational-finance-lecture/blob/master/src/main/java/net/finmath/aadexperiments/randomvalue/RandomValueDifferentiableFactory.java

You do not need to import this project.

The two factories allow to create the neutral element of add and mult (zero and one) and allow to create constants. You may implement the factory via an anonymous inner class. Hence you have to write to classes: one for `RandomValue` one for `RandomValueDifferentiable`.

### 1. Implement RandomValue

Your task is to create a class that implements the interfaces `RandomValue` and `ConvertableToFloatingPoint` that allows for basic arithmetic operations on sample vectors representing random variables.

The operator expectation should be implemented such that it results in a
random variable y where each sample path of y = x.expectation() has the value 1/n sum( x[i] ).

The operator variance should be implemented such that it results in a
random variable y where each sample path of y = x.variance() has the value 1/n sum( (x[i]-m)^2 ),
where m denote the expectation.

The operator sampleError should be implemented such that it results in a
random variable y where each sample path of y = x.sampleError() has the value sqrt(x.variance)/sqrt(n),
where n denote the number of sample paths.

The operator choice should be implemented such that it represents an approximation of the Dirac delta,
such that x.choose(a,b).expectation() leads to *an approximation* (see the lecture). A simple finite difference approximation is sufficient, no need for a regression.

All other operators are path wise operators.

### 2. Implement RandomValueDifferentiable

Implement an algorithmic differentiation for `RandomValue`s.

### 3. Implement the Monte-Carlo Valuation Interest Rate Options under the Black-Scholes Model

- Implement the valuation of a Digitial Caplet - this requires the use of the choice method.

- Implement the valuation of a Forward Rate in Arrears.

- Implement the calculation of the delta, that is the derivative of the valuation V(0) with respect to the initial value L(0) using your AAD implementation.

### Final Result

Implement the methods of the `info.quantlab.computationfinance.lecture.Assignment2`. This allows us to provide some test of your solutions. Our test will be not inside this repo, such that we can add them later.

### Tips

For the exercise 3 there are analytic formulas. Use these to write unit test.

### Remark

Don't panic. Please ask us if you do not know where to start or get stuck.
