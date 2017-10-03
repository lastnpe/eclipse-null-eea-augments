package org.lastnpe.examples.hello;

@SuppressWarnings("unused")
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
