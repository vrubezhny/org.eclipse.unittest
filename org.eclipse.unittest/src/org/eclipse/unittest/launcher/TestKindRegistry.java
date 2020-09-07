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
 *******************************************************************************/
package org.eclipse.unittest.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import org.eclipse.unittest.UnitTestPlugin;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

/**
 * Test Kind registry
 */
public class TestKindRegistry {
	public static TestKindRegistry getDefault() {
		if (fgRegistry != null)
			return fgRegistry;

		fgRegistry = new TestKindRegistry(
				Platform.getExtensionRegistry().getExtensionPoint(UnitTestPlugin.ID_EXTENSION_POINT_TEST_KINDS));
		return fgRegistry;
	}

	private static TestKindRegistry fgRegistry;

	private final IExtensionPoint fPoint;
	private ArrayList<TestKind> fTestKinds;

	private TestKindRegistry(IExtensionPoint point) {
		fPoint = point;
	}

	/**
	 * Returns all the registered kinds
	 *
	 * @return an {@link ArrayList} containing all the registry kinds
	 */
	public ArrayList<TestKind> getAllKinds() {
		loadKinds();
		return fTestKinds;
	}

	/**
	 * Returns all the registered kinds that suit the specified filter
	 *
	 * @param filter a registry identifier filter
	 * @return an {@link ArrayList} containing the registry kings filtered by
	 *         identifier
	 */
	public ArrayList<TestKind> getKinds(final String filter) {
		ArrayList<TestKind> allKinds = getAllKinds();
		return allKinds != null ? allKinds.stream().filter(p -> p.getId().startsWith(filter))
				.collect(Collectors.toCollection(ArrayList::new)) : null;
	}

	private void loadKinds() {
		if (fTestKinds != null)
			return;

		ArrayList<TestKind> items = new ArrayList<>();
		for (IConfigurationElement configurationElement : getConfigurationElements()) {
			items.add(new TestKind(configurationElement));
		}

		Collections.sort(items, (kind0, kind1) -> {
			if (kind0.precedes(kind1))
				return -1;
			if (kind1.precedes(kind0))
				return 1;
			return 0;
		});
		fTestKinds = items;
	}

	/**
	 * Returns the kinds names
	 *
	 * @return an {@link ArrayList} of kind display names
	 */
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
	 *
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
		ArrayList<IConfigurationElement> items = new ArrayList<>();
		for (IExtension extension : fPoint.getExtensions()) {
			Collections.addAll(items, extension.getConfigurationElements());
		}
		return items;
	}

	/**
	 * Returns all the registered kind identifiers
	 *
	 * @return a registry kind identifiers string
	 */
	public String getAllKindIds() {
		ArrayList<TestKind> allKinds = getAllKinds();
		String returnThis = ""; //$NON-NLS-1$
		for (ITestKind kind : allKinds) {
			returnThis += "(" + kind.getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return returnThis;
	}
}
