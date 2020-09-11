/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

/**
 * Interface to be implemented by for extension point
 * org.org.eclipse.unittest.unittestKinds.
 */
public interface ITestKind {
	/**
	 * Returns a Test Runner Client for this this Test Kind
	 *
	 * @return returns a Test Runner Client
	 */
	ITestRunnerClient newTestRunnerClient();

	/**
	 * Returns a Test View Support for this Test Kind
	 *
	 * @return returns a Test View Support
	 */
	ITestViewSupport newTestViewSupport();

}