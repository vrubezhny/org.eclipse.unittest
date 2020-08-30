package org.eclipse.unittest.launcher;

import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.IOpenEditorAction;
import org.eclipse.unittest.ui.TestRunnerViewPart;

public interface ITestViewSupport {
	ITestViewSupport NULL= new ITestViewSupport() {

		@Override
		public String[] getFilterPatterns() {
			return null;
		}

		@Override
		public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestCaseElement testCase) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IOpenEditorAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, ITestElement failure, String traceLine) {
			// TODO Auto-generated method stub
			return null;
		}

	};

	String[] getFilterPatterns();
	IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestCaseElement testCase);
	IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite);
	IOpenEditorAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, ITestElement failure, String traceLine);

}
