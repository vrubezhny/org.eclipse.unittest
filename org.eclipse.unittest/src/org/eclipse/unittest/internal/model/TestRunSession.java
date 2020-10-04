/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.unittest.internal.model;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.launcher.UnitTestLaunchConfigurationConstants;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElementContainer;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.ITestViewSupport;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;

/**
 * A test run session holds all information about a test run, i.e. launch
 * configuration, launch, test tree (including results).
 */
public class TestRunSession extends TestElement implements ITestRunSession, ITestRunSessionReport {

	/**
	 * The launch, or <code>null</code> iff this session was run externally.
	 */
	private final ILaunch fLaunch;
	private final String fTestRunName;
	private final ITestViewSupport fTestRunnerSupport;

	/**
	 * Test runner client or <code>null</code>.
	 */
	private ITestRunnerClient fTestRunnerClient;

	private final ListenerList<ITestSessionListener> fSessionListeners;
	private final List<ITestRunListener> testRunListeners = Collections.singletonList(new TestSessionNotifier());

	/**
	 * The model root, or <code>null</code> if swapped to disk.
	 */
	private TestRoot fTestRoot;

	/**
	 * The test run session's cached result, or <code>null</code> if
	 * <code>fTestRoot != null</code>.
	 */
	private Result fTestResult;

	/**
	 * Map from testId to testElement.
	 */
	private HashMap<String, TestElement> fIdToTest;

	/**
	 * The TestSuites for which additional children are expected.
	 */
	private List<IncompleteTestSuite> fIncompleteTestSuites;

	private List<IncompleteTestSuite> fFactoryTestSuites;

	/**
	 * Suite for unrooted test case elements, or <code>null</code>.
	 */
	private TestSuiteElement fUnrootedSuite;

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * Tags included in this test run.
	 */
	private String fIncludeTags;

	/**
	 * Tags excluded from this test run.
	 */
	private String fExcludeTags;

	/**
	 * Number of tests started during this test run.
	 */
	volatile int fStartedCount;
	/**
	 * Number of tests ignored during this test run.
	 */
	volatile int fIgnoredCount;
	/**
	 * Number of tests whose assumption failed during this test run.
	 */
	volatile int fAssumptionFailureCount;
	/**
	 * Number of errors during this test run.
	 */
	volatile int fErrorCount;
	/**
	 * Number of failures during this test run.
	 */
	volatile int fFailureCount;
	/**
	 * Total number of tests to run.
	 */
	volatile int fTotalCount;
	/**
	 * <ul>
	 * <li>If &gt; 0: Start time in millis</li>
	 * <li>If &lt; 0: Unique identifier for imported test run</li>
	 * <li>If = 0: Session not started yet</li>
	 * </ul>
	 */
	volatile long fStartTime;
	volatile boolean fIsRunning;

	volatile boolean fIsStopped;

	/**
	 * Creates a test run session.
	 *
	 * @param testRunName name of the test run
	 */
	public TestRunSession(String testRunName) {
		super(null, "-1", testRunName, null, null); //$NON-NLS-1$
		// TODO: check assumptions about non-null fields

		fLaunch = null;
		fStartTime = -System.currentTimeMillis();

		Assert.isNotNull(testRunName);
		fTestRunName = testRunName;
		fTestRunnerSupport = null;

		fTestRoot = new TestRoot(this);
		fIdToTest = new HashMap<>();

		fTestRunnerClient = null;

		fSessionListeners = new ListenerList<>();
	}

