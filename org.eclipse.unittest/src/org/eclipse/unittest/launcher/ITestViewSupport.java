package org.eclipse.unittest.launcher;

import org.eclipse.unittest.internal.model.TestCaseElement;
import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.model.TestSuiteElement;
import org.eclipse.unittest.ui.IOpenEditorAction;
import org.eclipse.unittest.ui.TestRunnerViewPart;

public interface ITestViewSupport {
	ITestViewSupport NULL= new ITestViewSupport() {

		@Override
		public String[] getFilterPatterns() {
			return null;
		}

		@Override
		public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, TestCaseElement testCase) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, TestSuiteElement testSuite) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IOpenEditorAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, TestElement failure, String traceLine) {
			// TODO Auto-generated method stub
			return null;
		}

	};

	String[] getFilterPatterns();
	IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, TestCaseElement testCase);
	IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, TestSuiteElement testSuite);
	IOpenEditorAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, TestElement failure, String traceLine);

}
