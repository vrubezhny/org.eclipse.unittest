/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package org.eclipse.unittest.junit.ui;

import org.eclipse.jdt.debug.ui.console.JavaStackTraceConsoleFactory;
import org.eclipse.unittest.model.ITestElement;

/**
 * Action delegate to show the stack trace of a failed test from JUnit view's
 * failure trace in debug's Java stack trace console.
 */
public class ShowStackTraceInConsoleViewActionDelegate implements Runnable {

	private ITestElement failedTest;
	private JavaStackTraceConsoleFactory fFactory;

	public ShowStackTraceInConsoleViewActionDelegate(ITestElement failedTest) {
		this.failedTest = failedTest;
	}

	@Override
	public void run() {
		String stackTrace = failedTest.getTrace();
		if (stackTrace != null) {
			if (fFactory == null) {
				fFactory = new JavaStackTraceConsoleFactory();
			}
			fFactory.openConsole(stackTrace);
		}
	}

}
