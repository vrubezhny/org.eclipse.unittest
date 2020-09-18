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

import org.eclipse.unittest.UnitTestPlugin;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Attribute keys used by the UnitTest LaunchConfiguration. Note that these
 * constants are not API and might change in the future.
 */
public class UnitTestLaunchConfigurationConstants {

	public static final String ID_UNITESTT_APPLICATION = "org.eclipse.unittest.launchconfig"; //$NON-NLS-1$

	public static final String ATTR_PORT = UnitTestPlugin.PLUGIN_ID + ".PORT"; //$NON-NLS-1$

	/**
	 * The test method name (followed by a comma-separated list of fully qualified
	 * parameter type names in parentheses, if exists), or "" iff running the whole
	 * test type.
	 */
	public static final String ATTR_TEST_NAME = UnitTestPlugin.PLUGIN_ID + ".TESTNAME"; //$NON-NLS-1$

	public static final String ATTR_KEEPRUNNING = UnitTestPlugin.PLUGIN_ID + ".KEEPRUNNING_ATTR"; //$NON-NLS-1$

	public static final String ATTR_FAILURES_NAMES = UnitTestPlugin.PLUGIN_ID + ".FAILURENAMES"; //$NON-NLS-1$

	public static final String ATTR_UNIT_TEST_VIEW_SUPPORT = UnitTestPlugin.PLUGIN_ID + ".TEST_KIND"; //$NON-NLS-1$

	public static final String ATTR_TEST_HAS_INCLUDE_TAGS = UnitTestPlugin.PLUGIN_ID + ".HAS_INCLUDE_TAGS"; //$NON-NLS-1$

	public static final String ATTR_TEST_HAS_EXCLUDE_TAGS = UnitTestPlugin.PLUGIN_ID + ".HAS_EXCLUDE_TAGS"; //$NON-NLS-1$

	public static final String ATTR_TEST_INCLUDE_TAGS = UnitTestPlugin.PLUGIN_ID + ".INCLUDE_TAGS"; //$NON-NLS-1$

	public static final String ATTR_TEST_EXCLUDE_TAGS = UnitTestPlugin.PLUGIN_ID + ".EXCLUDE_TAGS"; //$NON-NLS-1$

	/**
	 * The unique ID of test to run or "" if not available
	 */
	public static final String ATTR_TEST_UNIQUE_ID = UnitTestPlugin.PLUGIN_ID + ".TEST_UNIQUE_ID"; //$NON-NLS-1$

	/**
	 * Returns {@link ITestKind} instance from the given launch configuration
	 *
	 * @param launchConfiguration a launch configuration
	 * @return a test runner kind instance if exists or <code>null</code>.
	 */
	public static ITestKind newTestRunnerKind(ILaunchConfiguration launchConfiguration) {
		try {
			String loaderId = launchConfiguration
					.getAttribute(UnitTestLaunchConfigurationConstants.ATTR_UNIT_TEST_VIEW_SUPPORT, (String) null);
			if (loaderId != null) {
				return TestKindRegistry.getDefault().getTestKindInstance(loaderId);
			}
		} catch (CoreException e) {
			// Ignore
		}
		return null;
	}

	private UnitTestLaunchConfigurationConstants() {
		// No instance allowed
	}

	/* Copied from org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants */
	/**
	 * Launch configuration attribute key. The value is a fully qualified name of a
	 * main type to launch.
	 */
	public static final String ATTR_MAIN_TYPE_NAME = UnitTestPlugin.getPluginId() + ".MAIN_TYPE"; //$NON-NLS-1$

}
