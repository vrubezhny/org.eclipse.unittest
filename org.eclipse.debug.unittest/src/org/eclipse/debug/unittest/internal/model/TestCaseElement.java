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

package org.eclipse.debug.unittest.internal.model;

import org.eclipse.core.runtime.Assert;

import org.eclipse.debug.unittest.model.ITestCaseElement;
import org.eclipse.debug.unittest.model.ITestSuiteElement;

public class TestCaseElement extends TestElement implements ITestCaseElement {

	private boolean fIgnored;
	private boolean fIsDynamicTest;

	public TestCaseElement(ITestSuiteElement parent, String id, String testName, String displayName,
			boolean isDynamicTest, String[] parameterTypes, String uniqueId) {
		super(parent, id, testName, displayName, parameterTypes, uniqueId);
		Assert.isNotNull(parent);
		fIsDynamicTest = isDynamicTest;
	}

	@Override
	public String getTestMethodName() {
		String testName = getTestName();
		int index = testName.lastIndexOf('(');
		if (index > 0)
			return testName.substring(0, index);
		index = testName.indexOf('@');
		if (index > 0)
			return testName.substring(0, index);
		return testName;
	}

	@Override
	public String getTestClassName() {
		return getClassName();
	}

	@Override
	public Result getTestResult(boolean includeChildren) {
		if (fIgnored)
			return Result.IGNORED;
		else
			return super.getTestResult(includeChildren);
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
		return "TestCase: " + getTestClassName() + "." + getTestMethodName() + " : " + super.toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public boolean isDynamicTest() {
		return fIsDynamicTest;
	}
}
