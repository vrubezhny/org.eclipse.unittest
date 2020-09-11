/*******************************************************************************
 * Copyright (c) 2011, 2012 Anton Gorenkov.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Anton Gorenkov - initial API and implementation
 *******************************************************************************/
package org.eclipse.unittest.cdt.internal.launcher;

import org.eclipse.unittest.cdt.CDTPlugin;
import org.eclipse.unittest.cdt.launcher.BaseTestsLaunchDelegate;

/**
 * Launch delegate implementation that redirects its queries to DSF.
 */
public class DsfGdbRunTestsLaunchDelegate extends BaseTestsLaunchDelegate {

	@Override
	public String getPreferredDelegateId() {
		return "org.eclipse.cdt.dsf.gdb.launch.localCLaunch"; //$NON-NLS-1$
	}

	@Override
	public String getUnitTestViewSupportID() {
		return CDTPlugin.CDT_TEST_KIND_ID;
	}

}
