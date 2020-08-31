package org.eclipse.unittest.cdt.launcher;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Stack;

import org.eclipse.cdt.testsrunner.launcher.ITestsRunnerProvider;
import org.eclipse.cdt.testsrunner.model.IModelVisitor;
import org.eclipse.cdt.testsrunner.model.ITestCase;
import org.eclipse.cdt.testsrunner.model.ITestItem;
import org.eclipse.cdt.testsrunner.model.ITestItem.Status;
import org.eclipse.cdt.testsrunner.model.ITestLocation;
import org.eclipse.cdt.testsrunner.model.ITestMessage;
import org.eclipse.cdt.testsrunner.model.ITestMessage.Level;
import org.eclipse.cdt.testsrunner.model.ITestModelUpdater;
import org.eclipse.cdt.testsrunner.model.ITestSuite;
import org.eclipse.cdt.testsrunner.model.TestingException;
import org.eclipse.unittest.cdt.CDTPlugin;
import org.eclipse.unittest.internal.model.ModelMessages;
import org.eclipse.unittest.internal.model.TestCaseElement;
import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.internal.model.TestRunnerClient;
import org.eclipse.unittest.internal.model.TestSuiteElement;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;

public class CDTTestRunnerClient extends TestRunnerClient {
	private static final String FRAME_PREFIX = org.eclipse.unittest.ui.FailureTrace.FRAME_PREFIX;

	class TestModelUpdaterAdapter implements ITestModelUpdater {
		long testRunStartTime = -1;

		class TestElementReference {
			String parentId;
			String id;
			String name;
			boolean isSuite;
			long startTime;
			int testingTime;

			public TestElementReference(String parentId, String id, String name, boolean isSuite) {
				this.parentId = parentId;
				this.id = id;
				this.name = name;
				this.isSuite = isSuite;
				this.startTime = System.currentTimeMillis();
			}

			double getElapsedTimeInSecs() {
				long elapsed = System.currentTimeMillis() - startTime;
				return (elapsed) / 1000.0d;
			}
		}

		Stack<TestElementReference> testElementRefs = new Stack<>();

		String fCurrentTestCase;
		String fCurrentTestSuite;
		int fTestId  = 0;

		@Override
		public void enterTestSuite(String name) {
			System.out.println("TestModelUpdaterAdapter.enterTestSuite: name = " + name);

			TestElementReference pRef = testElementRefs.isEmpty() ? null : testElementRefs.peek();

			TestElementReference cRef = new TestElementReference(
					pRef == null ? String.valueOf("-1") : pRef.id,
					String.valueOf(fTestId++),
					name,
					true);
			testElementRefs.push(cRef);

			this.fCurrentTestSuite = cRef.id;

			notifyTestTreeEntry(cRef.id, cRef.name, cRef.isSuite, 0, true, cRef.parentId,
					cRef.name, null, "");
		}

		@Override
		public void exitTestSuite() {
			System.out.println("TestModelUpdaterAdapter.exitTestSuite");

			TestElementReference cRef = testElementRefs.pop();
			while (cRef != null && !cRef.isSuite) {
				logUnexpectedTest(cRef.id, cRef);
				cRef = testElementRefs.pop();
			}
		}

		@Override
		public void enterTestCase(String name) {
			System.out.println("TestModelUpdaterAdapter.enterTestCase: name = " + name);

			TestElementReference pRef = testElementRefs.isEmpty() ? null : testElementRefs.peek();

			String parentId = String.valueOf("-1");
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
					cRef.name, null, "");

			notifyTestStarted(cRef.id, cRef.name);
		}

		@Override
		public void setTestStatus(Status status) {
			System.out.println("TestModelUpdaterAdapter.setTestStatus: status = " + status.toString());
			if (status.isError()) {
				TestElementReference cRef = testElementRefs.isEmpty() ? null : testElementRefs.peek();
				if (cRef != null) {
					extractFailure(cRef.id, cRef.name,
							status == Status.Aborted ? TestElement.Status.FAILURE.getOldCode() :
								TestElement.Status.ERROR.getOldCode(),
								false);

					notifyTestFailed();

				} else {
					logUnexpectedTest(fCurrentTestCase, null);
				}
			}
		}

