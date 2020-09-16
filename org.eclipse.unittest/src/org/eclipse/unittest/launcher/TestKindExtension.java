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
 *     Victor Rubezhny, Mickael Istria (Red Hat Inc.) - Adaptation from JUnit
 *******************************************************************************/

package org.eclipse.unittest.launcher;

import org.eclipse.unittest.UnitTestPlugin;

import org.eclipse.core.runtime.IConfigurationElement;

public class TestKindExtension {

	private static final String ID = "id"; //$NON-NLS-1$
	private static final String CLASS = "class"; //$NON-NLS-1$
	private static final String PRECEDES = "precedesTestKind"; //$NON-NLS-1$

	private final IConfigurationElement fElement;

	public TestKindExtension(IConfigurationElement element) {
		fElement = element;
	}

	public String getId() {
		return getAttribute(ID);
	}

	public String getPrecededKindId() {
		String attribute = getAttribute(PRECEDES);
		return attribute == null ? "" : attribute; //$NON-NLS-1$
	}

	protected String getAttribute(String attributeName) {
		return fElement.getAttribute(attributeName);
	}

	boolean precedes(TestKindExtension otherKind) {
		final String precededKindId = getPrecededKindId();
		String[] ids = precededKindId.split(","); //$NON-NLS-1$
		for (String id : ids) {
			if (id.equals(otherKind.getId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 *
	 * @return an instance of ITestKind for the given extension. <code>null</code>
	 *         if class couldn't be loaded.
	 */
	ITestKind instantiateKind() {
		try {
			return (ITestKind) fElement.createExecutableExtension(CLASS);
		} catch (Exception e) {
			UnitTestPlugin.log(e);
			return null;
		}
	}

	@Override
	public String toString() {
		return getId();
	}
}
