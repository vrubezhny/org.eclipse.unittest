/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.unittest.junit.launcher;

import java.util.Set;

import org.eclipse.unittest.launcher.ITestFinder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

@SuppressWarnings("restriction")
public class JUnit3TestFinder extends org.eclipse.jdt.internal.junit.launcher.JUnit3TestFinder implements ITestFinder {

	@Override
	public void findTestsInContainer(Object element, Set result, IProgressMonitor pm) throws CoreException {
		if (element instanceof IJavaElement) {
			super.findTestsInContainer((IJavaElement) element, (Set<IType>) result, pm);
		}
	}

	@Override
	public boolean isTest(Object type) throws CoreException {
		if (type instanceof IType) {
			return super.isTest((IType) type);
		}
		return false;
	}

}