	public TestRunSession(ILaunch launch) {
		super(null, "-1", "<TestRunSession>", null, null); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.isNotNull(launch);

		fLaunch = launch;

		ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
		if (launchConfiguration != null) {
			fTestRunName = launchConfiguration.getName();
			fTestRunnerSupport = UnitTestModel.newTestRunnerViewSupport(launchConfiguration);
		} else {
			fTestRunName = "<TestRunSession>"; //$NON-NLS-1$
			fTestRunnerSupport = null;
		}

		fTestRoot = new TestRoot(this);
		fIdToTest = new HashMap<>();

		if (fTestRunnerSupport != null) {
			fTestRunnerClient = fTestRunnerSupport.newTestRunnerClient(this);
		}

		final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchListener(new ILaunchesListener2() {
			@Override
			public void launchesTerminated(ILaunch[] launches) {
				if (Arrays.asList(launches).contains(fLaunch)) {
					if (fTestRunnerClient != null) {
						fTestRunnerClient.stopWaiting();
					}
					launchManager.removeLaunchListener(this);
				}
			}

			@Override
			public void launchesRemoved(ILaunch[] launches) {
				if (Arrays.asList(launches).contains(fLaunch)) {
					if (fTestRunnerClient != null) {
						fTestRunnerClient.stopWaiting();
					}
					launchManager.removeLaunchListener(this);
				}
			}

			@Override
			public void launchesChanged(ILaunch[] launches) {
				// do nothing
			}

			@Override
			public void launchesAdded(ILaunch[] launches) {
				// do nothing
			}
		});

		fSessionListeners = new ListenerList<>();
		addTestSessionListener(new TestRunListenerAdapter(this));
	}

	// TODO Consider removal as it's only use in XML parsing
	public void reset() {
		fStartedCount = 0;
		fFailureCount = 0;
		fAssumptionFailureCount = 0;
		fErrorCount = 0;
		fIgnoredCount = 0;
		fTotalCount = 0;

		fTestRoot = new TestRoot(this);
		fTestResult = null;
		fIdToTest = new HashMap<>();
	}

	@Override
	public ProgressState getProgressState() {
		if (isRunning()) {
			return ProgressState.RUNNING;
		}
		if (isStopped()) {
			return ProgressState.STOPPED;
		}
		return ProgressState.COMPLETED;
	}

	@Override
	public Result getTestResult(boolean includeChildren) {
		if (fTestRoot != null) {
			return fTestRoot.getTestResult(true);
		} else {
			return fTestResult;
		}
	}

	@Override
	public List<TestElement> getChildren() {
		return getTestRoot().getChildren();
	}

	@Override
	public FailureTrace getFailureTrace() {
		return null;
	}

	@Override
	public ITestElementContainer getParentContainer() {
		return null;
	}

	@Override
	public TestRunSession getTestRunSession() {
		return this;
	}

	/**
	 * Returns the root test element of this test run session
	 *
	 * @return a root test element
	 */
	public synchronized TestRoot getTestRoot() {
		swapIn(); // TODO: TestRoot should stay (e.g. for getTestRoot().getStatus())
		return fTestRoot;
	}

	/**
	 * Returns the Test Runner View Support for which this test run session has been
	 * launched, or <code>null</code> if not available.
	 *
	 * @return the test runner view support, or <code>null</code> is not available.
	 */
	public ITestViewSupport getTestViewSupport() {
		return fTestRunnerSupport;
	}

	/**
	 * @return the launch, or <code>null</code> iff this session was run externally
	 */
	@Override
	public ILaunch getLaunch() {
		return fLaunch;
	}

	@Override
	public String getTestRunName() {
		return fTestRunName;
	}

	@Override
	public int getErrorCount() {
		return fErrorCount;
	}

	@Override
	public int getFailureCount() {
		return fFailureCount;
	}

	@Override
	public int getAssumptionFailureCount() {
		return fAssumptionFailureCount;
	}

	@Override
	public int getStartedCount() {
		return fStartedCount;
	}

	@Override
	public int getIgnoredCount() {
		return fIgnoredCount;
	}

	@Override
	public int getTotalCount() {
		return fTotalCount;
	}

	public long getStartTime() {
		return fStartTime;
	}

	/**
	 * Indicates if the test run session has been stopped or terminated
	 *
	 * @return <code>true</code> if the session has been stopped or terminated,
	 *         otherwise returns <code>false</code>
	 */
	@Override
	public boolean isStopped() {
		return fIsStopped;
	}

