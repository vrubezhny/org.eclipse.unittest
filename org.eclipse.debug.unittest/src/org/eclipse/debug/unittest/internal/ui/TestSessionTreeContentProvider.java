/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.debug.unittest.internal.model.TestElement;
import org.eclipse.debug.unittest.internal.model.TestRoot;
import org.eclipse.debug.unittest.internal.model.TestSuiteElement;

public class TestSessionTreeContentProvider implements ITreeContentProvider {

	private final Object[] NO_CHILDREN= new Object[0];

	@Override
	public void dispose() {
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TestSuiteElement)
			return ((TestSuiteElement) parentElement).getChildren();
		else
			return NO_CHILDREN;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return ((TestRoot) inputElement).getChildren();
	}

	@Override
	public Object getParent(Object element) {
		return ((TestElement) element).getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof TestSuiteElement)
			return ((TestSuiteElement) element).getChildren().length != 0;
		else
			return false;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
