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

import org.eclipse.unittest.model.ITestRunListener3;

/**
 * An interface to be implemented by a Test Runner Client
 */
public interface ITestRunnerClient {
	ITestRunnerClient NULL = new ITestRunnerClient() {

		@Override
		public boolean isRunning() {
			return false;
		}

		@Override
		public void rerunTest(String testId, String className, String testName) {
			// do nothing
		}

		@Override
		public void startListening(int port) {
			// do nothing
		}

		@Override
		public void setListeners(ITestRunListener3[] listeners) {
			// do nothing
		}

		@Override
		public void receiveMessage(String message) {
			// do nothing
		}

		@Override
		public void stopTest() {
			// do nothing
		}

		@Override
		public void stopWaiting() {
			// do nothing
		}

		@Override
		public void shutDown() {
			// do nothing
		}
	};

	/**
	 * Indicates if a test run is in progress
	 *
	 * @return returns true in case of a test run in progress, otherwise false
	 */
	boolean isRunning();

	/**
	 * Requests to Re-Runs a specified test case
	 *
	 * @param testId    test element identifier
	 * @param className test type
	 * @param testName  test name
	 */
	void rerunTest(String testId, String className, String testName);

	/**
	 * Setup listeners for a test run.
	 *
	 * @param listeners listeners to inform
	 */
	void setListeners(ITestRunListener3[] listeners);

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
