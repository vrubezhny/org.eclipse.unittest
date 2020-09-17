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
package org.eclipse.unittest.cdt.internal.launcher;

import java.io.InputStream;
import java.util.Map;

import org.eclipse.cdt.dsf.gdb.IGdbDebugConstants;
import org.eclipse.cdt.dsf.gdb.launching.GDBProcess;
import org.eclipse.cdt.dsf.gdb.launching.InferiorRuntimeProcess;
import org.eclipse.cdt.testsrunner.internal.TestsRunnerPlugin;
import org.eclipse.cdt.testsrunner.internal.launcher.TestsRunnerProviderInfo;
import org.eclipse.cdt.testsrunner.internal.launcher.TestsRunnerProvidersManager;
import org.eclipse.unittest.TestRunListener;
import org.eclipse.unittest.UnitTestPlugin;
import org.eclipse.unittest.cdt.launcher.CDTTestRunnerClient;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestRunSession;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IProcessFactory;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;

/**
 * Custom testing process factory allows to handle the output stream of the
 * testing process and prevent it from output to Console.
 */
public class TestingProcessFactory implements IProcessFactory {
	private TestsRunnerProvidersManager testsRunnerProvidersManager = new TestsRunnerProvidersManager();

	/**
	 * Runs data processing for the testing process and close IO stream when it
	 * is done.
	 */
	private class TestingSessionRunner implements Runnable {

		private CDTTestRunnerClient testRunner;
		private InputStream iStream;
		private ProcessWrapper processWrapper;

		TestingSessionRunner(CDTTestRunnerClient testRunnerClient, InputStream iStream, ProcessWrapper processWrapper) {
			this.testRunner = testRunnerClient;
			this.iStream = iStream;
			this.processWrapper = processWrapper;
		}

		@Override
		public void run() {
			try {
				testRunner.run(iStream);
			} finally {
				// Streams should be closed anyway to avoid testing process hang up
				processWrapper.allowStreamsClosing();
			}
		}
	}

	/**
	 * Creates a wrapper for the specified process to handle its input or error
	 * stream.
	 *
	 * @param launch launch
	 * @param process process to wrap
	 * @return wrapped process
	 * @throws CoreException
	 *
	private Process wrapProcess1(ILaunch launch, Process process) throws CoreException {
		TestingSession testingSession = TestsRunnerPlugin.getDefault().getTestingSessionsManager().newSession(launch);
		ITestsRunnerProviderInfo testsRunnerProvider = testingSession.getTestsRunnerProviderInfo();
		InputStream iStream = testsRunnerProvider.isOutputStreamRequired() ? process.getInputStream()
				: testsRunnerProvider.isErrorStreamRequired() ? process.getErrorStream() : null;
		ProcessWrapper processWrapper = new ProcessWrapper(process, testsRunnerProvider.isOutputStreamRequired(),
				testsRunnerProvider.isErrorStreamRequired());
		Thread t = new Thread(new TestingSessionRunner(testingSession, iStream, processWrapper));
		t.start();
		return processWrapper;
	}
*/
	private Process wrapProcess(ILaunch launch, Process process)  throws CoreException {
		TestsRunnerProviderInfo testsRunnerProvider = testsRunnerProvidersManager.getTestsRunnerProviderInfo(launch.getLaunchConfiguration());
		InputStream iStream = testsRunnerProvider.isOutputStreamRequired() ? process.getInputStream()
				: testsRunnerProvider.isErrorStreamRequired() ? process.getErrorStream() : null;

		ITestRunSession testRunSession= UnitTestPlugin.getModel().createTestRunSession(launch, -1);
		UnitTestPlugin.getModel().addTestRunSession(testRunSession);
		ITestRunnerClient runnerClient =  testRunSession.getTestRunnerClient();
		CDTTestRunnerClient runner = (runnerClient instanceof CDTTestRunnerClient ?
				(CDTTestRunnerClient)runnerClient : null);
		if (runner != null) {
			runner.setTestRunSession(testRunSession);
			runner.setTestsRunnerProvider(testsRunnerProvider.instantiateTestsRunnerProvider());
		}
		ProcessWrapper processWrapper = new ProcessWrapper(process, testsRunnerProvider.isOutputStreamRequired(),
				testsRunnerProvider.isErrorStreamRequired());
		Thread t = new Thread(new TestingSessionRunner(runner, iStream, processWrapper));


		for (TestRunListener listener : UnitTestPlugin.getDefault().getUnitTestRunListeners()) {
			listener.sessionLaunched(testRunSession);
		}

		t.start();
		return processWrapper;

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public IProcess newProcess(ILaunch launch, Process process, String label, Map attributes) {

		try {
			// Mimic the behavior of DSF GDBProcessFactory.
			if (attributes != null) {
				Object processTypeCreationAttrValue = attributes.get(IGdbDebugConstants.PROCESS_TYPE_CREATION_ATTR);
				if (IGdbDebugConstants.GDB_PROCESS_CREATION_VALUE.equals(processTypeCreationAttrValue)) {
					return new GDBProcess(launch, process, label, attributes);
				}

				if (IGdbDebugConstants.INFERIOR_PROCESS_CREATION_VALUE.equals(processTypeCreationAttrValue)) {
					return new InferiorRuntimeProcess(launch, wrapProcess(launch, process), label, attributes);
				}

				// Probably, it is CDI creating a new inferior process
			} else {
				return new RuntimeProcess(launch, wrapProcess(launch, process), label, attributes);
			}

		} catch (CoreException e) {
			TestsRunnerPlugin.log(e);
		}

		return null;
	}

}
