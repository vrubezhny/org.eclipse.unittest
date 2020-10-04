/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.internal.model;

/**
 * Running states of a test.
 */
public final class ProgressState {
	/** state that describes that the test element has not started */
	public static final ProgressState NOT_STARTED = new ProgressState("Not Started"); //$NON-NLS-1$
	/** state that describes that the test element has is running */
	public static final ProgressState RUNNING = new ProgressState("Running"); //$NON-NLS-1$
	/**
	 * state that describes that the test element has been stopped before being
	 * completed
	 */
	public static final ProgressState STOPPED = new ProgressState("Stopped"); //$NON-NLS-1$
	/** state that describes that the test element has completed */
	public static final ProgressState COMPLETED = new ProgressState("Completed"); //$NON-NLS-1$

	private String fName;

	private ProgressState(String name) {
		fName = name;
	}

	@Override
	public String toString() {
		return fName;
	}
}