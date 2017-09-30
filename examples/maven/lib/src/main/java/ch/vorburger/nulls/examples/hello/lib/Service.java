package ch.vorburger.nulls.examples.hello.lib;

// Because we have NonNullByDefault in package-info.java
// but here we can't use that because of the use of javax.annotation.Nullable
// which unfortunately cannot be mixed with org.eclipse.jdt.annotation.NonNullByDefault,
// we need to cancel it like this:
@org.eclipse.jdt.annotation.NonNullByDefault({})
public class Service {

    public @javax.annotation.Nullable java.io.File javaxAnnotationNullableAnnotated() {
        // NB This only works here if it's NOT @NonNullByDefault (see Hello)
        return null;
    }

    public java.io.@org.eclipse.jdt.annotation.Nullable File eclipseJdtAnnotationNullableAnnotated() {
        return null;
    }

}
