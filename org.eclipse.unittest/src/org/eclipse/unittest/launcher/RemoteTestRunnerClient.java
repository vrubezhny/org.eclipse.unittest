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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import org.eclipse.unittest.UnitTestPlugin;

import org.eclipse.core.runtime.ISafeRunnable;

/**
 * The client side of the RemoteTestRunner. Handles the marshaling of the
 * different messages.
 */
public abstract class RemoteTestRunnerClient extends TestRunnerClient {

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

	private int fPort = -1;
	/**
	 * The server socket
	 */
	private ServerSocket fServerSocket;
	private Socket fSocket;
	protected String fLastLineDelimiter;
	protected InputStream fInputStream;
	protected PrintWriter fWriter;
	protected PushbackReader fPushbackReader;

	/**
	 * The protocol version
	 */
	protected String fVersion;

	@SuppressWarnings("hiding")
	protected boolean fDebug = false;

	/**
	 * Reads the message stream from the RemoteTestRunner
	 */
	private class ServerConnection extends Thread {
		int fServerPort;

		public ServerConnection(int port) {
			super("ServerConnection"); //$NON-NLS-1$
			fServerPort = port;
		}

		@Override
		public void run() {
			try {
				if (fDebug)
					System.out.println("Creating server socket " + fServerPort); //$NON-NLS-1$
				fServerSocket = new ServerSocket(fServerPort);
				fSocket = fServerSocket.accept();
				fPushbackReader = new PushbackReader(
						new BufferedReader(new InputStreamReader(fSocket.getInputStream(), StandardCharsets.UTF_8)));
				fWriter = new PrintWriter(new OutputStreamWriter(fSocket.getOutputStream(), StandardCharsets.UTF_8),
						true);
				String message;
				while (fPushbackReader != null && (message = readMessage(fPushbackReader)) != null)
					receiveMessage(message);
			} catch (SocketException e) {
				notifyTestRunTerminated();
			} catch (IOException e) {
				UnitTestPlugin.log(e);
				// fall through
			}
			shutDown();
		}
	}

	public RemoteTestRunnerClient(int port) {
		this.fPort = port;
		startListening();
	}

	private void startListening() {
		ServerConnection connection = new ServerConnection(fPort);
		connection.start();
	}

	abstract public void receiveMessage(String message);

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
			System.out.println("shutdown " + fPort); //$NON-NLS-1$

		if (fWriter != null) {
			fWriter.close();
			fWriter = null;
		}
		try {
			if (fPushbackReader != null) {
				fPushbackReader.close();
				fPushbackReader = null;
			}
		} catch (IOException e) {
			// Ignore
		}
		super.shutDown();
	}

	@Override
	public boolean isRunning() {
		return fSocket != null;
	}

	private String readMessage(PushbackReader in) throws IOException {
		StringBuilder buf = new StringBuilder(128);
		int ch;
		while ((ch = in.read()) != -1) {
			switch (ch) {
			case '\n':
				fLastLineDelimiter = "\n"; //$NON-NLS-1$
				return buf.toString();
			case '\r':
				ch = in.read();
				if (ch == '\n') {
					fLastLineDelimiter = "\r\n"; //$NON-NLS-1$
				} else {
					in.unread(ch);
					fLastLineDelimiter = "\r"; //$NON-NLS-1$
				}
				return buf.toString();
			default:
				buf.append((char) ch);
				break;
			}
		}
		fLastLineDelimiter = null;
		if (buf.length() == 0)
			return null;
		return buf.toString();
	}
}
