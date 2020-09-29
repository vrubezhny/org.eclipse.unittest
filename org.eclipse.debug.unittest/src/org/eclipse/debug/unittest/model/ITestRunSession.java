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
package org.eclipse.debug.unittest.model;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.unittest.launcher.ITestRunnerClient;
import org.eclipse.debug.unittest.ui.ITestViewSupport;

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
	 * Returns the name of the test run. The name is the name of the launch
	 * configuration use to run this test.
	 *
	 * @return returns the test run name
	 */
	String getTestRunName();

	/**
	 * Returns the Java project from which this test run session has been launched,
	 * or <code>null</code> if not available.
	 *
	 * @return the launched project, or <code>null</code> is not available.
	 */
	ILaunch getLaunch();

	/**
	 * Returns the Test Runner View Support for which this test run session has been
	 * launched, or <code>null</code> if not available.
	 *
	 * @return the test runner view support, or <code>null</code> is not available.
	 */
	ITestViewSupport getTestViewSupport();

	/**
	 * Returns the Test Runner Client to be used to gather the test results, or
	 * <code>null</code> if not available.
	 *
	 * @return the test runner client instance, or <code>null</code> is not
	 *         available.
	 */
	ITestRunnerClient getTestRunnerClient();

	/**
	 * Creates a test element, either {@link ITestSuiteElement} or
	 * {@link ITestCaseElement} instance, depending on the arguments.
	 *
	 * @param parent         a parent test suite element
	 * @param id             an identifier of the test element
	 * @param testName       a name of the test element
	 * @param isSuite        a flag indicating if the test element should be
	 *                       represented by a test suite element
	 * @param testCount      a number of predefined test cases in case of test suite
	 *                       element
	 * @param isDynamicTest  a flag indicating that test suite is dynamic (that
	 *                       doesn't have predefined tests)
	 * @param displayName    a display name for the test element
	 * @param parameterTypes an array of parameter types, or <code>null</code>
	 * @param uniqueId       a unique identifier for the test element or
	 *                       <code>null</code>
	 * @return a created {@link ITestSuiteElement} or {@link ITestCaseElement}
	 *         instance
	 */
	ITestElement createTestElement(ITestSuiteElement parent, String id, String testName, boolean isSuite, int testCount,
			boolean isDynamicTest, String displayName, String[] parameterTypes, String uniqueId);

	/**
	 * Adds an {@link ITestSessionListener} to the test run session
	 *
	 * @param listener an {@link ITestSessionListener} instance
	 */
	void addTestSessionListener(ITestSessionListener listener);

	/**
	 * Removes an {@link ITestSessionListener} from the test run session
	 *
	 * @param listener an {@link ITestSessionListener} instance
	 */
	void removeTestSessionListener(ITestSessionListener listener);

	/**
	 * Returns the root test element of this test run session
	 *
	 * @return a root test element
	 */
	ITestRoot getTestRoot();

	/**
	 * Returns a test element by its identifier
	 *
	 * @param id a test element identifier
	 * @return a {@link ITestElement} found or <code>null</code>
	 */
	ITestElement getTestElement(String id);

	/**
	 * Returns an array of all failed {@link ITestElement}s
	 *
	 * @return an array of failed {@link ITestElement}s
	 */
	ITestElement[] getAllFailedTestElements();

	/**
	 * Returns the number of started test case elements
	 *
	 * @return a number of started test cases
	 */
	int getStartedCount();

	/**
	 * Returns the number of failed test case elements
	 *
	 * @return a number of failed test cases
	 */
	int getFailureCount();

	/**
	 * Returns the number of assumption failures
	 *
	 * @return a number of assumption failures
	 */
	int getAssumptionFailureCount();

	/**
	 * Returns the number of ignored test case elements
	 *
	 * @return a number of ignored test cases
	 */
	int getIgnoredCount();

	/**
	 * Returns the total number of test case elements
	 *
	 * @return a total number of test cases
	 */
	int getTotalCount();

	/**
	 * Returns the number of test case elements with errors
	 *
	 * @return a number of test cases with errors
	 */
	int getErrorCount();

	/**
	 * Indicates if the test run session is starting
	 *
	 * @return <code>true</code> in case of the test session is starting, otherwise
	 *         returns <code>false</code>
	 */
	boolean isStarting();

	/**
	 * Indicates if the test run session is running
	 *
	 * @return <code>true</code> in case of the test session is running, otherwise
	 *         returns <code>false</code>
	 */
	boolean isRunning();

	/**
	 * Indicates if the test run session has been kept alive
	 *
	 * @return <code>true</code> in case of the test session has been kept alive,
	 *         otherwise returns <code>false</code>
	 */
	boolean isKeptAlive();

	/**
	 * Indicates if the test run session has been stopped or terminated
	 *
	 * @return <code>true</code> if the session has been stopped or terminated,
	 *         otherwise returns <code>false</code>
	 */
	boolean isStopped();

	/**
	 * Stops the test run
	 */
	void stopTestRun();

	/**
	 * Swaps in the test run session info
	 */
	void swapIn();

	/**
	 * Swaps out the test run session info
	 */
	void swapOut();

}
