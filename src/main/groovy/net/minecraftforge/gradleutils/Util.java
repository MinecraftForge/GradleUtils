package net.minecraftforge.gradleutils;

import groovy.lang.Closure;

import java.util.function.Consumer;

interface Util {
    /**
     * Creates a closure backed by the given consumer.
     *
     * @param owner    The owner of the closure
     * @param consumer The consumer to execute
     * @param <T>      The type of the action
     * @return The closure
     */
    static <T> Closure<Void> closure(Object owner, Consumer<T> consumer) {
        return new Closures.Consuming<>(owner, consumer);
    }

    @SuppressWarnings("unused") // doCall() is consumed by Groovy
    final class Closures {
        private static final class Consuming<T> extends Closure<Void> {
            private final Consumer<T> consumer;

            private Consuming(Object owner, Consumer<T> consumer) {
                super(owner, owner);
                this.consumer = consumer;
            }

            public Void doCall(T object) {
                this.consumer.accept(object);
                return null;
            }
        }

        private static final class Empty extends Closure<Void> {
            private Empty(Object owner) {
                super(owner, owner);
            }

            public Void doCall(Object object) {
                return null;
            }
        }
    }
}
