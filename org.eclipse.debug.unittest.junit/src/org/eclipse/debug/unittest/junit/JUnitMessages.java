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
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/
package org.eclipse.debug.unittest.junit;

import org.eclipse.osgi.util.NLS;

public final class JUnitMessages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.debug.unittest.junit.JUnitMessages";//$NON-NLS-1$

	public static String JUnitLaunchConfigurationDelegate_create_source_locator_description;
	public static String JUnitLaunchConfigurationDelegate_error_input_element_deosn_not_exist;
	public static String JUnitLaunchConfigurationDelegate_error_invalidproject;
	public static String JUnitLaunchConfigurationDelegate_error_junit4notonpath;
	public static String JUnitLaunchConfigurationDelegate_error_junit5notonpath;
	public static String JUnitLaunchConfigurationDelegate_error_junitnotonpath;
	public static String JUnitLaunchConfigurationDelegate_error_no_socket;
	public static String JUnitLaunchConfigurationDelegate_error_notests_kind;
	public static String JUnitLaunchConfigurationDelegate_error_wrong_input;
	public static String JUnitLaunchConfigurationDelegate_input_type_does_not_exist;
	public static String JUnitLaunchConfigurationDelegate_verifying_attriburtes_description;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JUnitMessages.class);
	}

	private JUnitMessages() {
		// Do not instantiate
	}

}
