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
import java.util.List;
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
	private List<TestKindExtension> fTestKinds;

	private TestKindRegistry(IExtensionPoint point) {
		fPoint = point;
	}

	/**
	 * Returns all the registered kinds
	 *
	 * @return a {@link List} containing all the registry kinds
	 */
	private List<TestKindExtension> getAllKinds() {
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
	public List<TestKindExtension> getKinds(final String filter) {
		List<TestKindExtension> allKinds = getAllKinds();
		return allKinds != null
				? allKinds.stream().filter(p -> p.getId().startsWith(filter)).collect(Collectors.toList())
				: null;
	}

	private void loadKinds() {
		if (fTestKinds != null)
			return;

		List<TestKindExtension> items = new ArrayList<>();
		for (IConfigurationElement configurationElement : getConfigurationElements()) {
			items.add(new TestKindExtension(configurationElement));
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
	 * @param testKindId an id, can be <code>null</code>
	 *
	 * @return a TestKind, or <code>null</code> if not available
	 */
	public ITestKind getTestKindInstance(String testKindId) {
		if (testKindId != null) {
			return getAllKinds().stream().filter(kind -> kind.getId().equals(testKindId)).findFirst()
					.map(TestKindExtension::instantiateKind).orElse(null);
		}
		return null;
	}

	private List<IConfigurationElement> getConfigurationElements() {
		List<IConfigurationElement> items = new ArrayList<>();
		for (IExtension extension : fPoint.getExtensions()) {
			Collections.addAll(items, extension.getConfigurationElements());
		}
		return items;
	}

}
