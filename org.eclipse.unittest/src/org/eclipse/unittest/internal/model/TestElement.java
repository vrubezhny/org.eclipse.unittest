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
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *     Xavier Coulon <xcoulon@redhat.com> - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102512 - [JUnit] test method name cut off before (
 *******************************************************************************/

package org.eclipse.unittest.internal.model;

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
	 * The unique ID of the test element which can be <code>null</code> as it is
	 * applicable to JUnit 5 and above.
	 */
	private String fUniqueId;

	private Status fStatus;
	private String fTrace;
	private String fExpected;
	private String fActual;

	private boolean fAssumptionFailed;

	/**
	 * Running time in seconds. Contents depend on the current
	 * {@link #getProgressState()}:
	 * <ul>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.ProgressState#NOT_STARTED}:
	 * {@link Double#NaN}</li>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.ProgressState#RUNNING}:
	 * negated start time</li>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.ProgressState#STOPPED}:
	 * elapsed time</li>
	 * <li>{@link org.eclipse.unittest.model.ITestElement.ProgressState#COMPLETED}:
	 * elapsed time</li>
	 * </ul>
	 */
	/* default */ double fTime = Double.NaN;

	/**
	 * @param parent         the parent, can be <code>null</code>
	 * @param id             the test id
	 * @param testName       the test name
	 * @param displayName    the test display name, can be <code>null</code>
	 * @param parameterTypes the array of method parameter types if applicable,
	 *                       otherwise <code>null</code>
	 * @param uniqueId       the unique ID of the test element, can be
	 *                       <code>null</code> as it is applicable to JUnit 5 and
	 *                       above
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

	/**
	 * @return the parent suite, or <code>null</code> for the root
	 */
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
			fTime = -System.currentTimeMillis() / 1000d;
		} else if (status.convertToProgressState() == ProgressState.COMPLETED) {
			if (fTime < 0) { // assert ! Double.isNaN(fTime)
				double endTime = System.currentTimeMillis() / 1000.0d;
				fTime = endTime + fTime;
			}
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

	@Override
	public void setElapsedTimeInSeconds(double time) {
		fTime = time;
	}

	@Override
	public double getElapsedTimeInSeconds() {
		if (Double.isNaN(fTime) || fTime < 0.0d) {
			return Double.NaN;
		}

		return fTime;
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

	/**
	 * @return the array of method parameter types if applicable, otherwise
	 *         <code>null</code>
	 */
	@Override
	public String[] getParameterTypes() {
		return fParameterTypes;
	}

	/**
	 * Returns the unique ID of the test element. Can be <code>null</code> as it is
	 * applicable to JUnit 5 and above.
	 *
	 * @return the unique ID of the test, can be <code>null</code>
	 */
	@Override
	public String getUniqueId() {
		return fUniqueId;
	}

}
