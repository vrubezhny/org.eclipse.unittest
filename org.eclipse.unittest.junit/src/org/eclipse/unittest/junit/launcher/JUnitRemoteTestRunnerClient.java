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
package org.eclipse.unittest.junit.launcher;

import org.eclipse.unittest.UnitTestPlugin;
import org.eclipse.unittest.internal.model.ITestRunListener2;
import org.eclipse.unittest.internal.model.RemoteTestRunnerClient;

import org.eclipse.core.runtime.ISafeRunnable;

import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;

/**
 * The client side of the RemoteTestRunner. Handles the
 * marshaling of the different messages.
 */
public class JUnitRemoteTestRunnerClient extends RemoteTestRunnerClient {

	public abstract class ListenerSafeRunnable implements ISafeRunnable {
		@Override
		public void handleException(Throwable exception) {
			UnitTestPlugin.log(exception);
		}
	}
	/**
	 * A simple state machine to process requests from the RemoteTestRunner
	 */
	abstract class ProcessingState {
	    abstract ProcessingState readMessage(String message);
	}

	class DefaultProcessingState extends ProcessingState {
	    @Override
		ProcessingState readMessage(String message) {
	        if (message.startsWith(MessageIds.TRACE_START)) {
	        	fFailedTrace.setLength(0);
	            return fTraceState;
	        }
	        if (message.startsWith(MessageIds.EXPECTED_START)) {
	            fExpectedResult.setLength(0);
	            return fExpectedState;
	        }
	        if (message.startsWith(MessageIds.ACTUAL_START)) {
	            fActualResult.setLength(0);
	            return fActualState;
	        }
	        if (message.startsWith(MessageIds.RTRACE_START)) {
	            fFailedRerunTrace.setLength(0);
	            return fRerunState;
	        }
	        String arg= message.substring(MessageIds.MSG_HEADER_LENGTH);
	        if (message.startsWith(MessageIds.TEST_RUN_START)) {
	            // version < 2 format: count
	            // version >= 2 format: count+" "+version
	            int count= 0;
	            int v= arg.indexOf(' ');
	            if (v == -1) {
	                fVersion= "v1"; //$NON-NLS-1$
	                count= Integer.parseInt(arg);
	            } else {
	                fVersion= arg.substring(v+1);
	                String sc= arg.substring(0, v);
	                count= Integer.parseInt(sc);
	            }
	            notifyTestRunStarted(count);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_START)) {
	            notifyTestStarted(arg);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_END)) {
	            notifyTestEnded(arg);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_ERROR)) {
	            extractFailure(arg, ITestRunListener2.STATUS_ERROR);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_FAILED)) {
	            extractFailure(arg, ITestRunListener2.STATUS_FAILURE);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_RUN_END)) {
	            long elapsedTime = Long.parseLong(arg);
	            testRunEnded(elapsedTime);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_STOPPED)) {
	            long elapsedTime = Long.parseLong(arg);
	            notifyTestRunStopped(elapsedTime);
	            shutDown();
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_TREE)) {
	            notifyTestTreeEntry(arg);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_RERAN)) {
	            if (hasTestId())
	                scanReranMessage(arg);
	            else
	                scanOldReranMessage(arg);
	            return this;
	        }
	        return this;
	    }
	}

	/**
	 * Base class for states in which messages are appended to an internal
	 * string buffer until an end message is read.
	 */
	class AppendingProcessingState extends ProcessingState {
		private final StringBuffer fBuffer;
		private String fEndString;

		AppendingProcessingState(StringBuffer buffer, String endString) {
			this.fBuffer= buffer;
			this.fEndString = endString;
		}

		@Override
		ProcessingState readMessage(String message) {
			if (message.startsWith(fEndString)) {
				entireStringRead();
				return fDefaultState;
			}
			fBuffer.append(message);
			if (fLastLineDelimiter != null)
				fBuffer.append(fLastLineDelimiter);
			return this;
		}

		/**
		 * subclasses can override to do special things when end message is read
		 */
		void entireStringRead() {
		}
	}

	class TraceProcessingState extends AppendingProcessingState {
		TraceProcessingState() {
			super(fFailedTrace, MessageIds.TRACE_END);
		}

		@Override
		void entireStringRead() {
            notifyTestFailed();
            fExpectedResult.setLength(0);
            fActualResult.setLength(0);
		}

	    @Override
		ProcessingState readMessage(String message) {
	        if (message.startsWith(MessageIds.TRACE_END)) {
	            notifyTestFailed();
	            fFailedTrace.setLength(0);
	            fActualResult.setLength(0);
	            fExpectedResult.setLength(0);
	            return fDefaultState;
	        }
	        fFailedTrace.append(message);
	        if (fLastLineDelimiter != null)
	        	fFailedTrace.append(fLastLineDelimiter);
	        return this;
	    }
	}

	/**
	 * The failed trace that is currently reported from the RemoteTestRunner
	 */
