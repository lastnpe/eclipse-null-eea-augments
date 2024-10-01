package org.lastnpe.examples.hello;

@SuppressWarnings("unused")
public class GenericsExample {

    private static class Parent<T> {
    }

    private static class Child<T> extends Parent<T> {
    }
}
