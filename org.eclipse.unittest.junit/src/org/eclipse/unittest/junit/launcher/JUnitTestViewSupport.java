package org.eclipse.unittest.junit.launcher;

import org.eclipse.unittest.internal.model.TestCaseElement;
import org.eclipse.unittest.junit.ui.OpenEditorAtLineAction;
import org.eclipse.unittest.junit.ui.OpenTestAction;
import org.eclipse.unittest.junit.ui.ShowStackTraceInConsoleViewActionDelegate;
import org.eclipse.unittest.launcher.ITestViewSupport;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.FailureTrace;
import org.eclipse.unittest.ui.IOpenEditorAction;
import org.eclipse.unittest.ui.TestRunnerViewPart;

import org.eclipse.core.runtime.Platform;

import org.eclipse.ui.IActionDelegate;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitPreferencesConstants;

public class JUnitTestViewSupport implements ITestViewSupport {

	@Override
	public String[] getFilterPatterns() {
		return JUnitPreferencesConstants.parseList(Platform.getPreferencesService().getString(
				JUnitCorePlugin.CORE_PLUGIN_ID, JUnitPreferencesConstants.PREF_ACTIVE_FILTERS_LIST, null, null));

	}

	@Override
	public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestCaseElement testCase) {
		return new OpenTestAction(testRunnerPart, testCase, testCase.getParameterTypes());
	}

	@Override
	public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite) {
		String testName = testSuite.getSuiteTypeName();
		ITestElement[] children = testSuite.getChildren();

		if (testName.startsWith("[") && testName.endsWith("]") && children.length > 0 //$NON-NLS-1$ //$NON-NLS-2$
				&& children[0] instanceof TestCaseElement) {
			// a group of parameterized tests
			return new OpenTestAction(testRunnerPart, (TestCaseElement) children[0], null);
		}

		int index = testName.indexOf('(');
		// test factory method
		if (index > 0) {
			return new OpenTestAction(testRunnerPart, testSuite.getSuiteTypeName(), testName.substring(0, index),
					testSuite.getParameterTypes(), true);
		}

		// regular test class
		return new OpenTestAction(testRunnerPart, testName);

	}

	@Override
	public IOpenEditorAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, ITestElement failure,
			String traceLine) {
		try {
			String testName = traceLine;
			int indexOfFramePrefix = testName.indexOf(FailureTrace.FRAME_PREFIX);
			if (indexOfFramePrefix == -1) {
				return null;
			}
			testName = testName.substring(indexOfFramePrefix);
			testName = testName.substring(FailureTrace.FRAME_PREFIX.length(), testName.lastIndexOf('(')).trim();
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
			return new OpenEditorAtLineAction(testRunnerPart, testName, line);
		} catch (NumberFormatException | IndexOutOfBoundsException e) {
		}
		return null;
	}

	@Override
	public IActionDelegate createShowStackTraceInConsoleViewActionDelegate(FailureTrace view) {
		return new ShowStackTraceInConsoleViewActionDelegate(view);
	}

}
