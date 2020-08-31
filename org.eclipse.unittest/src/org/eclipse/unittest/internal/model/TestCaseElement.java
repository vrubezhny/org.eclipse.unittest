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
 *     Xavier Coulon <xcoulon@redhat.com> - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102512 - [JUnit] test method name cut off before (
 *******************************************************************************/

package org.eclipse.unittest.internal.model;

import org.eclipse.unittest.model.ITestCaseElement;

import org.eclipse.core.runtime.Assert;


public class TestCaseElement extends TestElement implements ITestCaseElement {

	private boolean fIgnored;
	private boolean fIsDynamicTest;

	public TestCaseElement(TestSuiteElement parent, String id, String testName, String displayName, boolean isDynamicTest, String[] parameterTypes, String uniqueId) {
		super(parent, id, testName, displayName, parameterTypes, uniqueId);
		Assert.isNotNull(parent);
		fIsDynamicTest= isDynamicTest;
	}

	@Override
	public String getTestMethodName() {
		String testName= getTestName();
		int index= testName.lastIndexOf('(');
		if (index > 0)
			return testName.substring(0, index);
		index= testName.indexOf('@');
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

	public void setIgnored(boolean ignored) {
		fIgnored= ignored;
	}

	public boolean isIgnored() {
		return fIgnored;
	}

	@Override
	public String toString() {
		return "TestCase: " + getTestClassName() + "." + getTestMethodName() + " : " + super.toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public boolean isDynamicTest() {
		return fIsDynamicTest;
	}
}