	public synchronized void addTestSessionListener(ITestSessionListener listener) {
		swapIn();
		fSessionListeners.add(listener);
	}

	public void removeTestSessionListener(ITestSessionListener listener) {
		fSessionListeners.remove(listener);
	}

	public synchronized void swapOut() {
		if (fTestRoot == null)
			return;
		if (isRunning() || isStarting() || isKeptAlive())
			return;

		for (ITestSessionListener registered : fSessionListeners) {
			if (!registered.acceptsSwapToDisk())
				return;
		}

		try {
			File swapFile = getSwapFile();

			UnitTestModel.exportTestRunSession(this, swapFile);
			fTestResult = fTestRoot.getTestResult(true);
			fTestRoot = null;
			fTestRunnerClient = null;
			fIdToTest = new HashMap<>();
			fIncompleteTestSuites = null;
			fFactoryTestSuites = null;
			fUnrootedSuite = null;

		} catch (IllegalStateException e) {
			UnitTestPlugin.log(e);
		} catch (CoreException e) {
			UnitTestPlugin.log(e);
		}
	}

	@Override
	public boolean isStarting() {
		return getStartTime() == 0 && fLaunch != null && !fLaunch.isTerminated();
	}

	public void removeSwapFile() {
		File swapFile = getSwapFile();
		if (swapFile.exists())
			swapFile.delete();
	}

	private File getSwapFile() throws IllegalStateException {
		File historyDir = UnitTestModel.getHistoryDirectory();
		String isoTime = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS").format(new Date(getStartTime())); //$NON-NLS-1$
		String swapFileName = isoTime + ".xml"; //$NON-NLS-1$
		return new File(historyDir, swapFileName);
	}

	public synchronized void swapIn() {
		if (fTestRoot != null)
			return;

		try {
			UnitTestModel.importIntoTestRunSession(getSwapFile(), this);
		} catch (IllegalStateException e) {
			UnitTestPlugin.log(e);
			fTestRoot = new TestRoot(this);
			fTestResult = null;
		} catch (CoreException e) {
			UnitTestPlugin.log(e);
			fTestRoot = new TestRoot(this);
			fTestResult = null;
		}
	}

	@Override
	public void stopTestRun() {
		if (isRunning() || !isKeptAlive())
			fIsStopped = true;
		if (fTestRunnerClient != null)
			fTestRunnerClient.stopTest();
	}

	/**
	 * @return <code>true</code> iff the runtime VM of this test session is still
	 *         alive
	 */
	@Override
	public boolean isKeptAlive() {
		if (fTestRunnerClient != null && fLaunch != null && fTestRunnerClient.isRunning()
				&& ILaunchManager.DEBUG_MODE.equals(fLaunch.getLaunchMode())) {
			ILaunchConfiguration config = fLaunch.getLaunchConfiguration();
			try {
				return config != null
						&& config.getAttribute(UnitTestLaunchConfigurationConstants.ATTR_KEEPRUNNING, false);
			} catch (CoreException e) {
				return false;
			}

		} else {
			return false;
		}
	}

	/**
	 * @return <code>true</code> if this session has been started, but not ended nor
	 *         stopped nor terminated
	 */
	@Override
	public boolean isRunning() {
		return fIsRunning;
	}

	@Override
	public TestElement getTestElement(String id) {
		return fIdToTest.get(id);
	}

