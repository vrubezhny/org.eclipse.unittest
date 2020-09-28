/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.unittest.internal.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.eclipse.unittest.TestRunListener;
import org.eclipse.unittest.UnitTestPlugin;
import org.eclipse.unittest.internal.UnitTestPreferencesConstants;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.launcher.ITestViewSupport;
import org.eclipse.unittest.launcher.UnitTestLaunchConfigurationConstants;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestRunSessionListener;
import org.eclipse.unittest.model.IUnitTestModel;
import org.eclipse.unittest.model.RemoteTestRunnerClient;
import org.eclipse.unittest.ui.BasicElementLabels;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;

/**
 * Central registry for Unit Test test runs.
 */
public final class UnitTestModel implements IUnitTestModel {

	private final class UnitTestLaunchListener implements ILaunchListener {

		/**
		 * Used to track new launches. We need to do this so that we only attach a
		 * TestRunner once to a launch. Once a test runner is connected, it is removed
		 * from the set.
		 */
		private HashSet<ILaunch> fTrackedLaunches = new HashSet<>(20);

		@Override
		public void launchAdded(ILaunch launch) {
			fTrackedLaunches.add(launch);
		}

		@Override
		public void launchRemoved(final ILaunch launch) {
			fTrackedLaunches.remove(launch);
		}

		@Override
		public void launchChanged(final ILaunch launch) {
			if (!fTrackedLaunches.contains(launch))
				return;

			ILaunchConfiguration config = launch.getLaunchConfiguration();
			if (config == null)
				return;

			try {
				if (!config.hasAttribute(UnitTestLaunchConfigurationConstants.ATTR_UNIT_TEST_VIEW_SUPPORT)) {
					return;
				}
			} catch (CoreException e1) {
				UnitTestPlugin.log(e1);
				return;
			}

			// This testRunnerViewSupport and testRunnerClient instances are not retained
			// (just
			// there for testing)
			// so it's ok instantiating them.
			ITestViewSupport testRunnerViewSupport = UnitTestLaunchConfigurationConstants
					.newTestRunnerViewSupport(config);
			ITestRunnerClient testRunnerClient = testRunnerViewSupport != null
					? testRunnerViewSupport.getTestRunnerClient()
					: null;

			// If a Remote Test Runner Client exists try to create a new Test Run Session,
			// connect the Remote Test Runner and listen it
			// Otherwize, it is expected that the Test Runner Process will be created
			// through
			// <pre><code>org.eclipse.debug.core.processFactories</code></pre> extension
			// point
			// and the factory will take care of creating the Test Run Session.
			//
			if (testRunnerClient instanceof RemoteTestRunnerClient) {
				String portStr = launch.getAttribute(UnitTestLaunchConfigurationConstants.ATTR_PORT);
				if (portStr == null)
					return;
				try {
					final int port = Integer.parseInt(portStr);
					fTrackedLaunches.remove(launch);
					connectTestRunner(launch, port);
				} catch (NumberFormatException e) {
					UnitTestPlugin.log(e);
					return;
				}
			}
		}

		private void connectTestRunner(ILaunch launch, int port) {
			TestRunSession testRunSession = new TestRunSession(launch, port);
			addTestRunSession(testRunSession);

			for (TestRunListener listener : UnitTestPlugin.getDefault().getUnitTestRunListeners()) {
				listener.sessionLaunched(testRunSession);
			}
		}
	}

	private final ListenerList<ITestRunSessionListener> fTestRunSessionListeners = new ListenerList<>();
	/**
	 * Active test run sessions, youngest first.
	 */
	private final LinkedList<ITestRunSession> fTestRunSessions = new LinkedList<>();
	private final ILaunchListener fLaunchListener = new UnitTestLaunchListener();

	/**
	 * Starts the model (called by the {@link UnitTestPlugin} on startup).
	 */
	public void start() {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchListener(fLaunchListener);

		/*
		 * TODO: restore on restart: - only import headers! - only import last n
		 * sessions; remove all other files in historyDirectory
		 */
//		File historyDirectory= UnitTestPlugin.getHistoryDirectory();
//		File[] swapFiles= historyDirectory.listFiles();
//		if (swapFiles != null) {
//			Arrays.sort(swapFiles, new Comparator() {
//				public int compare(Object o1, Object o2) {
//					String name1= ((File) o1).getName();
//					String name2= ((File) o2).getName();
//					return name1.compareTo(name2);
//				}
//			});
//			for (int i= 0; i < swapFiles.length; i++) {
//				final File file= swapFiles[i];
//				SafeRunner.run(new ISafeRunnable() {
//					public void run() throws Exception {
//						importTestRunSession(file );
//					}
//					public void handleException(Throwable exception) {
//						UnitTestPlugin.log(exception);
//					}
//				});
//			}
//		}

//		addTestRunSessionListener(new LegacyTestRunSessionListener());
	}

