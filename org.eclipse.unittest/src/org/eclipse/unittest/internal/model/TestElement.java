/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import java.time.Duration;
import java.time.Instant;

import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElementContainer;

import org.eclipse.core.runtime.Assert;

public abstract class TestElement implements ITestElement {
	private final TestSuiteElement fParent;
	private final String fId;
	private String fTestName;

	/**
	 * The display name of the test element, can be <code>null</code>. In that case,
	 * use {@link TestElement#fTestName fTestName}.
	 */
	private String fDisplayName;

	/**
	 * The array of method parameter types if applicable, otherwise
	 * <code>null</code>.
	 */
	private String[] fParameterTypes;

	/**
	 * The unique ID of the test element which can be <code>null</code>
	 */
	private String fUniqueId;

	private Status fStatus;
	protected FailureTrace fTrace;

	private boolean fAssumptionFailed;

	protected Instant testStartedInstant = null;
	protected Duration duration = null;

	/**
	 * Constructs the test element object
	 *
	 * @param parent         the parent, can be <code>null</code>
	 * @param id             the test id
	 * @param testName       the test name
	 * @param displayName    the test display name, can be <code>null</code>
	 * @param parameterTypes the array of method parameter types if applicable,
	 *                       otherwise <code>null</code>
	 * @param uniqueId       the unique ID of the test element, can be
	 *                       <code>null</code>
	 */
	public TestElement(TestSuiteElement parent, String id, String testName, String displayName, String[] parameterTypes,
			String uniqueId) {
		Assert.isNotNull(id);
		Assert.isNotNull(testName);
		fParent = parent;
		fId = id;
		fTestName = testName;
		fDisplayName = displayName;
		fParameterTypes = parameterTypes;
		fUniqueId = uniqueId;
		fStatus = Status.NOT_RUN;
		if (parent != null) {
			parent.addChild(this);
		}
	}

	/**
	 * Returns the progress state of this test element.
	 * <ul>
	 * <li>{@link ProgressState#NOT_STARTED}: the test has not yet started</li>
	 * <li>{@link ProgressState#RUNNING}: the test is currently running</li>
	 * <li>{@link ProgressState#STOPPED}: the test has stopped before being
	 * completed</li>
	 * <li>{@link ProgressState#COMPLETED}: the test (and all its children) has
	 * completed</li>
	 * </ul>
	 *
	 * @return returns one of {@link ProgressState#NOT_STARTED},
	 *         {@link ProgressState#RUNNING}, {@link ProgressState#STOPPED} or
	 *         {@link ProgressState#COMPLETED}.
	 */
	public ProgressState getProgressState() {
		return getStatus().convertToProgressState();
	}

	/**
	 * Returns the result of the test element.
	 * <ul>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.Result#UNDEFINED}: the
	 * result is not yet evaluated</li>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.Result#OK}: the test has
	 * succeeded</li>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.Result#ERROR}: the test
	 * has returned an error</li>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.Result#FAILURE}: the test
	 * has returned an failure</li>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.Result#IGNORED}: the test
	 * has been ignored (skipped)</li>
	 * </ul>
	 *
	 * @param includeChildren if <code>true</code>, the returned result is the
	 *                        combined result of the test and its children (if it
	 *                        has any). If <code>false</code>, only the test's
	 *                        result is returned.
	 *
	 * @return returns one of
	 *         {@link org.eclipse.unittest.model.ITestElement.Result#UNDEFINED},
	 *         {@link org.eclipse.unittest.model.ITestElement.Result#OK},
	 *         {@link org.eclipse.unittest.model.ITestElement.Result#ERROR},
	 *         {@link org.eclipse.unittest.model.ITestElement.Result#FAILURE} or
	 *         {@link org.eclipse.unittest.model.ITestElement.Result#IGNORED}.
	 *         Clients should also prepare for other, new values.
	 */
	public Result getTestResult(boolean includeChildren) {
		if (fAssumptionFailed) {
			return Result.IGNORED;
		}
		return getStatus().convertToResult();
	}

	@Override
	public TestRunSession getTestRunSession() {
		return getRoot().getTestRunSession();
	}

	/**
	 * Returns the parent test element container or <code>null</code> if the test
	 * element is the test run session.
	 *
	 * @return the parent test suite
	 */
	public ITestElementContainer getParentContainer() {
		if (fParent instanceof TestRoot) {
			return getTestRunSession();
		}
		return fParent;
	}

