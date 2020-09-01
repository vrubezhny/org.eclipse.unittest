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
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.unittest.cdt.launcher;

import org.eclipse.unittest.launcher.ITestKind;
import org.eclipse.unittest.launcher.TestKindRegistry;

public class CDTTestKindUtil {

	public static final String CDT_TEST_KIND_ID= "org.eclipse.unittest.cdt.loader"; //$NON-NLS-1$
	public static final String CDT_DSF_DBG_TEST_KIND_ID= "org.eclipse.unittest.cdt.loader"; //$NON-NLS-1$

	private CDTTestKindUtil() {
	}

	public static ITestKind getTestKind(String testKindId) {
		return TestKindRegistry.getDefault().getKind(testKindId);
	}
}
