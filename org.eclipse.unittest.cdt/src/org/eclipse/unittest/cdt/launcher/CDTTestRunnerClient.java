package org.eclipse.unittest.cdt.launcher;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Stack;

import org.eclipse.cdt.testsrunner.internal.model.TestCase;
import org.eclipse.cdt.testsrunner.internal.model.TestSuite;
import org.eclipse.cdt.testsrunner.launcher.ITestsRunnerProvider;
import org.eclipse.cdt.testsrunner.model.ITestCase;
import org.eclipse.cdt.testsrunner.model.ITestItem;
import org.eclipse.cdt.testsrunner.model.ITestItem.Status;
import org.eclipse.cdt.testsrunner.model.ITestMessage.Level;
import org.eclipse.cdt.testsrunner.model.ITestModelUpdater;
import org.eclipse.cdt.testsrunner.model.ITestSuite;
import org.eclipse.cdt.testsrunner.model.TestingException;
import org.eclipse.unittest.cdt.CDTUnitTestPlugin;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.debug.unittest.launcher.TestRunnerClient;
import org.eclipse.debug.unittest.model.ITestCaseElement;
import org.eclipse.debug.unittest.model.ITestElement;
import org.eclipse.debug.unittest.model.ITestRunSession;
import org.eclipse.debug.unittest.model.ITestSuiteElement;

public class CDTTestRunnerClient extends TestRunnerClient {
	private static final String FRAME_PREFIX = org.eclipse.debug.unittest.ui.FailureTraceUIBlock.FRAME_PREFIX;

	class TestModelUpdaterAdapter implements ITestModelUpdater {

		class TestElementReference {
			String parentId;
			String id;
			String name;
			boolean isSuite;
			private long startTime;
			private long testingTime;

			public TestElementReference(String parentId, String id, String name, boolean isSuite) {
				this.parentId = parentId;
				this.id = id;
				this.name = name;
				this.isSuite = isSuite;
				this.startTime = System.currentTimeMillis();
			}

			@Override
			public String toString() {
				return new StringBuilder().append("TestElementReference: ") //$NON-NLS-1$
						.append("parentId = ").append(parentId).append("\r\n") //$NON-NLS-1$ //$NON-NLS-2$
						.append("id = ").append(id).append("\r\n") //$NON-NLS-1$ //$NON-NLS-2$
						.append("name = ").append(name).append("\r\n") //$NON-NLS-1$ //$NON-NLS-2$
						.append("isSuite = ").append(isSuite).append("\r\n") //$NON-NLS-1$ //$NON-NLS-2$
						.append("startTime = ").append(startTime).append("\r\n") //$NON-NLS-1$ //$NON-NLS-2$
						.append("testingTime = ").append(testingTime).append("\r\n") //$NON-NLS-1$ //$NON-NLS-2$
						.toString();
			}
		}

		Stack<TestElementReference> testElementRefs = new Stack<>();

		String fCurrentTestCase;
		String fCurrentTestSuite;
		int fTestId  = 0;

		@Override
		public void enterTestSuite(String name) {
			if (fDebug) {
				System.out.println("TestModelUpdaterAdapter.enterTestSuite: name = " + name); //$NON-NLS-1$
			}
			TestElementReference pRef = testElementRefs.isEmpty() ? null : testElementRefs.peek();

			TestElementReference cRef = new TestElementReference(
					pRef == null ? String.valueOf("-1") : pRef.id, //$NON-NLS-1$
					String.valueOf(fTestId++),
					name,
					true);
			testElementRefs.push(cRef);

			this.fCurrentTestSuite = cRef.id;

			notifyTestTreeEntry(cRef.id, cRef.name, cRef.isSuite, 0, true, cRef.parentId,
					cRef.name, null, null);
		}

		@Override
		public void exitTestSuite() {
			if (fDebug) {
				System.out.println("TestModelUpdaterAdapter.exitTestSuite"); //$NON-NLS-1$
			}
			TestElementReference cRef = testElementRefs.pop();
			while (cRef != null && !cRef.isSuite) {
				logUnexpectedTest(cRef.id, cRef);
				cRef = testElementRefs.pop();
			}
		}