//	private final StringBuffer fFailedTrace = new StringBuffer();
	/**
	 * The expected test result
	 */
//	private final StringBuffer fExpectedResult = new StringBuffer();
	/**
	 * The actual test result
	 */
//	private final StringBuffer fActualResult = new StringBuffer();
	/**
	 * The failed trace of a reran test
	 */
//	private final StringBuffer fFailedRerunTrace = new StringBuffer();


	ProcessingState fDefaultState= new DefaultProcessingState();
	ProcessingState fTraceState= new TraceProcessingState();
	ProcessingState fExpectedState= new AppendingProcessingState(fExpectedResult, MessageIds.EXPECTED_END);
	ProcessingState fActualState= new AppendingProcessingState(fActualResult, MessageIds.ACTUAL_END);
	ProcessingState fRerunState= new AppendingProcessingState(fFailedRerunTrace, MessageIds.RTRACE_END);
	ProcessingState fCurrentState= fDefaultState;

	/**
	 * An array of listeners that are informed about test events.
	 */
//	private ITestRunListener2[] fListeners;

	/**
	 * The server socket
	 */
	/*
	private ServerSocket fServerSocket;
	private Socket fSocket;
	private int fPort= -1;
	private PrintWriter fWriter;
	private PushbackReader fPushbackReader;
	private String fLastLineDelimiter;
	*/
	/**
	 * The protocol version
	 */
//	private String fVersion;
	/**
	 * The failed test that is currently reported from the RemoteTestRunner
	 */
//	private String fFailedTest;
	/**
	 * The Id of the failed test
	 */
//	private String fFailedTestId;
	/**
	 * The kind of failure of the test that is currently reported as failed
	 */
//	private int fFailureKind;

//	private boolean fDebug= false;

	/**
	 * Requests to stop the remote test run.
	 */
	@Override
	public synchronized void stopTest() {
		if (isRunning()) {
			fWriter.println(MessageIds.TEST_STOP);
			fWriter.flush();
		}
	}

	@Override
	public void receiveMessage(String message) {
	    fCurrentState= fCurrentState.readMessage(message);
	}

	private void scanOldReranMessage(String arg) {
		// OLD V1 format
		// format: className" "testName" "status
		// status: FAILURE, ERROR, OK
		int c= arg.indexOf(" "); //$NON-NLS-1$
		int t= arg.indexOf(" ", c+1); //$NON-NLS-1$
		String className= arg.substring(0, c);
		String testName= arg.substring(c+1, t);
		String status= arg.substring(t+1);
		String testId = className+testName;
		notifyTestReran(testId, className, testName, status);
	}

	private void scanReranMessage(String arg) {
		// format: testId" "className" "testName" "status
		// status: FAILURE, ERROR, OK
		int i= arg.indexOf(' ');
		int c= arg.indexOf(' ', i+1);
		int t; // special treatment, since testName can contain spaces:
		if (arg.endsWith(RemoteTestRunner.RERAN_ERROR)) {
			t= arg.length() - RemoteTestRunner.RERAN_ERROR.length() - 1;
		} else if (arg.endsWith(RemoteTestRunner.RERAN_FAILURE)) {
			t= arg.length() - RemoteTestRunner.RERAN_FAILURE.length() - 1;
		} else if (arg.endsWith(RemoteTestRunner.RERAN_OK)) {
			t= arg.length() - RemoteTestRunner.RERAN_OK.length() - 1;
		} else {
			t= arg.indexOf(' ', c+1);
		}
		String testId= arg.substring(0, i);
		String className= arg.substring(i+1, c);
		String testName= arg.substring(c+1, t);
		String status= arg.substring(t+1);
		notifyTestReran(testId, className, testName, status);
	}
