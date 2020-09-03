/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.unittest.launcher;

/**
 * Interface to be implemented by for extension point
 * org.org.eclipse.unittest.unittestKinds.
 */
public interface ITestKind {
	static class NullTestKind extends TestKind {
		private NullTestKind() {
			super(null);
		}

		@Override
		public boolean isNull() {
			return true;
		}

	}

	TestKind NULL = new NullTestKind();

	String ID = "id"; //$NON-NLS-1$
	String DISPLAY_NAME = "displayName"; //$NON-NLS-1$
	String FINDER_CLASS_NAME = "finderClass"; //$NON-NLS-1$
	String LOADER_PLUGIN_ID = "loaderPluginId"; //$NON-NLS-1$
	String LOADER_CLASS_NAME = "loaderClass"; //$NON-NLS-1$
	String TEST_RUNNER_CLIENT_CLASS_NAME = "testRunnerClientClass"; //$NON-NLS-1$
	String TEST_VIEW_SUPPORT_CLASS_NAME = "testViewSupportClass"; //$NON-NLS-1$

	String PRECEDES = "precedesTestKind"; //$NON-NLS-1$

	String RUNTIME_CLASSPATH_ENTRY = "runtimeClasspathEntry"; //$NON-NLS-1$

	String CLASSPATH_PLUGIN_ID = "pluginId"; //$NON-NLS-1$
	String CLASSPATH_PATH_TO_JAR = "pathToJar"; //$NON-NLS-1$

	/**
	 * Returns a test finder defined for this Test Kind
	 *
	 * @return returns a test finder
	 */
	ITestFinder getFinder();

	/**
	 * Returns a Test Runner Client for this this Test Kind
	 *
	 * @return returns a Test Runner Client
	 */
	ITestRunnerClient getTestRunnerClient();

	/**
	 * Returns a Test View Support for this Test Kind
	 *
	 * @return returns a Test View Support
	 */
	ITestViewSupport getTestViewSupport();

	/**
	 * Returns a Test Kind identifier
	 *
	 * @return returns an identifier
	 */
	String getId();

	/**
	 * Returns a Test Kind display name
	 *
	 * @return returns a display name
	 */
	String getDisplayName();

	/**
	 * Returns a test finder qualified class name for this Test Kind
	 *
	 * @return a test finder qualified class name
	 */
	String getFinderClassName();

	/**
	 * Returns a Loader plug-in identifier for this Test Kind
	 *
	 * @return a Loader plug-in identifier
	 */
	String getLoaderPluginId();

	/**
	 * Returns a Loader qualified class name defined in loader plug-in for this Test
	 * Kind
	 *
	 * @return a Loader qualified class name
	 */
	String getLoaderClassName();

	/**
	 * Returns a test runner client qualified class name for this Test Kind
	 *
	 * @return a test runner client qualified class name
	 */
	String getTestRunnerClientClassName();

	/**
	 * Returns a test view support qualified class name for this Test Kind
	 *
	 * @return a test view support qualified class name
	 */
	String getTestViewSupportClassName();

	/**
	 * Returns a preceded Test Kind identifier
	 *
	 * @return a preceded Test Kind identifier
	 */
	String getPrecededKindId();

	/**
	 * Indicates if this Test Kind is an empty one
	 * 
	 * @return true in case of empty Test Kind, false otherwise
	 */
	boolean isNull();

	/**
	 * Returns an array of Runtime Classpath Entries for this Test Kind
	 *
	 * @return an array of Runtime Classpath Entries
	 */
	UnitTestRuntimeClasspathEntry[] getClasspathEntries();
}