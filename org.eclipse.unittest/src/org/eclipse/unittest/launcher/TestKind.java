/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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

import org.eclipse.unittest.UnitTestPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class TestKind implements ITestKind {

	private final IConfigurationElement fElement;
	private ITestFinder fFinder;
	private ITestRunnerClient fTestRunnerClient;
	private ITestViewSupport fTestViewSupport;

	public TestKind(IConfigurationElement element) {
		fElement = element;
		fFinder= null;
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestKind#createFinder()
	 */
	@Override
	public ITestFinder getFinder() {
		if (fFinder == null) {
			try {
				fFinder= (ITestFinder) fElement.createExecutableExtension(FINDER_CLASS_NAME);
			} catch (CoreException e1) {
				UnitTestPlugin.log(e1);
				fFinder= ITestFinder.NULL;
			}
		}
		return fFinder;
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestKind#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return getAttribute(DISPLAY_NAME);
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestKind#getFinderClassName()
	 */
	@Override
	public String getFinderClassName() {
		return getAttribute(FINDER_CLASS_NAME);
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestKind#getId()
	 */
	@Override
	public String getId() {
		return getAttribute(ID);
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestKind#getLoaderClassName()
	 */
	@Override
	public String getLoaderClassName() {
		return getAttribute(LOADER_CLASS_NAME);
	}

	@Override
	public String getLoaderPluginId() {
		return getAttribute(LOADER_PLUGIN_ID);
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestKind#getLoaderClassName()
	 */
	@Override
	public String getTestRunnerClientClassName() {
		return getAttribute(TEST_RUNNER_CLIENT_CLASS_NAME);
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestKind#createFinder()
	 */
	@Override
	public ITestRunnerClient getTestRunnerClient() {
		if (fTestRunnerClient == null) {
			try {
				fTestRunnerClient= (ITestRunnerClient) fElement.createExecutableExtension(TEST_RUNNER_CLIENT_CLASS_NAME);
			} catch (CoreException e1) {
				UnitTestPlugin.log(e1);
				fTestRunnerClient= ITestRunnerClient.NULL;
			}
		}
		return fTestRunnerClient;
	}

	@Override
	public ITestViewSupport getTestViewSupport() {
		if (fTestViewSupport == null) {
			try {
				fTestViewSupport= (ITestViewSupport) fElement.createExecutableExtension(TEST_VIEW_SUPPORT_CLASS_NAME);
			} catch (CoreException e1) {
				UnitTestPlugin.log(e1);
				fTestViewSupport= ITestViewSupport.NULL;
			}
		}
		return fTestViewSupport;
	}

	@Override
	public String getTestViewSupportClassName() {
		return getAttribute(TEST_VIEW_SUPPORT_CLASS_NAME);
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestKind#getPrecededKindId()
	 */
	@Override
	public String getPrecededKindId() {
		String attribute= getAttribute(PRECEDES);
		return attribute == null ? "" : attribute; //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestKind#isNull()
	 */
	@Override
	public boolean isNull() {
		return false;
	}

	protected String getAttribute(String attributeName) {
		return fElement.getAttribute(attributeName);
	}

	boolean precedes(ITestKind otherKind) {
		final String precededKindId = getPrecededKindId();
		String[] ids = precededKindId.split(","); //$NON-NLS-1$
		for (String id : ids) {
			if (id.equals(otherKind.getId())) {
				return true;
			}
		}
		return false;
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestKind#getClasspathEntries()
	 */
	@Override
	public UnitTestRuntimeClasspathEntry[] getClasspathEntries() {
		IConfigurationElement[] children= fElement.getChildren(ITestKind.RUNTIME_CLASSPATH_ENTRY);
		UnitTestRuntimeClasspathEntry[] returnThis= new UnitTestRuntimeClasspathEntry[children.length];
		for (int i= 0; i < children.length; i++) {
			IConfigurationElement element= children[i];
			String pluginID= element.getAttribute(ITestKind.CLASSPATH_PLUGIN_ID);
			String pathToJar= element.getAttribute(ITestKind.CLASSPATH_PATH_TO_JAR);
			returnThis[i]= new UnitTestRuntimeClasspathEntry(pluginID, pathToJar);
		}
		return returnThis;
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getDisplayName() + " (id: " + getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