		@Override
		public void enterTestCase(String name) {
			if (fDebug) {
				System.out.println("TestModelUpdaterAdapter.enterTestCase: name = " + name); //$NON-NLS-1$
			}

			TestElementReference pRef = testElementRefs.isEmpty() ? null : testElementRefs.peek();

			String parentId = String.valueOf("-1"); //$NON-NLS-1$
			if (pRef != null) {
				parentId = pRef.isSuite ? pRef.id : pRef.parentId;
			}

			TestElementReference cRef = new TestElementReference(
					parentId,
					String.valueOf(fTestId++),
					name,
					false);
			testElementRefs.push(cRef);

			this.fCurrentTestCase = cRef.id;
			fFailedTrace.setLength(0);
			fExpectedResult.setLength(0);
			fActualResult.setLength(0);

			notifyTestTreeEntry(cRef.id, cRef.name, cRef.isSuite, 0, true, cRef.parentId,
					cRef.name, null, null);

			notifyTestStarted(cRef.id, cRef.name);
		}

		@Override
		public void setTestStatus(Status status) {
			if (fDebug) {
				System.out.println("TestModelUpdaterAdapter.setTestStatus: status = " + status.toString()); //$NON-NLS-1$
			}

			if (status.isError()) {
				TestElementReference cRef = testElementRefs.isEmpty() ? null : testElementRefs.peek();
				if (cRef != null) {
					extractFailure(cRef.id, cRef.name,
							status == Status.Aborted ? ITestElement.Status.FAILURE.getOldCode() :
								ITestElement.Status.ERROR.getOldCode(),
								false);

					notifyTestFailed();

				} else {
					logUnexpectedTest(fCurrentTestCase, null);
				}
			}
		}

		@Override
		public void setTestingTime(int testingTime) {
			if (fDebug) {
				System.out.println("TestModelUpdaterAdapter.setTestingTime: testingTime = " + testingTime); //$NON-NLS-1$
			}

			TestElementReference cRef = testElementRefs.isEmpty() ? null : testElementRefs.peek();
			if (cRef != null) {
				cRef.testingTime =testingTime;
			} else {
				logUnexpectedTest(fCurrentTestCase, null);
			}
		}

		@Override
		public void exitTestCase() {
			if (fDebug) {
				System.out.println("TestModelUpdaterAdapter.exitTestCase"); //$NON-NLS-1$
			}

			TestElementReference cRef = testElementRefs.isEmpty() ? null : testElementRefs.peek();

			if (cRef != null && !cRef.isSuite) {
				testElementRefs.pop(); // Renove test case ref

				notifyTestEnded(cRef.id, cRef.name, false);
			} else {
				logUnexpectedTest(cRef == null ? "null" : cRef.id, cRef); //$NON-NLS-1$
			}

		}

		@Override
		public void addTestMessage(String file, int line, Level level, String text) {
			if (fDebug) {
				System.out.println("TestModelUpdaterAdapter.addTestMessage: file = " + file + //$NON-NLS-1$
					", line = " + line + //$NON-NLS-1$
					", level = " + level.toString() + //$NON-NLS-1$
					", text = " + text); //$NON-NLS-1$
			}

			fFailedTrace.append(level.toString()).append(": ").append(text).append("\r\n") //$NON-NLS-1$ //$NON-NLS-2$
				.append(FRAME_PREFIX).append(file).append(':').append(line).append("\r\n"); //$NON-NLS-1$
		}

		@Override
		public ITestSuite currentTestSuite() {
			if (fDebug) {
				System.out.println("TestModelUpdaterAdapter.currentTestSuite"); //$NON-NLS-1$
			}

			ITestElement testElement = fTestRunSession.getTestElement(fCurrentTestSuite);
			if (testElement instanceof ITestSuiteElement) {
				return convertFromTestSuiteElement((ITestSuiteElement)testElement);
			} else {
				return convertFromTestSuiteElement(testElement.getParent());
			}
		}

