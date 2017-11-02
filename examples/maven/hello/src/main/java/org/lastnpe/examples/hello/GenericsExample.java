package org.lastnpe.examples.hello;

import static org.eclipse.jdt.annotation.DefaultLocation.ARRAY_CONTENTS;
import static org.eclipse.jdt.annotation.DefaultLocation.FIELD;
import static org.eclipse.jdt.annotation.DefaultLocation.PARAMETER;
import static org.eclipse.jdt.annotation.DefaultLocation.RETURN_TYPE;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_ARGUMENT;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_BOUND;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_PARAMETER;

import org.eclipse.jdt.annotation.NonNullByDefault;

@SuppressWarnings("unused")
@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_PARAMETER, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS })
public class GenericsExample {

    private static class Parent<T> {
    }

    // Null constraint mismatch: The type 'T' is not a valid substitute for the type parameter '@NonNull T'
    // This seems wrong, because the mouse over hover clearly shows Parent<@NonNull T> (from @NonNullByDefault) and Child<@NonNull T>
    // Workaround "Child<@NonNull T>" makes it disappear, but why is this needed?
    // TODO Is this https://bugs.eclipse.org/bugs/show_bug.cgi?id=522142 ?
    private static class Child<T> extends Parent<T> {
    }
}