		@Override
		public void setTestingTime(int testingTime) {
			System.out.println("TestModelUpdaterAdapter.setTestingTime: testingTime = " + testingTime);
			TestElementReference cRef = testElementRefs.isEmpty() ? null : testElementRefs.peek();
			if (cRef != null) {
				cRef.testingTime = testingTime;
			} else {
				logUnexpectedTest(fCurrentTestCase, null);
			}
		}

		@Override
		public void exitTestCase() {
			System.out.println("TestModelUpdaterAdapter.exitTestCase");

			TestElementReference cRef = testElementRefs.isEmpty() ? null : testElementRefs.peek();

			if (cRef != null && !cRef.isSuite) {
				testElementRefs.pop(); // Renove test case ref

				notifyTestEnded(cRef.id, cRef.name, false);
			} else {
				logUnexpectedTest(cRef == null ? "null" : cRef.id, cRef);
			}

		}

		@Override
		public void addTestMessage(String file, int line, Level level, String text) {
			System.out.println("TestModelUpdaterAdapter.addTestMessage: file = " + file +
					", line = " + line +
					", level = " + level.toString() +
					", text = " + text);

			fFailedTrace.append(level.toString()).append(": ").append(text).append("\r\n")
				.append(FRAME_PREFIX).append(file).append(':').append(line).append("\r\n");
		}

		@Override
		public ITestSuite currentTestSuite() {
			System.out.println("TestModelUpdaterAdapter.currentTestSuite");

			ITestElement testElement = fTestRunSession.getTestElement(fCurrentTestSuite);
			if (testElement instanceof TestSuiteElement) {
				return convertFromTestSuiteElement((TestSuiteElement)testElement);
			} else if (testElement instanceof TestElement) {
				return convertFromTestSuiteElement(((TestElement)testElement).getParent());
			} else {
				return convertFromTestElement(testElement).getParent();
			}
		}

		@Override
		public ITestCase currentTestCase() {
			System.out.println("TestModelUpdaterAdapter.currentTestCase");

			ITestElement testElement = fTestRunSession.getTestElement(fCurrentTestCase);
			if (testElement instanceof TestCaseElement) {
				return convertFromTestCaseElement((TestCaseElement)testElement);
			}
			return null;
		}

		private Status convertFromStatus(org.eclipse.unittest.internal.model.TestElement.Status status) {
			//NotRun, Skipped, Passed, Failed, Aborted;
			if (status.isNotRun()) {
				return Status.NotRun;
			} else if (status.isOK()) {
				return Status.Passed;
			} else if (status.isErrorOrFailure()) {
				return Status.Failed;
			} else if (status.isDone()) {
				return Status.Passed;
			}
			// TODO Make this conversion more close to the reality
			return Status.Aborted;
		}

		ITestCase convertFromTestCaseElement(ITestCaseElement element) {
			if (element instanceof TestCaseElement) {
				TestCaseElement testCaseElement = (TestCaseElement)element;

				return new ITestCase() {
					@Override
					public void visit(IModelVisitor visitor) {
						// TODO Auto-generated method stub
					}

					@Override
					public boolean hasChildren() {
						return false;
					}

					@Override
					public int getTestingTime() {
						return (int)(testCaseElement.getElapsedTimeInSeconds() * 1000);
					}

					@Override
					public Status getStatus() {
						return convertFromStatus(testCaseElement.getStatus());
					}

					@Override
					public ITestSuite getParent() {
						return convertFromTestSuiteElement(testCaseElement.getParent());
					}

					@Override
					public String getName() {
						return testCaseElement.getTestName();
					}

					@Override
					public ITestItem[] getChildren() {
						return new ITestItem[0];
					}


					@Override
					public ITestMessage[] getTestMessages() {
						FailureTrace trace = testCaseElement.getFailureTrace();
						if (trace == null) {
							return new ITestMessage[0];
						}
						return new ITestMessage[] {
								new ITestMessage() {

									@Override
									public ITestLocation getLocation() {
										return null;
									}

									@Override
									public Level getLevel() {
										return Level.Info;
									}

									@Override
									public String getText() {
										return trace.toString();
									}

									@Override
									public void visit(IModelVisitor visitor) {
									}

								}
						};
					}
				};
			}
			return null;
		}

