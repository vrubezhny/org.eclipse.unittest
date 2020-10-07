/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.unittest.internal.model;

import org.eclipse.unittest.model.ITestElement.Result;

public final class Status {
	public static final Status RUNNING_ERROR = new Status("RUNNING_ERROR"); //$NON-NLS-1$
	public static final Status RUNNING_FAILURE = new Status("RUNNING_FAILURE"); //$NON-NLS-1$
	public static final Status RUNNING = new Status("RUNNING"); //$NON-NLS-1$

	public static final Status ERROR = new Status("ERROR"); //$NON-NLS-1$
	public static final Status FAILURE = new Status("FAILURE"); //$NON-NLS-1$
	public static final Status OK = new Status("OK"); //$NON-NLS-1$
	public static final Status NOT_RUN = new Status("NOT_RUN"); //$NON-NLS-1$

	private static final Status[] OLD_CODE = { OK, ERROR, FAILURE };

	private final String fName;

	private Status(String name) {
		fName = name;
	}

	@Override
	public String toString() {
		return fName;
	}

	/* error state predicates */

	public boolean isOK() {
		return this == OK || this == RUNNING || this == NOT_RUN;
	}

	public boolean isFailure() {
		return this == FAILURE || this == RUNNING_FAILURE;
	}

	public boolean isError() {
		return this == ERROR || this == RUNNING_ERROR;
	}

	public boolean isErrorOrFailure() {
		return isError() || isFailure();
	}

	/* progress state predicates */

	public boolean isNotRun() {
		return this == NOT_RUN;
	}

	public boolean isRunning() {
		return this == RUNNING || this == RUNNING_FAILURE || this == RUNNING_ERROR;
	}

	public boolean isDone() {
		return this == OK || this == FAILURE || this == ERROR;
	}

	/**
	 * @param oldStatus one of {@link Status}'s constants
	 * @return the Status
	 */
	public static Status convert(int oldStatus) {
		return OLD_CODE[oldStatus];
	}

	public Result convertToResult() {
		if (isNotRun())
			return Result.UNDEFINED;
		if (isError())
			return Result.ERROR;
		if (isFailure())
			return Result.FAILURE;
		if (isRunning()) {
			return Result.UNDEFINED;
		}
		return Result.OK;
	}

	public ProgressState convertToProgressState() {
		if (isRunning()) {
			return ProgressState.RUNNING;
		}
		if (isDone()) {
			return ProgressState.COMPLETED;
		}
		return ProgressState.NOT_STARTED;
	}

	public static Status fromResult(Result status) {
		switch (status) {
		case ERROR:
			return Status.ERROR;
		case FAILURE:
			return Status.FAILURE;
		case OK:
			return Status.OK;
		case IGNORED:
			return Status.OK;
		case UNDEFINED:
			return Status.NOT_RUN;
		default:
			return Status.NOT_RUN;
		}
	}

}