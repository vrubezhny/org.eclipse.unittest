/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.unittest.ui;

import java.util.Collection;
import java.util.List;

import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;

import org.eclipse.core.text.StringMatcher;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.IViewPart;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;

/**
 * Interface to be implemented by a Test View Support to be returned by
 * org.org.eclipse.unittest.unittestViewSupport extension.
 */
public interface ITestViewSupport {

	/**
	 * Activates UnitTestBundle. Eclipse uses lazy bundle loading by default, which
	 * means a bundle will not be loaded in many cases until some of its class is
	 * used. This method allows the clients to instantiate the Unit Test bundle in
	 * order to make it setup its launch listeners that are used to create and
	 * activate Unit Test View. The Unit Test client bundles must call this method
	 * before a Unit Test launch is created (preferably right before creation of the
	 * launch in order to not make Eclipse to load the Unit Test bundle when it is
	 * not really required), To load the Unit Test bundle this the clients, for
	 * example, might call this method inside
	 * {@link ILaunchConfigurationDelegate2#getLaunch(ILaunchConfiguration, String)}
	 * method of their launch configuration implementation.
	 */
	static void activateBundle() {
		// Nothing more to do
	}

	/**
	 * Returns a Test Runner Client.
	 *
	 * @param session the test session. ⚠️ The session may not be fully initialized
	 *                at that point, however {@link ITestRunSession#getLaunch()} is
	 *                supposed to return the proper launch.
	 *
	 * @return returns a Test Runner Client
	 */
	ITestRunnerClient newTestRunnerClient(ITestRunSession session);

	/**
	 * Returns filter patterns to exclude lines from stack trace or an error message
	 *
	 * @return filter patterns, matching lines will be hidden in the UI
	 */
	Collection<StringMatcher> getTraceExclusionFilterPatterns();

	/**
	 * Returns an action to open a specified test elements
	 *
	 * @param testRunnerPart a test runner view part instance
	 * @param testCase       a test case element
	 * @return an action to open a specified test case element, or <code>null</code>
	 */
	IAction getOpenTestAction(IViewPart testRunnerPart, ITestCaseElement testCase);

	/**
	 * Returns an action to open a specified test suite element
	 *
	 * @param testRunnerPart a test runner view part instance
	 * @param testSuite      a test suite element
	 * @return an action to open a specified test suite element, or
	 *         <code>null</code>
	 */
	IAction getOpenTestAction(IViewPart testRunnerPart, ITestSuiteElement testSuite);

	/**
	 * Returns an action to open a failure trace element
	 *
	 * @param testRunnerPart a test runner view part instance
	 * @param failure        a test element that is failed
	 * @param traceLine      a stack trace or an error message text
	 * @return an action to open a failure trace element, or <code>null</null>
	 */
	IAction createOpenEditorAction(IViewPart testRunnerPart, ITestElement failure, String traceLine);

	/**
	 * Returns an action to copy an existing stack trace/error message into a
	 * console view
	 *
	 * @param failedTest the failed test
	 * @return an {@link Runnable} if it can be created, otherwise -
	 *         <code>null</code>
	 */
	Runnable createShowStackTraceInConsoleViewActionDelegate(ITestElement failedTest);

	/**
	 * Returns a Rerun launch configuration for the given element
	 *
	 * @param testElements the tests to rerun
	 * @return a {@link ILaunchConfiguration}, derived from current test session and
	 *         selected element.
	 */
	ILaunchConfiguration getRerunLaunchConfiguration(List<ITestElement> testElements);

	/**
	 * Returns a Test View Support display name
	 *
	 * @return returns a display name
	 */
	String getDisplayName();
}
