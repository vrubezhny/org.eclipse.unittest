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
 *   Julien Ruaux: jruaux@octo.com
 * 	 Vincent Massol: vmassol@octo.com
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/
package org.eclipse.unittest.cdt;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * The plug-in runtime class for the JUnit core plug-in.
 */
public class CDTUnitTestPlugin extends Plugin {

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static CDTUnitTestPlugin fgPlugin= null;

	public static final String UNITTEST_PLUGIN_ID= "org.eclipse.unittest.ui"; //$NON-NLS-1$

	public static final String PLUGIN_ID= "org.eclipse.unittest.cdt"; //$NON-NLS-1$

	/*
	public final static String TEST_SUPERCLASS_NAME= "junit.framework.TestCase"; //$NON-NLS-1$
	public final static String TEST_INTERFACE_NAME= "junit.framework.Test"; //$NON-NLS-1$

	public final static String JUNIT5_TESTABLE_ANNOTATION_NAME= "org.junit.platform.commons.annotation.Testable"; //$NON-NLS-1$
	public final static String JUNIT5_JUPITER_TEST_ANNOTATION_NAME= "org.junit.jupiter.api.Test"; //$NON-NLS-1$
	public final static String JUNIT5_JUPITER_NESTED_ANNOTATION_NAME= "org.junit.jupiter.api.Nested"; //$NON-NLS-1$

	public final static String JUNIT4_ANNOTATION_NAME= "org.junit.Test"; //$NON-NLS-1$
	public static final String SIMPLE_TEST_INTERFACE_NAME= "Test"; //$NON-NLS-1$
*/

	public static final String CDT_TEST_VIEW_SUPPORT_ID= "org.eclipse.unittest.cdt.loader"; //$NON-NLS-1$
	public static final String CDT_DSF_DBG_TEST_VIEW_SUPPORT_ID= "org.eclipse.unittest.cdt.loader"; //$NON-NLS-1$


	/**
	 * The class path variable referring to the junit home location
	 */
	/*
	public final static String JUNIT_HOME= "JUNIT_HOME"; //$NON-NLS-1$
*/
	/**
	 * The class path variable referring to the junit source location
     * @since 3.2
	 */
	/*
	public static final String JUNIT_SRC_HOME= "JUNIT_SRC_HOME";  //$NON-NLS-1$

	private static final String HISTORY_DIR_NAME= "history"; //$NON-NLS-1$
*/
	private BundleContext fBundleContext;

	public CDTUnitTestPlugin() {
		fgPlugin= this;
	}

	public static CDTUnitTestPlugin getDefault() {
		return fgPlugin;
	}

	public static String getPluginId() {
		return PLUGIN_ID;
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext= context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			InstanceScope.INSTANCE.getNode(CDTUnitTestPlugin.UNITTEST_PLUGIN_ID).flush();
		} finally {
			super.stop(context);
		}
		fBundleContext= null;
	}

	/**
	 * Returns a service with the specified name or <code>null</code> if none.
	 *
	 * @param serviceName name of service
	 * @return service object or <code>null</code> if none
	 * @since 3.5
	 */
	public Object getService(String serviceName) {
		ServiceReference<?> reference= fBundleContext.getServiceReference(serviceName);
		if (reference == null)
			return null;
		return fBundleContext.getService(reference);
	}

	/**
	 * Activates UnitTestBundle. Eclipse uses lazy bundle loading by default, which
	 * means a bundle will not be loaded in many cases until some of its class is
	 * used. This method allows the clients to instantiate the Unit Test bundle in
	 * order to make it setup its launch listeners that are used to create and
	 * activate Unit Test View. The Unit Test client bundles must call this method
	 * before a Unit Test launch is created (preferably right before creation of the
	 * launch in order to not make Eclipse to load the Unit Test bundle when it is
	 * not really required), To load the Unit Test bundle the clients, for
	 * example, might call this method inside their
	 * 'ILaunchConfigurationDelegate2.getLaunch(ILaunchConfiguration, String)'
	 * method of their launch configuration implementation.
	 */
	public static void activateUnitTestCoreBundle() {
		Assert.isNotNull(Platform.getBundle("org.eclipse.unittest.ui")); //$NON-NLS-1$
	}
}