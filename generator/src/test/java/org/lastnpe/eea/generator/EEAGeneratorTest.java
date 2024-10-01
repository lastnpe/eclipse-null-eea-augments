/*
 * SPDX-FileCopyrightText: © Vegard IT GmbH (https://vegardit.com) and contributors.
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Sebastian Thomschke (Vegard IT GmbH) - initial API and implementation
 */
package org.lastnpe.eea.generator;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * @author Sebastian Thomschke (Vegard IT GmbH)
 */
@SuppressWarnings("null")
class EEAGeneratorTest {

	@Test
	void testVadilateValidEEAFiles() throws IOException {
		final var rootPath = Path.of("src/test/resources/valid");
		final var config = new EEAGenerator.Config(rootPath, EEAGeneratorTest.class.getPackageName());
		config.inputDirs.add(rootPath);

		assertThat(EEAGenerator.validateEEAFiles(config)).isEqualTo(2);
	}

	@Test
	void testVadilateInvalidEEAFiles() {
		final var rootPath = Path.of("src/test/resources/invalid");
		final var config = new EEAGenerator.Config(rootPath, EEAGeneratorTest.class.getPackageName());
		config.inputDirs.add(rootPath);

		final var wrongTypePath = rootPath.resolve(EEAFileTest.WRONG_TYPE_NAME_WITH_SLASHES + ".eea");
		assertThatThrownBy(() -> {
			EEAGenerator.validateEEAFiles(config);
		}) //
				.isInstanceOf(IllegalStateException.class) //
				.hasMessage("Type [org.lastnpe.eea.generator.EEAFileTest$WrongType] defined in [" + wrongTypePath
						+ "] no found on classpath.");
	}

	@Test
	void testPackageMissingOnClasspath() {
		final var rootPath = Path.of("src/test/resources/invalid");
		final var config = new EEAGenerator.Config(rootPath, "org.no_npe.foobar");
		config.inputDirs.add(rootPath);

		assertThatThrownBy(() -> {
			EEAGenerator.validateEEAFiles(config);
		}) //
				.isInstanceOf(IllegalArgumentException.class) //
				.hasMessage("No classes found for package [org.no_npe.foobar] on classpath");
	}
}
