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

import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.IActionDelegate;

import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Interface to be implemented by a Test View Support to be returned by
 * org.org.eclipse.unittest.unittestViewSupport extension.
 */
public interface ITestViewSupport {

	/**
	 * Returns a Test Runner Client for this this Test View Support
	 *
	 * @param session the test session
	 *
	 * @return returns a Test Runner Client
	 */
	ITestRunnerClient newTestRunnerClient(ITestRunSession session);

	/**
	 * Returns a filter patterns array for a stack trace or an error message
	 *
	 * @return a filter patterns array
	 */
	String[] getFilterPatterns();

	/**
	 * Returns an Open Test action for a specified test case element
	 *
	 * @param testRunnerPart a test runner view part instance
	 * @param testCase       a test case element
	 * @return an action implementing org.eclipse.unittest.ui.IOpenEditorAction
	 *         interface if an action can be created, otherwise - null
	 */
	IAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestCaseElement testCase);

	/**
	 * Returns an Open Test action for a specified test suite element
	 *
	 * @param testRunnerPart a test runner view part instance
	 * @param testSuite      a test suite element
	 * @return an action implementing org.eclipse.unittest.ui.IOpenEditorAction
	 *         interface if an action can be created, otherwise - null
	 */
	IAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite);

	/**
	 * Returns an Open Editor action for a specified failed test element
	 *
	 * @param testRunnerPart a test runner view part instance
	 * @param failure        a test element that is failed
	 * @param traceLine      a stack trace or an error message text
	 * @return an action implementing org.eclipse.unittest.ui.IOpenEditorAction
	 *         interface if an action can be created, otherwise - null
	 */
	IAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, ITestElement failure, String traceLine);

	/**
	 * Returns an action delegate to copy an existing stack trace/error message into
	 * a console view
	 *
	 * @param view a test runner view Failure Trace view instance
	 * @return an action delegate if it can be created, otherwise - null
	 */
	IActionDelegate createShowStackTraceInConsoleViewActionDelegate(FailureTraceUIBlock view);

	/**
	 * Returns an array of Rerun test actions for a specified Test Suite element
	 *
	 * @param testElement a test suite element
	 *
	 * @return a {@link ILaunchConfiguration}, derived from current test session and
	 *         selected element.
	 */
	ILaunchConfiguration getRerunLaunchConfiguration(ITestElement testElement);

	/**
	 * Returns a Test View Support display name
	 *
	 * @return returns a display name
	 */
	String getDisplayName();
}
