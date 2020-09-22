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
package org.eclipse.unittest.launcher;

import org.eclipse.unittest.model.ITestRunListener;

/**
 * An interface to be implemented by a Test Runner Client
 */
public interface ITestRunnerClient {

	/**
	 * Indicates if a test run is in progress
	 *
	 * @return returns true in case of a test run in progress, otherwise false
	 */
	boolean isRunning();

	/**
	 * Setup listeners for a test run.
	 *
	 * @param listeners listeners to inform
	 */
	void setListeners(ITestRunListener[] listeners);

	/**
	 * Start listening to a test run. Start a server connection that the
	 * RemoteTestRunner can connect to.
	 *
	 * @param port Port to setup a server connection.
	 */
	void startListening(int port);

	/**
	 * Receives parses a received message from a test runner
	 *
	 * @param message a message received from a test runner
	 */
	void receiveMessage(String message);

	/**
	 * Requests to stop the remote test run.
	 */
	void stopTest();

	/**
	 * Requests to stop waiting for test runner messages
	 */
	void stopWaiting();

	/**
	 * Requests to shutdown a rest run
	 */
	void shutDown();
}
