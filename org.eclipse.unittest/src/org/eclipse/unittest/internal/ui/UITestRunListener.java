/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import org.eclipse.unittest.TestRunListener;
import org.eclipse.unittest.UnitTestPlugin;
import org.eclipse.unittest.model.ITestRunSession;

/**
 * This test run listener is the entry point that makes sure the
 * org.eclipse.unittest plug-in gets loaded when a UnitTest launch configuration
 * is launched.
 */
public class UITestRunListener extends TestRunListener {
	@Override
	public void sessionLaunched(ITestRunSession session) {
		UnitTestPlugin.asyncShowTestRunnerViewPart();
	}
}