		ITestItem convertFromTestElement(ITestElement element) {
			if (element instanceof TestElement) {
				TestElement testElement = (TestElement)element;

				return new ITestItem() {
					@Override
					public void visit(IModelVisitor visitor) {
						// TODO Auto-generated method stub
					}

					@Override
					public boolean hasChildren() {
						return false;
					}

					@Override
					public int getTestingTime() {
						return (int)testElement.getElapsedTimeInSeconds() * 1000;
					}

					@Override
					public Status getStatus() {
						return convertFromStatus(testElement.getStatus());
					}

					@Override
					public ITestSuite getParent() {
						return convertFromTestSuiteElement(testElement.getParent());
					}

					@Override
					public String getName() {
						return testElement.getTestName();
					}

					@Override
					public ITestItem[] getChildren() {
						return new ITestItem[0];
					}
				};
			}
			return null;
		}

		ITestSuite convertFromTestSuiteElement(TestSuiteElement testSuiteElement) {
			if (testSuiteElement == null) {
				return null;
			}
			return new ITestSuite() {

				@Override
				public void visit(IModelVisitor visitor) {
					// TODO Auto-generated method stub
				}

				@Override
				public boolean hasChildren() {
					ITestElement[] children = testSuiteElement.getChildren();
					return children != null && children.length > 0;
				}

				@Override
				public int getTestingTime() {
					return (int)testSuiteElement.getElapsedTimeInSeconds() * 1000;
				}

				@Override
				public Status getStatus() {
					return convertFromStatus(testSuiteElement.getStatus());
				}

				@Override
				public ITestSuite getParent() {
					return convertFromTestSuiteElement(testSuiteElement.getParent());
				}

				@Override
				public String getName() {
					return testSuiteElement.getTestName();
				}

				@Override
				public ITestItem[] getChildren() {
					ITestElement[] children = testSuiteElement.getChildren();
					ITestItem[] childrenItems = new ITestItem[children == null ? 0 : children.length];
 					int i = 0;
					for (ITestElement child : children) {
 						childrenItems[i++] = convertFromTestElement(child);
 					}
					return childrenItems;
				}
			};
		}

		private void logUnexpectedTest(String testId, TestElementReference testElement) {
			CDTPlugin.log(new Exception("Unexpected TestElement type for testId '" + testId + "': " + testElement)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private TestRunSession fTestRunSession;
	private ITestsRunnerProvider fTestsRunnerProvider;
	private String fStatusMessage = "";
	private boolean fHasErrors = false;

	/*
	public CDTTestRunnerClient(ITestRunSession testRunSession, ITestsRunnerProvider testsRunnerProvider) {
		this.fTestRunSession = testRunSession;
		this.fTestsRunnerProvider = testsRunnerProvider;
	}
	*/

	public void setTestRunSession(TestRunSession testRunSession) {
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
				fStatusMessage = MessageFormat.format(ModelMessages.TestingSession_finished_status,
						testingTime);
			}
			notifyTestRunEnded((long)(fTestRunSession.getElapsedTimeInSeconds() * 1000));
		} catch (TestingException e) {
			// If testing session was stopped, the status is set in stop()
			if (isRunning()) {
				fStatusMessage = e.getLocalizedMessage();
				fHasErrors = true;
			}
			notifyTestRunTerminated();
		}
//		finished = true;
		shutDown();
	}

	@Override
	public void receiveMessage(String message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopTest() {
		// TODO Auto-generated method stub

	}

	@Override
	public void rerunTest(String testId, String className, String testName) {
		// TODO Auto-generated method stub

	}

}
