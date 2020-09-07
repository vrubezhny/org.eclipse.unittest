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

public class UnitTestRuntimeClasspathEntry {
	private final String fPluginId;

	private final String fPluginRelativePath;

	public UnitTestRuntimeClasspathEntry(String pluginId, String jarFile) {
		fPluginId = pluginId;
		fPluginRelativePath = jarFile;
	}

	public String getPluginId() {
		return fPluginId;
	}

	public String getPluginRelativePath() {
		return fPluginRelativePath;
	}

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
