/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.unittest.model.ITestElement.Status;

/**
 * A listener interface for observing the execution of a test session (initial
 * run and reruns).
 */
public interface ITestSessionListener {
	/**
	 * A test run has started.
	 */
	void sessionStarted();

	/**
	 * A test run has ended.
	 *
	 * @param elapsedTime the total elapsed time of the test run
	 */
	void sessionEnded(long elapsedTime);

	/**
	 * A test run has been stopped prematurely.
	 *
	 * @param elapsedTime the time elapsed before the test run was stopped
	 */
	void sessionStopped(long elapsedTime);

	/**
	 * The VM instance performing the tests has terminated.
	 */
	void sessionTerminated();

	/**
	 * A test has been added to the plan.
	 *
	 * @param testElement the test
	 */
	void testAdded(ITestElement testElement);

	/**
	 * All test have been added and running begins
	 */
	void runningBegins();

	/**
	 * An individual test has started.
	 *
	 * @param testCaseElement the test
	 */
	void testStarted(ITestCaseElement testCaseElement);

	/**
	 * An individual test has ended.
	 *
	 * @param testCaseElement the test
	 */
	void testEnded(ITestCaseElement testCaseElement);

	/**
	 * An individual test has failed with a stack trace.
	 *
	 * @param testElement the test
	 * @param status      the outcome of the test; one of
	 *                    {@link ITestElement.Status#ERROR} or
	 *                    {@link ITestElement.Status#FAILURE}
	 * @param trace       the stack trace
	 * @param expected    expected value
	 * @param actual      actual value
	 */
	void testFailed(ITestElement testElement, Status status, String trace, String expected, String actual);

	/**
	 * An individual test has been rerun.
	 *
	 * @param testCaseElement the test
	 * @param status          the outcome of the test that was rerun; one of
	 *                        {@link ITestElement.Status#OK},
	 *                        {@link ITestElement.Status#ERROR}, or
	 *                        {@link ITestElement.Status#FAILURE}
	 * @param trace           the stack trace in the case of abnormal termination,
	 *                        or the empty string if none
	 * @param expectedResult  expected value
	 * @param actualResult    actual value
	 */
	void testReran(ITestCaseElement testCaseElement, Status status, String trace, String expectedResult,
			String actualResult);

	/**
	 * @return <code>true</code> if the test run session can be swapped to disk
	 *         although this listener is still installed
	 */
	boolean acceptsSwapToDisk();

}
