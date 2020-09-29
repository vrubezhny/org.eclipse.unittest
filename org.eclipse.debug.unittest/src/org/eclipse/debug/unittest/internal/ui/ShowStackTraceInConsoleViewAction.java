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
package org.eclipse.debug.unittest.internal.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

import org.eclipse.ui.IActionDelegate;

/**
 * Action to show the stack trace of a failed test from Unit Test view's failure
 * trace in debug's Java stack trace console.
 */
public class ShowStackTraceInConsoleViewAction extends Action {

	private IActionDelegate fDelegate;

	public ShowStackTraceInConsoleViewAction() {
		super(Messages.ShowStackTraceInConsoleViewAction_label, IAction.AS_PUSH_BUTTON);
		setDescription(Messages.ShowStackTraceInConsoleViewAction_description);
		setToolTipText(Messages.ShowStackTraceInConsoleViewAction_tooltip);

		setHoverImageDescriptor(Images.getImageDescriptor("elcl16/open_console.png")); //$NON-NLS-1$
		setImageDescriptor(Images.getImageDescriptor("elcl16/open_console.png")); //$NON-NLS-1$
		setDisabledImageDescriptor(Images.getImageDescriptor("dlcl16/open_console.png")); //$NON-NLS-1$

		fDelegate = null;
	}

	@Override
	public void run() {
		if (fDelegate != null) {
			fDelegate.run(this);
		}
	}

	public void setDelegate(IActionDelegate delegate) {
		fDelegate = delegate;
	}

	@Override
	public boolean isEnabled() {
		return super.isEnabled() && fDelegate != null;
	}
}
