package org.eclipse.unittest.cdt.launcher;

import org.eclipse.unittest.cdt.ui.OpenInEditorAction;
import org.eclipse.unittest.internal.model.TestCaseElement;
import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.model.TestSuiteElement;
import org.eclipse.unittest.launcher.ITestViewSupport;
import org.eclipse.unittest.ui.FailureTrace;
import org.eclipse.unittest.ui.IOpenEditorAction;
import org.eclipse.unittest.ui.TestRunnerViewPart;

public class CDTTestViewSupport implements ITestViewSupport {

	@Override
	public String[] getFilterPatterns() {
		return new String[0];
//		return JUnitPreferencesConstants.parseList(Platform.getPreferencesService().getString(
//				JUnitCorePlugin.CORE_PLUGIN_ID, JUnitPreferencesConstants.PREF_ACTIVE_FILTERS_LIST, null, null));
	}

	@Override
	public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, TestCaseElement testCase) {
		return null;
//		return new OpenTestAction(testRunnerPart, testCase, testCase.getParameterTypes());
	}

	@Override
	public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, TestSuiteElement testSuite) {
		return null;
/*
		String testName= testSuite.getTestName();
		ITestElement[] children= testSuite.getChildren();

		if (testName.startsWith("[") && testName.endsWith("]") && children.length > 0 && children[0] instanceof TestCaseElement) { //$NON-NLS-1$ //$NON-NLS-2$
			// a group of parameterized tests
//			return new OpenTestAction(testRunnerPart, (TestCaseElement) children[0], null);
			return null;
		}

		int index= testName.indexOf('(');
		// test factory method
		if (index > 0) {
//			return new OpenTestAction(testRunnerPart, testSuite.getSuiteTypeName(), testName.substring(0, index), testSuite.getParameterTypes(), true);
			return null;
		}

		// regular test class
//		return new OpenTestAction(testRunnerPart, testName);
 */
	}

	@Override
	public IOpenEditorAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, TestElement failure, String traceLine) {
		try {
			String testName= traceLine;
			int indexOfFramePrefix= testName.indexOf(FailureTrace.FRAME_PREFIX);
			if (indexOfFramePrefix == -1) {
				return null;
			}
			testName= testName.substring(indexOfFramePrefix);
			testName= testName.substring(FailureTrace.FRAME_PREFIX.length(), testName.lastIndexOf(':')).trim();

			String lineNumber= traceLine;
			lineNumber= lineNumber.substring(lineNumber.indexOf(':') + 1);
			int line= Integer.valueOf(lineNumber).intValue();
			return new OpenInEditorAction(testRunnerPart, testName, line);
		} catch(NumberFormatException e) {
		}
		catch(IndexOutOfBoundsException e) {
		}
		return null;
	}

}