	/**
	 * Stops the model (called by the {@link UnitTestPlugin} on shutdown).
	 */
	public void stop() {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.removeLaunchListener(fLaunchListener);

		File historyDirectory = UnitTestPlugin.getHistoryDirectory();
		File[] swapFiles = historyDirectory.listFiles();
		if (swapFiles != null) {
			for (File swapFile : swapFiles) {
				swapFile.delete();
			}
		}

//		for (Iterator iter= fTestRunSessions.iterator(); iter.hasNext();) {
//			final TestRunSession session= (TestRunSession) iter.next();
//			SafeRunner.run(new ISafeRunnable() {
//				public void run() throws Exception {
//					session.swapOut();
//				}
//				public void handleException(Throwable exception) {
//					UnitTestPlugin.log(exception);
//				}
//			});
//		}
	}

	@Override
	public void addTestRunSessionListener(ITestRunSessionListener listener) {
		fTestRunSessionListeners.add(listener);
	}

	@Override
	public void removeTestRunSessionListener(ITestRunSessionListener listener) {
		fTestRunSessionListeners.remove(listener);
	}

	@Override
	public synchronized List<ITestRunSession> getTestRunSessions() {
		return new ArrayList<>(fTestRunSessions);
	}

	@Override
	public void addTestRunSession(ITestRunSession testRunSession) {
		Assert.isNotNull(testRunSession);
		ArrayList<ITestRunSession> toRemove = new ArrayList<>();

		synchronized (this) {
			Assert.isLegal(!fTestRunSessions.contains(testRunSession));
			fTestRunSessions.addFirst(testRunSession);

			int maxCount = Platform.getPreferencesService().getInt(UnitTestPlugin.PLUGIN_ID,
					UnitTestPreferencesConstants.MAX_TEST_RUNS, 10, null);
			int size = fTestRunSessions.size();
			if (size > maxCount) {
				List<ITestRunSession> excess = fTestRunSessions.subList(maxCount, size);
				for (Iterator<ITestRunSession> iter = excess.iterator(); iter.hasNext();) {
					ITestRunSession oldSession = iter.next();
					if (!(oldSession.isStarting() || oldSession.isRunning() || oldSession.isKeptAlive())) {
						toRemove.add(oldSession);
						iter.remove();
					}
				}
			}
		}

		for (ITestRunSession oldSession : toRemove) {
			notifyTestRunSessionRemoved(oldSession);
		}
		notifyTestRunSessionAdded(testRunSession);
	}

