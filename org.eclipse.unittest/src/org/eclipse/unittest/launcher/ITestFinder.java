/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

package org.eclipse.unittest.launcher;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface to be implemented by for extension point
 * org.org.eclipse.unittest.unittestKinds.
 */
public interface ITestFinder {
	ITestFinder NULL = new ITestFinder() {
		@Override
		public void findTestsInContainer(Object element, Set result, IProgressMonitor pm) {
			// do nothing
		}

		@Override
		public boolean isTest(Object type) {
			return false;
		}
	};

	/**
	 * @param element element to search for tests
	 * @param result  a Set to add ITypes
	 * @param pm      the progress monitor
	 * @throws CoreException thrown when tests can not be found
	 */
	void findTestsInContainer(Object element, @SuppressWarnings("rawtypes") Set result, IProgressMonitor pm)
			throws CoreException;

	boolean isTest(Object type) throws CoreException;
}
