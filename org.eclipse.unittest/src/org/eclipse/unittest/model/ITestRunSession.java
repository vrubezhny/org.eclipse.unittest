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
	 * @param testId     a unique Id identifying the test
	 * @param className  the name of the test class that was rerun
	 * @param testName   the name of the test that was rerun
	 * @param statusCode the outcome of the test that was rerun; one of
	 *                   {@link ITestRunListener#STATUS_OK},
	 *                   {@link ITestRunListener#STATUS_ERROR}, or
	 *                   {@link ITestRunListener#STATUS_FAILURE}
	 * @param trace      the stack trace in the case of abnormal termination, or the
	 *                   empty string if none
	 * @param expected   the expected value in case of abnormal termination, or the
	 *                   empty string if none
	 * @param actual     the actual value in case of abnormal termination, or the
	 *                   empty string if none
	 */
	void notifyTestReran(String testId, String className, String testName, int statusCode, String trace,
			String expected, String actual);

	/**
	 * Notifies on a member of the test suite that is about to be run.
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
	void notifyTestTreeEntry(String testId, String testName, boolean isSuite, int testCount, boolean isDynamicTest,
			String parentId, String displayName, String[] parameterTypes, String uniqueId);

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
	 * @param testId    a unique Id identifying the test
	 * @param testName  the name of the test that failed
	 * @param isIgnored <code>true</code> indicates that the specified test was
	 *                  ignored, otherwise - <code>false</code>
	 */
	void notifyTestEnded(String testId, String testName, boolean isIgnored);

	/**
	 * Notifies on an individual test started.
	 *
	 * @param testId   a unique Id identifying the test
	 * @param testName the name of the test that started
	 */
	void notifyTestStarted(final String testId, final String testName);

	/**
	 * Notifies on a test run started.
	 *
	 * @param count the number of individual tests that will be run
	 */
	void notifyTestRunStarted(final int count);

	/**
	 * Notifies on an individual test failed with a stack trace.
	 *
	 * @param status             the outcome of the test; one of
	 *                           {@link ITestRunListener#STATUS_ERROR STATUS_ERROR}
	 *                           or {@link ITestRunListener#STATUS_FAILURE
	 *                           STATUS_FAILURE}
	 * @param testId             a unique Id identifying the test
	 * @param testName           the name of the test that failed
	 * @param isAssumptionFailed indicates that an assumption is failed
	 * @param trace              the stack trace
	 * @param expected           the expected value
	 * @param actual             the actual value
	 */
	void notifyTestFailed(int status, String testId, String testName, boolean isAssumptionFailed, String trace,
			String expected, String actual);

	/**
	 * Notifies on a test run terminated.
	 */
	void notifyTestRunTerminated();
}
