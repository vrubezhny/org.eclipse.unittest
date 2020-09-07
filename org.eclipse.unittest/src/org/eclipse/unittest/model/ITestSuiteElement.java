/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.unittest.model;

/**
 * Represents a test suite element.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 */
public interface ITestSuiteElement extends ITestElementContainer {

	/**
	 * Returns the name of the suite. This is either the qualified type name of the
	 * suite class, or a custom name if one has been set.
	 *
	 * @return the name of the suite
	 */
	String getSuiteTypeName();

	/**
	 * Adds a child {@link ITestElement} to this test suite element
	 *
	 * @param child a child {@link ITestElement}
	 */
	void addChild(ITestElement child);

	/**
	 * Notifies on the status changes in a specified child {@link ITestElement}
	 * element
	 *
	 * @param child       a child {@link ITestElement} element
	 * @param childStatus a new status value
	 */
	void childChangedStatus(ITestElement child, ITestElement.Status childStatus);

}
