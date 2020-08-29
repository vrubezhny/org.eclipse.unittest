package org.eclipse.some.tests;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SomeTests extends TestCase {

	public SomeTests() {
		super("Some Tests");
	}

	/**
	 * <p>
	 * Use this method to add these tests to a larger test suite so set up and tear down can be
	 * performed
	 * </p>
	 * 
	 * @return a {@link TestSetup} that will run all of the tests in this class
	 *         with set up and tear down.
	 */
	public static Test suite() {
		TestSuite ts = new TestSuite(SomeTests.class, "Some Tests");
		return new SomeTestsSetup(ts);
	}

	public void test1() {
		assertTrue("test1 success", true);
	}

	public void test2() {
		assertTrue("test2 fail", false);
	}

	public void test3() {
		assertTrue("test3 success", true);
	}

	/**
	 * <p>
	 * This inner class is used to do set up and tear down before and after (respectively) all tests
	 * in the inclosing class have run.
	 * </p>
	 */
	private static class SomeTestsSetup extends TestSetup {
		/**
		 * Default constructor
		 * 
		 * @param test
		 *            do setup for the given test
		 */
		public SomeTestsSetup(Test test) {
			super(test);
		}

		/**
		 * <p>
		 * This is run once before all of the tests
		 * </p>
		 * 
		 * @see junit.extensions.TestSetup#setUp()
		 */
		public void setUp() throws Exception {
			// Set Up operations
		}

		/**
		 * <p>
		 * This is run once after all of the tests have been run
		 * </p>
		 * 
		 * @see junit.extensions.TestSetup#tearDown()
		 */
		public void tearDown() throws Exception {
			// Tear Down operations
		}
	}
}
