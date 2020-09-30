/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.unittest.internal.launcher;

import org.eclipse.unittest.TestRunListener;
import org.eclipse.unittest.UnitTestPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;

/**
 * Test View Support registry
 */
public class TestListenerRegistry {
	public static final String ID_EXTENSION_POINT_TESTRUN_LISTENERS = UnitTestPlugin.PLUGIN_ID + "." //$NON-NLS-1$
			+ "testRunListeners"; //$NON-NLS-1$

	public static TestListenerRegistry getDefault() {
		if (fgRegistry != null)
			return fgRegistry;

		fgRegistry = new TestListenerRegistry();
		return fgRegistry;
	}

	private static TestListenerRegistry fgRegistry;

	/**
	 * List storing the registered test run listeners
	 */
	private ListenerList<TestRunListener> fUnitTestRunListeners = new ListenerList<>();

	private TestListenerRegistry() {
	}

	/**
	 * @return a <code>ListenerList</code> of all <code>TestRunListener</code>s
	 */
	public ListenerList<TestRunListener> getUnitTestRunListeners() {
		loadUnitTestRunListeners();
		return fUnitTestRunListeners;
	}

	/**
	 * Initializes TestRun Listener extensions
	 */
	private synchronized void loadUnitTestRunListeners() {
		if (!fUnitTestRunListeners.isEmpty()) {
			return;
		}

		IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
				.getExtensionPoint(ID_EXTENSION_POINT_TESTRUN_LISTENERS);
		if (extensionPoint == null) {
			return;
		}
		IConfigurationElement[] configs = extensionPoint.getConfigurationElements();
		MultiStatus status = new MultiStatus(UnitTestPlugin.PLUGIN_ID, IStatus.OK,
				"Could not load some testRunner extension points", //$NON-NLS-1$
				null);
		for (IConfigurationElement config : configs) {
			try {
				Object testRunListener = config.createExecutableExtension("class"); //$NON-NLS-1$
				if (testRunListener instanceof TestRunListener) {
					fUnitTestRunListeners.add((TestRunListener) testRunListener);
				}
			} catch (CoreException e) {
				status.add(e.getStatus());
			}
		}
		if (!status.isOK()) {
			UnitTestPlugin.log(status);
		}
	}

}
