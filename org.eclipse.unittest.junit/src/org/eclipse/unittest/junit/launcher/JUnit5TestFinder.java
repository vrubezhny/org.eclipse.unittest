package org.eclipse.unittest.junit.launcher;

import java.util.Set;

import org.eclipse.unittest.launcher.ITestFinder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

@SuppressWarnings("restriction")
public class JUnit5TestFinder extends org.eclipse.jdt.internal.junit.launcher.JUnit5TestFinder implements ITestFinder {

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