		@Override
		public ITestCase currentTestCase() {
			if (fDebug) {
				System.out.println("TestModelUpdaterAdapter.currentTestCase"); //$NON-NLS-1$
			}

			ITestElement testElement = fTestRunSession.getTestElement(fCurrentTestCase);
			if (testElement instanceof ITestCaseElement) {
				return convertFromTestCaseElement((ITestCaseElement)testElement);
			}
			return null;
		}

		private void logUnexpectedTest(String testId, TestElementReference testElement) {
			CDTUnitTestPlugin.log(new Exception("Unexpected TestElement type for testId '" + testId + "': " + testElement)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private ITestRunSession fTestRunSession;
	private ITestsRunnerProvider fTestsRunnerProvider;

	public void setTestRunSession(ITestRunSession testRunSession) {
		this.fTestRunSession = testRunSession;
	}

	public void setTestsRunnerProvider(ITestsRunnerProvider testsRunnerProvider) {
		this.fTestsRunnerProvider = testsRunnerProvider;
	}

	@Override
	public void startListening(int port) {
		// Nothing to do here
	}

	public void run(InputStream iStream) {
		notifyTestRunStarted(0);
		try {
			fTestsRunnerProvider.run(new TestModelUpdaterAdapter(), iStream);
			// If testing session was stopped, the status is set in stop()
			if (isRunning()) {
				double testingTime = fTestRunSession.getElapsedTimeInSeconds();
				CDTUnitTestPlugin.log(
						new org.eclipse.core.runtime.Status(IStatus.WARNING, CDTUnitTestPlugin.PLUGIN_ID,
						MessageFormat.format(CDTMessages.TestingSession_finished_status,
								testingTime)));
			}

			notifyTestRunEnded((long)(fTestRunSession.getElapsedTimeInSeconds() * 1000));
		} catch (TestingException e) {
			// If testing session was stopped, the status is set in stop()
			if (isRunning()) {
				CDTUnitTestPlugin.log(
						new org.eclipse.core.runtime.Status(IStatus.WARNING,
								CDTUnitTestPlugin.PLUGIN_ID,
						e.getLocalizedMessage()));
			}
			notifyTestRunTerminated();
		}
		shutDown();
	}

	@Override
	public void receiveMessage(String message) {
		// Does nothing
	}

	@Override
	public void stopTest() {
		// Does nothing
	}

/*
	private Status convertFromStatus(ITestElement.Status status) {
		//NotRun, Skipped, Passed, Failed, Aborted;
		if (status.isNotRun()) {
			return Status.NotRun;
		} else if (status.isOK()) {
			return Status.Passed;
		} else if (status.isFailure()) {
			return Status.Failed;
		} else if (status.isError()) {
			return Status.Aborted;
		} else if (status.isDone()) {
			return Status.Passed;
		}
		// TODO Make this conversion more close to the reality
		return Status.Aborted;
	}
*/
	ITestItem convertFromTestElement(final ITestElement element) {
		if (element instanceof ITestSuiteElement) {
			return convertFromTestSuiteElement(((ITestSuiteElement)element));
		}
		if (element instanceof ITestCaseElement) {
			return convertFromTestCaseElement((ITestCaseElement)element);
		}
		return null;
	}

	ITestCase convertFromTestCaseElement(final ITestCaseElement element) {
		if (element == null) {
			return null;
		}
		return new TestCase(element.getTestName(), (TestSuite)convertFromTestSuiteElement(element.getParent()));
	}

	ITestSuite convertFromTestSuiteElement(ITestSuiteElement testSuiteElement) {
		if (testSuiteElement == null) {
			return null;
		}
		return new TestSuite(testSuiteElement.getTestName(), (TestSuite)convertFromTestSuiteElement(testSuiteElement.getParent()));
	}

}
