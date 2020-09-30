/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.unittest;

import org.osgi.framework.BundleContext;

import org.eclipse.unittest.internal.model.UnitTestModel;
import org.eclipse.unittest.model.IUnitTestModel;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The plug-in runtime class for the Unit Test plug-in.
 */
public class UnitTestPlugin extends AbstractUIPlugin {

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static UnitTestPlugin fgPlugin = null;

	public static final String PLUGIN_ID = "org.eclipse.unittest"; //$NON-NLS-1$

	/**
	 * Constructs a {@link UnitTestPlugin} object
	 */
	public UnitTestPlugin() {
		fgPlugin = this;
	}

	/**
	 * Returns the {@link UnitTestPlugin} instance
	 *
	 * @return a {@link UnitTestPlugin} instance
	 */
	public static UnitTestPlugin getDefault() {
		return fgPlugin;
	}

	/**
	 * Logs the given exception.
	 *
	 * @param e the {@link Throwable} to log
	 */
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	/**
	 * Logs the given status.
	 *
	 * @param status the status to log
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fUnitTestModel.start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		try {
			InstanceScope.INSTANCE.getNode(UnitTestPlugin.PLUGIN_ID).flush();
			fUnitTestModel.stop();
		} finally {
			super.stop(context);
		}
	}

	/*
	 * The following is copied here from JUnitCorePlugin Most likely we need to
	 * place it into UnitTestCorePlugin
	 */

	private final UnitTestModel fUnitTestModel = new UnitTestModel();

	/**
	 * Returns a {@link IUnitTestModel} instance
	 *
	 * @return a {@link IUnitTestModel} instance
	 */
	public static IUnitTestModel getModel() {
		return getDefault().fUnitTestModel;
	}

}
