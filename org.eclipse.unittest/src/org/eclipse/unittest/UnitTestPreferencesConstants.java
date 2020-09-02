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
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/
package org.eclipse.unittest;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.unittest.launcher.ITestViewSupport;
import org.eclipse.unittest.model.ITestRunSession;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Defines constants which are used to refer to values in the plugin's
 * preference store.
 */
public class UnitTestPreferencesConstants {
	/**
	 * Boolean preference controlling whether the failure stack should be filtered.
	 */
	public static final String DO_FILTER_STACK = UnitTestPlugin.PLUGIN_ID + ".do_filter_stack"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether the JUnit view should be shown on
	 * errors only.
	 */
	public static final String SHOW_ON_ERROR_ONLY = UnitTestPlugin.PLUGIN_ID + ".show_on_error"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether '-ea' should be added to VM arguments
	 * when creating a new JUnit launch configuration.
	 */
	public static final String ENABLE_ASSERTIONS = UnitTestPlugin.PLUGIN_ID + ".enable_assertions"; //$NON-NLS-1$

	public static final boolean ENABLE_ASSERTIONS_DEFAULT = true;

	/**
	 * List of active stack filters. A String containing a comma separated list of
	 * fully qualified type names/patterns.
	 */
	public static final String PREF_ACTIVE_FILTERS_LIST = UnitTestPlugin.PLUGIN_ID + ".active_filters"; //$NON-NLS-1$

	/**
	 * List of inactive stack filters. A String containing a comma separated list of
	 * fully qualified type names/patterns.
	 */
	public static final String PREF_INACTIVE_FILTERS_LIST = UnitTestPlugin.PLUGIN_ID + ".inactive_filters"; //$NON-NLS-1$

	/**
	 * Maximum number of remembered test runs.
	 */
	public static final String MAX_TEST_RUNS = UnitTestPlugin.PLUGIN_ID + ".max_test_runs"; //$NON-NLS-1$

	private UnitTestPreferencesConstants() {
		// no instance
	}

	/**
	 * Serializes the array of strings into one comma-separated string.
	 *
	 * @param list array of strings
	 * @return a single string composed of the given list
	 */
	public static String serializeList(String[] list) {
		if (list == null)
			return ""; //$NON-NLS-1$

		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < list.length; i++) {
			if (i > 0)
				buffer.append(',');

			buffer.append(list[i]);
		}
		return buffer.toString();
	}

	/**
	 * Parses the comma-separated string into an array of strings.
	 *
	 * @param listString a comma-separated string
	 * @return an array of strings
	 */
	public static String[] parseList(String listString) {
		List<String> list = new ArrayList<>(10);
		StringTokenizer tokenizer = new StringTokenizer(listString, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens())
			list.add(tokenizer.nextToken());
		return list.toArray(new String[list.size()]);
	}

	public static String[] getFilterPatterns(ITestRunSession session) {
		if (session != null && !session.getTestRunnerKind().isNull()) {
			ITestViewSupport viewSupport = session.getTestRunnerKind().getTestViewSupport();
			if (viewSupport != ITestViewSupport.NULL) {
				return viewSupport.getFilterPatterns();
			}
		}
		return new String[0];
	}

	public static boolean getFilterStack() {
		return Platform.getPreferencesService().getBoolean(UnitTestPlugin.PLUGIN_ID, DO_FILTER_STACK, true, null);
	}

	public static void setFilterStack(boolean filter) {
		InstanceScope.INSTANCE.getNode(UnitTestPlugin.PLUGIN_ID).putBoolean(DO_FILTER_STACK, filter);
	}
}
