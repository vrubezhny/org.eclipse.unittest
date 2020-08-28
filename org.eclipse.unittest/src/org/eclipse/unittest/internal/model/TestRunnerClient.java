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
 *     Julien Ruaux: jruaux@octo.com
 * 	   Vincent Massol: vmassol@octo.com
 *******************************************************************************/
package org.eclipse.unittest.internal.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.unittest.UnitTestPlugin;
import org.eclipse.unittest.launcher.ITestRunnerClient;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;

/**
 * The client side of the RemoteTestRunner. Handles the
 * marshaling of the different messages.
 */
public abstract class TestRunnerClient implements ITestRunnerClient {

	public abstract class ListenerSafeRunnable implements ISafeRunnable {
		@Override
		public void handleException(Throwable exception) {
			UnitTestPlugin.log(exception);
		}
	}

	/**
	 * The failed trace that is currently reported from the RemoteTestRunner
	 */
	protected final StringBuffer fFailedTrace = new StringBuffer();
	/**
	 * The expected test result
	 */
	protected final StringBuffer fExpectedResult = new StringBuffer();
	/**
	 * The actual test result
	 */
	protected final StringBuffer fActualResult = new StringBuffer();
	/**
	 * The failed trace of a reran test
	 */
	protected final StringBuffer fFailedRerunTrace = new StringBuffer();

	/**
	 * An array of listeners that are informed about test events.
	 */
	protected ITestRunListener3[] fListeners;

	/**
	 * The server socket
	 */
	private ServerSocket fServerSocket;
	private Socket fSocket;
	protected int fPort= -1;
	protected InputStream fInputStream;
	protected PrintWriter fWriter;
	private PushbackReader fPushbackReader;
	protected String fLastLineDelimiter;
	/**
	 * The protocol version
	 */
	protected String fVersion;
	/**
	 * The failed test that is currently reported from the RemoteTestRunner
	 */
	protected String fFailedTest;
	/**
	 * The Id of the failed test
	 */
	protected String fFailedTestId;
	/**
	 * The kind of failure of the test that is currently reported as failed
	 */
	protected int fFailureKind;

	protected boolean fDebug= false;


	@Override
	public void setListeners(ITestRunListener3[] listeners) {
		this.fListeners = listeners;
	}

	@Override
	abstract public void startListening(int port);

	@Override
	abstract public void receiveMessage(String message);

	@Override
	abstract public void stopTest();

	@Override
	public synchronized void stopWaiting() {
		if (fServerSocket != null  && ! fServerSocket.isClosed() && fSocket == null) {
			shutDown(); // will throw a SocketException in Threads that wait in ServerSocket#accept()
		}
	}

	@Override
	public synchronized void shutDown() {
		if (fDebug)
			System.out.println("shutdown "+fPort); //$NON-NLS-1$

		if (fWriter != null) {
			fWriter.close();
			fWriter= null;
		}
		try {
			if (fPushbackReader != null) {
				fPushbackReader.close();
				fPushbackReader= null;
			}
		} catch(IOException e) {
		}
		try {
			if (fSocket != null) {
				fSocket.close();
				fSocket= null;
			}
		} catch(IOException e) {
		}
		try{
			if (fServerSocket != null) {
				fServerSocket.close();
				fServerSocket= null;
			}
		} catch(IOException e) {
		}
	}

	@Override
	public boolean isRunning() {
		return fSocket != null;
	}

	private String readMessage(PushbackReader in) throws IOException {
		StringBuilder buf= new StringBuilder(128);
		int ch;
		while ((ch= in.read()) != -1) {
			switch (ch) {
			case '\n':
				fLastLineDelimiter= "\n"; //$NON-NLS-1$
				return buf.toString();
			case '\r':
				ch= in.read();
				if (ch == '\n') {
					fLastLineDelimiter= "\r\n"; //$NON-NLS-1$
				} else {
					in.unread(ch);
					fLastLineDelimiter= "\r"; //$NON-NLS-1$
				}
				return buf.toString();
			default:
				buf.append((char) ch);
				break;
			}
		}
		fLastLineDelimiter= null;
		if (buf.length() == 0)
			return null;
		return buf.toString();
	}

