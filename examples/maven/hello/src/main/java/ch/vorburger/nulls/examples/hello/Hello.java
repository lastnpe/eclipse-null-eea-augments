package ch.vorburger.nulls.examples.hello;

import ch.vorburger.nulls.examples.hello.lib.Service;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hello implements SomeInterface {

    private static final Logger LOG = LoggerFactory.getLogger(Hello.class);

    @Override
    // TODO @Nullable should ideally not have to be repeated here, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=505828
    public void someMethodWithNullableArgument(@Nullable Object anObject) {
    }

    public void anotherMethodWithNullableArgument(@Nullable Object anObject) {
    }

    @Override
    // TODO @Nullable should ideally not have to be repeated here, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=505828
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        LOG.info("hello");

        Optional<String> optionalString = Optional.of("hello");

        String nullString = null;
        optionalString = Optional.ofNullable(nullString);

        Map<String, String> map = new HashMap<>();

        String entry = map.get("key");
        // NOPE, entry is @Nullable, so red: entry.toUpperCase();
        if (entry != null) {
            String entryAsUpper = entry.toUpperCase();
            // OK, entryAsUpper is @NonNull
            entryAsUpper.toLowerCase();
        }

    }

    void foo(@Nullable File canBeNull) {
        if (canBeNull == null) {
            return;
        }
        bar(canBeNull);
    }

    void bar(File nonNull) {

    }

    @Nullable File file2() {
        return null;
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=506376
    java.io.@Nullable File file1() {
        return null;
    }

    // NB: We CANNOT use @javax.annotation.Nullable
    // with @org.eclipse.jdt.annotation.NonNullByDefault
    // (BUT we can elsewhere, e.g. in lib's Service!)
/*
    @javax.annotation.Nullable File file3() {
        return null;
    }
*/

    @SuppressWarnings("unused")
    void callExternalService() {
        Service service = new Service();

        File fileMayBeNull = service.eclipseJdtAnnotationNullableAnnotated();
        // OK, great; this is NOT allowed:
        // bar(fileMayBeNull);

        File fileMayBeNull2 = service.javaxAnnotationNullableAnnotated();
        // TODO This is slightly problematic - it shouldn't be allowed..
        // and in the build it isn't (lib is JAR), but in the IDE
        // if it's an open project it's a hard-to-spot double warning/error in Editor,
        // but error in Problems view; but if it's a binary dep.
        // (because Project closed => M2E takes JAR) then it's
        // fine.
        // bar(fileMayBeNull2);
    }
}
