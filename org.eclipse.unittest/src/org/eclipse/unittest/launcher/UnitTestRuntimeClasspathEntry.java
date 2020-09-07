/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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

import java.util.Objects;

/**
 * An interface to be implemented by for extension point
 * org.org.eclipse.unittest.unittestKinds.
 */
public class UnitTestRuntimeClasspathEntry {
	private final String fPluginId;

	private final String fPluginRelativePath;

	/**
	 * Construct an {@link UnitTestRuntimeClasspathEntry} instance
	 *
	 * @param pluginId A Runtime Classpath Entry Plug-in identifier
	 * @param jarFile  A path to jar containing the plug-in
	 *                 &quot;loaderPluginId&quot;
	 */
	public UnitTestRuntimeClasspathEntry(String pluginId, String jarFile) {
		fPluginId = pluginId;
		fPluginRelativePath = jarFile;
	}

	/**
	 * Returns a plug-in identifier for this Classpath Entry
	 *
	 * @return a plug-in identifier
	 */
	public String getPluginId() {
		return fPluginId;
	}

	/**
	 * Returns a plug-in relative path
	 *
	 * @return a plug-in relative path
	 */
	public String getPluginRelativePath() {
		return fPluginRelativePath;
	}

	/**
	 * Returns a development Classpath entry
	 *
	 * @return a development Classpath entry
	 */
	public UnitTestRuntimeClasspathEntry developmentModeEntry() {
		return new UnitTestRuntimeClasspathEntry(getPluginId(), "bin"); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "ClasspathEntry(" + fPluginId + "/" + fPluginRelativePath + ")"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof UnitTestRuntimeClasspathEntry))
			return false;
		UnitTestRuntimeClasspathEntry other = (UnitTestRuntimeClasspathEntry) obj;
		if (!fPluginId.equals(other.getPluginId()))
			return false;
		return Objects.equals(fPluginRelativePath, other.getPluginRelativePath());
	}

	@Override
	public int hashCode() {
		return Objects.hash(fPluginId, fPluginRelativePath);
	}
}
