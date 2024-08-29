/*
 * SPDX-FileCopyrightText: © Vegard IT GmbH (https://vegardit.com) and contributors.
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Sebastian Thomschke (Vegard IT GmbH) - initial API and implementation
 */
package org.lastnpe.eea.generator;

/**
 * Utility class to be executed from within the IDE for convenient debugging of
 * the EEAGenerator.
 *
 * @author Sebastian Thomschke (Vegard IT GmbH)
 */
public final class EEAGeneratorTester {

	public static void main(final String[] args) throws Exception {

		final String lib = "../libs/eea-java-17";
		System.setProperty("eea-generator.input.dirs", lib + "/src/main/resources");

		System.setProperty("eea-generator.action", "generate");
		System.setProperty("eea-generator.output.dir", lib + "/src/main/resources");

		// System.setProperty("eea-generator.action", "minimize");
		// System.setProperty("eea-generator.output.dir", lib + "/target/classes");

		EEAGenerator.main(lib + "/eea-generator.properties");
	}

	private EEAGeneratorTester() {
	}
}
