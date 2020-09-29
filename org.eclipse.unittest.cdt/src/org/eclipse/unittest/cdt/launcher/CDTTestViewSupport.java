/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.cdt.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.testsrunner.internal.launcher.ITestsLaunchConfigurationConstants;
import org.eclipse.unittest.cdt.CDTUnitTestPlugin;
import org.eclipse.unittest.cdt.ui.OpenEditorAtLineAction;
import org.eclipse.unittest.cdt.ui.OpenTestAction;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRoot;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.FailureTraceUIBlock;
import org.eclipse.unittest.ui.ITestViewSupport;
import org.eclipse.unittest.ui.TestRunnerViewPart;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.IActionDelegate;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

public class CDTTestViewSupport implements ITestViewSupport {
	/**
	 * The delimiter between parts of serialized test path. Should not be met in
	 * test paths names.
	 */
	private static final String TEST_PATH_PART_DELIMITER = "\n"; //$NON-NLS-1$

	@Override
	public String[] getFilterPatterns() {
		return new String[0];
//		return JUnitPreferencesConstants.parseList(Platform.getPreferencesService().getString(
//				JUnitCorePlugin.CORE_PLUGIN_ID, JUnitPreferencesConstants.PREF_ACTIVE_FILTERS_LIST, null, null));
	}

	@Override
	public IAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestCaseElement testCase) {
		return new OpenTestAction(testRunnerPart, testCase.getParent(), testCase);
	}

	@Override
	public IAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite) {
		return new OpenTestAction(testRunnerPart, testSuite);
	}

	@Override
	public IAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, ITestElement failure, String traceLine) {
		try {
			String testName= traceLine;
			int indexOfFramePrefix= testName.indexOf(FailureTraceUIBlock.FRAME_PREFIX);
			if (indexOfFramePrefix == -1) {
				return null;
			}
			testName= testName.substring(indexOfFramePrefix);
			testName= testName.substring(FailureTraceUIBlock.FRAME_PREFIX.length(), testName.lastIndexOf(':')).trim();

			String lineNumber= traceLine;
			lineNumber= lineNumber.substring(lineNumber.indexOf(':') + 1);
			int line= Integer.valueOf(lineNumber).intValue();
			return new OpenEditorAtLineAction(testRunnerPart, testName, line);
		} catch(NumberFormatException e) {
			CDTUnitTestPlugin.log(e);
		}
		catch(IndexOutOfBoundsException e) {
			CDTUnitTestPlugin.log(e);
		}
		return null;
	}

	@Override
	public IActionDelegate createShowStackTraceInConsoleViewActionDelegate(FailureTraceUIBlock view) {
		return null;
	}

	@Override
	public ILaunchConfiguration getRerunLaunchConfiguration(ITestElement testSuite) {
		ILaunchConfiguration origin = testSuite.getTestRunSession().getLaunch().getLaunchConfiguration();
		ILaunchConfigurationWorkingCopy res;
		try {
			res= origin.copy(origin.getName() + "\uD83D\uDD03" + testSuite.getTestName()); //$NON-NLS-1$
			List<String> testsFilterAttr = Arrays.asList(packTestPaths(testSuite));
			res.setAttribute(ITestsLaunchConfigurationConstants.ATTR_TESTS_FILTER, testsFilterAttr);
			return res;
		} catch (CoreException e) {
			CDTUnitTestPlugin.log(e);
			return null;
		}
	}

	/**
	 * Pack the paths to specified test items to string list.
	 * @param testElement test element to pack
	 *
	 * @return string list
	 */
	private static String[] packTestPaths(ITestElement testElement) {
		String[] result = new String[1];
		List<String> testPath = new ArrayList<>();

		// Collect test path parts (in reverse order)
		testPath.clear();
		ITestElement element = testElement;
		while (element != null && !(element.getParent() instanceof ITestRoot)) {
			// Exclude root test suite
			if (element.getParent() != null) {
				testPath.add(element.getTestName());
			}
			element = element.getParent();
		}
		// Join path parts into the only string
		StringBuilder sb = new StringBuilder();
		boolean needDelimiter = false;
		for (int pathPartIdx = testPath.size() - 1; pathPartIdx >= 0; pathPartIdx--) {
			if (needDelimiter) {
				sb.append(TEST_PATH_PART_DELIMITER);
			} else {
				needDelimiter = true;
			}
			sb.append(testPath.get(pathPartIdx));
		}
		result[0] = sb.toString();
		return result;
	}

	@Override
	public String getDisplayName() {
		return "C/C++ Unit"; //$NON-NLS-1$
	}

	@Override
	public ITestRunnerClient newTestRunnerClient(ITestRunSession session) {
		return new CDTTestRunnerClient(session);
	}
}
