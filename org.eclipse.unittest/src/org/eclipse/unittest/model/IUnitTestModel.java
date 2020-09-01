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

import org.eclipse.core.resources.IProject;

import org.eclipse.debug.core.ILaunch;

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

	void addTestRunSessionListener(ITestRunSessionListener listener);

	void removeTestRunSessionListener(ITestRunSessionListener listener);

	ITestRunSession importTestRunSession(String url, IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException;

	ITestRunSession createTestRunSession(ILaunch launch, IProject project, int port);
}
