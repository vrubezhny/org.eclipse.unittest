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
package org.eclipse.unittest.ui;

import org.eclipse.unittest.UnitTestPlugin;
import org.eclipse.unittest.model.ITestElement;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

/**
 * Action to enable/disable stack trace filtering.
 */
public class CompareResultsAction extends Action {

	private FailureTrace fView;
	private CompareResultDialog fOpenDialog;

	public CompareResultsAction(FailureTrace view) {
		super(Messages.CompareResultsAction_label);
		setDescription(Messages.CompareResultsAction_description);
		setToolTipText(Messages.CompareResultsAction_tooltip);

		setDisabledImageDescriptor(UnitTestPlugin.getImageDescriptor("dlcl16/compare.png")); //$NON-NLS-1$
		setHoverImageDescriptor(UnitTestPlugin.getImageDescriptor("elcl16/compare.png")); //$NON-NLS-1$
		setImageDescriptor(UnitTestPlugin.getImageDescriptor("elcl16/compare.png")); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IUnitTestHelpContextIds.ENABLEFILTER_ACTION);
		fView = view;
	}

	@Override
	public void run() {
		ITestElement failedTest = fView.getFailedTest();
		if (fOpenDialog != null) {
			fOpenDialog.setInput(failedTest);
			fOpenDialog.getShell().setActive();

		} else {
			fOpenDialog = new CompareResultDialog(fView.getShell(), failedTest);
			fOpenDialog.create();
			fOpenDialog.getShell().addDisposeListener(e -> fOpenDialog = null);
			fOpenDialog.setBlockOnOpen(false);
			fOpenDialog.open();
		}
	}

	/**
	 * Updates the CompareResultDialog with a failed {@link ITestElement} as input
	 *
	 * @param failedTest a failed test element
	 */
	public void updateOpenDialog(ITestElement failedTest) {
		if (fOpenDialog != null) {
			fOpenDialog.setInput(failedTest);
		}
	}
}
