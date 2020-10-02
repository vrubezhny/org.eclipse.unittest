/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.unittest.launcher;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;

import org.osgi.framework.Bundle;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.model.ITestRunListener;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;

/**
 * The client side of the RemoteTestRunner. Handles the marshaling of the
 * different messages.
 */
public abstract class TestRunnerClient implements ITestRunnerClient {

	public abstract class ListenerSafeRunnable implements ISafeRunnable {
		@Override
		public void handleException(Throwable exception) {
			UnitTestPlugin.log(exception);
		}
	}

	/**
	 * An array of listeners that are informed about test events.
	 */
	protected ITestRunListener[] fListeners;

	/**
	 * The server socket
	 */
	protected ServerSocket fServerSocket;
	protected Socket fSocket;

	protected boolean fDebug = false;

	@Override
	public void setListeners(ITestRunListener[] listeners) {
		this.fListeners = listeners;
	}

	@Override
	abstract public void stopTest();

	@Override
	public synchronized void stopWaiting() {
		if (fServerSocket != null && !fServerSocket.isClosed() && fSocket == null) {
			shutDown(); // will throw a SocketException in Threads that wait in ServerSocket#accept()
		}
	}

	@Override
	public synchronized void shutDown() {
		if (fDebug)
			System.out.println("shutdown"); //$NON-NLS-1$

		try {
			if (fSocket != null) {
				fSocket.close();
				fSocket = null;
			}
		} catch (IOException e) {
			// Ignore
		}
		try {
			if (fServerSocket != null) {
				fServerSocket.close();
				fServerSocket = null;
			}
		} catch (IOException e) {
			// Ignore
		}
	}

	@Override
	public boolean isRunning() {
		return fSocket != null;
	}

	/**
	 * Notifies on an individual test re-run.
	 *
	 * @param testId     a unique Id identifying the test
	 * @param className  the name of the test class that was rerun
	 * @param testName   the name of the test that was rerun
	 * @param statusCode the outcome of the test that was rerun; one of
	 *                   {@link ITestRunListener#STATUS_OK},
	 *                   {@link ITestRunListener#STATUS_ERROR}, or
	 *                   {@link ITestRunListener#STATUS_FAILURE}
	 * @param trace      the stack trace in the case of abnormal termination, or the
	 *                   empty string if none
	 * @param expected   the expected value in case of abnormal termination, or the
	 *                   empty string if none
	 * @param actual     the actual value in case of abnormal termination, or the
	 *                   empty string if none
	 */
	protected void notifyTestReran(String testId, String className, String testName, int statusCode, String trace,
			String expected, String actual) {
		for (ITestRunListener listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testReran(testId, className, testName, statusCode, trace, expected, actual);
				}
			});
		}
	}

	/**
	 * Notifies on a member of the test suite that is about to be run.
	 *
	 * @param testId         a unique id for the test
	 * @param testName       the name of the test
	 * @param isSuite        true or false depending on whether the test is a suite
	 * @param testCount      an integer indicating the number of tests
	 * @param isDynamicTest  true or false
	 * @param parentId       the unique testId of its parent if it is a dynamic
	 *                       test, otherwise can be "-1"
	 * @param displayName    the display name of the test
	 * @param parameterTypes comma-separated list of method parameter types if
	 *                       applicable, otherwise an empty string
	 * @param uniqueId       the unique ID of the test provided, otherwise an empty
	 *                       string
	 */
	protected void notifyTestTreeEntry(String testId, String testName, boolean isSuite, int testCount,
			boolean isDynamicTest, String parentId, String displayName, String[] parameterTypes, String uniqueId) {
		for (ITestRunListener listener : fListeners) {
			listener.testTreeEntry(testId, testName, isSuite, testCount, isDynamicTest, parentId, displayName,
					parameterTypes, uniqueId);
		}
	}

	/**
	 * Notifies on a test run stopped.
	 *
	 * @param duration the total elapsed time of the test run
	 */
	protected void notifyTestRunStopped(final Duration duration) {
		if (isStopped())
			return;
		for (ITestRunListener listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunStopped(duration);
				}
			});
		}
	}

	/**
	 * Notifies on a test run ended.
	 *
	 * @param duration the total elapsed time of the test run
	 */
	protected void notifyTestRunEnded(final Duration duration) {
		if (isStopped())
			return;
		for (ITestRunListener listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunEnded(duration);
				}
			});
		}
	}

	/**
	 * Notifies on an individual test ended.
	 *
	 * @param testId    a unique Id identifying the test
	 * @param testName  the name of the test that failed
	 * @param isIgnored <code>true</code> indicates that the specified test was
	 *                  ignored, otherwise - <code>false</code>
	 */
	protected void notifyTestEnded(String testId, String testName, boolean isIgnored) {
		if (isStopped())
			return;
		for (ITestRunListener listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testEnded(testId, testName, isIgnored);
				}
			});
		}
	}

	/**
	 * Notifies on an individual test started.
	 *
	 * @param testId   a unique Id identifying the test
	 * @param testName the name of the test that started
	 */
	protected void notifyTestStarted(final String testId, final String testName) {
		if (isStopped())
			return;
		for (ITestRunListener listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testStarted(testId, testName);
				}
			});
		}
	}

	/**
	 * Notifies on a test run started.
	 *
	 * @param count the number of individual tests that will be run
	 */
	protected void notifyTestRunStarted(final int count) {
		if (isStopped())
			return;
		for (ITestRunListener listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunStarted(count);
				}
			});
		}
	}

	/**
	 * Notifies on an individual test failed with a stack trace.
	 *
	 * @param status             the outcome of the test; one of
	 *                           {@link ITestRunListener#STATUS_ERROR STATUS_ERROR}
	 *                           or {@link ITestRunListener#STATUS_FAILURE
	 *                           STATUS_FAILURE}
	 * @param testId             a unique Id identifying the test
	 * @param testName           the name of the test that failed
	 * @param isAssumptionFailed indicates that an assumption is failed
	 * @param trace              the stack trace
	 * @param expected           the expected value
	 * @param actual             the actual value
	 */
	protected void notifyTestFailed(int status, String testId, String testName, boolean isAssumptionFailed,
			String trace, String expected, String actual) {
		if (isStopped())
			return;
		for (ITestRunListener listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testFailed(status, testId, testName, isAssumptionFailed, trace, expected, actual);
				}
			});
		}
	}

	/**
	 * Returns a comparison result from the given buffer. Removes the terminating
	 * line delimiter.
	 *
	 * @param buf the comparison result
	 * @return the result or <code>null</code> if empty
	 */
	public static String nullifyEmpty(StringBuilder buf) {
		int length = buf.length();
		if (length == 0)
			return null;

		char last = buf.charAt(length - 1);
		if (last == '\n') {
			if (length > 1 && buf.charAt(length - 2) == '\r')
				return buf.substring(0, length - 2);
			else
				return buf.substring(0, length - 1);
		} else if (last == '\r') {
			return buf.substring(0, length - 1);
		}
		return buf.toString();
	}

	/**
	 * Notifies on a test run terminated.
	 */
	protected void notifyTestRunTerminated() {
		if (isStopped())
			return;
		for (ITestRunListener listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunTerminated();
				}
			});
		}
	}

	private boolean isStopped() {
		switch (UnitTestPlugin.getDefault().getBundle().getState()) {
		case Bundle.ACTIVE:
		case Bundle.STARTING:
			return false;
		default:
			return true;
		}
	}
}