	private TestElement addTreeEntry(String id, String testName, boolean isSuite, int testCount, boolean isDynamicTest,
			String parentId, String displayName, String uniqueId) {
		if (isDynamicTest) {
			if (parentId != null) {
				for (IncompleteTestSuite suite : fFactoryTestSuites) {
					if (parentId.equals(suite.fTestSuiteElement.getId())) {
						return createTestElement(suite.fTestSuiteElement, id, testName, isSuite, testCount,
								isDynamicTest, displayName, uniqueId);
					}
				}
			}
			return createTestElement(getUnrootedSuite(), id, testName, isSuite, testCount, isDynamicTest, displayName,
					uniqueId); // should not reach here
		} else {
			if (fIncompleteTestSuites.isEmpty()) {
				return createTestElement(fTestRoot, id, testName, isSuite, testCount, isDynamicTest, displayName,
						uniqueId);
			} else {
				int suiteIndex = fIncompleteTestSuites.size() - 1;
				IncompleteTestSuite openSuite = fIncompleteTestSuites.get(suiteIndex);
				openSuite.fOutstandingChildren--;
				if (openSuite.fOutstandingChildren <= 0)
					fIncompleteTestSuites.remove(suiteIndex);
				return createTestElement(openSuite.fTestSuiteElement, id, testName, isSuite, testCount, isDynamicTest,
						displayName, uniqueId);
			}
		}
	}

	/**
	 * Creates a test element, either {@link ITestSuiteElement} or
	 * {@link ITestCaseElement} instance, depending on the arguments.
	 *
	 * @param parent        a parent test suite element
	 * @param id            an identifier of the test element
	 * @param testName      a name of the test element
	 * @param isSuite       a flag indicating if the test element should be
	 *                      represented by a test suite element
	 * @param testCount     a number of predefined test cases in case of test suite
	 *                      element
	 * @param isDynamicTest a flag indicating that test suite is dynamic (that
	 *                      doesn't have predefined tests)
	 * @param displayName   a display name for the test element
	 * @param data          some test runner specific data, can be <code>null</code>
	 * @return a created {@link ITestSuiteElement} or {@link ITestCaseElement}
	 *         instance
	 */
	public TestElement createTestElement(ITestSuiteElement parent, String id, String testName, boolean isSuite,
			int testCount, boolean isDynamicTest, String displayName, String data) {
		TestElement testElement;
		if (isSuite) {
			TestSuiteElement testSuiteElement = new TestSuiteElement((TestSuiteElement) parent, id, testName, testCount,
					displayName, data);
			testElement = testSuiteElement;
			if (testCount > 0) {
				fIncompleteTestSuites.add(new IncompleteTestSuite(testSuiteElement, testCount));
			} else if (fFactoryTestSuites != null) {
				fFactoryTestSuites.add(new IncompleteTestSuite(testSuiteElement, testCount));
			}
		} else {
			testElement = new TestCaseElement((TestSuiteElement) parent, id, testName, displayName, isDynamicTest,
					data);
		}
		fIdToTest.put(id, testElement);
		return testElement;
	}

	private TestSuiteElement getUnrootedSuite() {
		if (fUnrootedSuite == null) {
			fUnrootedSuite = (TestSuiteElement) createTestElement(fTestRoot, "-2", //$NON-NLS-1$
					ModelMessages.TestRunSession_unrootedTests, true, 0, false,
					ModelMessages.TestRunSession_unrootedTests, null);
		}
		return fUnrootedSuite;
	}

	/**
	 * An {@link ITestRunListener} that listens to events from the and translates
	 * {@link ITestRunnerClient} them into high-level model events (broadcasted to
	 * {@link ITestSessionListener}s).
	 */
	private class TestSessionNotifier implements ITestRunListener {

		@Override
		public void testRunStarted(int testCount) {
			fIncompleteTestSuites = new ArrayList<>();
			fFactoryTestSuites = new ArrayList<>();

			fStartedCount = 0;
			fIgnoredCount = 0;
			fFailureCount = 0;
			fAssumptionFailureCount = 0;
			fErrorCount = 0;
			fTotalCount = testCount;

			fStartTime = System.currentTimeMillis();
			fIsRunning = true;

			for (ITestSessionListener listener : fSessionListeners) {
				listener.sessionStarted();
			}
		}

		@Override
		public void testRunEnded(Duration duration) {
			fIsRunning = false;

			for (ITestSessionListener listener : fSessionListeners) {
				listener.sessionEnded(duration);
			}
		}

