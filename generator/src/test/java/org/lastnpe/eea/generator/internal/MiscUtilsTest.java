/*
 * SPDX-FileCopyrightText: © Vegard IT GmbH (https://vegardit.com) and contributors.
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Sebastian Thomschke (Vegard IT GmbH) - initial API and implementation
 */
package org.lastnpe.eea.generator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lastnpe.eea.generator.internal.MiscUtils.replaceAll;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * @author Sebastian Thomschke (Vegard IT GmbH)
 */
class MiscUtilsTest {

	@Test
	void testReplaceAll() {
		assertThat(replaceAll("", Pattern.compile("o"), 0, w -> "a")).isEmpty();
		assertThat(replaceAll("o", Pattern.compile("o"), 0, w -> "a")).isEqualTo("a");
		assertThat(replaceAll("ooo", Pattern.compile("o"), 0, w -> "a")).isEqualTo("aaa");
		assertThat(replaceAll("oX", Pattern.compile("o"), 0, w -> "a")).isEqualTo("aX");
		assertThat(replaceAll("oooX", Pattern.compile("o"), 0, w -> "a")).isEqualTo("aaaX");
		assertThat(replaceAll("Xo", Pattern.compile("o"), 0, w -> "a")).isEqualTo("Xa");
		assertThat(replaceAll("Xooo", Pattern.compile("o"), 0, w -> "a")).isEqualTo("Xaaa");
		assertThat(replaceAll("aXcaXc", Pattern.compile("a(X)c"), 1, w -> "b")).isEqualTo("abcabc");
	}
}
