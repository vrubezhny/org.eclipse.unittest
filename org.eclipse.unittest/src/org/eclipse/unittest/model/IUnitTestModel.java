/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.unittest.model;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.unittest.internal.model.TestRunSession;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.debug.core.ILaunch;

/**
 * Represents a Unit Test Model element.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 */
public interface IUnitTestModel {

	/**
	 * Adds the given {@link ITestRunSession} and notifies all registered
	 * {@link ITestRunSessionListener}s.
	 *
	 * @param testRunSession the session to add
	 */
	void addTestRunSession(ITestRunSession testRunSession);

	/**
	 * @return a list of active {@link TestRunSession}s. The list is a copy of the
	 *         internal data structure and modifications do not affect the global
	 *         list of active sessions. The list is sorted by age, youngest first.
	 */
	List<ITestRunSession> getTestRunSessions();

	/**
	 * Adds an {@link ITestRunSessionListener}
	 *
	 * @param listener an {@link ITestRunSessionListener} instance
	 */
	void addTestRunSessionListener(ITestRunSessionListener listener);

	/**
	 * Removes an {@link ITestRunSessionListener} instance.
	 *
	 * @param listener an {@link ITestRunSessionListener} instance to remove
	 */
	void removeTestRunSessionListener(ITestRunSessionListener listener);

	/**
	 * Loads an {@link ITestRunSession} from a saved copy
	 *
	 * @param url     an URL of the test run session saved copy
	 * @param monitor an {@link IProgressMonitor} monitor
	 * @return a loaded {@link ITestRunSession} instance
	 * @throws InvocationTargetException in case of an error
	 * @throws InterruptedException      in case of operation is canceled
	 */
	ITestRunSession importTestRunSession(String url, IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException;

	/**
	 * Creates an {@link ITestRunSession}
	 *
	 * @param launch an {@link ILaunch} to start a test run session
	 * @param port   a port number to listen during the run of remote test runner or
	 *               <code>-1</code> in case of a local test runner
	 * @return a created {@link ITestRunSession} instance
	 */
	ITestRunSession createTestRunSession(ILaunch launch, int port);
}
