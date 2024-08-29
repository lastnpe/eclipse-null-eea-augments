/*
 * SPDX-FileCopyrightText: © Vegard IT GmbH (https://vegardit.com) and others.
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Sebastian Thomschke (Vegard IT GmbH) - initial API and implementation
 */
package org.lastnpe.eea.generator.internal;

import java.util.Arrays;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Sebastian Thomschke (Vegard IT GmbH)
 */
public final class CompositeKey {

	private final @Nullable Object[] keys;

	public CompositeKey(final @Nullable Object... keys) {
		this.keys = keys;
	}

	@Override
	public boolean equals(final @Nullable Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final CompositeKey other = (CompositeKey) obj;
		return Arrays.deepEquals(keys, other.keys);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(keys);
		return result;
	}

	@Override
	public String toString() {
		return "CompositeKey [keys=" + Arrays.toString(keys) + "]";
	}
}
