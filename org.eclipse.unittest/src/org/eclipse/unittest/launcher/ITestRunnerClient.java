package org.eclipse.unittest.launcher;

import org.eclipse.unittest.model.ITestRunListener3;

public interface ITestRunnerClient {
	ITestRunnerClient NULL= new ITestRunnerClient() {

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

	boolean isRunning();
	void rerunTest(String testId, String className, String testName);

	/**
	 * Setup listeners for a test run.
	 *
	 * @param listeners listeners to inform
	 */
	void setListeners(ITestRunListener3[] listeners);

	/**
	 * Start listening to a test run. Start a server connection that
	 * the RemoteTestRunner can connect to.
	 *
	 * @param port Port to setup a server connection.
	 */
	void startListening(int port);

	void receiveMessage(String message);

	/**
	 * Requests to stop the remote test run.
	 */
	void stopTest();
	void stopWaiting();
	void shutDown();
}
