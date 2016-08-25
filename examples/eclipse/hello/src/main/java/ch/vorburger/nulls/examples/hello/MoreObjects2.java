/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.nulls.examples.hello;

import java.util.function.BiFunction;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Utility helping to implement readable equals() methods.
 *
 * <p>Usage:
 * <pre>
 *{@literal @}Override
 * public boolean equals(Object obj) {
 *     return EvenMoreObjects.equalsHelper(this, obj,
 *        (one, another) -&gt; Objects.equals(one.name, another.name) &amp;&amp; Objects.equals(one.age, another.age));
 * }
 * </pre>
 *
 * <p>See <a href="https://github.com/google/guava/issues/2521">Guava issue proposing contributing this</a>.
 *
 * @see com.google.common.base.MoreObjects
 *
 * @author Michael Vorburger, Red Hat
 */
public final class MoreObjects2 {

    @SuppressWarnings("unchecked")
    public static <@NonNull T> boolean equalsHelper(T self, @Nullable Object other, BooleanEqualsFunction<T> equals) {
        if (other == null) {
            return false;
        }
        if (other == self) {
            return true;
        }
        if (self.getClass() != other.getClass()) {
            return false;
        }
        return equals.apply(self, (T) other);
    }

    @FunctionalInterface
    public interface BooleanEqualsFunction<T> extends BiFunction<T, T, Boolean> { }

    private MoreObjects2() { }
}
