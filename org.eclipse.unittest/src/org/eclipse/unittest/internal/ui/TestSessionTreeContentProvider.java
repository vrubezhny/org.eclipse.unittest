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

package org.eclipse.unittest.internal.ui;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.unittest.internal.model.TestCaseElement;
import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.internal.model.TestSuiteElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestSuiteElement;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class TestSessionTreeContentProvider implements ITreeContentProvider {
	private static class TestElementComparator implements Comparator<ITestElement> {

		@Override
		public int compare(ITestElement o1, ITestElement o2) {
			// Show test suites on top of test messages
			int weight1 = (o1 instanceof ITestSuiteElement) ? 0 : 1;
			int weight2 = (o2 instanceof ITestSuiteElement) ? 0 : 1;
			if (weight1 != weight2) {
				return weight1 - weight2;
			}
			// Compare by element names
			return o1.getTestName().compareTo(o2.getTestName());
		}

	}

	/**
	 * Compares two {@link TestElement}s: - {@link TestSuiteElement}s are placed on
	 * top of {@link TestCaseElement}s - TestElements are alphabetically ordered(by
	 * their names)
	 */
	public static final Comparator<ITestElement> TEST_ELEMENT_ALPHABETIC_ORDER = new TestElementComparator();

	private static final Object[] NO_CHILDREN = new Object[0];

	@Override
	public void dispose() {
		// nothing to dispose
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TestSuiteElement) {
			Set<ITestElement> sortedChildren = new TreeSet<>(TEST_ELEMENT_ALPHABETIC_ORDER);
			sortedChildren.addAll(((TestSuiteElement) parentElement).getChildren());
			return sortedChildren.toArray(Object[]::new);
		} else {
			return NO_CHILDREN;
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		TestRunnerViewPart part = (TestRunnerViewPart) inputElement;
		TestRunSession session = part.getCurrentTestRunSession();
		return new Object[] { session.getChildren().size() == 1 ? session.getChildren().get(0) : session };
	}

	@Override
	public Object getParent(Object element) {
		return ((TestElement) element).getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof TestSuiteElement) {
			return !((TestSuiteElement) element).getChildren().isEmpty();
		} else {
			return false;
		}
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing
	}
}
