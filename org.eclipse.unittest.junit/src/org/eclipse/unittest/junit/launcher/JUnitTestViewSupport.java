package org.eclipse.unittest.junit.launcher;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.unittest.junit.JUnitPlugin;
import org.eclipse.unittest.junit.JUnitPlugin.JUnitVersion;
import org.eclipse.unittest.junit.ui.OpenEditorAtLineAction;
import org.eclipse.unittest.junit.ui.OpenTestAction;
import org.eclipse.unittest.junit.ui.ShowStackTraceInConsoleViewActionDelegate;
import org.eclipse.unittest.launcher.ITestViewSupport;
import org.eclipse.unittest.launcher.UnitTestLaunchConfigurationConstants;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRoot;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.FailureTrace;
import org.eclipse.unittest.ui.IOpenEditorAction;
import org.eclipse.unittest.ui.RerunAction;
import org.eclipse.unittest.ui.TestRunnerViewPart;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IProject;

import org.eclipse.ui.IActionDelegate;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitPreferencesConstants;
import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;

@SuppressWarnings("restriction")
public class JUnitTestViewSupport implements ITestViewSupport {

	@Override
	public String[] getFilterPatterns() {
		return JUnitPreferencesConstants.parseList(Platform.getPreferencesService().getString(
				JUnitCorePlugin.CORE_PLUGIN_ID, JUnitPreferencesConstants.PREF_ACTIVE_FILTERS_LIST, null, null));

	}