		@Override
		public void testRunStopped(Duration duration) {
			fIsRunning = false;
			fIsStopped = true;

			for (ITestSessionListener listener : fSessionListeners) {
				listener.sessionStopped(duration);
			}
		}

		@Override
		public void testRunTerminated() {
			fIsRunning = false;
			fIsStopped = true;

			for (ITestSessionListener listener : fSessionListeners) {
				listener.sessionTerminated();
			}
		}

		@Override
		public void testTreeEntry(String testId, String testName, boolean isSuite, int testCount, boolean isDynamicTest,
				String parentId, String displayName, String uniqueId) {
			ITestElement testElement = addTreeEntry(testId, testName, isSuite, testCount, isDynamicTest, parentId,
					displayName, uniqueId);

			for (ITestSessionListener listener : fSessionListeners) {
				listener.testAdded(testElement);
			}
		}

		private ITestElement createUnrootedTestElement(String testId, String testName) {
			ITestSuiteElement unrootedSuite = getUnrootedSuite();
			ITestElement testElement = createTestElement(unrootedSuite, testId, testName, false, 1, false, testName,
					null);

			for (ITestSessionListener listener : fSessionListeners) {
				listener.testAdded(testElement);
			}

			return testElement;
		}

		@Override
		public void testStarted(String testId, String testName) {
			if (fStartedCount == 0) {
				for (ITestSessionListener listener : fSessionListeners) {
					listener.runningBegins();
				}
			}
			ITestElement testElement = getTestElement(testId);
			if (testElement == null) {
				testElement = createUnrootedTestElement(testId, testName);
			} else if (!(testElement instanceof TestCaseElement)) {
				logUnexpectedTest(testId, testElement);
				return;
			}
			TestCaseElement testCaseElement = (TestCaseElement) testElement;
			setStatus(testCaseElement, Status.RUNNING);

			fStartedCount++;
			if (testCaseElement.isDynamicTest()) {
				fTotalCount++;
			}
			fTotalCount = Math.max(fStartedCount, fTotalCount);

			for (ITestSessionListener listener : fSessionListeners) {
				listener.testStarted(testCaseElement);
			}
		}

		@Override
		public void testEnded(ITestElement testElement, boolean isIgnored) {
			if (testElement == null) {
				return;
			}
			if (!(testElement instanceof TestCaseElement)) {
				if (isIgnored) {
					((TestElement) testElement).setAssumptionFailed(true);
					fAssumptionFailureCount++;
					setStatus(testElement, Status.OK);
				} else {
					logUnexpectedTest(testElement.getId(), testElement);
				}
				return;
			}
			TestCaseElement testCaseElement = (TestCaseElement) testElement;
			if (isIgnored) {
				testCaseElement.setIgnored(true);
				fIgnoredCount++;
			}

			if (testCaseElement.getStatus() == Status.RUNNING)
				setStatus(testCaseElement, Status.OK);

			for (ITestSessionListener listener : fSessionListeners) {
				listener.testEnded(testCaseElement);
			}
		}

		@Override
		public void testFailed(ITestElement testElement, Result status, boolean isAssumptionFailed,
				FailureTrace trace) {
			if (testElement == null) {
				return;
			}

			if (isAssumptionFailed) {
				((TestElement) testElement).setAssumptionFailed(true);
				fAssumptionFailureCount++;
				status = Result.OK;
			}

			registerTestFailureStatus((TestElement) testElement, status, trace);

			for (ITestSessionListener listener : fSessionListeners) {
				listener.testFailed(testElement, status, trace);
			}
		}

		@Override
		public void testReran(String testId, String className, String testName, Result status,
				FailureTrace failureTrace) {
			ITestElement testElement = getTestElement(testId);
			if (testElement == null) {
				testElement = createUnrootedTestElement(testId, testName);
			} else if (!(testElement instanceof TestCaseElement)) {
				logUnexpectedTest(testId, testElement);
				return;
			}
			TestCaseElement testCaseElement = (TestCaseElement) testElement;

			registerTestFailureStatus(testCaseElement, status, failureTrace);

			for (ITestSessionListener listener : fSessionListeners) {
				// TODO: post old & new status?
				listener.testReran(testCaseElement, status, failureTrace);
			}
		}

