/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *******************************************************************************/
package org.eclipse.unittest.model;

/**
 * Common protocol for test elements. This set consists of
 * {@link ITestCaseElement} , {@link ITestSuiteElement} and
 * {@link ITestRunSession}
 *
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 */
public interface ITestElement {

	public final static class Status {
		public static final Status RUNNING_ERROR = new Status("RUNNING_ERROR", 5); //$NON-NLS-1$
		public static final Status RUNNING_FAILURE = new Status("RUNNING_FAILURE", 6); //$NON-NLS-1$
		public static final Status RUNNING = new Status("RUNNING", 3); //$NON-NLS-1$

		public static final Status ERROR = new Status("ERROR", /* 1 */ITestRunListener3.STATUS_ERROR); //$NON-NLS-1$
		public static final Status FAILURE = new Status("FAILURE", /* 2 */ITestRunListener3.STATUS_FAILURE); //$NON-NLS-1$
		public static final Status OK = new Status("OK", /* 0 */ITestRunListener3.STATUS_OK); //$NON-NLS-1$
		public static final Status NOT_RUN = new Status("NOT_RUN", 4); //$NON-NLS-1$

		private static final Status[] OLD_CODE = { OK, ERROR, FAILURE };

		private final String fName;
		private final int fOldCode;

		private Status(String name, int oldCode) {
			fName = name;
			fOldCode = oldCode;
		}

		public int getOldCode() {
			return fOldCode;
		}

		@Override
		public String toString() {
			return fName;
		}

		/* error state predicates */

		public boolean isOK() {
			return this == OK || this == RUNNING || this == NOT_RUN;
		}

		public boolean isFailure() {
			return this == FAILURE || this == RUNNING_FAILURE;
		}

		public boolean isError() {
			return this == ERROR || this == RUNNING_ERROR;
		}

		public boolean isErrorOrFailure() {
			return isError() || isFailure();
		}

		/* progress state predicates */

		public boolean isNotRun() {
			return this == NOT_RUN;
		}

		public boolean isRunning() {
			return this == RUNNING || this == RUNNING_FAILURE || this == RUNNING_ERROR;
		}

		public boolean isDone() {
			return this == OK || this == FAILURE || this == ERROR;
		}

		public static Status combineStatus(Status one, Status two) {
			Status progress = combineProgress(one, two);
			Status error = combineError(one, two);
			return combineProgressAndErrorStatus(progress, error);
		}

		private static Status combineProgress(Status one, Status two) {
			if (one.isNotRun() && two.isNotRun())
				return NOT_RUN;
			else if (one.isDone() && two.isDone())
				return OK;
			else if (!one.isRunning() && !two.isRunning())
				return OK; // one done, one not-run -> a parent failed and its children are not run
			else
				return RUNNING;
		}

		private static Status combineError(Status one, Status two) {
			if (one.isError() || two.isError())
				return ERROR;
			else if (one.isFailure() || two.isFailure())
				return FAILURE;
			else
				return OK;
		}

		private static Status combineProgressAndErrorStatus(Status progress, Status error) {
			if (progress.isDone()) {
				if (error.isError())
					return ERROR;
				if (error.isFailure())
					return FAILURE;
				return OK;
			}

			if (progress.isNotRun()) {
//				Assert.isTrue(!error.isErrorOrFailure());
				return NOT_RUN;
			}

//			Assert.isTrue(progress.isRunning());
			if (error.isError())
				return RUNNING_ERROR;
			if (error.isFailure())
				return RUNNING_FAILURE;
//			Assert.isTrue(error.isOK());
			return RUNNING;
		}

		/**
		 * @param oldStatus one of {@link ITestRunListener3}'s STATUS_* constants
		 * @return the Status
		 */
		public static Status convert(int oldStatus) {
			return OLD_CODE[oldStatus];
		}

		public Result convertToResult() {
			if (isNotRun())
				return Result.UNDEFINED;
			if (isError())
				return Result.ERROR;
			if (isFailure())
				return Result.FAILURE;
			if (isRunning()) {
				return Result.UNDEFINED;
			}
			return Result.OK;
		}

		public ProgressState convertToProgressState() {
			if (isRunning()) {
				return ProgressState.RUNNING;
			}
			if (isDone()) {
				return ProgressState.COMPLETED;
			}
			return ProgressState.NOT_STARTED;
		}

	}

	/**
	 * Running states of a test.
	 */
	public static final class ProgressState {
		/** state that describes that the test element has not started */
		public static final ProgressState NOT_STARTED = new ProgressState("Not Started"); //$NON-NLS-1$
		/** state that describes that the test element has is running */
		public static final ProgressState RUNNING = new ProgressState("Running"); //$NON-NLS-1$
		/**
		 * state that describes that the test element has been stopped before being
		 * completed
		 */
		public static final ProgressState STOPPED = new ProgressState("Stopped"); //$NON-NLS-1$
		/** state that describes that the test element has completed */
		public static final ProgressState COMPLETED = new ProgressState("Completed"); //$NON-NLS-1$

		private String fName;

		private ProgressState(String name) {
			fName = name;
		}

		@Override
		public String toString() {
			return fName;
		}
	}

	/**
	 * Result states of a test.
	 */
	public static final class Result {
		/** state that describes that the test result is undefined */
		public static final Result UNDEFINED = new Result("Undefined"); //$NON-NLS-1$
		/** state that describes that the test result is 'OK' */
		public static final Result OK = new Result("OK"); //$NON-NLS-1$
		/** state that describes that the test result is 'Error' */
		public static final Result ERROR = new Result("Error"); //$NON-NLS-1$
		/** state that describes that the test result is 'Failure' */
		public static final Result FAILURE = new Result("Failure"); //$NON-NLS-1$
		/** state that describes that the test result is 'Ignored' */
		public static final Result IGNORED = new Result("Ignored"); //$NON-NLS-1$

