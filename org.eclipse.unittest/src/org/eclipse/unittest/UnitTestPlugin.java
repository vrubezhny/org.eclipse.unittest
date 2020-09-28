/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.unittest;

import java.io.File;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.eclipse.unittest.internal.model.UnitTestModel;
import org.eclipse.unittest.model.IUnitTestModel;
import org.eclipse.unittest.ui.TestRunnerViewPart;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The plug-in runtime class for the Unit Test plug-in.
 */
public class UnitTestPlugin extends AbstractUIPlugin {

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static UnitTestPlugin fgPlugin = null;

	public static final String PLUGIN_ID = "org.eclipse.unittest"; //$NON-NLS-1$
//	public static final String ID_EXTENSION_POINT_UNITTEST_LAUNCHCONFIGS = PLUGIN_ID + "." + "unittestLaunchConfigs"; //$NON-NLS-1$ //$NON-NLS-2$

	private BundleContext fBundleContext;

	private static boolean fIsStopped = false;

	/**
	 * Constructs a {@link UnitTestPlugin} object
	 */
	public UnitTestPlugin() {
		fgPlugin = this;
	}

	/**
	 * Returns the {@link UnitTestPlugin} instance
	 *
	 * @return a {@link UnitTestPlugin} instance
	 */
	public static UnitTestPlugin getDefault() {
		return fgPlugin;
	}

	/**
	 * Returns this workbench window's shell.
	 *
	 * @return the shell containing this window's controls or <code>null</code> if
	 *         the shell has not been created yet or if the window has been closed
	 */
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow workBenchWindow = getActiveWorkbenchWindow();
		if (workBenchWindow == null)
			return null;
		return workBenchWindow.getShell();
	}

	/**
	 * Returns the active workbench window
	 *
	 * @return the active workbench window, or <code>null</code> if there is no
	 *         active workbench window or if called from a non-UI thread
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		if (fgPlugin == null)
			return null;
		IWorkbench workBench = PlatformUI.getWorkbench();
		if (workBench == null)
			return null;
		return workBench.getActiveWorkbenchWindow();
	}

	/**
	 * Returns the currently active page for this workbench window.
	 *
	 * @return the active page, or <code>null</code> if none
	 */
	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null)
			return null;
		return activeWorkbenchWindow.getActivePage();
	}

	/**
	 * Logs the given exception.
	 *
	 * @param e the {@link Throwable} to log
	 */
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	/**
	 * Logs the given status.
	 *
	 * @param status the status to log
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext = context;
		fUnitTestModel.start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		fIsStopped = true;
		super.stop(context);
		try {
			InstanceScope.INSTANCE.getNode(UnitTestPlugin.PLUGIN_ID).flush();
			fUnitTestModel.stop();
		} finally {
			super.stop(context);
		}
		fBundleContext = null;
	}

	/**
	 * Returns a service with the specified name or <code>null</code> if none.
	 *
	 * @param serviceName name of service
	 * @return service object or <code>null</code> if none
	 */
	public Object getService(String serviceName) {
		ServiceReference<?> reference = fBundleContext.getServiceReference(serviceName);
		if (reference == null)
			return null;
		return fBundleContext.getService(reference);
	}

	/**
	 * Indicates that the plug-in is stopped
	 *
	 * @return <code>true</code> in case the plug-in is stopped, otherwise returns
	 *         <code>false</code>
	 */
	public static boolean isStopped() {
		return fIsStopped;
	}

	/**
	 * Asynchronously makes visible the Test Runner View Part
	 */
	public static void asyncShowTestRunnerViewPart() {
		getDisplay().asyncExec(UnitTestPlugin::showTestRunnerViewPartInActivePage);
	}

	/**
	 * Creates a Test Runner View Part if it's not yet created and makes it visible
	 * in active page
	 *
	 * @return a {@link TestRunnerViewPart} instance
	 */
	public static TestRunnerViewPart showTestRunnerViewPartInActivePage() {
		try {
			// Have to force the creation of view part contents
			// otherwise the UI will not be updated
			IWorkbenchPage page = UnitTestPlugin.getActivePage();
			if (page == null)
				return null;
			TestRunnerViewPart view = (TestRunnerViewPart) page.findView(TestRunnerViewPart.NAME);
			if (view == null) {
				// create and show the result view if it isn't created yet.
				return (TestRunnerViewPart) page.showView(TestRunnerViewPart.NAME, null, IWorkbenchPage.VIEW_VISIBLE);
			} else {
				page.activate(view);
				return view;
			}
		} catch (PartInitException pie) {
			UnitTestPlugin.log(pie);
			return null;
		}
	}

	private static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	/*
	 * The following is copied here from JUnitCorePlugin Most likely we need to
	 * place it into UnitTestCorePlugin
	 */

	public static final String ID_EXTENSION_POINT_TESTRUN_LISTENERS = PLUGIN_ID + "." + "testRunListeners"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String ID_EXTENSION_POINT_TEST_VIEW_SUPPORTS = PLUGIN_ID + "." + "unittestViewSupport"; //$NON-NLS-1$ //$NON-NLS-2$

	private static final String HISTORY_DIR_NAME = "history"; //$NON-NLS-1$

	private final UnitTestModel fUnitTestModel = new UnitTestModel();

	/**
	 * List storing the registered test run listeners
	 */
	private ListenerList<TestRunListener> fUnitTestRunListeners = new ListenerList<>();

	/**
	 * Returns a {@link IUnitTestModel} instance
	 *
	 * @return a {@link IUnitTestModel} instance
	 */
	public static IUnitTestModel getModel() {
		return getDefault().fUnitTestModel;
	}

	/**
	 * @return a <code>ListenerList</code> of all <code>TestRunListener</code>s
	 */
	public ListenerList<TestRunListener> getUnitTestRunListeners() {
		loadUnitTestRunListeners();
		return fUnitTestRunListeners;
	}

	/**
	 * Initializes TestRun Listener extensions
	 */
	private synchronized void loadUnitTestRunListeners() {
		if (!fUnitTestRunListeners.isEmpty()) {
			return;
		}

		IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
				.getExtensionPoint(ID_EXTENSION_POINT_TESTRUN_LISTENERS);
		if (extensionPoint == null) {
			return;
		}
		IConfigurationElement[] configs = extensionPoint.getConfigurationElements();
		MultiStatus status = new MultiStatus(PLUGIN_ID, IStatus.OK, "Could not load some testRunner extension points", //$NON-NLS-1$
				null);
		for (IConfigurationElement config : configs) {
			try {
				Object testRunListener = config.createExecutableExtension("class"); //$NON-NLS-1$
				if (testRunListener instanceof TestRunListener) {
					fUnitTestRunListeners.add((TestRunListener) testRunListener);
				}
			} catch (CoreException e) {
				status.add(e.getStatus());
			}
		}
		if (!status.isOK()) {
			log(status);
		}
	}

	/**
	 * Creates and returns a directory to store the History information
	 *
	 * @return the file corresponding to History directory
	 * @throws IllegalStateException in case of failed to create or find an existing
	 *                               directory
	 */
	public static File getHistoryDirectory() throws IllegalStateException {
		File historyDir = getDefault().getStateLocation().append(HISTORY_DIR_NAME).toFile();
		if (!historyDir.isDirectory()) {
			historyDir.mkdir();
		}
		return historyDir;
	}
}
