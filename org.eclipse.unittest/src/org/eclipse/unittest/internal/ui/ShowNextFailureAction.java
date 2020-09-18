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
import org.eclipse.unittest.ui.Messages;
import org.eclipse.unittest.ui.TestRunnerViewPart;

import org.eclipse.jface.action.Action;

public class ShowNextFailureAction extends Action {

	private TestRunnerViewPart fPart;

	public ShowNextFailureAction(TestRunnerViewPart part) {
		super(Messages.ShowNextFailureAction_label);
		setDisabledImageDescriptor(UnitTestPlugin.getImageDescriptor("dlcl16/select_next.png")); //$NON-NLS-1$
		setHoverImageDescriptor(UnitTestPlugin.getImageDescriptor("elcl16/select_next.png")); //$NON-NLS-1$
		setImageDescriptor(UnitTestPlugin.getImageDescriptor("elcl16/select_next.png")); //$NON-NLS-1$
		setToolTipText(Messages.ShowNextFailureAction_tooltip);
		fPart = part;
	}

	@Override
	public void run() {
		fPart.selectNextFailure();
	}
}
