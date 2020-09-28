// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=525512
// Add TYPE_PARAMETER & ARRAY_CONTENTS to NonNullByDefault value() default:
@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_PARAMETER, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS })
package org.lastnpe.examples.hello;

import static org.eclipse.jdt.annotation.DefaultLocation.ARRAY_CONTENTS;
import static org.eclipse.jdt.annotation.DefaultLocation.FIELD;
import static org.eclipse.jdt.annotation.DefaultLocation.PARAMETER;
import static org.eclipse.jdt.annotation.DefaultLocation.RETURN_TYPE;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_ARGUMENT;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_BOUND;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_PARAMETER;

import org.eclipse.jdt.annotation.NonNullByDefault;