	@Override
	public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestCaseElement testCase) {
		return new OpenTestAction(testRunnerPart, testCase, testCase.getParameterTypes());
	}

	@Override
	public IOpenEditorAction getOpenTestAction(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite) {
		String testName = testSuite.getSuiteTypeName();
		ITestElement[] children = testSuite.getChildren();

		if (testName.startsWith("[") && testName.endsWith("]") && children.length > 0 //$NON-NLS-1$ //$NON-NLS-2$
				&& children[0] instanceof ITestCaseElement) {
			// a group of parameterized tests
			return new OpenTestAction(testRunnerPart, (ITestCaseElement) children[0], null);
		}

		int index = testName.indexOf('(');
		// test factory method
		if (index > 0) {
			return new OpenTestAction(testRunnerPart, testSuite.getSuiteTypeName(), testName.substring(0, index),
					testSuite.getParameterTypes(), true);
		}

		// regular test class
		return new OpenTestAction(testRunnerPart, testName);

	}

	@Override
	public IOpenEditorAction createOpenEditorAction(TestRunnerViewPart testRunnerPart, ITestElement failure,
			String traceLine) {
		try {
			String testName = traceLine;
			int indexOfFramePrefix = testName.indexOf(FailureTrace.FRAME_PREFIX);
			if (indexOfFramePrefix == -1) {
				return null;
			}
			testName = testName.substring(indexOfFramePrefix);
			testName = testName.substring(FailureTrace.FRAME_PREFIX.length(), testName.lastIndexOf('(')).trim();
			int indexOfModuleSeparator = testName.lastIndexOf('/');
			if (indexOfModuleSeparator != -1) {
				testName = testName.substring(indexOfModuleSeparator + 1);
			}
			testName = testName.substring(0, testName.lastIndexOf('.'));
			int innerSeparatorIndex = testName.indexOf('$');
			if (innerSeparatorIndex != -1)
				testName = testName.substring(0, innerSeparatorIndex);

			String lineNumber = traceLine;
			lineNumber = lineNumber.substring(lineNumber.indexOf(':') + 1, lineNumber.lastIndexOf(')'));
			int line = Integer.valueOf(lineNumber).intValue();
			return new OpenEditorAtLineAction(testRunnerPart, testName, line);
		} catch (NumberFormatException | IndexOutOfBoundsException e) {
		}
		return null;
	}

	@Override
	public IActionDelegate createShowStackTraceInConsoleViewActionDelegate(FailureTrace view) {
		return new ShowStackTraceInConsoleViewActionDelegate(view);
	}

	@Override
	public RerunAction[] getRerunActions(TestRunnerViewPart testRunnerPart, ITestSuiteElement testSuite) {
		String testMethodName = null; // test method name is null when re-running a regular test class
		String testName = testSuite.getTestName();

		ILaunchConfiguration launchConfiguration = testSuite.getTestRunSession().getLaunch().getLaunchConfiguration();
		ITestKind junitKind;
		try {
			junitKind = JUnitVersion
					.fromJUnitTestKindId(launchConfiguration
							.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, "")) //$NON-NLS-1$
					.getJUnitTestKind();
		} catch (CoreException e) {
			JUnitPlugin.log(e);
			return new RerunAction[0];
		}

		IJavaProject project = getJavaProject(testSuite.getTestRunSession().getLaunchedProject());
		if (project == null) {
			return new RerunAction[0];
		}

		String qualifiedName = null;
		IType testType = findTestClass(testSuite, junitKind.getFinder(), project, true);
		if (testType != null) {
			qualifiedName = testType.getFullyQualifiedName();

			if (!qualifiedName.equals(testName)) {
				int index = testName.indexOf('(');
				if (index > 0) { // test factory method
					testMethodName = testName.substring(0, index);
				}
			}
			String[] parameterTypes = testSuite.getParameterTypes();
			if (testMethodName != null && parameterTypes != null) {
				String paramTypesStr = Arrays.stream(parameterTypes).collect(Collectors.joining(",")); //$NON-NLS-1$
				testMethodName = testMethodName + "(" + paramTypesStr + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			// see bug 443498
			testType = findTestClass(testSuite.getParent(), junitKind.getFinder(), project, false);
			if (testType != null) {
				qualifiedName = testType.getFullyQualifiedName();

				String className = testSuite.getSuiteTypeName();
				if (!qualifiedName.equals(className)) {
					testMethodName = testName;
				}
			}
		}

		return qualifiedName != null ? new RerunAction[] {
				new RerunAction(Messages.RerunAction_label_run, testRunnerPart, testSuite.getId(), qualifiedName,
						testMethodName, testSuite.getDisplayName(), testSuite.getUniqueId(), ILaunchManager.RUN_MODE),
				new RerunAction(Messages.RerunAction_label_debug, testRunnerPart, testSuite.getId(), qualifiedName,
						testMethodName, testSuite.getDisplayName(), testSuite.getUniqueId(),
						ILaunchManager.DEBUG_MODE) }
				: null;
	}

	/*
	 * Returns the element's test class or the next container's test class, which
	 * exists, and for which ITestFinder.isTest() is true.
	 */
	private IType findTestClass(ITestElement element, ITestFinder finder, IJavaProject project,
			boolean checkOnlyCurrentElement) {
		ITestElement current = element;
		while (current != null) {
			try {
				String className = null;
				if (current instanceof ITestRoot) {
					ILaunch launch = element.getTestRunSession().getLaunch();
					if (launch != null) {
						ILaunchConfiguration configuration = launch.getLaunchConfiguration();
						if (configuration != null) {
							className = configuration.getAttribute(
									UnitTestLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String) null);
						}
					}
				} else {
					className = current.getClassName();
				}

				if (className != null) {
					IType type = project.findType(className);
					if (type != null && finder.isTest(type)) {
						return type;
					} else if (checkOnlyCurrentElement) {
						return null;
					}
				}
			} catch (JavaModelException e) {
				// fall through
			} catch (CoreException e) {
				// fall through
			}
			current = current.getParent();
		}
		return null;
	}

	private IJavaProject getJavaProject(IProject testProject) {
		if (testProject != null) {
			IJavaProject javaProject = JavaCore.create(testProject);
			if (javaProject != null && javaProject.exists()) {
				return javaProject;
			}
		}
		return null;
	}

	@Override
	public String getDisplayName() {
		return "JUnit";
	}
}
