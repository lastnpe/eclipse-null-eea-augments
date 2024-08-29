/*
 * SPDX-FileCopyrightText: © Vegard IT GmbH (https://vegardit.com) and others.
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Sebastian Thomschke (Vegard IT GmbH) - initial API and implementation
 */
package org.lastnpe.eea.generator.internal;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Sebastian Thomschke (Vegard IT GmbH)
 */
public final class Props {

	private static final Logger LOG = System.getLogger(Props.class.getName());

	public static class Prop<T> {
		public static final String SOURCE_JVM_SYSTEM_ROPERTY = "JVM system property";
		public static final String SOURCE_DEFAULT_VALUE = "default value";

		/**
		 * Either a reference to {@link Props#propertiesFile} or
		 * {@link Prop#SOURCE_JVM_SYSTEM_ROPERTY} or {@link Prop#SOURCE_DEFAULT_VALUE}
		 */
		public final Object source;
		public final String name;
		public final T value;

		public Prop(final Object source, final String name, final T value) {
			this.source = source;
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return "Prop [" //
					+ "name=" + name + ", " //
					+ "value=" + value + ", " //
					+ "source=" + source //
					+ "]";
		}
	}

	public final String jvmPropertyPrefix;
	public final @Nullable Path propertiesFile;
	private final @Nullable Properties properties;

	public Props(final String jvmPropertyPrefix, final @Nullable Path propertiesFilePath) throws IOException {
		this.jvmPropertyPrefix = jvmPropertyPrefix;
		propertiesFile = propertiesFilePath;

		if (propertiesFilePath == null) {
			properties = null;
		} else {
			final var properties = this.properties = new Properties();
			try (var r = Files.newBufferedReader(propertiesFilePath)) {
				properties.load(r);
			}
		}
	}

	public Prop<String> get(final String propName, final @Nullable String defaultValue) {
		var propValue = System.getProperty(jvmPropertyPrefix + propName);
		Prop<String> prop = null;
		if (propValue != null) {
			prop = new Prop<>(Prop.SOURCE_JVM_SYSTEM_ROPERTY, propName, propValue);
		}

		final var properties = this.properties;
		if (prop == null && properties != null) {
			propValue = properties.getProperty(propName);
			if (propValue != null) {
				assert propertiesFile != null;
				prop = new Prop<>(propertiesFile, propName, propValue);
			}
		}

		if (prop == null && defaultValue != null) {
			prop = new Prop<>(Prop.SOURCE_DEFAULT_VALUE, propName, defaultValue);
		}

		if (prop != null) {
			LOG.log(Level.INFO, "Resolved property [{0}] \"{1}\" << {2}", prop.name, prop.value, prop.source);
			return prop;
		}

		if (propertiesFile != null)
			throw new IllegalArgumentException(
					"Required property [" + propName + "] not found in [" + propertiesFile + "]!");

		throw new IllegalArgumentException(
				"Required " + Prop.SOURCE_JVM_SYSTEM_ROPERTY + " [" + propName + "] not found!");
	}
}
