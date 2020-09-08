/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
import org.eclipse.core.runtime.IConfigurationElement;

public class TestKind implements ITestKind {

	private final IConfigurationElement fElement;
	private ITestRunnerClient fTestRunnerClient;
	private ITestViewSupport fTestViewSupport;

	public TestKind(IConfigurationElement element) {
		fElement = element;
	}

	@Override
	public String getDisplayName() {
		return getAttribute(DISPLAY_NAME);
	}

	@Override
	public String getId() {
		return getAttribute(ID);
	}

	@Override
	public String getLoaderClassName() {
		return getAttribute(LOADER_CLASS_NAME);
	}

	@Override
	public String getLoaderPluginId() {
		return getAttribute(LOADER_PLUGIN_ID);
	}

	@Override
	public String getTestRunnerClientClassName() {
		return getAttribute(TEST_RUNNER_CLIENT_CLASS_NAME);
	}

	@Override
	public ITestRunnerClient getTestRunnerClient() {
		if (fTestRunnerClient == null) {
			try {
				fTestRunnerClient = (ITestRunnerClient) fElement
						.createExecutableExtension(TEST_RUNNER_CLIENT_CLASS_NAME);
			} catch (CoreException e1) {
				UnitTestPlugin.log(e1);
				fTestRunnerClient = ITestRunnerClient.NULL;
			}
		}
		return fTestRunnerClient;
	}

	@Override
	public ITestViewSupport getTestViewSupport() {
		if (fTestViewSupport == null) {
			try {
				fTestViewSupport = (ITestViewSupport) fElement.createExecutableExtension(TEST_VIEW_SUPPORT_CLASS_NAME);
			} catch (CoreException e1) {
				UnitTestPlugin.log(e1);
				fTestViewSupport = ITestViewSupport.NULL;
			}
		}
		return fTestViewSupport;
	}

	@Override
	public String getTestViewSupportClassName() {
		return getAttribute(TEST_VIEW_SUPPORT_CLASS_NAME);
	}

	@Override
	public String getPrecededKindId() {
		String attribute = getAttribute(PRECEDES);
		return attribute == null ? "" : attribute; //$NON-NLS-1$
	}

	@Override
	public boolean isNull() {
		return false;
	}

	protected String getAttribute(String attributeName) {
		return fElement.getAttribute(attributeName);
	}

	boolean precedes(ITestKind otherKind) {
		final String precededKindId = getPrecededKindId();
		String[] ids = precededKindId.split(","); //$NON-NLS-1$
		for (String id : ids) {
			if (id.equals(otherKind.getId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return getDisplayName() + " (id: " + getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
