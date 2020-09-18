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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.model.TestSuiteElement;
import org.eclipse.unittest.internal.ui.CopyFailureListAction;
import org.eclipse.unittest.internal.ui.SelectionProviderMediator;
import org.eclipse.unittest.internal.ui.TestSessionTableContentProvider;
import org.eclipse.unittest.internal.ui.TestSessionTreeContentProvider;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.Result;
import org.eclipse.unittest.model.ITestElement.Status;
import org.eclipse.unittest.model.ITestRoot;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.PageBook;

import org.eclipse.debug.core.ILaunchManager;

class TestViewer {
	private final class TestSelectionListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			handleSelected();
		}
	}

	private final class TestOpenListener extends SelectionAdapter {
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			handleDefaultSelected();
		}
	}

	private final class FailuresOnlyFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return select(((ITestElement) element));
		}

		public boolean select(ITestElement testElement) {
			Status status = testElement.getStatus();
			if (status.isErrorOrFailure())
				return true;
			else
				return !fTestRunSession.isRunning() && status == Status.RUNNING; // rerunning
		}
	}

	private final class IgnoredOnlyFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return select(((ITestElement) element));
		}

		public boolean select(ITestElement testElement) {
			if (hasIgnoredInTestResult(testElement))
				return true;
			else
				return !fTestRunSession.isRunning() && testElement.getStatus() == Status.RUNNING; // rerunning
		}

		/**
		 * Checks whether a test was skipped i.e. it was ignored (<code>@Ignored</code>)
		 * or had any assumption failure.
		 *
		 * @param testElement the test element (a test suite or a single test case)
		 *
		 * @return <code>true</code> if the test element or any of its children has
		 *         {@link Result#IGNORED} test result
		 */
		private boolean hasIgnoredInTestResult(ITestElement testElement) {
			if (testElement instanceof ITestSuiteElement) {
				ITestElement[] children = ((ITestSuiteElement) testElement).getChildren();
				for (ITestElement child : children) {
					boolean hasIgnoredTestResult = hasIgnoredInTestResult(child);
					if (hasIgnoredTestResult) {
						return true;
					}
				}
				return false;
			}

			return testElement.getTestResult(false) == Result.IGNORED;
		}
	}

	private static class ReverseList<E> extends AbstractList<E> {
		private final List<E> fList;

		public ReverseList(List<E> list) {
			fList = list;
		}

		@Override
		public E get(int index) {
			return fList.get(fList.size() - index - 1);
		}

		@Override
		public int size() {
			return fList.size();
		}
	}

	private class ExpandAllAction extends Action {
		public ExpandAllAction() {
			setText(Messages.ExpandAllAction_text);
			setToolTipText(Messages.ExpandAllAction_tooltip);
		}

		@Override
		public void run() {
			fTreeViewer.expandAll();
		}
	}

	private class CollapseAllAction extends Action {
		public CollapseAllAction() {
			setText(Messages.CollapseAllAction_text);
			setToolTipText(Messages.CollapseAllAction_tooltip);
		}

		@Override
		public void run() {
			fTreeViewer.collapseAll();
		}
	}

	private final FailuresOnlyFilter fFailuresOnlyFilter = new FailuresOnlyFilter();
	private final IgnoredOnlyFilter fIgnoredOnlyFilter = new IgnoredOnlyFilter();

	private final TestRunnerViewPart fTestRunnerPart;
	private final Clipboard fClipboard;

	private PageBook fViewerbook;
	private TreeViewer fTreeViewer;
	private TestSessionTreeContentProvider fTreeContentProvider;
	private TestSessionLabelProvider fTreeLabelProvider;
	private TableViewer fTableViewer;
	private TestSessionTableContentProvider fTableContentProvider;
	private TestSessionLabelProvider fTableLabelProvider;
	private SelectionProviderMediator fSelectionProvider;

	private int fLayoutMode;
	private boolean fTreeHasFilter;
	private boolean fTableHasFilter;

	private ITestRunSession fTestRunSession;

	private boolean fTreeNeedsRefresh;
	private boolean fTableNeedsRefresh;
	private HashSet<ITestElement> fNeedUpdate;
	private ITestCaseElement fAutoScrollTarget;

	private LinkedList<ITestSuiteElement> fAutoClose;
	private HashSet<ITestSuiteElement> fAutoExpand;

	public TestViewer(Composite parent, Clipboard clipboard, TestRunnerViewPart runner) {
		fTestRunnerPart = runner;
		fClipboard = clipboard;

		fLayoutMode = TestRunnerViewPart.LAYOUT_HIERARCHICAL;

		createTestViewers(parent);

		registerViewersRefresh();

		initContextMenu();
	}

	private void createTestViewers(Composite parent) {
		fViewerbook = new PageBook(parent, SWT.NULL);

		fTreeViewer = new TreeViewer(fViewerbook, SWT.V_SCROLL | SWT.SINGLE);
		fTreeViewer.setUseHashlookup(true);
		fTreeContentProvider = new TestSessionTreeContentProvider();
		fTreeViewer.setContentProvider(fTreeContentProvider);
		fTreeLabelProvider = new TestSessionLabelProvider(fTestRunnerPart, TestRunnerViewPart.LAYOUT_HIERARCHICAL);
//		fTreeViewer.setLabelProvider(new ColoringLabelProvider(fTreeLabelProvider));
		fTreeViewer.setLabelProvider(fTreeLabelProvider);

		fTableViewer = new TableViewer(fViewerbook, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE);
		fTableViewer.setUseHashlookup(true);
		fTableContentProvider = new TestSessionTableContentProvider();
		fTableViewer.setContentProvider(fTableContentProvider);
		fTableLabelProvider = new TestSessionLabelProvider(fTestRunnerPart, TestRunnerViewPart.LAYOUT_FLAT);
//		fTableViewer.setLabelProvider(new ColoringLabelProvider(fTableLabelProvider));
		fTableViewer.setLabelProvider(fTableLabelProvider);

		fSelectionProvider = new SelectionProviderMediator(new StructuredViewer[] { fTreeViewer, fTableViewer },
				fTreeViewer);
		fSelectionProvider.addSelectionChangedListener(new TestSelectionListener());
		TestOpenListener testOpenListener = new TestOpenListener();
		fTreeViewer.getTree().addSelectionListener(testOpenListener);
		fTableViewer.getTable().addSelectionListener(testOpenListener);

		fTestRunnerPart.getSite().setSelectionProvider(fSelectionProvider);

		fViewerbook.showPage(fTreeViewer.getTree());
	}

	private void initContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this::handleMenuAboutToShow);
		fTestRunnerPart.getSite().registerContextMenu(menuMgr, fSelectionProvider);
		Menu menu = menuMgr.createContextMenu(fViewerbook);
		fTreeViewer.getTree().setMenu(menu);
		fTableViewer.getTable().setMenu(menu);
	}

	void handleMenuAboutToShow(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) fSelectionProvider.getSelection();
		if (!selection.isEmpty()) {
			ITestElement testElement = (ITestElement) selection.getFirstElement();

			if (testElement instanceof ITestSuiteElement) {
				ITestSuiteElement testSuiteElement = (ITestSuiteElement) testElement;
				IAction openTestAction = testSuiteElement.getTestRunSession().getTestViewSupport()
						.getOpenTestAction(fTestRunnerPart, testSuiteElement);
				if (openTestAction != null) {
					manager.add(openTestAction);
				}
				manager.add(new Separator());
				if (!fTestRunnerPart.lastLaunchIsKeptAlive()) {
					addRerunActions(manager, testSuiteElement);
				}
			} else {
				ITestCaseElement testCaseElement = (ITestCaseElement) testElement;
				IAction openTestAction = testElement.getTestRunSession().getTestViewSupport()
						.getOpenTestAction(fTestRunnerPart, testCaseElement);
				if (openTestAction != null) {
					manager.add(openTestAction);
				}
				manager.add(new Separator());
				addRerunActions(manager, testCaseElement);
			}
			if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL) {
				manager.add(new Separator());
				manager.add(new ExpandAllAction());
				manager.add(new CollapseAllAction());
			}

		}
		if (fTestRunSession != null && fTestRunSession.getFailureCount() + fTestRunSession.getErrorCount() > 0) {
			if (fLayoutMode != TestRunnerViewPart.LAYOUT_HIERARCHICAL)
				manager.add(new Separator());
			manager.add(new CopyFailureListAction(fTestRunnerPart, fClipboard));
		}
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS + "-end")); //$NON-NLS-1$
	}

	private void addRerunActions(IMenuManager manager, ITestCaseElement testCaseElement) {
		String className = testCaseElement.getTestClassName();
		String testMethodName = testCaseElement.getTestMethodName();
		String[] parameterTypes = testCaseElement.getParameterTypes();
		if (parameterTypes != null) {
			String paramTypesStr = Arrays.stream(parameterTypes).collect(Collectors.joining(",")); //$NON-NLS-1$
			testMethodName = testMethodName + "(" + paramTypesStr + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fTestRunnerPart.lastLaunchIsKeptAlive()) {
			manager.add(new RerunAction(Messages.RerunAction_label_rerun, fTestRunnerPart, testCaseElement.getId(),
					className, testMethodName, testCaseElement.getDisplayName(), testCaseElement.getUniqueId(),
					ILaunchManager.RUN_MODE));
		} else {
			manager.add(new RerunAction(Messages.RerunAction_label_run, fTestRunnerPart, testCaseElement.getId(),
					className, testMethodName, testCaseElement.getDisplayName(), testCaseElement.getUniqueId(),
					ILaunchManager.RUN_MODE));
			manager.add(new RerunAction(Messages.RerunAction_label_debug, fTestRunnerPart, testCaseElement.getId(),
					className, testMethodName, testCaseElement.getDisplayName(), testCaseElement.getUniqueId(),
					ILaunchManager.DEBUG_MODE));
		}
	}

	private void addRerunActions(IMenuManager manager, ITestSuiteElement testSuiteElement) {
		RerunAction[] rerunActions = testSuiteElement.getTestRunSession().getTestViewSupport()
				.getRerunActions(fTestRunnerPart, testSuiteElement);
		if (rerunActions != null) {
			for (RerunAction action : rerunActions) {
				if (action != null) {
					manager.add(action);
				}
			}
		}
	}

	public Control getTestViewerControl() {
		return fViewerbook;
	}

	public synchronized void registerActiveSession(ITestRunSession testRunSession) {
		fTestRunSession = testRunSession;
		registerAutoScrollTarget(null);
		registerViewersRefresh();
	}

	void handleDefaultSelected() {
		IStructuredSelection selection = (IStructuredSelection) fSelectionProvider.getSelection();
		if (selection.size() != 1)
			return;

		ITestElement testElement = (ITestElement) selection.getFirstElement();
		IOpenEditorAction action;
		if (testElement instanceof ITestSuiteElement) {
			action = testElement.getTestRunSession().getTestViewSupport().getOpenTestAction(fTestRunnerPart,
					(ITestSuiteElement) testElement);
		} else if (testElement instanceof ITestCaseElement) {
			action = testElement.getTestRunSession().getTestViewSupport().getOpenTestAction(fTestRunnerPart,
					(ITestCaseElement) testElement);
		} else {
			throw new IllegalStateException(String.valueOf(testElement));
		}

		if (action != null && action.isEnabled())
			action.run();
		return;
	}

	private void handleSelected() {
		IStructuredSelection selection = (IStructuredSelection) fSelectionProvider.getSelection();
		ITestElement testElement = null;
		if (selection.size() == 1) {
			testElement = (ITestElement) selection.getFirstElement();
		}
		fTestRunnerPart.handleTestSelected(testElement);
	}

	public synchronized void setShowTime(boolean showTime) {
		try {
			fViewerbook.setRedraw(false);
			fTreeLabelProvider.setShowTime(showTime);
			fTableLabelProvider.setShowTime(showTime);
		} finally {
			fViewerbook.setRedraw(true);
		}
	}

	/**
	 * It makes sense to display either failed or ignored tests, not both together.
	 *
	 * @param failuresOnly whether to show only failed tests
	 * @param ignoredOnly  whether to show only skipped tests
	 * @param layoutMode   the layout mode
	 */
	public synchronized void setShowFailuresOrIgnoredOnly(boolean failuresOnly, boolean ignoredOnly, int layoutMode) {
		/*
		 * Management of fTreeViewer and fTableViewer
		 * ****************************************** - invisible viewer is updated on
		 * registerViewerUpdate unless its f*NeedsRefresh is true - invisible viewer is
		 * not refreshed upfront - on layout change, new viewer is refreshed if
		 * necessary - filter only applies to "current" layout mode / viewer
		 */
		try {
			fViewerbook.setRedraw(false);

			IStructuredSelection selection = null;
			boolean switchLayout = layoutMode != fLayoutMode;
			if (switchLayout) {
				selection = (IStructuredSelection) fSelectionProvider.getSelection();
				if (layoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL) {
					if (fTreeNeedsRefresh) {
						clearUpdateAndExpansion();
					}
				} else {
					if (fTableNeedsRefresh) {
						clearUpdateAndExpansion();
					}
				}
				fLayoutMode = layoutMode;
				fViewerbook.showPage(getActiveViewer().getControl());
			}
			// avoid realizing all TableItems, especially in flat mode!
			StructuredViewer viewer = getActiveViewer();
			if (failuresOnly || ignoredOnly) {
				if (getActiveViewerHasFilter()) {
					// For simplicity clear both filters (only one of them is used)
					viewer.removeFilter(fFailuresOnlyFilter);
					viewer.removeFilter(fIgnoredOnlyFilter);
				}
				setActiveViewerHasFilter(true);
				viewer.setInput(null);
				// Set either the failures or the skipped tests filter
				ViewerFilter filter = fFailuresOnlyFilter;
				if (ignoredOnly == true) {
					filter = fIgnoredOnlyFilter;
				}
				viewer.addFilter(filter);
				setActiveViewerNeedsRefresh(true);

			} else {
				if (getActiveViewerHasFilter()) {
					setActiveViewerNeedsRefresh(true);
					setActiveViewerHasFilter(false);
					viewer.setInput(null);
					viewer.removeFilter(fIgnoredOnlyFilter);
					viewer.removeFilter(fFailuresOnlyFilter);
				}
			}
			processChangesInUI();

			if (selection != null) {
				// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=125708
				// (ITreeSelection not adapted if TreePaths changed):
				StructuredSelection flatSelection = new StructuredSelection(selection.toList());
				fSelectionProvider.setSelection(flatSelection, true);
			}

		} finally {
			fViewerbook.setRedraw(true);
		}
	}

	private boolean getActiveViewerHasFilter() {
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL)
			return fTreeHasFilter;
		else
			return fTableHasFilter;
	}

	private void setActiveViewerHasFilter(boolean filter) {
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL)
			fTreeHasFilter = filter;
		else
			fTableHasFilter = filter;
	}

	private StructuredViewer getActiveViewer() {
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL)
			return fTreeViewer;
		else
			return fTableViewer;
	}

	private boolean getActiveViewerNeedsRefresh() {
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL)
			return fTreeNeedsRefresh;
		else
			return fTableNeedsRefresh;
	}

	private void setActiveViewerNeedsRefresh(boolean needsRefresh) {
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL)
			fTreeNeedsRefresh = needsRefresh;
		else
			fTableNeedsRefresh = needsRefresh;
	}

	/**
	 * To be called periodically by the TestRunnerViewPart (in the UI thread).
	 */
	public void processChangesInUI() {
		if (fTestRunSession == null) {
			registerViewersRefresh();
			fTreeNeedsRefresh = false;
			fTableNeedsRefresh = false;
			fTreeViewer.setInput(null);
			fTableViewer.setInput(null);
			return;
		}

		ITestRoot testRoot = fTestRunSession.getTestRoot();

		StructuredViewer viewer = getActiveViewer();
		if (getActiveViewerNeedsRefresh()) {
			clearUpdateAndExpansion();
			setActiveViewerNeedsRefresh(false);
			viewer.setInput(testRoot);

		} else {
			Object[] toUpdate;
			synchronized (this) {
				toUpdate = fNeedUpdate.toArray();
				fNeedUpdate.clear();
			}
			if (!fTreeNeedsRefresh && toUpdate.length > 0) {
				if (fTreeHasFilter)
					for (Object element : toUpdate)
						updateElementInTree((ITestElement) element);
				else {
					HashSet<Object> toUpdateWithParents = new HashSet<>();
					toUpdateWithParents.addAll(Arrays.asList(toUpdate));
					for (Object element : toUpdate) {
						ITestElement parent = ((ITestElement) element).getParent();
						while (parent != null) {
							toUpdateWithParents.add(parent);
							parent = parent.getParent();
						}
					}
					fTreeViewer.update(toUpdateWithParents.toArray(), null);
				}
			}
			if (!fTableNeedsRefresh && toUpdate.length > 0) {
				if (fTableHasFilter)
					for (Object element : toUpdate)
						updateElementInTable((ITestElement) element);
				else
					fTableViewer.update(toUpdate, null);
			}
		}
		autoScrollInUI();
	}

	private void updateElementInTree(final ITestElement testElement) {
		if (isShown(testElement)) {
			updateShownElementInTree(testElement);
		} else {
			ITestElement current = testElement;
			do {
				if (fTreeViewer.testFindItem(current) != null)
					fTreeViewer.remove(current);
				current = current.getParent();
			} while (!(current instanceof ITestRoot) && !isShown(current));

			while (current != null && !(current instanceof ITestRoot)) {
				fTreeViewer.update(current, null);
				current = current.getParent();
			}
		}
	}

	private void updateShownElementInTree(ITestElement testElement) {
		if (testElement == null || testElement instanceof ITestRoot) // paranoia null check
			return;

		ITestSuiteElement parent = testElement.getParent();
		updateShownElementInTree(parent); // make sure parent is shown and up-to-date

		if (fTreeViewer.testFindItem(testElement) == null) {
			fTreeViewer.add(parent, testElement); // if not yet in tree: add
		} else {
			fTreeViewer.update(testElement, null); // if in tree: update
		}
	}

	private void updateElementInTable(ITestElement element) {
		if (isShown(element)) {
			if (fTableViewer.testFindItem(element) == null) {
				ITestElement previous = getNextFailure(element, false);
				int insertionIndex = -1;
				if (previous != null) {
					TableItem item = (TableItem) fTableViewer.testFindItem(previous);
					if (item != null)
						insertionIndex = fTableViewer.getTable().indexOf(item);
				}
				fTableViewer.insert(element, insertionIndex);
			} else {
				fTableViewer.update(element, null);
			}
		} else {
			fTableViewer.remove(element);
		}
	}

	private boolean isShown(ITestElement current) {
		return fFailuresOnlyFilter.select(current);
	}

	private void autoScrollInUI() {
		if (!fTestRunnerPart.isAutoScroll()) {
			clearAutoExpand();
			fAutoClose.clear();
			return;
		}

		if (fLayoutMode == TestRunnerViewPart.LAYOUT_FLAT) {
			if (fAutoScrollTarget != null)
				fTableViewer.reveal(fAutoScrollTarget);
			return;
		}

		synchronized (this) {
			for (ITestSuiteElement suite : fAutoExpand) {
				fTreeViewer.setExpandedState(suite, true);
			}
			clearAutoExpand();
		}

		ITestCaseElement current = fAutoScrollTarget;
		fAutoScrollTarget = null;

		ITestSuiteElement parent = current == null ? null : (ITestSuiteElement) fTreeContentProvider.getParent(current);
		if (fAutoClose.isEmpty() || !fAutoClose.getLast().equals(parent)) {
			// we're in a new branch, so let's close old OK branches:
			for (ListIterator<ITestSuiteElement> iter = fAutoClose.listIterator(fAutoClose.size()); iter
					.hasPrevious();) {
				ITestSuiteElement previousAutoOpened = iter.previous();
				if (previousAutoOpened.equals(parent))
					break;

				if (previousAutoOpened.getStatus() == TestElement.Status.OK) {
					// auto-opened the element, and all children are OK -> auto close
					iter.remove();
					fTreeViewer.collapseToLevel(previousAutoOpened, AbstractTreeViewer.ALL_LEVELS);
				}
			}

			while (parent != null && !fTestRunSession.getTestRoot().equals(parent)
					&& fTreeViewer.getExpandedState(parent) == false) {
				fAutoClose.add(parent); // add to auto-opened elements -> close later if STATUS_OK
				parent = (ITestSuiteElement) fTreeContentProvider.getParent(parent);
			}
		}
		if (current != null)
			fTreeViewer.reveal(current);
	}

	public void selectFirstFailure() {
		ITestElement firstFailure = getNextChildFailure(fTestRunSession.getTestRoot(), true);
		if (firstFailure != null)
			getActiveViewer().setSelection(new StructuredSelection(firstFailure), true);
	}

	public void selectFailure(boolean showNext) {
		IStructuredSelection selection = (IStructuredSelection) getActiveViewer().getSelection();
		ITestElement selected = (ITestElement) selection.getFirstElement();
		ITestElement next;

		if (selected == null) {
			next = getNextChildFailure(fTestRunSession.getTestRoot(), showNext);
		} else {
			next = getNextFailure(selected, showNext);
		}

		if (next != null)
			getActiveViewer().setSelection(new StructuredSelection(next), true);
	}

	private ITestElement getNextFailure(ITestElement selected, boolean showNext) {
		if (selected instanceof ITestSuiteElement) {
			ITestElement nextChild = getNextChildFailure((ITestSuiteElement) selected, showNext);
			if (nextChild != null)
				return nextChild;
		}
		return getNextFailureSibling(selected, showNext);
	}

	private ITestElement getNextFailureSibling(ITestElement current, boolean showNext) {
		ITestSuiteElement parent = current.getParent();
		if (parent == null)
			return null;

		List<ITestElement> siblings = Arrays.asList(parent.getChildren());
		if (!showNext)
			siblings = new ReverseList<>(siblings);

		int nextIndex = siblings.indexOf(current) + 1;
		for (int i = nextIndex; i < siblings.size(); i++) {
			ITestElement sibling = siblings.get(i);
			if (sibling.getStatus().isErrorOrFailure()) {
				if (sibling instanceof ITestCaseElement) {
					return sibling;
				} else {
					ITestSuiteElement testSuiteElement = (ITestSuiteElement) sibling;
					if (testSuiteElement.getChildren().length == 0) {
						return testSuiteElement;
					}
					return getNextChildFailure(testSuiteElement, showNext);
				}
			}
		}
		return getNextFailureSibling(parent, showNext);
	}

	private ITestElement getNextChildFailure(ITestSuiteElement root, boolean showNext) {
		List<ITestElement> children = Arrays.asList(root.getChildren());
		if (!showNext)
			children = new ReverseList<>(children);
		for (ITestElement element : children) {
			ITestElement child = element;
			if (child.getStatus().isErrorOrFailure()) {
				if (child instanceof ITestCaseElement) {
					return child;
				} else {
					ITestSuiteElement testSuiteElement = (ITestSuiteElement) child;
					if (testSuiteElement.getChildren().length == 0) {
						return testSuiteElement;
					}
					return getNextChildFailure(testSuiteElement, showNext);
				}
			}
		}
		return null;
	}

	public synchronized void registerViewersRefresh() {
		fTreeNeedsRefresh = true;
		fTableNeedsRefresh = true;
		clearUpdateAndExpansion();
	}

	private void clearUpdateAndExpansion() {
		fNeedUpdate = new LinkedHashSet<>();
		fAutoClose = new LinkedList<>();
		fAutoExpand = new HashSet<>();
	}

	/**
	 * @param testElement the added test
	 */
	public synchronized void registerTestAdded(ITestElement testElement) {
		// TODO: performance: would only need to refresh parent of added element
		fTreeNeedsRefresh = true;
		fTableNeedsRefresh = true;
	}

	public synchronized void registerViewerUpdate(final ITestElement testElement) {
		fNeedUpdate.add(testElement);
	}

	private synchronized void clearAutoExpand() {
		fAutoExpand.clear();
	}

	public void registerAutoScrollTarget(ITestCaseElement testCaseElement) {
		fAutoScrollTarget = testCaseElement;
	}

	public synchronized void registerFailedForAutoScroll(ITestElement testElement) {
		ITestSuiteElement parent = (TestSuiteElement) fTreeContentProvider.getParent(testElement);
		if (parent != null)
			fAutoExpand.add(parent);
	}

	public void expandFirstLevel() {
		fTreeViewer.expandToLevel(2);
	}

}
