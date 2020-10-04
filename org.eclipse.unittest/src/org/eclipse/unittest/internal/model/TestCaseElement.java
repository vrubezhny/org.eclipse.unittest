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
		return "TestCase: " + super.toString(); //$NON-NLS-1$
	}

	@Override
	public boolean isDynamicTest() {
		return fIsDynamicTest;
	}
}
