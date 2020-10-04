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
import org.eclipse.unittest.model.ITestRoot;
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
		super(null, "-1", testRunName, null, null, null); //$NON-NLS-1$
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
		super(null, "-1", "<TestRunSession>", null, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.isNotNull(launch);

		fLaunch = launch;

		ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
		if (launchConfiguration != null) {
			fTestRunName = launchConfiguration.getName();
			fTestRunnerSupport = UnitTestLaunchConfigurationConstants.newTestRunnerViewSupport(launchConfiguration);
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

	void reset() {
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
	public ITestElement[] getChildren() {
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

	@Override
	public synchronized ITestRoot getTestRoot() {
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
			String parentId, String displayName, String[] parameterTypes, String uniqueId) {
		if (isDynamicTest) {
			if (parentId != null) {
				for (IncompleteTestSuite suite : fFactoryTestSuites) {
					if (parentId.equals(suite.fTestSuiteElement.getId())) {
						return createTestElement(suite.fTestSuiteElement, id, testName, isSuite, testCount,
								isDynamicTest, displayName, parameterTypes, uniqueId);
					}
				}
			}
			return createTestElement(getUnrootedSuite(), id, testName, isSuite, testCount, isDynamicTest, displayName,
					parameterTypes, uniqueId); // should not reach here
		} else {
			if (fIncompleteTestSuites.isEmpty()) {
				return createTestElement(fTestRoot, id, testName, isSuite, testCount, isDynamicTest, displayName,
						parameterTypes, uniqueId);
			} else {
				int suiteIndex = fIncompleteTestSuites.size() - 1;
				IncompleteTestSuite openSuite = fIncompleteTestSuites.get(suiteIndex);
				openSuite.fOutstandingChildren--;
				if (openSuite.fOutstandingChildren <= 0)
					fIncompleteTestSuites.remove(suiteIndex);
				return createTestElement(openSuite.fTestSuiteElement, id, testName, isSuite, testCount, isDynamicTest,
						displayName, parameterTypes, uniqueId);
			}
		}
	}

	/**
	 * Creates a test element, either {@link ITestSuiteElement} or
	 * {@link ITestCaseElement} instance, depending on the arguments.
	 *
	 * @param parent         a parent test suite element
	 * @param id             an identifier of the test element
	 * @param testName       a name of the test element
	 * @param isSuite        a flag indicating if the test element should be
	 *                       represented by a test suite element
	 * @param testCount      a number of predefined test cases in case of test suite
	 *                       element
	 * @param isDynamicTest  a flag indicating that test suite is dynamic (that
	 *                       doesn't have predefined tests)
	 * @param displayName    a display name for the test element
	 * @param parameterTypes an array of parameter types, or <code>null</code>
	 * @param uniqueId       a unique identifier for the test element or
	 *                       <code>null</code>
	 * @return a created {@link ITestSuiteElement} or {@link ITestCaseElement}
	 *         instance
	 */
	public TestElement createTestElement(ITestSuiteElement parent, String id, String testName, boolean isSuite,
			int testCount, boolean isDynamicTest, String displayName, String[] parameterTypes, String uniqueId) {
		TestElement testElement;
		if (parameterTypes != null && parameterTypes.length > 1) {
			parameterTypes = Arrays.stream(parameterTypes).map(String::trim).toArray(String[]::new);
		}
		if (isSuite) {
			TestSuiteElement testSuiteElement = new TestSuiteElement((TestSuiteElement) parent, id, testName, testCount,
					displayName, parameterTypes, uniqueId);
			testElement = testSuiteElement;
			if (testCount > 0) {
				fIncompleteTestSuites.add(new IncompleteTestSuite(testSuiteElement, testCount));
			} else if (fFactoryTestSuites != null) {
				fFactoryTestSuites.add(new IncompleteTestSuite(testSuiteElement, testCount));
			}
		} else {
			testElement = new TestCaseElement((TestSuiteElement) parent, id, testName, displayName, isDynamicTest,
					parameterTypes, uniqueId);
		}
		fIdToTest.put(id, testElement);
		return testElement;
	}

	private TestSuiteElement getUnrootedSuite() {
		if (fUnrootedSuite == null) {
			fUnrootedSuite = (TestSuiteElement) createTestElement(fTestRoot, "-2", //$NON-NLS-1$
					ModelMessages.TestRunSession_unrootedTests, true, 0, false,
					ModelMessages.TestRunSession_unrootedTests, null, null);
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
				String parentId, String displayName, String[] parameterTypes, String uniqueId) {
			ITestElement testElement = addTreeEntry(testId, testName, isSuite, testCount, isDynamicTest, parentId,
					displayName, parameterTypes, uniqueId);

			for (ITestSessionListener listener : fSessionListeners) {
				listener.testAdded(testElement);
			}
		}

		private ITestElement createUnrootedTestElement(String testId, String testName) {
			ITestSuiteElement unrootedSuite = getUnrootedSuite();
			ITestElement testElement = createTestElement(unrootedSuite, testId, testName, false, 1, false, testName,
					null, null);

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
					testElement.setAssumptionFailed(true);
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
		public void testFailed(ITestElement testElement, Status status, boolean isAssumptionFailed, String trace,
				String expected, String actual) {
			if (testElement == null) {
				return;
			}

			if (isAssumptionFailed) {
				testElement.setAssumptionFailed(true);
				fAssumptionFailureCount++;
				status = Status.OK;
			}

			registerTestFailureStatus(testElement, status, trace, expected, actual);

			for (ITestSessionListener listener : fSessionListeners) {
				listener.testFailed(testElement, status, trace, expected, actual);
			}
		}

		@Override
		public void testReran(String testId, String className, String testName, Status status, String trace,
				String expectedResult, String actualResult) {
			ITestElement testElement = getTestElement(testId);
			if (testElement == null) {
				testElement = createUnrootedTestElement(testId, testName);
			} else if (!(testElement instanceof TestCaseElement)) {
				logUnexpectedTest(testId, testElement);
				return;
			}
			TestCaseElement testCaseElement = (TestCaseElement) testElement;

			registerTestFailureStatus(testElement, status, trace, expectedResult, actualResult);

			for (ITestSessionListener listener : fSessionListeners) {
				// TODO: post old & new status?
				listener.testReran(testCaseElement, status, trace, expectedResult, actualResult);
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

	public void registerTestFailureStatus(ITestElement testElement, Status status, String trace, String expected,
			String actual) {
		testElement.setStatus(status, trace, expected, actual);
		if (!testElement.isAssumptionFailure()) {
			if (status.isError()) {
				fErrorCount++;
			} else if (status.isFailure()) {
				fFailureCount++;
			}
		}
	}

	public void registerTestEnded(ITestElement testElement, boolean completed) {
		if (testElement instanceof TestCaseElement) {
			fTotalCount++;
			if (!completed) {
				return;
			}
			fStartedCount++;
			if (((TestCaseElement) testElement).isIgnored()) {
				fIgnoredCount++;
			}
			if (!testElement.getStatus().isErrorOrFailure())
				setStatus(testElement, Status.OK);
		}

		if (testElement.isAssumptionFailure()) {
			fAssumptionFailureCount++;
		}
	}

	private void setStatus(ITestElement testElement, Status status) {
		testElement.setStatus(status);
	}

	@Override
	public ITestElement[] getAllFailedTestElements() {
		ArrayList<ITestElement> failures = new ArrayList<>();
		addFailures(failures, getTestRoot());
		return failures.toArray(new TestElement[failures.size()]);
	}

	private void addFailures(ArrayList<ITestElement> failures, ITestElement testElement) {
		Result testResult = testElement.getTestResult(true);
		if (testResult == Result.ERROR || testResult == Result.FAILURE) {
			failures.add(testElement);
		}
		if (testElement instanceof TestSuiteElement) {
			TestSuiteElement testSuiteElement = (TestSuiteElement) testElement;
			ITestElement[] children = testSuiteElement.getChildren();
			for (ITestElement child : children) {
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
	public String[] getParameterTypes() {
		return null;
	}

	@Override
	public String getTestName() {
		return getTestRunName();
	}

	@Override
	public String getTrace() {
		return null;
	}

	@Override
	public String getExpected() {
		return null;
	}

	@Override
	public String getActual() {
		return null;
	}

	@Override
	public boolean isComparisonFailure() {
		return false;
	}

	public abstract class ListenerSafeRunnable implements ISafeRunnable {
		@Override
		public void handleException(Throwable exception) {
			UnitTestPlugin.log(exception);
		}
	}

	/**
	 * Notifies on an individual test re-run.
	 *
	 * @param testId     a unique Id identifying the test
	 * @param className  the name of the test class that was rerun
	 * @param testName   the name of the test that was rerun
	 * @param statusCode the outcome of the test that was rerun; one of
	 *                   {@link ITestRunListener#STATUS_OK},
	 *                   {@link ITestRunListener#STATUS_ERROR}, or
	 *                   {@link ITestRunListener#STATUS_FAILURE}
	 * @param trace      the stack trace in the case of abnormal termination, or the
	 *                   empty string if none
	 * @param expected   the expected value in case of abnormal termination, or the
	 *                   empty string if none
	 * @param actual     the actual value in case of abnormal termination, or the
	 *                   empty string if none
	 */
	@Override
	public void notifyTestReran(String testId, String className, String testName, Status status, String trace,
			String expected, String actual) {
		for (ITestRunListener listener : testRunListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testReran(testId, className, testName, status, trace, expected, actual);
				}
			});
		}
	}

	/**
	 * Notifies on a member of the test suite that is about to be run.
	 *
	 * @param testId         a unique id for the test
	 * @param testName       the name of the test
	 * @param isSuite        true or false depending on whether the test is a suite
	 * @param testCount      an integer indicating the number of tests
	 * @param isDynamicTest  true or false
	 * @param parentId       the unique testId of its parent if it is a dynamic
	 *                       test, otherwise can be "-1"
	 * @param displayName    the display name of the test
	 * @param parameterTypes comma-separated list of method parameter types if
	 *                       applicable, otherwise an empty string
	 * @param uniqueId       the unique ID of the test provided, otherwise an empty
	 *                       string
	 */
	@Override
	public void notifyTestTreeEntry(String testId, String testName, boolean isSuite, int testCount,
			boolean isDynamicTest, String parentId, String displayName, String[] parameterTypes, String uniqueId) {
		for (ITestRunListener listener : testRunListeners) {
			listener.testTreeEntry(testId, testName, isSuite, testCount, isDynamicTest, parentId, displayName,
					parameterTypes, uniqueId);
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
	public void notifyTestFailed(ITestElement test, Status status, boolean isAssumptionFailed, String trace,
			String expected, String actual) {
		if (isStopped())
			return;
		if (status != Status.FAILURE && status != Status.ERROR) {
			throw new IllegalArgumentException("Status has to be FAILURE or ERROR");
		}
		for (ITestRunListener listener : testRunListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testFailed(test, status, isAssumptionFailed, trace, expected, actual);
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
