/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.junit.launcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.unittest.junit.JUnitTestPlugin;
import org.eclipse.unittest.junit.JUnitTestPlugin.JUnitVersion;
import org.eclipse.unittest.junit.ui.OpenEditorAtLineAction;
import org.eclipse.unittest.junit.ui.OpenTestAction;
import org.eclipse.unittest.junit.ui.ShowStackTraceInConsoleViewActionDelegate;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.ITestViewSupport;

import org.eclipse.core.text.StringMatcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.IViewPart;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitPreferencesConstants;
import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

@SuppressWarnings("restriction")
public class JUnitTestViewSupport implements ITestViewSupport {

	public static final String FRAME_LINE_PREFIX = "at "; //$NON-NLS-1$

	@Override
	public Collection<StringMatcher> getTraceExclusionFilterPatterns() {
		return Arrays
				.stream(JUnitPreferencesConstants.parseList(Platform.getPreferencesService().getString(
						JUnitCorePlugin.CORE_PLUGIN_ID, JUnitPreferencesConstants.PREF_ACTIVE_FILTERS_LIST, "", null))) //$NON-NLS-1$
				.filter(Predicate.not(String::isBlank)) //
				.map(pattern -> new StringMatcher(pattern, true, false)) //
				.collect(Collectors.toList());
	}

	@Override
	public IAction getOpenTestAction(IViewPart testRunnerPart, ITestCaseElement testCase) {
		return new OpenTestAction(testRunnerPart, testCase, getParameterTypes(testCase));
	}

	@Override
	public IAction getOpenTestAction(IViewPart testRunnerPart, ITestSuiteElement testSuite) {
		String testName = testSuite.getTestName();
		List<? extends ITestElement> children = testSuite.getChildren();
		if (testName.startsWith("[") && testName.endsWith("]") && !children.isEmpty() //$NON-NLS-1$ //$NON-NLS-2$
				&& children.get(0) instanceof ITestCaseElement) {
			// a group of parameterized tests
			return new OpenTestAction(testRunnerPart, (ITestCaseElement) children.get(0), null);
		}

		int index = testName.indexOf('(');
		// test factory method
		if (index > 0) {
			return new OpenTestAction(testRunnerPart, testSuite.getTestName(), testName.substring(0, index),
					getParameterTypes(testSuite), true, testSuite.getTestRunSession());
		}

		// regular test class
		return new OpenTestAction(testRunnerPart, testName, testSuite.getTestRunSession());

	}

	@Override
	public IAction createOpenEditorAction(IViewPart testRunnerPart, ITestElement failure, String traceLine) {
		try {
			String testName = traceLine;
			int indexOfFramePrefix = testName.indexOf(FRAME_LINE_PREFIX);
			if (indexOfFramePrefix == -1) {
				return null;
			}
			testName = testName.substring(indexOfFramePrefix);
			testName = testName.substring(FRAME_LINE_PREFIX.length(), testName.lastIndexOf('(')).trim();
			int indexOfModuleSeparator = testName.lastIndexOf('/');
			if (indexOfModuleSeparator != -1) {
				testName = testName.substring(indexOfModuleSeparator + 1);
			}
			testName = testName.substring(0, testName.lastIndexOf('.'));
			int innerSeparatorIndex = testName.indexOf('$');
			if (innerSeparatorIndex != -1)
				testName = testName.substring(0, innerSeparatorIndex);

			String lineNumber = traceLine;
			lineNumber = lineNumber.substring(lineNumber.indexOf(':') + 1, lineNumber.lastIndexOf(')'));
			int line = Integer.valueOf(lineNumber).intValue();
			return new OpenEditorAtLineAction(testRunnerPart, testName, line, failure.getTestRunSession());
		} catch (NumberFormatException | IndexOutOfBoundsException e) {
		}
		return null;
	}

	@Override
	public Runnable createShowStackTraceInConsoleViewActionDelegate(ITestElement failedTest) {
		return new ShowStackTraceInConsoleViewActionDelegate(failedTest);
	}

	@Override
	public ILaunchConfiguration getRerunLaunchConfiguration(List<ITestElement> tests) {
		if (tests == null || tests.size() != 1) {
			return null;
		}
		ITestElement testSuite = tests.get(0);
		String testMethodName = null; // test method name is null when re-running a regular test class
		String testName = testSuite.getTestName();

		ILaunchConfiguration launchConfiguration = testSuite.getTestRunSession().getLaunch().getLaunchConfiguration();
		ITestKind junitKind;
		try {
			junitKind = JUnitVersion
					.fromJUnitTestKindId(launchConfiguration
							.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, "")) //$NON-NLS-1$
					.getJUnitTestKind();
		} catch (CoreException e) {
			JUnitTestPlugin.log(e);
			return null;
		}

		IJavaProject project = JUnitLaunchConfigurationConstants
				.getJavaProject(testSuite.getTestRunSession().getLaunch().getLaunchConfiguration());
		if (project == null) {
			return null;
		}