		private void logUnexpectedTest(String testId, ITestElement testElement) {
			UnitTestPlugin
					.log(new Exception("Unexpected TestElement type for testId '" + testId + "': " + testElement)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static class IncompleteTestSuite {
		public final TestSuiteElement fTestSuiteElement;
		public int fOutstandingChildren;

		public IncompleteTestSuite(TestSuiteElement testSuiteElement, int outstandingChildren) {
			fTestSuiteElement = testSuiteElement;
			fOutstandingChildren = outstandingChildren;
		}
	}

	public void registerTestFailureStatus(TestElement testElement, Result status, FailureTrace failureTrace) {
		testElement.setStatus(Status.fromResult(status), failureTrace);
		if (!testElement.isAssumptionFailure()) {
			if (status == Result.ERROR) {
				fErrorCount++;
			} else if (status == Result.FAILURE) {
				fFailureCount++;
			}
		}
	}

	public void registerTestEnded(TestElement testElement, boolean completed) {
		if (testElement instanceof TestCaseElement) {
			fTotalCount++;
			if (!completed) {
				return;
			}
			fStartedCount++;
			TestCaseElement testCaseElement = (TestCaseElement) testElement;
			if (testCaseElement.isIgnored()) {
				fIgnoredCount++;
			}
			if (!testCaseElement.getStatus().isErrorOrFailure())
				setStatus(testElement, Status.OK);
		}

		if (testElement.isAssumptionFailure()) {
			fAssumptionFailureCount++;
		}
	}

	private void setStatus(ITestElement testElement, Status status) {
		((TestElement) testElement).setStatus(status);
	}

	/**
	 * Returns an array of all failed {@link ITestElement}s
	 *
	 * @return an array of failed {@link ITestElement}s
	 */
	public List<TestElement> getAllFailedTestElements() {
		List<TestElement> failures = new ArrayList<>();
		addFailures(failures, getTestRoot());
		return Collections.unmodifiableList(failures);
	}

	private void addFailures(Collection<TestElement> failures, TestElement testElement) {
		Result testResult = testElement.getTestResult(true);
		if (testResult == Result.ERROR || testResult == Result.FAILURE) {
			failures.add(testElement);
		}
		if (testElement instanceof TestSuiteElement) {
			TestSuiteElement testSuiteElement = (TestSuiteElement) testElement;
			for (TestElement child : testSuiteElement.getChildren()) {
				addFailures(failures, child);
			}
		}
	}

	@Override
	public Duration getDuration() {
		if (fTestRoot == null) {
			return null;
		}
		return fTestRoot.getDuration();
	}

	public String getIncludeTags() {
		if (fLaunch != null) {
			try {
				ILaunchConfiguration launchConfig = fLaunch.getLaunchConfiguration();
				if (launchConfig != null) {
					boolean hasIncludeTags = launchConfig
							.getAttribute(UnitTestLaunchConfigurationConstants.ATTR_TEST_HAS_INCLUDE_TAGS, false);
					if (hasIncludeTags) {
						return launchConfig.getAttribute(UnitTestLaunchConfigurationConstants.ATTR_TEST_INCLUDE_TAGS,
								EMPTY_STRING);
					}
				}
			} catch (CoreException e) {
				// ignore
			}
			return EMPTY_STRING;
		}
		return fIncludeTags;
	}

	public String getExcludeTags() {
		if (fLaunch != null) {
			try {
				ILaunchConfiguration launchConfig = fLaunch.getLaunchConfiguration();
				if (launchConfig != null) {
					boolean hasExcludeTags = launchConfig
							.getAttribute(UnitTestLaunchConfigurationConstants.ATTR_TEST_HAS_EXCLUDE_TAGS, false);
					if (hasExcludeTags) {
						return launchConfig.getAttribute(UnitTestLaunchConfigurationConstants.ATTR_TEST_EXCLUDE_TAGS,
								EMPTY_STRING);
					}
				}
			} catch (CoreException e) {
				// ignore
			}
			return EMPTY_STRING;
		}
		return fExcludeTags;
	}

	public void setIncludeTags(String includeTags) {
		fIncludeTags = includeTags;
	}

	public void setExcludeTags(String excludeTags) {
		fExcludeTags = excludeTags;
	}

	@Override
	public String toString() {
		return fTestRunName + " " + DateFormat.getDateTimeInstance().format(new Date(fStartTime)); //$NON-NLS-1$
	}

	@Override
	public TestSuiteElement getParent() {
		return null;
	}

	@Override
	public String getTestName() {
		return getTestRunName();
	}

	public abstract class ListenerSafeRunnable implements ISafeRunnable {
		@Override
		public void handleException(Throwable exception) {
			UnitTestPlugin.log(exception);
		}
	}

	@Override
	public void notifyTestReran(String testId, String className, String testName, Result status,
			FailureTrace failureTrace) {
		for (ITestRunListener listener : testRunListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testReran(testId, className, testName, status, failureTrace);
				}
			});
		}
	}