/*
	private void notifyTestReran(String testId, String className, String testName, String status) {
		int statusCode= ITestRunListener2.STATUS_OK;
		if (status.equals("FAILURE")) //$NON-NLS-1$
			statusCode= ITestRunListener2.STATUS_FAILURE;
		else if (status.equals("ERROR")) //$NON-NLS-1$
			statusCode= ITestRunListener2.STATUS_ERROR;

		String trace= ""; //$NON-NLS-1$
		if (statusCode != ITestRunListener2.STATUS_OK)
			trace = fFailedRerunTrace.toString();
		// assumption a rerun trace was sent before
		notifyTestReran(testId, className, testName, statusCode, trace);
	}
*/
	@Override
	protected void extractFailure(String arg, int status) {
		String s[]= extractTestId(arg);
		fFailedTestId= s[0];
		fFailedTest= s[1];
		fFailureKind= status;
	}

	/**
	 * @param arg test name
	 * @return an array with two elements. The first one is the testId, the second one the testName.
	 */
	@Override
	protected String[] extractTestId(String arg) {
		String[] result= new String[2];
		if (!hasTestId()) {
			result[0]= arg; // use the test name as the test Id
			result[1]= arg;
			return result;
		}
		int i= arg.indexOf(',');
		result[0]= arg.substring(0, i);
		result[1]= arg.substring(i+1, arg.length());
		return result;
	}

	protected boolean hasTestId() {
		if (fVersion == null) // TODO fix me
			return true;
		return fVersion.equals("v2"); //$NON-NLS-1$
	}
/*
	private void notifyTestReran(final String testId, final String className, final String testName, final int statusCode, final String trace) {
		for (ITestRunListener2 listener : fListeners) {
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
	private void notifyTestTreeEntry(final String treeEntry) {
		for (ITestRunListener2 listener : fListeners) {
			if (!hasTestId())
				listener.testTreeEntry(fakeTestId(treeEntry));
			else
				listener.testTreeEntry(treeEntry);
		}
	}
*/

	private String fakeTestId(String treeEntry) {
		// extract the test name and add it as the testId
		int index0= treeEntry.indexOf(',');
		String testName= treeEntry.substring(0, index0).trim();
		return testName+","+treeEntry; //$NON-NLS-1$
	}
/*
	private void notifyTestRunStopped(final long elapsedTime) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunStopped(elapsedTime);
				}
			});
		}
	}
	private void testRunEnded(final long elapsedTime) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunEnded(elapsedTime);
				}
			});
		}
	}

	private void notifyTestEnded(final String test) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					String s[]= extractTestId(test);
					listener.testEnded(s[0], s[1]);
				}
			});
		}
	}

	private void notifyTestStarted(final String test) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					String s[]= extractTestId(test);
					listener.testStarted(s[0], s[1]);
				}
			});
		}
	}

	private void notifyTestRunStarted(final int count) {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunStarted(count);
				}
			});
		}
	}

	private void notifyTestFailed() {
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testFailed(fFailureKind, fFailedTestId,
						fFailedTest, fFailedTrace.toString(), nullifyEmpty(fExpectedResult), nullifyEmpty(fActualResult));
				}
			});
		}
	}
*/

	/**
	 * Returns a comparison result from the given buffer.
	 * Removes the terminating line delimiter.
	 *
	 * @param buf the comparison result
	 * @return the result or <code>null</code> if empty
	 * @since 3.7
	 */

	/*
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

	private void notifyTestRunTerminated() {
		// fix for 77771 RemoteTestRunnerClient doing work after junit shutdown [JUnit]
		if (UnitTestPlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunTerminated();
				}
			});
		}
	}
*/
	public void rerunTest(String testId, String className, String testName) {
		if (isRunning()) {
			fActualResult.setLength(0);
			fExpectedResult.setLength(0);
			fWriter.println(MessageIds.TEST_RERUN+testId+" "+className+" "+testName); //$NON-NLS-1$ //$NON-NLS-2$
			fWriter.flush();
		}
	}
}
