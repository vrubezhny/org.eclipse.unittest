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

import java.util.Arrays;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.eclipse.unittest.UnitTestPlugin;

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

import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;

/**
 * The plug-in runtime class for the JUnit core plug-in.
 */
public class JUnitTestPlugin extends AbstractUIPlugin {

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static JUnitTestPlugin fgPlugin = null;

	public static final String CORE_PLUGIN_ID = "org.eclipse.unittest"; //$NON-NLS-1$

	public static final String PLUGIN_ID = "org.eclipse.unittest.junit"; //$NON-NLS-1$

	public static final String UNIT_TEST_VIEW_SUPPORT_ID = "org.eclipse.unittest.junit"; //$NON-NLS-1$

	public enum JUnitVersion {
		JUNIT3("org.eclipse.jdt.junit.loader.junit3"), //$NON-NLS-1$
		JUNIT4("org.eclipse.jdt.junit.loader.junit4"), //$NON-NLS-1$
		JUNIT5("org.eclipse.jdt.junit.loader.junit5"); //$NON-NLS-1$

		public final String junitTestKindId;

		private JUnitVersion(String junitTestKindId) {
			this.junitTestKindId = junitTestKindId;
		}

		public static JUnitVersion fromJUnitTestKindId(String junitTestKindId) {
			return Arrays.stream(values()).filter(version -> version.junitTestKindId.equals(junitTestKindId)).findAny()
					.orElse(null);
		}

		public ITestKind getJUnitTestKind() {
			return org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.getDefault().getKind(junitTestKindId);
		}
	}

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

	public JUnitTestPlugin() {
		fgPlugin = this;
	}

	public static JUnitTestPlugin getDefault() {
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
			InstanceScope.INSTANCE.getNode(JUnitTestPlugin.CORE_PLUGIN_ID).flush();
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

	public static JUnitVersion getJUnitVersion(IJavaElement element) {
		if (element != null) {
			IJavaProject project = element.getJavaProject();
			if (CoreTestSearchEngine.is50OrHigher(project)) {
				if (CoreTestSearchEngine.is18OrHigher(project)) {
					if (isRunWithJUnitPlatform(element)) {
						return JUnitVersion.JUNIT4;
					}
					if (CoreTestSearchEngine.hasJUnit5TestAnnotation(project)) {
						return JUnitVersion.JUNIT5;
					}
				}
				if (CoreTestSearchEngine.hasJUnit4TestAnnotation(project)) {
					return JUnitVersion.JUNIT4;
				}
			}
		}
		return JUnitVersion.JUNIT3;
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

}