/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.internal.ui.history;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.junitXmlReport.HistoryEntryHandler;
import org.eclipse.unittest.internal.junitXmlReport.TestRunHandler;
import org.eclipse.unittest.internal.junitXmlReport.TestRunSessionSerializer;
import org.eclipse.unittest.internal.model.ITestSessionListener;
import org.eclipse.unittest.internal.model.ModelMessages;
import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.internal.ui.BasicElementLabels;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;
import org.eclipse.unittest.model.ITestElement.Result;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

public class HistoryItem {

	private File historyFile;

	private TestRunSession session;

	private String name;

	private Instant startTime;

	private int failuresAndErrors;

	public HistoryItem(TestRunSession session) {
		this.session = session;
		this.name = session.getTestRunName();
		this.startTime = session.getStartTime();
		this.failuresAndErrors = session.getCurrentErrorCount() + session.getCurrentFailureCount();
		session.addTestSessionListener(new ITestSessionListener() {
			@Override
			public void testStarted(ITestCaseElement testCaseElement) {
				// nothing
			}

			@Override
			public void testFailed(ITestElement testElement, Result status, FailureTrace trace) {
				// nothing
			}

			@Override
			public void testEnded(ITestCaseElement testCaseElement) {
				// nothing
			}

			@Override
			public void testAdded(ITestElement testElement) {
				// nothing
			}

			@Override
			public void sessionStarted() {
				File historyDir = History.INSTANCE.getDirectory();
				String isoTime = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS") //$NON-NLS-1$
						.format(new Date(session.getStartTime().toEpochMilli()));
				String swapFileName = session.getTestRunName() + '@' + isoTime + ".xml"; //$NON-NLS-1$
				HistoryItem.this.historyFile = new File(historyDir, swapFileName);
			}

			@Override
			public void sessionCompleted(Duration duration) {
				try {
					storeSessionToFile(getFile());
				} catch (CoreException e) {
					UnitTestPlugin.log(e);
				}
			}

			@Override
			public void sessionAborted(Duration duration) {
				sessionCompleted(duration);
			}

			@Override
			public void runningBegins() {
				// nothing
			}
		});
	}

	public HistoryItem(File file) {
		this.historyFile = file;
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			HistoryEntryHandler handler = new HistoryEntryHandler();
			parser.parse(getFile(), handler);
			this.name = handler.getName();
			this.startTime = handler.getStartTime();
			this.failuresAndErrors = handler.getFailuresAndErrors();
		} catch (Exception e) {
			UnitTestPlugin.log(e);
		}
	}

	public TestRunSession reloadTestRunSession() throws CoreException {
		if (this.session == null && getFile() != null) {
			try {
				SAXParserFactory parserFactory = SAXParserFactory.newInstance();
				SAXParser parser = parserFactory.newSAXParser();
				TestRunHandler handler = new TestRunHandler(new NullProgressMonitor());
				parser.parse(getFile(), handler);
				this.session = handler.getTestRunSession();
			} catch (Exception e) {
				throwImportError(getFile(), e);
			}
		}
		return this.session;
	}

	public Optional<TestRunSession> getCurrentTestRunSession() {
		return Optional.ofNullable(this.session);
	}

	public void removeSwapFile() throws IOException {
		if (historyFile.exists()) {
			Files.delete(historyFile.toPath());
		}
	}

	void storeSessionToFile(File target) throws TransformerFactoryConfigurationError, CoreException {
		if (this.session == null) {
			return;
		}
		try (FileOutputStream out = new FileOutputStream(target)) {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			InputSource inputSource = new InputSource();
			SAXSource source = new SAXSource(new TestRunSessionSerializer(this.session), inputSource);
			StreamResult result = new StreamResult(out);
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			/*
			 * Bug in Xalan: Only indents if proprietary property
			 * org.apache.xalan.templates.OutputProperties.S_KEY_INDENT_AMOUNT is set.
			 *
			 * Bug in Xalan as shipped with J2SE 5.0: Does not read the indent-amount
			 * property at all >:-(.
			 */
			try {
				transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IllegalArgumentException e) {
				// no indentation today...
			}
			transformer.transform(source, result);
		} catch (Exception e) {
			throwExportError(historyFile, e);
		}
	}

	public File getFile() {
		return historyFile;
	}

	public void swapOut() throws CoreException {
		if (session != null && session.isStopped()) {
			storeSessionToFile(getFile());
			session = null;
		}
	}

	public String getName() {
		if (session != null) {
			return session.getTestRunName();
		}
		if (name != null) {
			return name;
		}
		return getFile().getName();
	}

	public Instant getStartDate() {
		if (session != null) {
			return session.getStartTime();
		}
		if (startTime != null) {
			return startTime;
		}
		try {
			return Files.getLastModifiedTime(historyFile.toPath()).toInstant();
		} catch (IOException e) {
			UnitTestPlugin.log(e);
			return Instant.now();
		}
	}

	public int getFailureCount() {
		if (session != null) {
			return session.getCurrentErrorCount() + session.getCurrentFailureCount();
		}
		return failuresAndErrors;
	}

	private static void throwExportError(File file, Exception e) throws CoreException {
		throw new CoreException(
				new org.eclipse.core.runtime.Status(IStatus.ERROR, UnitTestPlugin.PLUGIN_ID, MessageFormat.format(
						ModelMessages.UnitTestModel_could_not_write, BasicElementLabels.getPathLabel(file)), e));
	}

	private static void throwImportError(File file, Exception e) throws CoreException {
		throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR, UnitTestPlugin.PLUGIN_ID,
				MessageFormat.format(ModelMessages.UnitTestModel_could_not_read, BasicElementLabels.getPathLabel(file)),
				e));
	}

	public Long getSizeOnDisk() {
		File file = getFile();
		if (file != null && file.isFile()) {
			return Long.valueOf(file.length());
		}
		return null;
	}

}