		String qualifiedName = null;
		IType testType = findTestClass(testSuite, junitKind.getFinder(), project, true);
		if (testType != null) {
			qualifiedName = testType.getFullyQualifiedName();

			if (!qualifiedName.equals(testName)) {
				int index = testName.indexOf('(');
				if (index > 0) { // test factory method
					testMethodName = testName.substring(0, index);
				}
			}
			String[] parameterTypes = getParameterTypes(testSuite);
			if (testMethodName != null && parameterTypes != null) {
				String paramTypesStr = Arrays.stream(parameterTypes).collect(Collectors.joining(",")); //$NON-NLS-1$
				testMethodName = testMethodName + "(" + paramTypesStr + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			// see bug 443498
			testType = findTestClass(testSuite.getParent(), junitKind.getFinder(), project, false);
			if (testType != null && testSuite instanceof ITestSuiteElement) {
				qualifiedName = testType.getFullyQualifiedName();

				String className = getClassName(testSuite);
				if (!qualifiedName.equals(className)) {
					testMethodName = testName;
				}
			}
		}

		ILaunchConfigurationWorkingCopy res;
		try {
			res = launchConfiguration.copy(launchConfiguration.getName() + " - rerun"); //$NON-NLS-1$
			res.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME, testMethodName);
			return res;
		} catch (CoreException e) {
			JUnitTestPlugin.log(e);
			return null;
		}

	}

	/*
	 * Returns the element's test class or the next container's test class, which
	 * exists, and for which ITestFinder.isTest() is true.
	 */
	private IType findTestClass(ITestElement element, ITestFinder finder, IJavaProject project,
			boolean checkOnlyCurrentElement) {
		ITestElement current = element;
		while (current != null) {
			try {
				String className = null;
				if (current instanceof ITestRunSession) {
					ILaunch launch = element.getTestRunSession().getLaunch();
					if (launch != null) {
						ILaunchConfiguration configuration = launch.getLaunchConfiguration();
						if (configuration != null) {
							className = configuration
									.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String) null);
						}
					}
				} else {
					className = getClassName(current);
				}

				if (className != null) {
					IType type = project.findType(className);
					if (type != null && finder.isTest(type)) {
						return type;
					} else if (checkOnlyCurrentElement) {
						return null;
					}
				}
			} catch (JavaModelException e) {
				// fall through
			} catch (CoreException e) {
				// fall through
			}
			current = current.getParent();
		}
		return null;
	}

	@Override
	public String getDisplayName() {
		return "JUnit"; //$NON-NLS-1$
	}

	@Override
	public ITestRunnerClient newTestRunnerClient(ITestRunSession session) {
		String portAsString = session.getLaunch().getAttribute(JUnitLaunchConfigurationDelegate.ATTR_PORT);
		return new JUnitRemoteTestRunnerClient(portAsString != null ? Integer.parseInt(portAsString) : -1, session);
	}

	/**
	 * Returns the parameter types specified for this test element
	 *
	 * @param test test
	 * @return a parameter type array
	 */
	private String[] getParameterTypes(ITestElement test) {
		String testName = test.getData();
		if (testName != null) {
			int index = testName.lastIndexOf("method:"); //$NON-NLS-1$
			index = testName.indexOf('(', index);
			if (index > 0) {
				int closeIndex = testName.indexOf(')', index);
				if (closeIndex > 0) {
					String params = testName.substring(index + 1, closeIndex);
					return params.split(","); //$NON-NLS-1$
				}
			}
		}
		return null;
	}

	/**
	 * Returns the type/class of the test element
	 *
	 * @param test test
	 * @return return the type/class name
	 */
	public static String getClassName(ITestElement test) {
		return extractClassName(test.getTestName());
	}

	private static String extractClassName(String testNameString) {
		testNameString = extractRawClassName(testNameString);
		testNameString = testNameString.replace('$', '.'); // see bug 178503
		return testNameString;
	}

	/**
	 * Extracts and returns a raw class name from a test element name
	 *
	 * @param testNameString a test element name
	 *
	 * @return an extracted raw class name
	 */
	public static String extractRawClassName(String testNameString) {
		if (testNameString.startsWith("[") && testNameString.endsWith("]")) { //$NON-NLS-1$ //$NON-NLS-2$
			// a group of parameterized tests, see
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102512
			return testNameString;
		}
		int index = testNameString.lastIndexOf('(');
		if (index < 0)
			return testNameString;
		int end = testNameString.lastIndexOf(')');
		testNameString = testNameString.substring(index + 1, end > index ? end : testNameString.length());
		return testNameString;
	}

	public static String getTestMethodName(ITestElement test) {
		String testName = test.getTestName();
		int index = testName.lastIndexOf('(');
		if (index > 0)
			return testName.substring(0, index);
		index = testName.indexOf('@');
		if (index > 0)
			return testName.substring(0, index);
		return testName;
	}

}