	@Override
	public FailureTrace getFailureTrace() {
		Result testResult = getTestResult(false);
		if ((testResult == Result.ERROR || testResult == Result.FAILURE
				|| (testResult == Result.IGNORED) && fTrace != null)) {
			return fTrace;
		}
		return null;
	}

	@Override
	public TestSuiteElement getParent() {
		return fParent;
	}

	@Override
	public String getId() {
		return fId;
	}

	@Override
	public String getTestName() {
		return fTestName;
	}

	public void setName(String name) {
		fTestName = name;
	}

	/**
	 * Sets the current test element status
	 *
	 * @param status one of {@link Status#NOT_RUN}, {@link Status#OK},
	 *               {@link Status#ERROR} or {@link Status#FAILURE}.
	 */
	public void setStatus(Status status) {
		if (status == Status.RUNNING) {
			testStartedInstant = Instant.now();
		} else if (status.convertToProgressState() == ProgressState.COMPLETED && testStartedInstant != null) {
			this.duration = Duration.between(testStartedInstant, Instant.now());
		}

		fStatus = status;
		TestSuiteElement parent = getParent();
		if (parent != null) {
			parent.childChangedStatus(this, status);
		}
	}

	/**
	 * Sets the extended status for this test element
	 *
	 * @param status       one of {@link Status#NOT_RUN}, {@link Status#OK},
	 *                     {@link Status#ERROR} or {@link Status#FAILURE}.
	 * @param failureTrace stacktracee/error message or null
	 */
	public void setStatus(Status status, FailureTrace failureTrace) {
		if (failureTrace != null && fTrace != null) {
			// don't overwrite first trace if same test run logs multiple errors
			fTrace = new FailureTrace(fTrace.getTrace() + failureTrace.getTrace(), fTrace.getExpected(),
					fTrace.getActual());
		} else {
			fTrace = failureTrace;
		}
		setStatus(status);
	}

	/**
	 * Returns the status of this test element
	 * <ul>
	 * <li>{@link Status#NOT_RUN}: the test has not executed</li>
	 * <li>{@link Status#OK}: the test is successful</li>
	 * <li>{@link Status#ERROR}: the test had an error</li>
	 * <li>{@link Status#FAILURE}: the test had an assertion failure</li>
	 * </ul>
	 *
	 * @return returns one of {@link Status#NOT_RUN}, {@link Status#OK},
	 *         {@link Status#ERROR} or {@link Status#FAILURE}.
	 */
	public Status getStatus() {
		return fStatus;
	}

	@Override
	public boolean isComparisonFailure() {
		return getFailureTrace().getExpected() != null && getFailureTrace().getActual() != null;
	}

	@Override
	public String getClassName() {
		return extractClassName(getTestName());
	}

	private String extractClassName(String testNameString) {
		testNameString = extractRawClassName(testNameString);
		testNameString = testNameString.replace('$', '.'); // see bug 178503
		return testNameString;
	}

	@Override
	public String extractRawClassName(String testNameString) {
		if (testNameString.startsWith("[") && testNameString.endsWith("]")) { //$NON-NLS-1$ //$NON-NLS-2$
			// a group of parameterized tests, see
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102512
			return testNameString;
		}
		int index = testNameString.lastIndexOf('(');
		if (index < 0)
			return testNameString;
		int end = testNameString.lastIndexOf(')');
		testNameString = testNameString.substring(index + 1, end > index ? end : testNameString.length());
		return testNameString;
	}

	/**
	 * Returns the root test element
	 *
	 * @return a root test element
	 */
	public TestRoot getRoot() {
		return getParent().getRoot();
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	@Override
	public Duration getDuration() {
		return this.duration;
	}

	@Override
	public void setAssumptionFailed(boolean assumptionFailed) {
		fAssumptionFailed = assumptionFailed;
	}

	@Override
	public boolean isAssumptionFailure() {
		return fAssumptionFailed;
	}

	@Override
	public String toString() {
		return getProgressState() + " - " + getTestResult(true); //$NON-NLS-1$
	}

	@Override
	public String getDisplayName() {
		return fDisplayName;
	}

	@Override
	public String[] getParameterTypes() {
		return fParameterTypes;
	}

	@Override
	public String getUniqueId() {
		return fUniqueId;
	}

}
