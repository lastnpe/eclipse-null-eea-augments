package org.lastnpe.examples.hello;

import org.eclipse.jdt.annotation.Nullable;

public interface SomeInterface {

    void someMethodWithNullableArgument(@Nullable Object anObject);

    void someMethodWithNonNullableByDefaultArgument(Object anObject);

}
