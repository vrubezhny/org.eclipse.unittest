package org.eclipse.unittest.launcher;

import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.FailureTrace;
import org.eclipse.unittest.ui.IOpenEditorAction;
import org.eclipse.unittest.ui.RerunAction;
import org.eclipse.unittest.ui.TestRunnerViewPart;

import org.eclipse.ui.IActionDelegate;

public interface ITestViewSupport {
	ITestViewSupport NULL = new ITestViewSupport() {

		@Override
		public String[] getFilterPatterns() {
			return null;
		}

		@Override
		public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestCaseElement testCase) {
			return null;
		}

		@Override
		public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite) {
			return null;
		}

		@Override
		public IOpenEditorAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, ITestElement failure,
				String traceLine) {
			return null;
		}

		@Override
		public IActionDelegate createShowStackTraceInConsoleViewActionDelegate(FailureTrace view) {
			return null;
		}

		@Override
		public RerunAction[] getRerunActions(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite) {
			return null;
		}
	};

	String[] getFilterPatterns();

	IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestCaseElement testCase);

	IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite);

	IOpenEditorAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, ITestElement failure, String traceLine);

	IActionDelegate createShowStackTraceInConsoleViewActionDelegate(FailureTrace view);

	RerunAction[] getRerunActions(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite);
}
