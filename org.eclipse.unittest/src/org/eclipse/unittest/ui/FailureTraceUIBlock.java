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
 *******************************************************************************/
package org.eclipse.unittest.ui;

import org.eclipse.unittest.internal.UnitTestPreferencesConstants;
import org.eclipse.unittest.internal.ui.CompareResultsAction;
import org.eclipse.unittest.internal.ui.EnableStackFilterAction;
import org.eclipse.unittest.internal.ui.FailureTableDisplay;
import org.eclipse.unittest.internal.ui.ShowStackTraceInConsoleViewAction;
import org.eclipse.unittest.internal.ui.TextualTrace;
import org.eclipse.unittest.internal.ui.UnitTestCopyAction;
import org.eclipse.unittest.launcher.ITestViewSupport;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.OpenStrategy;

/**
 * A pane that shows a stack trace of a failed test.
 */
public class FailureTraceUIBlock implements IMenuListener {

	/*
	 * Internal property change listener for handling workbench font changes.
	 *
	 * private class FontPropertyChangeListener implements IPropertyChangeListener {
	 *
	 * @Override public void propertyChange(PropertyChangeEvent event) { if (fTable
	 * == null) return;
	 *
	 * String property= event.getProperty();
	 *
	 * if (FAILURE_FONT.equals(property))
	 * fTable.setFont(JFaceResources.getFont(FAILURE_FONT)); } }
	 */
	private static final int MAX_LABEL_LENGTH = 256;

	public static final String FRAME_PREFIX = "at "; //$NON-NLS-1$
	private Table fTable;
	private TestRunnerViewPart fTestRunner;
	private String fInputTrace;
	private final Clipboard fClipboard;
	private ITestElement fFailure;
	private CompareResultsAction fCompareAction;
	private final FailureTableDisplay fFailureTableDisplay;
	private ShowStackTraceInConsoleViewAction fShowTraceInConsoleAction;

	public FailureTraceUIBlock(Composite parent, Clipboard clipboard, TestRunnerViewPart testRunner, ToolBar toolBar) {
		Assert.isNotNull(clipboard);

		// fill the failure trace viewer toolbar
		ToolBarManager failureToolBarmanager = new ToolBarManager(toolBar);
		fShowTraceInConsoleAction = new ShowStackTraceInConsoleViewAction();
		fShowTraceInConsoleAction.setDelegate(null);
		fShowTraceInConsoleAction.setEnabled(false);
		failureToolBarmanager.add(fShowTraceInConsoleAction);
		failureToolBarmanager.add(new EnableStackFilterAction(this));
		fCompareAction = new CompareResultsAction(this);
		fCompareAction.setEnabled(false);
		failureToolBarmanager.add(fCompareAction);
		failureToolBarmanager.update(true);
		fTable = new Table(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		fTestRunner = testRunner;
		fClipboard = clipboard;

		OpenStrategy handler = new OpenStrategy(fTable);
		handler.addOpenListener(e -> {
			if (fTable.getSelectionIndex() == 0 && fFailure.isComparisonFailure()) {
				fCompareAction.run();
			}
			if (fTable.getSelection().length != 0) {
				IAction a = createOpenEditorAction(getSelectedText());
				if (a != null)
					a.run();
			}
		});

		initMenu();

		fFailureTableDisplay = new FailureTableDisplay(fTable);
	}

	private void initMenu() {
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this);
		Menu menu = menuMgr.createContextMenu(fTable);
		fTable.setMenu(menu);
	}

	@Override
	public void menuAboutToShow(IMenuManager manager) {
		if (fTable.getSelectionCount() > 0) {
			IAction a = createOpenEditorAction(getSelectedText());
			if (a != null)
				manager.add(a);
			manager.add(new UnitTestCopyAction(FailureTraceUIBlock.this, fClipboard));
		}
		// fix for bug 68058
		if (fFailure != null && fFailure.isComparisonFailure())
			manager.add(fCompareAction);
	}

	public String getTrace() {
		return fInputTrace;
	}

	private String getSelectedText() {
		return fTable.getSelection()[0].getText();
	}

	private IAction createOpenEditorAction(String traceLine) {
		return fFailure.getTestRunSession().getTestViewSupport().createOpenEditorAction(fTestRunner, fFailure,
				traceLine);
	}

	/**
	 * Returns the composite used to present the trace
	 *
	 * @return The composite
	 */
	public Composite getComposite() {
		return fTable;
	}

	/**
	 * Refresh the table from the trace.
	 */
	public void refresh() {
		updateTable(fInputTrace);
	}

	/**
	 * Shows a TestFailure
	 *
	 * @param test the failed test
	 */
	public void showFailure(ITestElement test) {
		fFailure = test;
		String trace = ""; //$NON-NLS-1$
		updateActions(test);
		updateEnablement(test);
		if (test != null)
			trace = test.getTrace();
		if (fInputTrace == trace)
			return;
		fInputTrace = trace;
		updateTable(trace);
	}

	private void updateActions(ITestElement test) {
		ITestViewSupport testViewSupport = test != null ? fFailure.getTestRunSession().getTestViewSupport() : null;
		fShowTraceInConsoleAction.setDelegate(
				testViewSupport != null ? testViewSupport.createShowStackTraceInConsoleViewActionDelegate(this) : null);
	}

	private void updateEnablement(ITestElement test) {
		boolean enableCompare = test != null && test.isComparisonFailure();
		fCompareAction.setEnabled(enableCompare);
		if (enableCompare) {
			fCompareAction.updateOpenDialog(test);
		}

		boolean enableShowTraceInConsole = test != null && test.getFailureTrace() != null;
		fShowTraceInConsoleAction.setEnabled(enableShowTraceInConsole);
	}

	private void updateTable(String trace) {
		if (trace == null || trace.trim().isEmpty()) {
			clear();
			return;
		}
		trace = trace.trim();
		fTable.setRedraw(false);
		fTable.removeAll();
		new TextualTrace(trace, getFilterPatterns()).display(fFailureTableDisplay, MAX_LABEL_LENGTH);
		fTable.setRedraw(true);
	}

	private String[] getFilterPatterns() {
		if (UnitTestPreferencesConstants.getFilterStack())
			return getFilterPatterns(fFailure.getTestRunSession());
		return new String[0];
	}

	/**
	 * Returns an array of Filter patterns for Stacktraces/Error messages
	 *
	 * @param session a {@link ITestRunSession} to ask the filter pattern for
	 * @return an array of filter patterns
	 */
	public String[] getFilterPatterns(ITestRunSession session) {
		if (session != null && session.getTestViewSupport() != null) {
			return session.getTestViewSupport().getFilterPatterns();
		}
		return new String[0];
	}

	/**
	 * Shows other information than a stack trace.
	 *
	 * @param text the informational message to be shown
	 */
	public void setInformation(String text) {
		clear();
		TableItem tableItem = fFailureTableDisplay.newTableItem();
		tableItem.setText(text);
	}

	/**
	 * Clears the non-stack trace info
	 */
	public void clear() {
		fTable.removeAll();
		fInputTrace = null;
	}

	public ITestElement getFailedTest() {
		return fFailure;
	}

	public Shell getShell() {
		return fTable.getShell();
	}

	public FailureTableDisplay getFailureTableDisplay() {
		return fFailureTableDisplay;
	}

	public void dispose() {
//		JFaceResources.getFontRegistry().removeListener(fFontPropertyChangeListener);
	}
}
