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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.UnitTestPreferencesConstants;
import org.eclipse.unittest.internal.junitXmlReport.TestRunHandler;
import org.eclipse.unittest.internal.launcher.TestListenerRegistry;
import org.eclipse.unittest.internal.launcher.TestRunListener;
import org.eclipse.unittest.internal.launcher.TestViewSupportRegistry;
import org.eclipse.unittest.launcher.UnitTestLaunchConfigurationConstants;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.ui.ITestViewSupport;

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
public final class UnitTestModel {

	private final class UnitTestLaunchListener implements ILaunchListener {

		/**
		 * Used to track new launches. We need to do this so that we only attach a
		 * TestRunner once to a launch. Once a test runner is connected, it is removed
		 * from the set.
		 */
		private HashSet<ILaunch> fTrackedLaunches = new HashSet<>(20);

		@Override
		public void launchAdded(ILaunch launch) {
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

			ITestViewSupport testRunnerViewSupport = UnitTestModel.newTestRunnerViewSupport(config);
			if (testRunnerViewSupport == null) {
				return;
			}

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
			// Load session on 1st change (usually 1st process added), although it's not
			// much reliable. Each TestRunnerClient should take care of listening to the
			// launch to get the right IProcess or stream or whatever else i useful
			if (getTestRunSessions().stream().noneMatch(session -> launch.equals(session.getLaunch()))) {
				TestRunSession testRunSession = new TestRunSession(launch);
				addTestRunSession(testRunSession);
				for (TestRunListener listener : TestListenerRegistry.getDefault().getUnitTestRunListeners()) {
					listener.sessionLaunched(testRunSession);
				}
			}
		}

	}

	private final ListenerList<ITestRunSessionListener> fTestRunSessionListeners = new ListenerList<>();
	/**
	 * Active test run sessions, youngest first.
	 */
	private final LinkedList<TestRunSession> fTestRunSessions = new LinkedList<>();
	private final ILaunchListener fLaunchListener = new UnitTestLaunchListener();

	private static UnitTestModel INSTANCE = null;

	private UnitTestModel() {

	}

	public static synchronized UnitTestModel getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new UnitTestModel();
		}
		return INSTANCE;
	}

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

	public void addTestRunSessionListener(ITestRunSessionListener listener) {
		fTestRunSessionListeners.add(listener);
	}

	public void removeTestRunSessionListener(ITestRunSessionListener listener) {
		fTestRunSessionListeners.remove(listener);
	}

	public synchronized List<TestRunSession> getTestRunSessions() {
		return new ArrayList<>(fTestRunSessions);
	}

	private void addTestRunSession(TestRunSession testRunSession) {
		Assert.isNotNull(testRunSession);
		ArrayList<TestRunSession> toRemove = new ArrayList<>();

		synchronized (this) {
			Assert.isLegal(!fTestRunSessions.contains(testRunSession));
			fTestRunSessions.addFirst(testRunSession);

			int maxCount = Platform.getPreferencesService().getInt(UnitTestPlugin.PLUGIN_ID,
					UnitTestPreferencesConstants.MAX_TEST_RUNS, 10, null);
			int size = fTestRunSessions.size();
			if (size > maxCount) {
				List<TestRunSession> excess = fTestRunSessions.subList(maxCount, size);
				for (Iterator<TestRunSession> iter = excess.iterator(); iter.hasNext();) {
					TestRunSession oldSession = iter.next();
					if (oldSession.isStopped()) {
						toRemove.add(oldSession);
						iter.remove();
					}
				}
			}
		}

		toRemove.forEach(this::notifyTestRunSessionRemoved);
		notifyTestRunSessionAdded(testRunSession);
	}

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

		addTestRunSession(session[0]);
		monitor.done();
		return session[0];
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
	}

	private void notifyTestRunSessionRemoved(TestRunSession testRunSession) {
		for (ITestRunSessionListener listener : fTestRunSessionListeners) {
			listener.sessionRemoved(testRunSession);
		}
	}

	private void notifyTestRunSessionAdded(ITestRunSession testRunSession) {
		for (ITestRunSessionListener listener : fTestRunSessionListeners) {
			listener.sessionAdded(testRunSession);
		}
	}

	/**
	 * Returns {@link ITestViewSupport} instance from the given launch configuration
	 *
	 * @param launchConfiguration a launch configuration
	 * @return a test runner view support instance if exists or <code>null</code>.
	 */
	public static ITestViewSupport newTestRunnerViewSupport(ILaunchConfiguration launchConfiguration) {
		try {
			return TestViewSupportRegistry.getDefault().getTestViewSupportInstance(launchConfiguration
					.getAttribute(UnitTestLaunchConfigurationConstants.ATTR_UNIT_TEST_VIEW_SUPPORT, (String) null));
		} catch (CoreException e) {
			// Ignore
		}
		return null;
	}
}
