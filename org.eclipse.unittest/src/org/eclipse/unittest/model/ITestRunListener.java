/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.unittest.model;

import java.time.Duration;

import org.eclipse.unittest.internal.model.ITestSessionListener;

/**
 * An interface to listen to the events from the
 * {@link org.eclipse.unittest.launcher.ITestRunnerClient} and translates them
 * into high-level model events (broadcasted to {@link ITestSessionListener}s).
 */
public interface ITestRunListener {

	/**
	 * Status constant indicating that a test passed (constant value 0).
	 */
	int STATUS_OK = 0;
	/**
	 * Status constant indicating that a test had an error an unanticipated
	 * exception (constant value 1).
	 */
	int STATUS_ERROR = 1;
	/**
	 * Status constant indicating that a test failed an assertion (constant value
	 * 2).
	 */
	int STATUS_FAILURE = 2;

	/**
	 * A test run has started.
	 *
	 * @param testCount the number of individual tests that will be run
	 */
	void testRunStarted(int testCount);

	/**
	 * A test run has ended.
	 *
	 * @param duration the total elapsed time of the test run
	 */
	void testRunEnded(Duration duration);

	/**
	 * A test run has been stopped prematurely.
	 *
	 * @param duration the time elapsed before the test run was stopped
	 */
	void testRunStopped(Duration duration);

	/**
	 * An individual test has started.
	 *
	 * @param testId   a unique Id identifying the test
	 * @param testName the name of the test that started
	 */
	void testStarted(String testId, String testName);

	/**
	 * An individual test has ended.
	 *
	 * @param testId    a unique Id identifying the test
	 * @param testName  the name of the test that ended
	 * @param isIgnored indicates that test case was ignored
	 */
	void testEnded(String testId, String testName, boolean isIgnored);

	/**
	 * The VM instance performing the tests has terminated.
	 */
	void testRunTerminated();

	/**
	 * Information about a member of the test suite that is about to be run.
	 *
	 * @param testId         a unique id for the test
	 * @param testName       the name of the test
	 * @param isSuite        true or false depending on whether the test is a suite
	 * @param testCount      an integer indicating the number of tests
	 * @param isDynamicTest  true or false
	 * @param parentId       the unique testId of its parent if it is a dynamic
	 *                       test, otherwise can be "-1"
	 * @param displayName    the display name of the test
	 * @param parameterTypes comma-separated list of method parameter types if
	 *                       applicable, otherwise an empty string
	 * @param uniqueId       the unique ID of the test provided, otherwise an empty
	 *                       string
	 */

	void testTreeEntry(String testId, String testName, boolean isSuite, int testCount, boolean isDynamicTest,
			String parentId, String displayName, String[] parameterTypes, String uniqueId);

	/**
	 * An individual test has failed with a stack trace.
	 *
	 * @param status             the outcome of the test; one of
	 *                           {@link #STATUS_ERROR STATUS_ERROR} or
	 *                           {@link #STATUS_FAILURE STATUS_FAILURE}
	 * @param testId             a unique Id identifying the test
	 * @param testName           the name of the test that failed
	 * @param isAssumptionFailed indicates that an assumption is failed
	 * @param trace              the stack trace
	 * @param expected           the expected value
	 * @param actual             the actual value
	 */
	void testFailed(int status, String testId, String testName, boolean isAssumptionFailed, String trace,
			String expected, String actual);

	/**
	 * An individual test has been rerun.
	 *
	 * @param testId    a unique Id identifying the test
	 * @param testClass the name of the test class that was rerun
	 * @param testName  the name of the test that was rerun
	 * @param status    the outcome of the test that was rerun; one of
	 *                  {@link #STATUS_OK}, {@link #STATUS_ERROR}, or
	 *                  {@link #STATUS_FAILURE}
	 * @param trace     the stack trace in the case of abnormal termination, or the
	 *                  empty string if none
	 * @param expected  the expected value in case of abnormal termination, or the
	 *                  empty string if none
	 * @param actual    the actual value in case of abnormal termination, or the
	 *                  empty string if none
	 */
	void testReran(String testId, String testClass, String testName, int status, String trace, String expected,
			String actual);

}
