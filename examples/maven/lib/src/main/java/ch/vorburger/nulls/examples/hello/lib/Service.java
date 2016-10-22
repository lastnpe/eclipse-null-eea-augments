/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.nulls.examples.hello.lib;

import java.io.File;

// @org.eclipse.jdt.annotation.NonNullByDefault
public class Service {

    public @javax.annotation.Nullable File javaxAnnotationNullableAnnotated() {
        // NB This only works here if it's NOT @NonNullByDefault (see Hello)
        return null;
    }

    public @org.eclipse.jdt.annotation.Nullable File eclipseJdtAnnotationNullableAnnotated() {
        return null;
    }

}
