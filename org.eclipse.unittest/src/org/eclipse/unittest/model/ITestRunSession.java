/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.unittest.model;

import org.eclipse.unittest.launcher.ITestKind;

import org.eclipse.core.resources.IProject;

import org.eclipse.debug.core.ILaunch;

/**
 * Represents a test run session.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @since 3.3
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ITestRunSession extends ITestElementContainer {

	/**
	 * Returns the name of the test run. The name is the name of the launch configuration use to run this test.
	 *
	 * @return returns the test run name
	 */
	public String getTestRunName();

	/**
	 * Returns the Java project from which this test run session has been launched, or <code>null</code> if not available.
	 *
	 * @return the launched project, or <code>null</code> is not available.
	 * @since 3.6
	 */
	public abstract IProject getLaunchedProject();

	/**
	 * Returns the Java project from which this test run session has been launched, or <code>null</code> if not available.
	 *
	 * @return the launched project, or <code>null</code> is not available.
	 */
	public abstract ILaunch getLaunch();
	/**
	 * Returns the Test Runner Kind for which this test run session has been launched, or <code>null</code> if not available.
	 *
	 * @return the test runner kind, or <code>null</code> is not available.
	 * @since 3.6
	 */
	public ITestKind getTestRunnerKind();

	public ITestElement createTestElement(ITestSuiteElement parent, String id, String testName, boolean isSuite, int testCount, boolean isDynamicTest, String displayName, String[] parameterTypes, String uniqueId);

	public ITestSuiteElement getTestRoot();

	public ITestElement getTestElement(String id);

	public int getFailureCount();

	public int getErrorCount();

	public boolean isRunning();


}
