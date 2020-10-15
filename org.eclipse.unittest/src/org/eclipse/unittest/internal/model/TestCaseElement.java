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

import org.eclipse.unittest.model.ITestCaseElement;

import org.eclipse.core.runtime.Assert;

public class TestCaseElement extends TestElement implements ITestCaseElement {

	private boolean fIgnored;
	private boolean fIsDynamicTest;

	public TestCaseElement(TestSuiteElement parent, String id, String testName, String displayName,
			boolean isDynamicTest, String uniqueId) {
		super(parent, id, testName, displayName, uniqueId);
		Assert.isNotNull(parent);
		fIsDynamicTest = isDynamicTest;
	}

	@Override
	public Result getTestResult(boolean includeChildren) {
		return isIgnored() ? Result.IGNORED : super.getTestResult(includeChildren);
	}

	@Override
	public void setIgnored(boolean ignored) {
		fIgnored = ignored;
	}

	@Override
	public boolean isIgnored() {
		return fIgnored;
	}

	@Override
	public String toString() {
		return "TestCase: " + super.toString(); //$NON-NLS-1$
	}

	@Override
	public boolean isDynamicTest() {
		return fIsDynamicTest;
	}

	@Override
	Integer getFinalTestCaseCount() {
		return Integer.valueOf(1);
	}

	@Override
	int countStartedTestCases() {
		return getProgressState() != ProgressState.NOT_STARTED || testStartedInstant != null ? 1 : 0;
	}

	@Override
	int getCurrentFailureCount() {
		return getStatus() == Status.FAILURE ? 1 : 0;
	}

	@Override
	int getCurrentAssumptionFailureCount() {
		return isAssumptionFailure() ? 1 : 0;
	}

	@Override
	int getCurrentIgnoredCount() {
		return isIgnored() ? 1 : 0;
	}

	@Override
	int getCurrentErrorCount() {
		return getStatus() == Status.ERROR ? 1 : 0;
	}
}