/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/// This is a simple implementation of a [groovy.lang.Lazy] value, primarily aimed for use in Java code.
///
/// @param <T> The type of result
/// @apiNote This lazy implementation uses Groovy's [closures][Closure] instead of typical [suppliers][Supplier] or
/// [callables][Callable], as the closure API allows chaining via [Closure#compose(Closure)] and
/// [Closure#andThen(Closure)]. They can still be created using callables.
/// @see Actionable
public sealed class Lazy<T> implements Supplier<T>, Callable<T> permits Lazy.Actionable {
    /// Creates a simple lazy of the given callable.
    ///
    /// @param <T>      The return type of the callable
    /// @param callable The callable to use
    /// @return The lazy value
    public static <T> Lazy<T> simple(Callable<T> callable) {
        return simple(Closures.callable(callable));
    }

    /// Creates a simple lazy of the given closure.
    ///
    /// @param <T>     The return type of the closure
    /// @param closure The callable to use
    /// @return The lazy value
    public static <T> Lazy<T> simple(Closure<T> closure) {
        return new Lazy<>(closure);
    }

    /// Creates an actionable lazy of the given callable.
    ///
    /// @param <T>      The return type of the callable
    /// @param callable The callable to use
    /// @return The lazy value
    public static <T> Actionable<T> actionable(Callable<T> callable) {
        return actionable(Closures.callable(callable));
    }

    /// Creates an actionable lazy of the given closure.
    ///
    /// @param <T>     The return type of the closure
    /// @param closure The callable to use
    /// @return The lazy value
    public static <T> Actionable<T> actionable(Closure<T> closure) {
        return new Actionable<>(closure);
    }

    /// The closure that will provide the value for this lazy.
    protected final Closure<T> closure;
    /// The value of this lazy, will be `null` if it has not yet been computed with [#get()].
    protected @Nullable T value;

    private Lazy(Closure<T> closure) {
        this.closure = closure;
    }

    /// Checks if this lazy value is present. It is not a requirement that the value has to be resolved.
    ///
    /// For simple lazies, it is simply a check if the value has been resolved. See [Actionable#isPresent()] for
    /// actionable lazies.
    ///
    /// @return If this lazy is present
    public boolean isPresent() {
        return this.value != null;
    }

    /// Runs the given action on this lazy value if it is present.
    ///
    /// @param action The action to run
    /// @see #isPresent()
    public final void ifPresent(Action<? super T> action) {
        if (this.isPresent())
            action.execute(this.get());
    }

    /// Gets (and resolves if absent) this lazy value.
    ///
    /// @return The value
    @Override
    public T get() {
        return this.value == null ? this.value = Closures.invoke(this.closure) : this.value;
    }

    @Override
    public T call() {
        return this.get();
    }

    /// Represents a lazily computed value with the ability to optionally work with it using [#ifPresent(Action)] and
    /// safely mutate it using [#map(Action)].
    ///
    /// @param <T> The type of result
    public static final class Actionable<T> extends Lazy<T> {
        private boolean present = false;

        private Closure<T> modifications = Closures.unaryOperator(o -> {
            this.present = true;
            return o;
        });

        private Actionable(Closure<T> closure) {
            super(closure);
        }

        /// Queues the given action to run on the value once it has been computed. If the value has already been
        /// resolved, the action will be executed instantly.
        ///
        /// This marks this lazy as present, meaning that any usage of [#ifPresent(Action)] will resolve the value.
        ///
        /// @param action The action to run
        public void map(Action<? super T> action) {
            this.present = true;
            if (this.value != null) {
                action.execute(this.value);
            } else {
                this.modifications = this.modifications.andThen(Closures.unaryOperator(value -> {
                    action.execute(value);
                    return value;
                }));
            }
        }

        /// Checks if this actionable lazy is present. Presence can either mean that the value has already been computed
        /// or has been mutated by [#map(Action)].
        ///
        /// @return If this actionable lazy is present
        @Override
        public boolean isPresent() {
            return this.present;
        }

        /// Copies this actionable lazy. This can be useful if you need to split off execution paths and have the same
        /// object with different mutations at a specific time.
        ///
        /// @return The a new actionable lazy copied from this one
        public Actionable<T> copy() {
            var ret = new Actionable<>(this.closure);
            ret.value = this.value;
            ret.present = this.present;
            ret.modifications = this.modifications;
            return ret;
        }

        @Override
        public T get() {
            return this.value == null ? this.value = Closures.invoke(this.closure.andThen(this.modifications)) : this.value;
        }
    }
}
