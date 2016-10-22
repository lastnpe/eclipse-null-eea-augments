package ch.vorburger.nulls.examples.hello;

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

    void foo(@Nullable String canBeNull) {
        if (canBeNull == null) {
            return;
        }
        bar(canBeNull);
    }

    void bar(String nonNull) {

    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=506376
/*    
    @Nullable java.io.File file() {
        return null;
    }
*/    
}
