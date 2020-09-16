/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.junit.internal.launcher;

import org.eclipse.unittest.junit.launcher.JUnitRemoteTestRunnerClient;
import org.eclipse.unittest.junit.launcher.JUnitTestViewSupport;
import org.eclipse.unittest.launcher.ITestKind;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.launcher.ITestViewSupport;

public class JUnitTestKind implements ITestKind {

	@Override
	public ITestRunnerClient newTestRunnerClient() {
		return new JUnitRemoteTestRunnerClient();
	}

	@Override
	public ITestViewSupport newTestViewSupport() {
		return new JUnitTestViewSupport();
	}

}
