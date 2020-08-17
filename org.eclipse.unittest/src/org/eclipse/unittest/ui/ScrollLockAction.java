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

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

/**
 * Toggles console auto-scroll
 */
public class ScrollLockAction extends Action {

	private TestRunnerViewPart fRunnerViewPart;

	public ScrollLockAction(TestRunnerViewPart viewer) {
		super(Messages.ScrollLockAction_action_label);
		fRunnerViewPart= viewer;
		setToolTipText(Messages.ScrollLockAction_action_tooltip);
		setDisabledImageDescriptor(UnitTestPlugin.getImageDescriptor("dlcl16/lock.png")); //$NON-NLS-1$
		setHoverImageDescriptor(UnitTestPlugin.getImageDescriptor("elcl16/lock.png")); //$NON-NLS-1$
		setImageDescriptor(UnitTestPlugin.getImageDescriptor("elcl16/lock.png")); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(
			this,
			IUnitTestHelpContextIds.OUTPUT_SCROLL_LOCK_ACTION);
		setChecked(false);
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		fRunnerViewPart.setAutoScroll(!isChecked());
	}
}
