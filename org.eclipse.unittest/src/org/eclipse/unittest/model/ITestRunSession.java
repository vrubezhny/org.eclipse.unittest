/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.debug.core.ILaunch;

/**
 * Represents a test run session.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ITestRunSession extends ITestElementContainer {

	/**
	 * Returns the Java project from which this test run session has been launched,
	 * or <code>null</code> if not available.
	 *
	 * @return the launched project, or <code>null</code> is not available.
	 */
	ILaunch getLaunch();

	/**
	 * Returns a test element by its identifier
	 *
	 * @param id a test element identifier
	 * @return a {@link ITestElement} found or <code>null</code>
	 */
	ITestElement getTestElement(String id);

	/**
	 * Notifies on an individual test re-run.
	 *
	 * @param testId       a unique Id identifying the test
	 * @param className    the name of the test class that was rerun
	 * @param testName     the name of the test that was rerun
	 * @param status       the outcome of the test that was rerun; one of
	 *                     {@link org.eclipse.unittest.model.ITestElement.Result#OK},
	 *                     {@link org.eclipse.unittest.model.ITestElement.Result#ERROR},
	 *                     or
	 *                     {@link org.eclipse.unittest.model.ITestElement.Result#FAILURE}
	 * @param failureTrace the failure trace
	 */
	void notifyTestReran(String testId, String className, String testName, Result status, FailureTrace failureTrace);

	/**
	 * Notifies on a member of the test suite that is about to be run.
	 *
	 * @param testId        a unique id for the test
	 * @param testName      the name of the test
	 * @param isSuite       true or false depending on whether the test is a suite
	 * @param testCount     an integer indicating the number of tests
	 * @param isDynamicTest true or false
	 * @param parentId      the unique testId of its parent if it is a dynamic test,
	 *                      otherwise can be "-1"
	 * @param displayName   the display name of the test
	 * @param uniqueId      the unique ID of the test provided, otherwise an empty
	 *                      string
	 */
	void notifyTestTreeEntry(String testId, String testName, boolean isSuite, int testCount, boolean isDynamicTest,
			String parentId, String displayName, String uniqueId);

	/**
	 * Notifies on a test run stopped.
	 *
	 * @param duration the total elapsed time of the test run
	 */
	void notifyTestRunStopped(final Duration duration);

	/**
	 * Notifies on a test run ended.
	 *
	 * @param duration the total elapsed time of the test run
	 */
	void notifyTestRunEnded(final Duration duration);

	/**
	 * Notifies on an individual test ended.
	 *
	 * @param test      a unique Id identifying the test
	 * @param isIgnored <code>true</code> indicates that the specified test was
	 *                  ignored, otherwise - <code>false</code>
	 */
	void notifyTestEnded(ITestElement test, boolean isIgnored);

	/**
	 * Notifies on an individual test started.
	 *
	 * @param testId   a unique Id identifying the test
	 * @param testName the name of the test that started
	 * @return a test element
	 */
	ITestElement notifyTestStarted(final String testId, final String testName);

	/**
	 * Notifies on a test run started.
	 *
	 * @param count the number of individual tests that will be run
	 */
	void notifyTestRunStarted(final int count);

	/**
	 * Notifies on an individual test failed with a stack trace.
	 *
	 * @param test               the test
	 * @param status             the outcome of the test; one of
	 *                           {@link org.eclipse.unittest.model.ITestElement.Result#ERROR}
	 *                           or
	 *                           {@link org.eclipse.unittest.model.ITestElement.Result#FAILURE}.
	 *                           An exception is thrown otherwise
	 * @param isAssumptionFailed indicates that an assumption is failed
	 * @param failureTrace       The failure trace
	 * @throws IllegalArgumentException if status doesn't indicate ERROR or FAILURE.
	 */
	void notifyTestFailed(ITestElement test, Result status, boolean isAssumptionFailed, FailureTrace failureTrace)
			throws IllegalArgumentException;

	/**
	 * Notifies on a test run terminated.
	 */
	void notifyTestRunTerminated();
}
