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

package org.eclipse.unittest.internal.launcher;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.ui.ITestViewSupport;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * A {@link TestViewSupportExtension} implementation
 */
public class TestViewSupportExtension {

	private static final String ID = "id"; //$NON-NLS-1$
	private static final String CLASS = "class"; //$NON-NLS-1$

	private final IConfigurationElement fElement;

	/**
	 * Constructs a {@link TestViewSupportExtension} object
	 *
	 * @param element an {@link IConfigurationElement} object
	 */
	public TestViewSupportExtension(IConfigurationElement element) {
		fElement = element;
	}

	/**
	 * Returns an identifier of the test view support extension
	 *
	 * @return A test view support identifier
	 */
	public String getId() {
		return getAttribute(ID);
	}

	/**
	 * Returns an attribute by a given name
	 *
	 * @param attributeName an attribute name
	 *
	 * @return a value of an attribute
	 */
	protected String getAttribute(String attributeName) {
		return fElement.getAttribute(attributeName);
	}

	/**
	 * Instantiates an {@link ITestViewSupport} object
	 *
	 * @return an instance of {@link ITestViewSupport} for the given extension.
	 *         <code>null</code> if class couldn't be loaded.
	 */
	ITestViewSupport instantiateTestViewSupport() {
		try {
			return (ITestViewSupport) fElement.createExecutableExtension(CLASS);
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