	@Override
	public void notifyTestTreeEntry(String testId, String testName, boolean isSuite, int testCount,
			boolean isDynamicTest, String parentId, String displayName, String uniqueId) {
		for (ITestRunListener listener : testRunListeners) {
			listener.testTreeEntry(testId, testName, isSuite, testCount, isDynamicTest, parentId, displayName,
					uniqueId);
		}
	}

	/**
	 * Notifies on a test run stopped.
	 *
	 * @param duration the total elapsed time of the test run
	 */
	@Override
	public void notifyTestRunStopped(final Duration duration) {
		if (isStopped())
			return;
		for (ITestRunListener listener : testRunListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunStopped(duration);
				}
			});
		}
	}

	/**
	 * Notifies on a test run ended.
	 *
	 * @param duration the total elapsed time of the test run
	 */
	@Override
	public void notifyTestRunEnded(final Duration duration) {
		if (isStopped())
			return;
		for (ITestRunListener listener : testRunListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunEnded(duration);
				}
			});
		}
	}

	/**
	 * Notifies on an individual test ended.
	 *
	 * @param testId    a unique Id identifying the test
	 * @param testName  the name of the test that failed
	 * @param isIgnored <code>true</code> indicates that the specified test was
	 *                  ignored, otherwise - <code>false</code>
	 */
	@Override
	public void notifyTestEnded(ITestElement test, boolean isIgnored) {
		if (isStopped())
			return;
		for (ITestRunListener listener : testRunListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testEnded(test, isIgnored);
				}
			});
		}
	}

	@Override
	public ITestElement notifyTestStarted(final String testId, final String testName) {
		if (isStopped())
			return null;
		for (ITestRunListener listener : testRunListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testStarted(testId, testName);
				}
			});
		}
		return getTestElement(testId);
	}

	/**
	 * Notifies on a test run started.
	 *
	 * @param count the number of individual tests that will be run
	 */
	@Override
	public void notifyTestRunStarted(final int count) {
		if (isStopped())
			return;
		for (ITestRunListener listener : testRunListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunStarted(count);
				}
			});
		}
	}

	@Override
	public void notifyTestFailed(ITestElement test, Result status, boolean isAssumptionFailed,
			FailureTrace failureTrace) {
		if (isStopped())
			return;
		if (status != Result.FAILURE && status != Result.ERROR) {
			throw new IllegalArgumentException("Status has to be FAILURE or ERROR");
		}
		for (ITestRunListener listener : testRunListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testFailed(test, status, isAssumptionFailed, failureTrace);
				}
			});
		}
	}

	/**
	 * Notifies on a test run terminated.
	 */
	@Override
	public void notifyTestRunTerminated() {
		if (isStopped())
			return;
		for (ITestRunListener listener : testRunListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunTerminated();
				}
			});
		}
	}
}
