/*******************************************************************************
 * Copyright (c) 2020, Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.cdt.internal.launcher;

import org.eclipse.unittest.cdt.launcher.CDTTestRunnerClient;
import org.eclipse.unittest.cdt.launcher.CDTTestViewSupport;
import org.eclipse.unittest.launcher.ITestKind;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.launcher.ITestViewSupport;

public class CDTTestKind implements ITestKind {

	@Override
	public ITestRunnerClient newTestRunnerClient() {
		return new CDTTestRunnerClient();
	}

	@Override
	public ITestViewSupport newTestViewSupport() {
		return new CDTTestViewSupport();
	}

}