	/**
	 * Imports a test run session from the given file.
	 *
	 * @param file a file containing a test run session transcript
	 * @return the imported test run session
	 * @throws CoreException if the import failed
	 */
	public static TestRunSession importTestRunSession(File file) throws CoreException {
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
//			parserFactory.setValidating(true); // TODO: add DTD and debug flag
			SAXParser parser = parserFactory.newSAXParser();
			TestRunHandler handler = new TestRunHandler();
			parser.parse(file, handler);
			TestRunSession session = handler.getTestRunSession();
			UnitTestPlugin.getModel().addTestRunSession(session);
			return session;
		} catch (ParserConfigurationException e) {
			throwImportError(file, e);
		} catch (SAXException e) {
			throwImportError(file, e);
		} catch (IOException e) {
			throwImportError(file, e);
		} catch (IllegalArgumentException e) {
			// Bug in parser: can throw IAE even if file is not null
			throwImportError(file, e);
		}
		return null; // does not happen
	}

	@Override
	public ITestRunSession importTestRunSession(String url, IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		monitor.beginTask(ModelMessages.UnitTestModel_importing_from_url, IProgressMonitor.UNKNOWN);
		final String trimmedUrl = url.trim().replaceAll("\r\n?|\n", ""); //$NON-NLS-1$ //$NON-NLS-2$
		final TestRunHandler handler = new TestRunHandler(monitor);

		final CoreException[] exception = { null };
		final TestRunSession[] session = { null };

		Thread importThread = new Thread("UnitTest URL importer") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					SAXParserFactory parserFactory = SAXParserFactory.newInstance();
//					parserFactory.setValidating(true); // TODO: add DTD and debug flag
					SAXParser parser = parserFactory.newSAXParser();
					parser.parse(trimmedUrl, handler);
					session[0] = handler.getTestRunSession();
				} catch (OperationCanceledException e) {
					// canceled
				} catch (ParserConfigurationException e) {
					storeImportError(e);
				} catch (SAXException e) {
					storeImportError(e);
				} catch (IOException e) {
					storeImportError(e);
				} catch (IllegalArgumentException e) {
					// Bug in parser: can throw IAE even if URL is not null
					storeImportError(e);
				}
			}

			private void storeImportError(Exception e) {
				exception[0] = new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR,
						UnitTestPlugin.PLUGIN_ID, ModelMessages.UnitTestModel_could_not_import, e));
			}
		};
		importThread.start();

		while (session[0] == null && exception[0] == null && !monitor.isCanceled()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// that's OK
			}
		}
		if (session[0] == null) {
			if (exception[0] != null) {
				throw new InvocationTargetException(exception[0]);
			} else {
				importThread.interrupt(); // have to kill the thread since we don't control URLConnection and XML
											// parsing
				throw new InterruptedException();
			}
		}

		UnitTestPlugin.getModel().addTestRunSession(session[0]);
		monitor.done();
		return session[0];
	}

	/**
	 * Loads an {@link ITestRunSession} from a swap file
	 *
	 * @param swapFile       a swap file
	 * @param testRunSession to be set from the swap file
	 * @throws CoreException in case of import failure
	 */
	public static void importIntoTestRunSession(File swapFile, TestRunSession testRunSession) throws CoreException {
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
//			parserFactory.setValidating(true); // TODO: add DTD and debug flag
			SAXParser parser = parserFactory.newSAXParser();
			TestRunHandler handler = new TestRunHandler(testRunSession);
			parser.parse(swapFile, handler);
		} catch (ParserConfigurationException e) {
			throwImportError(swapFile, e);
		} catch (SAXException e) {
			throwImportError(swapFile, e);
		} catch (IOException e) {
			throwImportError(swapFile, e);
		} catch (IllegalArgumentException e) {
			// Bug in parser: can throw IAE even if file is not null
			throwImportError(swapFile, e);
		}
	}

	/**
	 * Exports the given test run session.
	 *
	 * @param testRunSession the test run session
	 * @param file           the destination
	 * @throws CoreException if an error occurred
	 */
	public static void exportTestRunSession(TestRunSession testRunSession, File file) throws CoreException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			exportTestRunSession(testRunSession, out);
		} catch (IOException e) {
			throwExportError(file, e);
		} catch (TransformerConfigurationException e) {
			throwExportError(file, e);
		} catch (TransformerException e) {
			throwExportError(file, e);
		}
	}

	/**
	 * Exports the given test run session.
	 *
	 * @param testRunSession the test run session
	 * @param out            an {@link OutputStream} instance
	 * @throws TransformerFactoryConfigurationError if a transformer factory
	 *                                              configuration error occurred
	 * @throws TransformerException                 if a transformer error occurred
	 */
	public static void exportTestRunSession(TestRunSession testRunSession, OutputStream out)
			throws TransformerFactoryConfigurationError, TransformerException {

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		InputSource inputSource = new InputSource();
		SAXSource source = new SAXSource(new TestRunSessionSerializer(testRunSession), inputSource);
		StreamResult result = new StreamResult(out);
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		/*
		 * Bug in Xalan: Only indents if proprietary property
		 * org.apache.xalan.templates.OutputProperties.S_KEY_INDENT_AMOUNT is set.
		 *
		 * Bug in Xalan as shipped with J2SE 5.0: Does not read the indent-amount
		 * property at all >:-(.
		 */
		try {
			transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IllegalArgumentException e) {
			// no indentation today...
		}
		transformer.transform(source, result);
	}

	private static void throwExportError(File file, Exception e) throws CoreException {
		throw new CoreException(
				new org.eclipse.core.runtime.Status(IStatus.ERROR, UnitTestPlugin.PLUGIN_ID, MessageFormat.format(
						ModelMessages.UnitTestModel_could_not_write, BasicElementLabels.getPathLabel(file)), e));
	}

	private static void throwImportError(File file, Exception e) throws CoreException {
		throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR, UnitTestPlugin.PLUGIN_ID,
				MessageFormat.format(ModelMessages.UnitTestModel_could_not_read, BasicElementLabels.getPathLabel(file)),
				e));
	}

	/**
	 * Removes the given {@link TestRunSession} and notifies all registered
	 * {@link ITestRunSessionListener}s.
	 *
	 * @param testRunSession the session to remove
	 */
	public void removeTestRunSession(TestRunSession testRunSession) {
		boolean existed;
		synchronized (this) {
			existed = fTestRunSessions.remove(testRunSession);
		}
		if (existed) {
			notifyTestRunSessionRemoved(testRunSession);
		}
		testRunSession.removeSwapFile();
	}

	private void notifyTestRunSessionRemoved(ITestRunSession testRunSession) {
		testRunSession.stopTestRun();
		ILaunch launch = testRunSession.getLaunch();
		if (launch != null) {
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			launchManager.removeLaunch(launch);
		}

		for (ITestRunSessionListener listener : fTestRunSessionListeners) {
			listener.sessionRemoved(testRunSession);
		}
	}

	private void notifyTestRunSessionAdded(ITestRunSession testRunSession) {
		for (ITestRunSessionListener listener : fTestRunSessionListeners) {
			listener.sessionAdded(testRunSession);
		}
	}

	@Override
	public ITestRunSession createTestRunSession(ILaunch launch, int port) {
		return new TestRunSession(launch, port);
	}

}
