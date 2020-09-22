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
 * Test View Support registry
 */
public class TestViewSupportRegistry {
	public static TestViewSupportRegistry getDefault() {
		if (fgRegistry != null)
			return fgRegistry;

		fgRegistry = new TestViewSupportRegistry(Platform.getExtensionRegistry()
				.getExtensionPoint(UnitTestPlugin.ID_EXTENSION_POINT_TEST_VIEW_SUPPORTS));
		return fgRegistry;
	}

	private static TestViewSupportRegistry fgRegistry;

	private final IExtensionPoint fPoint;
	private List<TestViewSupportExtension> fTestViewSupportExtensions;

	private TestViewSupportRegistry(IExtensionPoint point) {
		fPoint = point;
	}

	/**
	 * Returns all the registered View Support extensions
	 *
	 * @return a {@link List} containing all the registered View Support extensions
	 */
	private List<TestViewSupportExtension> getAllTestViewSupportExtensions() {
		loadTestViewSupportExtensions();
		return fTestViewSupportExtensions;
	}

	/**
	 * Returns all the registered View Support extensions that suit the specified
	 * filter
	 *
	 * @param filter a registry identifier filter
	 * @return an {@link ArrayList} containing the registry kings filtered by
	 *         identifier
	 */
	public List<TestViewSupportExtension> getTestViewSupportExtensions(final String filter) {
		List<TestViewSupportExtension> all = getAllTestViewSupportExtensions();
		return all != null ? all.stream().filter(p -> p.getId().startsWith(filter)).collect(Collectors.toList()) : null;
	}

	private void loadTestViewSupportExtensions() {
		if (fTestViewSupportExtensions != null)
			return;

		List<TestViewSupportExtension> items = new ArrayList<>();
		for (IConfigurationElement configurationElement : getConfigurationElements()) {
			items.add(new TestViewSupportExtension(configurationElement));
		}

		fTestViewSupportExtensions = items;
	}

	/**
	 * @param id an identifier, can be <code>null</code>
	 *
	 * @return an {@link ITestViewSupport} object instance, or <code>null</code> if
	 *         not available
	 */
	public ITestViewSupport getTestViewSupportInstance(String id) {
		return getAllTestViewSupportExtensions().stream().filter(ext -> ext.getId().equals(id)).findFirst()
				.map(TestViewSupportExtension::instantiateTestViewSupport).orElse(null);
	}

	private List<IConfigurationElement> getConfigurationElements() {
		List<IConfigurationElement> items = new ArrayList<>();
		for (IExtension extension : fPoint.getExtensions()) {
			Collections.addAll(items, extension.getConfigurationElements());
		}
		return items;
	}

}