	protected void notifyTestReran(String testId, String className, String testName, String status) {
		int statusCode= ITestRunListener3.STATUS_OK;
		if (status.equals("FAILURE")) //$NON-NLS-1$
			statusCode= ITestRunListener3.STATUS_FAILURE;
		else if (status.equals("ERROR")) //$NON-NLS-1$
			statusCode= ITestRunListener3.STATUS_ERROR;

		String trace= ""; //$NON-NLS-1$
		if (statusCode != ITestRunListener3.STATUS_OK)
			trace = fFailedRerunTrace.toString();
		// assumption a rerun trace was sent before
		notifyTestReran(testId, className, testName, statusCode, trace);
	}

	abstract protected void extractFailure(String arg, int status);

	/**
	 * @param arg test name
	 * @return an array with two elements. The first one is the testId, the second one the testName.
	 */
//	abstract protected String[] extractTestId(String arg);

//	abstract protected boolean hasTestId();

	protected void notifyTestReran(final String testId, final String className, final String testName, final int statusCode, final String trace) {
		for (ITestRunListener3 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testReran(testId,
						className, testName, statusCode, trace,
						nullifyEmpty(fExpectedResult), nullifyEmpty(fActualResult));
				}
			});
		}
	}

	protected void notifyTestTreeEntry(final String testId, final String testName, final boolean isSuite, final int testCount,
			final boolean isDynamicTest, final String parentId, final String displayName, final String[] parameterTypes, final String uniqueId) {
		for (ITestRunListener3 listener : fListeners) {
			listener.testTreeEntry(testId, testName, isSuite, testCount, isDynamicTest,
					parentId, displayName, parameterTypes, uniqueId);

/*
			if (!hasTestId())
				listener.testTreeEntry(fakeTestId(treeEntry));
			else
				listener.testTreeEntry(treeEntry);
*/
		}
	}
/*
	private String fakeTestId(String treeEntry) {
		// extract the test name and add it as the testId
		int index0= treeEntry.indexOf(',');
		String testName= treeEntry.substring(0, index0).trim();
		return testName+","+treeEntry; //$NON-NLS-1$
	}
*/
	protected void notifyTestRunStopped(final long elapsedTime) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener3 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunStopped(elapsedTime);
				}
			});
		}
	}

	protected void notifyTestRunEnded(final long elapsedTime) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener3 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunEnded(elapsedTime);
				}
			});
		}
	}

	protected void notifyTestEnded(final String testId, final String testName) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener3 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testEnded(testId, testName);
				}
			});
		}
	}
	/*
	protected void notifyTestEnded(final String test) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener3 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					String s[]= extractTestId(test);
					listener.testEnded(s[0], s[1]);
				}
			});
		}
	}
	*/
	protected void notifyTestStarted(final String testId, final String testName) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener3 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testStarted(testId, testName);
				}
			});
		}
	}
/*
	protected void notifyTestStarted(final String test) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener3 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					String s[]= extractTestId(test);
					listener.testStarted(s[0], s[1]);
				}
			});
		}
	}
*/
	protected void notifyTestRunStarted(final int count) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener3 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunStarted(count);
				}
			});
		}
	}

	protected void notifyTestFailed() {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener3 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testFailed(fFailureKind, fFailedTestId,
						fFailedTest, fFailedTrace.toString(), nullifyEmpty(fExpectedResult), nullifyEmpty(fActualResult));
				}
			});
		}
	}

	/**
	 * Returns a comparison result from the given buffer.
	 * Removes the terminating line delimiter.
	 *
	 * @param buf the comparison result
	 * @return the result or <code>null</code> if empty
	 * @since 3.7
	 */
	private static String nullifyEmpty(StringBuffer buf) {
		int length= buf.length();
		if (length == 0)
			return null;

		char last= buf.charAt(length - 1);
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

	protected void notifyTestRunTerminated() {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener3 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunTerminated();
				}
			});
		}
	}

	@Override
	abstract public void rerunTest(String testId, String className, String testName);
}
