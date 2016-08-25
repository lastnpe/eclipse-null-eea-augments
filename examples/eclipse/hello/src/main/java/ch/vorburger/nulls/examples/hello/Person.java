package ch.vorburger.nulls.examples.hello;

import java.util.Objects;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;

public class Person {

    private final String id;
    private final String name;
    private final int fingers;

    private final Optional<Integer> age;
    private final Optional<Integer> height;

    protected Person(String id, String name, int fingers, Optional<Integer> age, Optional<Integer> height) {
        this.id = id;
        this.name = name;
        this.fingers = fingers;
        this.age = age;
        this.height = height;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getFingers() {
        return fingers;
    }

    public Optional<Integer> getAge() {
        return age;
    }

    public Optional<Integer> getHeight() {
        return height;
    }

    public static Builders.IdBuilder builder() {
        return new Builders.IdBuilder();
    }

    protected interface Builders {
        // Inner interface just to group all builders, and make them less visible on Ctrl-Space in IDE

        public static class IdBuilder {
            public NameBuilder id(String id) {
                return new NameBuilder(id);
            }
        }

        public static class NameBuilder {
            protected final String id;

            protected NameBuilder(String id) {
                this.id = id;
            }

            public FingersBuilder name(String name) {
                return new FingersBuilder(id, name);
            }
        }

        // Following *Builder should NOT extend previous one, but repeat
        // properties, so that each property can only be set once.

        public static class FingersBuilder {
            protected final String id;
            protected final String name;

            protected FingersBuilder(String id, String name) {
                this.id = id;
                this.name = name;
            }

            public OptionalsBuilder fingers(int fingers) {
                return new OptionalsBuilder(id, name, fingers);
            }
        }

        public static class OptionalsBuilder {
            protected final String id;
            protected final String name;
            protected int fingers;

            protected Optional<Integer> age = Optional.empty();
            protected Optional<Integer> height = Optional.empty();

            private OptionalsBuilder(String id, String name, int fingers) {
                this.id = id;
                this.name = name;
                this.fingers = fingers;
            }

            public OptionalsBuilder age(int age) {
                this.age = Optional.of(age);
                return this;
            }

            public OptionalsBuilder height(int height) {
                this.height = Optional.of(height);
                return this;
            }

            public Person build() {
                return new Person(id, name, fingers, age, height);
            }

        }

    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, fingers, age, height);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return MoreObjects2.equalsHelper(this, obj, (one, another) ->
               one.id.equals(another.id)
            && one.name.equals(another.name)
            && one.fingers == another.fingers
            && one.age.equals(another.age)
            && one.height.equals(another.height)
        );
    }

    @Override
    public String toString() {
        return "TODO"; // MoreObjects.toStringHelper(this).add("id", id).add("name", name).add("fingers", fingers).add("age", age).add("height", height).toString();
    }

}
