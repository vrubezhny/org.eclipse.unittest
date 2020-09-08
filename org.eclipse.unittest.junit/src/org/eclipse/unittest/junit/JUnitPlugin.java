/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *   Julien Ruaux: jruaux@octo.com
 * 	 Vincent Massol: vmassol@octo.com
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/
package org.eclipse.unittest.junit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.eclipse.unittest.UnitTestPlugin;
import org.eclipse.unittest.launcher.ITestKind;
import org.eclipse.unittest.launcher.TestKindRegistry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;

/**
 * The plug-in runtime class for the JUnit core plug-in.
 */
public class JUnitPlugin extends AbstractUIPlugin {

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static JUnitPlugin fgPlugin = null;

	public static final String CORE_PLUGIN_ID = "org.eclipse.unittest"; //$NON-NLS-1$

	public static final String PLUGIN_ID = "org.eclipse.unittest.junit"; //$NON-NLS-1$

	public final static String TEST_SUPERCLASS_NAME = "junit.framework.TestCase"; //$NON-NLS-1$
	public final static String TEST_INTERFACE_NAME = "junit.framework.Test"; //$NON-NLS-1$

	public final static String JUNIT5_TESTABLE_ANNOTATION_NAME = "org.junit.platform.commons.annotation.Testable"; //$NON-NLS-1$
	public final static String JUNIT5_JUPITER_TEST_ANNOTATION_NAME = "org.junit.jupiter.api.Test"; //$NON-NLS-1$
	public final static String JUNIT5_JUPITER_NESTED_ANNOTATION_NAME = "org.junit.jupiter.api.Nested"; //$NON-NLS-1$

	public final static String JUNIT4_ANNOTATION_NAME = "org.junit.Test"; //$NON-NLS-1$
	public static final String SIMPLE_TEST_INTERFACE_NAME = "Test"; //$NON-NLS-1$

	public static final String JUNIT3_TEST_KIND_ID = "org.eclipse.unittest.junit.loader.junit3"; //$NON-NLS-1$
	public static final String JUNIT4_TEST_KIND_ID = "org.eclipse.unittest.junit.loader.junit4"; //$NON-NLS-1$
	public static final String JUNIT5_TEST_KIND_ID = "org.eclipse.unittest.junit.loader.junit5"; //$NON-NLS-1$

	/**
	 * The class path variable referring to the junit home location
	 */
	/*
	 * public final static String JUNIT_HOME= "JUNIT_HOME"; //$NON-NLS-1$
	 */
	/**
	 * The class path variable referring to the junit source location
	 *
	 * @since 3.2
	 */
	/*
	 * public static final String JUNIT_SRC_HOME= "JUNIT_SRC_HOME"; //$NON-NLS-1$
	 *
	 * private static final String HISTORY_DIR_NAME= "history"; //$NON-NLS-1$
	 */
	private BundleContext fBundleContext;

	private static boolean fIsStopped = false;

	public JUnitPlugin() {
		fgPlugin = this;
	}

	public static JUnitPlugin getDefault() {
		return fgPlugin;
	}

	public static String getPluginId() {
		return PLUGIN_ID;
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext = context;
		UnitTestPlugin.getDefault();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		fIsStopped = true;
		try {
			InstanceScope.INSTANCE.getNode(JUnitPlugin.CORE_PLUGIN_ID).flush();
		} finally {
			super.stop(context);
		}
		fBundleContext = null;
	}

	/**
	 * Returns a service with the specified name or <code>null</code> if none.
	 *
	 * @param serviceName name of service
	 * @return service object or <code>null</code> if none
	 * @since 3.5
	 */
	public Object getService(String serviceName) {
		ServiceReference<?> reference = fBundleContext.getServiceReference(serviceName);
		if (reference == null)
			return null;
		return fBundleContext.getService(reference);
	}

	public static String getContainerTestKindId(IJavaElement element) {
		if (element != null) {
			IJavaProject project = element.getJavaProject();
			if (CoreTestSearchEngine.is50OrHigher(project)) {
				if (CoreTestSearchEngine.is18OrHigher(project)) {
					if (isRunWithJUnitPlatform(element)) {
						return JUNIT4_TEST_KIND_ID;
					}
					if (CoreTestSearchEngine.hasJUnit5TestAnnotation(project)) {
						return JUNIT5_TEST_KIND_ID;
					}
				}
				if (CoreTestSearchEngine.hasJUnit4TestAnnotation(project)) {
					return JUNIT4_TEST_KIND_ID;
				}
			}
		}
		return JUNIT3_TEST_KIND_ID;
	}

	/**
	 * @param element the element
	 * @return <code>true</code> if the element is a test class annotated with
	 *         <code>@RunWith(JUnitPlatform.class)</code>
	 */
	public static boolean isRunWithJUnitPlatform(IJavaElement element) {
		if (element instanceof ICompilationUnit) {
			element = ((ICompilationUnit) element).findPrimaryType();
		}
		if (element instanceof IType) {
			IType type = (IType) element;
			try {
				IAnnotation runWithAnnotation = type.getAnnotation("RunWith"); //$NON-NLS-1$
				if (!runWithAnnotation.exists()) {
					runWithAnnotation = type.getAnnotation("org.junit.runner.RunWith"); //$NON-NLS-1$
				}
				if (runWithAnnotation.exists()) {
					IMemberValuePair[] memberValuePairs = runWithAnnotation.getMemberValuePairs();
					for (IMemberValuePair memberValuePair : memberValuePairs) {
						if (memberValuePair.getMemberName().equals("value") //$NON-NLS-1$
								&& memberValuePair.getValue().equals("JUnitPlatform")) { //$NON-NLS-1$
							return true;
						}
					}
				}
			} catch (JavaModelException e) {
				// ignore
			}
		}
		return false;
	}

	public static ITestKind getContainerTestKind(IJavaElement element) {
		return TestKindRegistry.getDefault().getKind(getContainerTestKindId(element));
	}

	public static final String ORIGINAL_JUNIT3_TEST_KIND_ID = "org.eclipse.jdt.junit.loader.junit3"; //$NON-NLS-1$
	public static final String ORIGINAL_JUNIT4_TEST_KIND_ID = "org.eclipse.jdt.junit.loader.junit4"; //$NON-NLS-1$
	public static final String ORIGINAL_JUNIT5_TEST_KIND_ID = "org.eclipse.jdt.junit.loader.junit5"; //$NON-NLS-1$

	@SuppressWarnings("restriction")
	public static ITestFinder getTestFinder(ITestKind kind) {
		switch (kind.getId()) {
		case JUNIT3_TEST_KIND_ID:
			return org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.getDefault()
					.getKind(ORIGINAL_JUNIT3_TEST_KIND_ID).getFinder();
		case JUNIT4_TEST_KIND_ID:
			return org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.getDefault()
					.getKind(ORIGINAL_JUNIT4_TEST_KIND_ID).getFinder();
		case JUNIT5_TEST_KIND_ID:
			return org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.getDefault()
					.getKind(ORIGINAL_JUNIT5_TEST_KIND_ID).getFinder();
		}
		return ITestFinder.NULL;
	}
}