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
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

import org.eclipse.unittest.internal.model.UnitTestModel;
import org.eclipse.unittest.model.IUnitTestModel;
import org.eclipse.unittest.ui.TestRunnerViewPart;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The plug-in runtime class for the Unit Test plug-in.
 */
@SuppressWarnings("deprecation")
public class UnitTestPlugin extends AbstractUIPlugin {

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static UnitTestPlugin fgPlugin = null;

	public static final String PLUGIN_ID = "org.eclipse.unittest"; //$NON-NLS-1$
//	public static final String ID_EXTENSION_POINT_UNITTEST_LAUNCHCONFIGS = PLUGIN_ID + "." + "unittestLaunchConfigs"; //$NON-NLS-1$ //$NON-NLS-2$

	private static final IPath ICONS_PATH = new Path("$nl$/icons/full"); //$NON-NLS-1$

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
	 * Returns an identifier of {@link UnitTestPlugin}
	 *
	 * @return an {@link UnitTestPlugin} identifier
	 */
	public static String getPluginId() {
		return PLUGIN_ID;
	}

	/**
	 * Logs the given exception.
	 *
	 * @param e the {@link Throwable} to log
	 */
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	/**
	 * Logs the given status.
	 *
	 * @param status the status to log
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	/**
	 * Create an {@link ImageDescriptor} from a given path
	 *
	 * @param relativePath relative path to the image
	 * @return an {@link ImageDescriptor}, or <code>null</code> iff there's no image
	 *         at the given location and <code>useMissingImageDescriptor</code> is
	 *         <code>true</code>
	 */
	public static ImageDescriptor getImageDescriptor(String relativePath) {
		IPath path = ICONS_PATH.append(relativePath);
		return createImageDescriptor(getDefault().getBundle(), path, true);
	}

	/**
	 * Creates an {@link Image} from a given path
	 *
	 * @param path path to the image
	 * @return a new image or <code>null</code> if the image could not be created
	 */
	public static Image createImage(String path) {
		return getImageDescriptor(path).createImage();
	}

	/**
	 * Sets the three image descriptors for enabled, disabled, and hovered to an
	 * action. The actions are retrieved from the *lcl16 folders.
	 *
	 * @param action   the action
	 * @param iconName the icon name
	 */
	public static void setLocalImageDescriptors(IAction action, String iconName) {
		setImageDescriptors(action, "lcl16", iconName); //$NON-NLS-1$
	}

	private static void setImageDescriptors(IAction action, String type, String relPath) {
		ImageDescriptor id = createImageDescriptor("d" + type, relPath, false); //$NON-NLS-1$
		if (id != null)
			action.setDisabledImageDescriptor(id);

		ImageDescriptor descriptor = createImageDescriptor("e" + type, relPath, true); //$NON-NLS-1$
		action.setHoverImageDescriptor(descriptor);
		action.setImageDescriptor(descriptor);
	}

	/*
	 * Creates an image descriptor for the given prefix and name in the JDT UI
	 * bundle. The path can contain variables like $NL$. If no image could be found,
	 * <code>useMissingImageDescriptor</code> decides if either the 'missing image
	 * descriptor' is returned or <code>null</code>. or <code>null</code>.
	 */
	private static ImageDescriptor createImageDescriptor(String pathPrefix, String imageName,
			boolean useMissingImageDescriptor) {
		IPath path = ICONS_PATH.append(pathPrefix).append(imageName);
		return createImageDescriptor(UnitTestPlugin.getDefault().getBundle(), path, useMissingImageDescriptor);
	}

	/**
	 * Creates an image descriptor for the given path in a bundle. The path can
	 * contain variables like $NL$. If no image could be found,
	 * <code>useMissingImageDescriptor</code> decides if either the 'missing image
	 * descriptor' is returned or <code>null</code>.
	 *
	 * @param bundle                    a bundle
	 * @param path                      path in the bundle
	 * @param useMissingImageDescriptor if <code>true</code>, returns the shared
	 *                                  image descriptor for a missing image.
	 *                                  Otherwise, returns <code>null</code> if the
	 *                                  image could not be found
	 * @return an {@link ImageDescriptor}, or <code>null</code> iff there's no image
	 *         at the given location and <code>useMissingImageDescriptor</code> is
	 *         <code>true</code>
	 */
	private static ImageDescriptor createImageDescriptor(Bundle bundle, IPath path, boolean useMissingImageDescriptor) {
		URL url = FileLocator.find(bundle, path, null);
		if (url != null) {
			return ImageDescriptor.createFromURL(url);
		}
		if (useMissingImageDescriptor) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
		return null;
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
	 * Returns the bundle for a given bundle name, regardless whether the bundle is
	 * resolved or not.
	 *
	 * @param bundleName the bundle name
	 * @return the bundle
	 */
	public Bundle getBundle(String bundleName) {
		Bundle[] bundles = getBundles(bundleName, null);
		if (bundles != null && bundles.length > 0)
			return bundles[0];
		return null;
	}

	/**
	 * Returns the bundles for a given bundle name,
	 *
	 * @param bundleName the bundle name
	 * @param version    the version of the bundle
	 * @return the bundles of the given name
	 */
	public Bundle[] getBundles(String bundleName, String version) {
		Bundle[] bundles = Platform.getBundles(bundleName, version);
		if (bundles != null)
			return bundles;

		// Accessing unresolved bundle
		ServiceReference<PackageAdmin> serviceRef = fBundleContext.getServiceReference(PackageAdmin.class);
		PackageAdmin admin = fBundleContext.getService(serviceRef);
		bundles = admin.getBundles(bundleName, version);
		if (bundles != null && bundles.length > 0)
			return bundles;
		return null;
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
	 * Returns the section with the given name in this dialog settings.
	 *
	 * @param name the key
	 * @return {@link IDialogSettings} (the section), or <code>null</code> if none
	 */
	public IDialogSettings getDialogSettingsSection(String name) {
		IDialogSettings dialogSettings = getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(name);
		if (section == null) {
			section = dialogSettings.addNewSection(name);
		}
		return section;
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
	public static final String ID_EXTENSION_POINT_TEST_KINDS = PLUGIN_ID + "." + "unittestKinds"; //$NON-NLS-1$ //$NON-NLS-2$

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
