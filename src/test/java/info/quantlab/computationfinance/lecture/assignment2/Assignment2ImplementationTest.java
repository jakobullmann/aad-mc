package info.quantlab.computationfinance.lecture.assignment2;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import info.quantlab.computationfinance.lecture.Assignment2Checker;

public class Assignment2ImplementationTest {

	@Test
	public void test1() {
		Assignment2Checker.Result result = (new Assignment2Checker()).check(new Assignment2Implementation(), 1);
		System.out.println(result.message);
		if(!result.success) fail(result.message);
	}

	@Test
	public void test2() {
		Assignment2Checker.Result result = (new Assignment2Checker()).check(new Assignment2Implementation(), 2);
		System.out.println(result.message);
		if(!result.success) fail(result.message);
	}

	@Test
	public void test3() {
		Assignment2Checker.Result result = (new Assignment2Checker()).check(new Assignment2Implementation(), 3);
		System.out.println(result.message);
		if(!result.success) fail(result.message);
	}

	@Test
	void test4() {
		Assignment2Checker.Result result = (new Assignment2Checker()).check(new Assignment2Implementation(), 4);
		System.out.println(result.message);
		if(!result.success) fail(result.message);
	}

	@Test
	void test5() {
		Assignment2Checker.Result result = (new Assignment2Checker()).check(new Assignment2Implementation(), 5);
		System.out.println(result.message);
		if(!result.success) fail(result.message);
	}
}
