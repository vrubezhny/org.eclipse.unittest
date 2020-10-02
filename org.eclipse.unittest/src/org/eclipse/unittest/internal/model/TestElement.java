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
import org.eclipse.unittest.model.ITestRoot;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;

import org.eclipse.core.runtime.Assert;

public abstract class TestElement implements ITestElement {
	private final ITestSuiteElement fParent;
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
	private String fTrace;
	private String fExpected;
	private String fActual;

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
	public TestElement(ITestSuiteElement parent, String id, String testName, String displayName,
			String[] parameterTypes, String uniqueId) {
		Assert.isNotNull(id);
		Assert.isNotNull(testName);
		fParent = parent;
		fId = id;
		fTestName = testName;
		fDisplayName = displayName;
		fParameterTypes = parameterTypes;
		fUniqueId = uniqueId;
		fStatus = Status.NOT_RUN;
		if (parent != null)
			parent.addChild(this);
	}

	@Override
	public ProgressState getProgressState() {
		return getStatus().convertToProgressState();
	}

	@Override
	public Result getTestResult(boolean includeChildren) {
		if (fAssumptionFailed) {
			return Result.IGNORED;
		}
		return getStatus().convertToResult();
	}

	@Override
	public ITestRunSession getTestRunSession() {
		return getRoot().getTestRunSession();
	}

	@Override
	public ITestElementContainer getParentContainer() {
		if (fParent instanceof TestRoot) {
			return getTestRunSession();
		}
		return fParent;
	}

	@Override
	public FailureTrace getFailureTrace() {
		Result testResult = getTestResult(false);
		if (testResult == Result.ERROR || testResult == Result.FAILURE
				|| (testResult == Result.IGNORED && fTrace != null)) {
			return new FailureTrace(fTrace, fExpected, fActual);
		}
		return null;
	}

	@Override
	public ITestSuiteElement getParent() {
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

	@Override
	public void setStatus(Status status) {
		if (status == Status.RUNNING) {
			testStartedInstant = Instant.now();
		} else if (status.convertToProgressState() == ProgressState.COMPLETED && testStartedInstant != null) {
			this.duration = Duration.between(testStartedInstant, Instant.now());
		}

		fStatus = status;
		ITestSuiteElement parent = getParent();
		if (parent != null)
			parent.childChangedStatus(this, status);
	}

	@Override
	public void setStatus(Status status, String trace, String expected, String actual) {
		if (trace != null && fTrace != null) {
			// don't overwrite first trace if same test run logs multiple errors
			fTrace = fTrace + trace;
		} else {
			fTrace = trace;
			fExpected = expected;
			fActual = actual;
		}
		setStatus(status);
	}

	@Override
	public Status getStatus() {
		return fStatus;
	}

	@Override
	public String getTrace() {
		return fTrace;
	}

	@Override
	public String getExpected() {
		return fExpected;
	}

	@Override
	public String getActual() {
		return fActual;
	}

	@Override
	public boolean isComparisonFailure() {
		return fExpected != null && fActual != null;
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

	@Override
	public ITestRoot getRoot() {
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
