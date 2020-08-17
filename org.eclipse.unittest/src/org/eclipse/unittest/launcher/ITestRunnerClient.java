package org.eclipse.unittest.launcher;

import org.eclipse.unittest.internal.model.ITestRunListener2;

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
		public void startListening(ITestRunListener2[] listeners, int port) {
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
	 * Start listening to a test run. Start a server connection that
	 * the RemoteTestRunner can connect to.
	 *
	 * @param listeners listeners to inform
	 * @param port port on which the server socket will be opened
	 */
	void startListening(ITestRunListener2[] listeners, int port);

	void receiveMessage(String message);

	/**
	 * Requests to stop the remote test run.
	 */
	void stopTest();
	void stopWaiting();
	void shutDown();
}
