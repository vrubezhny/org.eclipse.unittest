/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/
package org.eclipse.unittest.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.unittest.UnitTestPlugin;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;


public class TestKindRegistry {
	public static TestKindRegistry getDefault() {
		if (fgRegistry != null)
			return fgRegistry;

		fgRegistry= new TestKindRegistry(Platform.getExtensionRegistry().getExtensionPoint(UnitTestPlugin.ID_EXTENSION_POINT_TEST_KINDS));
		return fgRegistry;
	}

	private static TestKindRegistry fgRegistry;

	private final IExtensionPoint fPoint;
	private ArrayList<TestKind> fTestKinds;

	private TestKindRegistry(IExtensionPoint point) {
		fPoint = point;
	}

	public ArrayList<TestKind> getAllKinds() {
		loadKinds();
		return fTestKinds;
	}

	private void loadKinds() {
		if (fTestKinds != null)
			return;

		ArrayList<TestKind> items= new ArrayList<>();
		for (IConfigurationElement configurationElement : getConfigurationElements()) {
			items.add(new TestKind(configurationElement));
		}

		Collections.sort(items, new Comparator<TestKind>() {
			@Override
			public int compare(TestKind kind0, TestKind kind1) {
				if (kind0.precedes(kind1))
					return -1;
				if (kind1.precedes(kind0))
					return 1;
				return 0;
			}
		});
		fTestKinds= items;
	}

	public ArrayList<String> getDisplayNames() {
		ArrayList<String> result = new ArrayList<>();
		ArrayList<TestKind> testTypes = getAllKinds();
		for (ITestKind type : testTypes) {
			result.add(type.getDisplayName());
		}
		return result;
	}

	/**
	 * @param testKindId an id, can be <code>null</code>
	 * @return a TestKind, ITestKind.NULL if not available
	 */
	public ITestKind getKind(String testKindId) {
		if (testKindId != null) {
			for (TestKind kind : getAllKinds()) {
				if (testKindId.equals(kind.getId()))
					return kind;
			}
		}
		return ITestKind.NULL;
	}



	private ArrayList<IConfigurationElement> getConfigurationElements() {
		ArrayList<IConfigurationElement> items= new ArrayList<>();
		for (IExtension extension : fPoint.getExtensions()) {
			Collections.addAll(items, extension.getConfigurationElements());
		}
		return items;
	}

	public String getAllKindIds() {
		ArrayList<TestKind> allKinds= getAllKinds();
		String returnThis= ""; //$NON-NLS-1$
		for (ITestKind kind : allKinds) {
			returnThis+= "(" + kind.getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return returnThis;
	}
}
