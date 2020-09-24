package org.lastnpe.examples.hello;

import org.eclipse.jdt.annotation.Nullable;

import org.eclipse.jdt.annotation.Checks;
import org.eclipse.jdt.annotation.NonNull;

@SuppressWarnings("unused")
public class ArrayExample {

    // This class illustrates some "negative examples"
    // lines marked "//-" are commented out because they produce errors (which would fail the build)
    // un-comment them to see and learn

    void testNnSNnA(/*@NonNull*/ String /*@NonNull*/ [] tmp) {
        //- tmp = null; // error: incompatible types in assignment.
        //- tmp[0] = null; // error: incompatible types in assignment.
    }

    void testNnSCnA(@NonNull String @Nullable [] tmp) {
        // TODO: assigning parameter not allowed
        // tmp = null;
        //- tmp[0] = null; // error: accessing a possibly-null array tmp + incompatible types in assignment
    }

    void testCnSNnA(@Nullable String /*@NonNull*/ [] tmp) {
        //- tmp = null; // error: incompatible types in assignment
        tmp[0] = null;
    }

    void testCnSCnA(@Nullable String @Nullable [] tmp) {
        // TODO: assigning parameter not allowed
        // tmp = null;
        //- tmp[0] = null; // error: accessing a possibly-null array tmp
    }

    void arrayLiteral() {
        // despite ARRAY_CONTENTS (above), this leads to when built on CLI, even though it works in IDE:
        // error: Null type safety (type annotations): The expression of type 'String @NonNull[]' needs unchecked conversion to conform to '@NonNull String @NonNull[]'
        //- @NonNull String @NonNull [] lines = {"foo", "bar"};
        // TDO uncomment after JDT version used in mvn CLI build is upgraded
    }

    void nullableArray() {
        @NonNull String @Nullable [] lines = null;
        Checks.ifNonNull(lines, theLines -> {
            // error: Null type safety (type annotations): The expression of type '@NonNull String []' needs unchecked conversion to conform to '@NonNull String @NonNull[]'
            // because Checks.ifNonNull does not (cannot?) affect the array type in the general non-array @NonNull Consumer<@NonNull ? super T> argument
            // it would perhaps be possible to write a method in Checks specific to arrays?
            // but, as seen below, best just avoid arrays in null safe programs, if you can..
            //- foo(theLines);
        });
    }

    void foo(String[] linesArgument) {
        // comment
    }

    void newArray() {
        // despite ARRAY_CONTENTS (above), this leads to:
        // error: Null type safety (type annotations): The expression of type 'String[]' needs unchecked conversion to conform to '@NonNull String []'
        // because "new Type[N]" is not automatically recognized as @NonNull [] ... which seems wrong :-(
        //- String[] newLine = new String[43];
        //- String @Nullable[] newLine = new String[43];

        // As https://bugs.eclipse.org/bugs/show_bug.cgi?id=499730 points out:
        // "Arrays are a pain :)" [for null analysis], as newLine[0] == null anyway... :-(
        // so best just avoid arrays in null safe programs, if you can.
    }

}
