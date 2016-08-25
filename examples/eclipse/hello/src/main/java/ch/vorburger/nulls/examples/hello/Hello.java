package ch.vorburger.nulls.examples.hello;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hello {

    private static final Logger LOG = LoggerFactory.getLogger(Hello.class);

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public static void main(String[] args) {
        LOG.info("hello");

        Optional<String> optionalString = Optional.of("hello");

        String nullString = null;
        optionalString = Optional.ofNullable(nullString);

        Person p = Person.builder().id("123").name("Tony").fingers(5).age(32).build();

        Map<String, String> map = new HashMap<>();
        @NonNull
        String entry = map.get("key");
    }
}
