/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.nulls.examples.hello;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author Michael Vorburger.ch
 */
class HelloTest {

    @Test
    @SuppressWarnings("null")
    void testSomeMethodWithNonNullableByDefaultArgument() {
        SomeInterface service = new Hello();
        try {
            service.someMethodWithNonNullableByDefaultArgument(null);
            fail("should have failed");
        } catch (NullPointerException e) {
            assertEquals("anObject", e.getMessage());
        }
    }

}
