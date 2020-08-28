/*******************************************************************************
 * Copyright (c) 2011, 2012 Anton Gorenkov
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Anton Gorenkov - initial API and implementation
 *******************************************************************************/
package org.eclipse.unittest.cdt.ui;

import java.net.URI;

import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.ui.CDebugUIPlugin;
import org.eclipse.unittest.UnitTestPlugin;
import org.eclipse.unittest.cdt.CDTPlugin;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.ui.IOpenEditorAction;
import org.eclipse.unittest.ui.TestRunnerViewPart;

import org.eclipse.core.filesystem.URIUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.action.Action;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.sourcelookup.ISourceLookupResult;

/**
 * Opens the editor in place where the currently selected message is pointed to.
 */
public class OpenInEditorAction extends Action implements IOpenEditorAction {
	private TestRunnerViewPart testRunner;
	private String fileName;
	private int line;

	public OpenInEditorAction(TestRunnerViewPart testRunner, String fileName, int line) {
		super(ActionsMessages.OpenInEditorAction_text);
		this.testRunner = testRunner;
		this.fileName = fileName;
		this.line = line;
		setToolTipText(ActionsMessages.OpenInEditorAction_tooltip);
	}

	@Override
	public void run() {
		ITestRunSession testRunSession = testRunner.getCurrentTestRunSession();
		ILaunch launch= testRunSession != null ? testRunSession.getLaunch() : null;
		if (launch == null)
			return;
		lookupSource(fileName, line, launch);
	}

	// NOTE: This method is copied from Linux Tools Project (http://www.eclipse.org/linuxtools).
	// Valgrind Support Plugin is implementing similar functionality so it is just reused.
	// See also org.eclipse.linuxtools.valgrind.ui/src/org/eclipse/linuxtools/internal/valgrind/ui/CoreMessagesViewer.java
	private void lookupSource(String file, int line,ILaunch launch) {
		ISourceLocator locator = launch.getSourceLocator();
		if (locator instanceof AbstractSourceLookupDirector) {
			AbstractSourceLookupDirector director = (AbstractSourceLookupDirector) locator;
			ISourceLookupParticipant[] participants = director.getParticipants();
			if (participants.length == 0) {
				// source locator likely disposed, try recreating it
				IPersistableSourceLocator sourceLocator;
				ILaunchConfiguration config = launch.getLaunchConfiguration();
				if (config != null) {
					try {
						String id = config.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String) null);
						if (id == null) {
							sourceLocator = CDebugUIPlugin.createDefaultSourceLocator();
							sourceLocator.initializeDefaults(config);
						} else {
							sourceLocator = DebugPlugin.getDefault().getLaunchManager().newSourceLocator(id);
							String memento = config.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO,
									(String) null);
							if (memento == null) {
								sourceLocator.initializeDefaults(config);
							} else {
								sourceLocator.initializeFromMemento(memento);
							}
						}

						// replace old source locator
						locator = sourceLocator;
						launch.setSourceLocator(sourceLocator);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
		ISourceLookupResult result = DebugUITools.lookupSource(file, locator);
		try {
			openEditorAndSelect(result, line);
		} catch (PartInitException e) {
			CDTPlugin.log(e);
		} catch (BadLocationException e) {
			CDTPlugin.log(e);
		}
	}

	// NOTE: This method is copied from Linux Tools Project (http://www.eclipse.org/linuxtools).
	// Valgrind Support Plugin is implementing similar functionality so it is just reused.
	// See also org.eclipse.linuxtools.valgrind.ui/src/org/eclipse/linuxtools/internal/valgrind/ui/CoreMessagesViewer.java
	private void openEditorAndSelect(ISourceLookupResult result, int line)
			throws PartInitException, BadLocationException {
		IEditorInput input = result.getEditorInput();
		String editorID = result.getEditorId();

		if (input == null || editorID == null) {
			// Consult the CDT DebugModelPresentation
			Object sourceElement = result.getSourceElement();
			if (sourceElement != null) {
				// Resolve IResource in case we get a LocalFileStorage object
				if (sourceElement instanceof LocalFileStorage) {
					IPath filePath = ((LocalFileStorage) sourceElement).getFullPath();
					URI fileURI = URIUtil.toURI(filePath);
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IFile[] files = root.findFilesForLocationURI(fileURI);
					if (files.length > 0) {
						// Take the first match
						sourceElement = files[0];
					}
				}

				IDebugModelPresentation pres = DebugUITools
						.newDebugModelPresentation(CDebugCorePlugin.getUniqueIdentifier());
				input = pres.getEditorInput(sourceElement);
				editorID = pres.getEditorId(input, sourceElement);
				pres.dispose();
			}
		}
		if (input != null && editorID != null) {
			// Open the editor
			IWorkbenchPage activePage = UnitTestPlugin.getActivePage();

			IEditorPart editor = IDE.openEditor(activePage, input, editorID);
			// Select the line
			if (editor instanceof ITextEditor) {
				ITextEditor textEditor = (ITextEditor) editor;

				if (line > 0) {
					IDocumentProvider provider = textEditor.getDocumentProvider();
					IDocument document = provider.getDocument(textEditor.getEditorInput());

					IRegion lineRegion = document.getLineInformation(line - 1); //zero-indexed
					textEditor.selectAndReveal(lineRegion.getOffset(), lineRegion.getLength());
				}
			}
		}
	}

}