		private String fName;

		private Result(String name) {
			fName = name;
		}

		@Override
		public String toString() {
			return fName;
		}
	}

	/**
	 * A failure trace of a test.
	 *
	 * This class is not intended to be instantiated or extended by clients.
	 */
	public static final class FailureTrace {
		private final String fActual;
		private final String fExpected;
		private final String fTrace;

		public FailureTrace(String trace, String expected, String actual) {
			fActual = actual;
			fExpected = expected;
			fTrace = trace;
		}

		/**
		 * Returns the failure stack trace.
		 *
		 * @return the failure stack trace
		 */
		public String getTrace() {
			return fTrace;
		}

		/**
		 * Returns the expected result or <code>null</code> if the trace is not a
		 * comparison failure.
		 *
		 * @return the expected result or <code>null</code> if the trace is not a
		 *         comparison failure.
		 */
		public String getExpected() {
			return fExpected;
		}

		/**
		 * Returns the actual result or <code>null</code> if the trace is not a
		 * comparison failure.
		 *
		 * @return the actual result or <code>null</code> if the trace is not a
		 *         comparison failure.
		 */
		public String getActual() {
			return fActual;
		}
	}

	String getId();

	String getUniqueId();

	/**
	 * Returns the progress state of this test element.
	 * <ul>
	 * <li>{@link ITestElement.ProgressState#NOT_STARTED}: the test has not yet
	 * started</li>
	 * <li>{@link ITestElement.ProgressState#RUNNING}: the test is currently
	 * running</li>
	 * <li>{@link ITestElement.ProgressState#STOPPED}: the test has stopped before
	 * being completed</li>
	 * <li>{@link ITestElement.ProgressState#COMPLETED}: the test (and all its
	 * children) has completed</li>
	 * </ul>
	 *
	 * @return returns one of {@link ITestElement.ProgressState#NOT_STARTED},
	 *         {@link ITestElement.ProgressState#RUNNING},
	 *         {@link ITestElement.ProgressState#STOPPED} or
	 *         {@link ITestElement.ProgressState#COMPLETED}.
	 */
	ProgressState getProgressState();

	/**
	 * Returns the result of the test element.
	 * <ul>
	 * <li>{@link ITestElement.Result#UNDEFINED}: the result is not yet
	 * evaluated</li>
	 * <li>{@link ITestElement.Result#OK}: the test has succeeded</li>
	 * <li>{@link ITestElement.Result#ERROR}: the test has returned an error</li>
	 * <li>{@link ITestElement.Result#FAILURE}: the test has returned an
	 * failure</li>
	 * <li>{@link ITestElement.Result#IGNORED}: the test has been ignored
	 * (skipped)</li>
	 * </ul>
	 *
	 * @param includeChildren if <code>true</code>, the returned result is the
	 *                        combined result of the test and its children (if it
	 *                        has any). If <code>false</code>, only the test's
	 *                        result is returned.
	 *
	 * @return returns one of {@link ITestElement.Result#UNDEFINED},
	 *         {@link ITestElement.Result#OK}, {@link ITestElement.Result#ERROR},
	 *         {@link ITestElement.Result#FAILURE} or
	 *         {@link ITestElement.Result#IGNORED}. Clients should also prepare for
	 *         other, new values.
	 */
	Result getTestResult(boolean includeChildren);

	/**
	 * Returns the failure trace of this test element or <code>null</code> if the
	 * test has not resulted in an error or failure.
	 *
	 * @return the failure trace of this test or <code>null</code>.
	 */
	FailureTrace getFailureTrace();

	/**
	 * Returns the parent test element container or <code>null</code> if the test
	 * element is the test run session.
	 *
	 * @return the parent test suite
	 */
	ITestElementContainer getParentContainer();

	/**
	 * Returns the test run session.
	 *
	 * @return the parent test run session.
	 */
	ITestRunSession getTestRunSession();

	/**
	 * Returns the estimated total time elapsed in seconds while executing this test
	 * element. The total time for a test suite includes the time used for all tests
	 * in that suite. The total time for a test session includes the time used for
	 * all tests in that session.
	 * <p>
	 * <strong>Note:</strong> The elapsed time is only valid for
	 * {@link ITestElement.ProgressState#COMPLETED} test elements.
	 * </p>
	 *
	 * @return total execution time for the test element in seconds, or
	 *         {@link Double#NaN} if the state of the element is not
	 *         {@link ITestElement.ProgressState#COMPLETED}
	 *
	 * @since 3.4
	 */
	double getElapsedTimeInSeconds();

	ITestSuiteElement getParent();

	ITestRoot getRoot();

	String[] getParameterTypes();

	String getTestName();

	/**
	 * Returns the display name of the test. Can be <code>null</code>. In that case,
	 * use {@link ITestElement#getTestName() getTestName()}.
	 *
	 * @return the test display name, can be <code>null</code>
	 */
	String getDisplayName();

	Status getStatus();

	void setStatus(Status status);

	void setStatus(Status status, String trace, String expected, String actual);

	String getTrace();

	String getExpected();

	String getActual();

	boolean isComparisonFailure();

	void setAssumptionFailed(boolean assumptionFailed);

	boolean isAssumptionFailure();

	void setElapsedTimeInSeconds(double time);

	String extractRawClassName(String testNameString);

}
