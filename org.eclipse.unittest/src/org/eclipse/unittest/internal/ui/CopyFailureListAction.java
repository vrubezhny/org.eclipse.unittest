/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

import org.eclipse.unittest.UnitTestPlugin;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.ui.Messages;
import org.eclipse.unittest.ui.TestRunnerViewPart;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.PlatformUI;

/**
 * Copies the names of the methods that failed and their traces to the
 * clipboard.
 */
public class CopyFailureListAction extends Action {

	private final Clipboard fClipboard;
	private final TestRunnerViewPart fRunner;

	public CopyFailureListAction(TestRunnerViewPart runner, Clipboard clipboard) {
		super(Messages.CopyFailureList_action_label);
		fRunner = runner;
		fClipboard = clipboard;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IUnitTestHelpContextIds.COPYFAILURELIST_ACTION);
	}

	@Override
	public void run() {
		TextTransfer plainTextTransfer = TextTransfer.getInstance();

		try {
			fClipboard.setContents(new String[] { getAllFailureTraces() }, new Transfer[] { plainTextTransfer });
		} catch (SWTError e) {
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD)
				throw e;
			if (MessageDialog.openQuestion(UnitTestPlugin.getActiveWorkbenchShell(), Messages.CopyFailureList_problem,
					Messages.CopyFailureList_clipboard_busy))
				run();
		}
	}

	/**
	 * Returns the failure trace lines as a string
	 *
	 * @return a failure traces string
	 */
	public String getAllFailureTraces() {
		StringBuilder buf = new StringBuilder();
		ITestElement[] failures = fRunner.getCurrentTestRunSession().getAllFailedTestElements();

		String lineDelim = System.getProperty("line.separator", "\n"); //$NON-NLS-1$//$NON-NLS-2$
		for (ITestElement failure : failures) {
			buf.append(failure.getTestName()).append(lineDelim);
			String failureTrace = failure.getTrace();
			if (failureTrace != null) {
				int start = 0;
				while (start < failureTrace.length()) {
					int idx = failureTrace.indexOf('\n', start);
					if (idx != -1) {
						String line = failureTrace.substring(start, idx);
						buf.append(line).append(lineDelim);
						start = idx + 1;
					} else {
						start = Integer.MAX_VALUE;
					}
				}
			}
		}
		return buf.toString();
	}

}
