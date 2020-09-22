package org.eclipse.unittest.cdt.launcher;

import org.eclipse.cdt.testsrunner.internal.launcher.ITestsLaunchConfigurationConstants;
import org.eclipse.unittest.cdt.CDTUnitTestPlugin;
import org.eclipse.unittest.cdt.ui.OpenEditorAtLineAction;
import org.eclipse.unittest.cdt.ui.OpenTestAction;
import org.eclipse.unittest.launcher.ITestViewSupport;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.FailureTraceUIBlock;
import org.eclipse.unittest.ui.IOpenEditorAction;
import org.eclipse.unittest.ui.TestRunnerViewPart;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IActionDelegate;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

public class CDTTestViewSupport implements ITestViewSupport {

	@Override
	public String[] getFilterPatterns() {
		return new String[0];
//		return JUnitPreferencesConstants.parseList(Platform.getPreferencesService().getString(
//				JUnitCorePlugin.CORE_PLUGIN_ID, JUnitPreferencesConstants.PREF_ACTIVE_FILTERS_LIST, null, null));
	}

	@Override
	public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestCaseElement testCase) {
		return new OpenTestAction(testRunnerPart, testCase.getParent(), testCase);
	}

	@Override
	public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite) {
		return new OpenTestAction(testRunnerPart, testSuite);
	}

	@Override
	public IOpenEditorAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, ITestElement failure, String traceLine) {
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
		String qualifiedName = testSuite.getClassName();
		ILaunchConfiguration origin = testSuite.getTestRunSession().getLaunch().getLaunchConfiguration();
		ILaunchConfigurationWorkingCopy res;
		try {
			res= origin.copy(origin.getName() + "\uD83D\uDD03" + testSuite.getTestName()); //$NON-NLS-1$
			res.setAttribute(ITestsLaunchConfigurationConstants.ATTR_TESTS_FILTER, qualifiedName);
			return res;
		} catch (CoreException e) {
			CDTUnitTestPlugin.log(e);
			return null;
		}
	}

	@Override
	public String getDisplayName() {
		return "C/C++ Unit";
	}
}
