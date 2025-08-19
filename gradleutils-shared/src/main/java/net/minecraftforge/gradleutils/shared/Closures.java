/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.codehaus.groovy.reflection.ReflectionUtils;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.gradle.api.Action;
import org.jetbrains.annotations.UnknownNullability;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/// This class contains helper methods for creating closures in Java code.
///
/// @apiNote <strong>Always</strong> use [#invoke] instead of [Closure#call] to avoid issues with classloader exceptions
/// and closure parameter delegations.
public final class Closures {
    /// Invokes a given closure with the given object as the delegate type and parameter.
    ///
    /// This is used to work around a Groovy DSL implementation detail that involves dynamic objects within
    /// buildscripts. By default, Gradle will attempt to locate the dynamic object that is being referenced within a
    /// closure and use handlers within the buildscript's class loader to work with it. This is unfortunately very
    /// unfriendly to trying to use closures on traditional objects. The solution is to both manually set the closure's
    /// [delegate][Closure#setDelegate(Object)], [resolve strategy][Closure#setResolveStrategy(int)], and temporarily
    /// swap out the [current thread's context class loader][Thread#setContextClassLoader(ClassLoader)] with that of the
    /// closure in order to force resolution of the groovy metaclass to the delegate object.
    ///
    /// I'm very sorry.
    ///
    /// @param <T>     The return type
    /// @param closure The closure to invoke
    /// @param object  The parameter(s) to pass into the closure
    /// @return The result of the closure execution casted to the generic type parameter
    /// @see org.gradle.api.internal.AbstractTask.ClosureTaskAction#doExecute(org.gradle.api.Task)
    @SuppressWarnings({"rawtypes", "JavadocReference"})
    public static <T> @UnknownNullability T invoke(@DelegatesTo(strategy = Closure.DELEGATE_FIRST) Closure closure, Object... object) {
        closure.setDelegate(object[0]);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        return invokeInternal(closure, object);
    }

    private static final Object[] EMPTY_ARGS = {};

    /// Invokes a given closure with no parameters.
    ///
    /// @param <T>     The return type
    /// @param closure The closure to invoke
    /// @return The result of the closure execution casted to the generic type parameter
    /// @see #invoke(Closure, Object...)
    @SuppressWarnings("rawtypes")
    public static <T> @UnknownNullability T invoke(Closure closure) {
        return invokeInternal(closure, EMPTY_ARGS);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> @UnknownNullability T invokeInternal(Closure closure, Object... object) {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(closure.getClass().getClassLoader());
        try {
            Object ret = closure.getMaximumNumberOfParameters() == 0 ? closure.call() : closure.call(object);
            return ret != null ? (T) ret : null;
        } catch (InvokerInvocationException e) {
            Throwable cause = e.getCause();
            throw cause instanceof RuntimeException ? (RuntimeException) cause : e;
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    /// Creates a closure backed by the given unary operator.
    ///
    /// @param function The function to apply
    /// @param <R>      The return type of the function
    /// @return The closure
    /// @apiNote For static methods only.
    public static <R> Closure<R> unaryOperator(UnaryOperator<R> function) {
        return unaryOperator(ReflectionUtils.getCallingClass(), function);
    }

    /// Creates a closure backed by the given unary operator.
    ///
    /// @param owner    The owner of the closure
    /// @param function The function to apply
    /// @param <R>      The return type of the function
    /// @return The closure
    /// @apiNote For instance methods only.
    public static <R> Closure<R> unaryOperator(Object owner, UnaryOperator<R> function) {
        return function(owner, function);
    }

    /// Creates a closure backed by the given function.
    ///
    /// @param function The function to apply
    /// @param <T>      The parameter type of the function
    /// @param <R>      The return type of the function
    /// @return The closure
    /// @apiNote For static methods only.
    public static <T, R> Closure<R> function(Function<? super T, ? extends R> function) {
        return function(ReflectionUtils.getCallingClass(), function);
    }

    /// Creates a closure backed by the given function.
    ///
    /// @param owner    The owner of the closure
    /// @param function The function to apply
    /// @param <T>      The parameter type of the function
    /// @param <R>      The return type of the function
    /// @return The closure
    /// @apiNote For instance methods only.
    public static <T, R> Closure<R> function(Object owner, Function<? super T, ? extends R> function) {
        return new Functional<T, R>(owner, function);
    }

    /// Creates a closure backed by the given supplier.
    ///
    /// @param supplier The supplier to use
    /// @param <R>      The return type of the supplier
    /// @return The closure
    /// @apiNote For static methods only.
    public static <R> Closure<R> supplier(Supplier<? extends R> supplier) {
        return supplier(ReflectionUtils.getCallingClass(), supplier);
    }

    /// Creates a closure backed by the given supplier.
    ///
    /// @param owner    The owner of the closure
    /// @param supplier The supplier to use
    /// @param <R>      The return type of the supplier
    /// @return The closure
    /// @apiNote For instance methods only.
    public static <R> Closure<R> supplier(Object owner, Supplier<? extends R> supplier) {
        return callable(owner, supplier::get);
    }

    /// Creates a closure backed by the given callable.
    ///
    /// @param callable The callable to use
    /// @param <R>      The return type of the callable
    /// @return The closure
    /// @apiNote For static methods only.
    public static <R> Closure<R> callable(Callable<? extends R> callable) {
        return callable(ReflectionUtils.getCallingClass(), callable);
    }

    /// Creates a closure backed by the given callable.
    ///
    /// @param owner    The owner of the closure
    /// @param callable The callable to use
    /// @param <R>      The return type of the callable
    /// @return The closure
    /// @apiNote For instance methods only.
    public static <R> Closure<R> callable(Object owner, Callable<? extends R> callable) {
        return new Supplying<>(owner, callable);
    }

    /// Creates a closure backed by the given action.
    ///
    /// @param action The action to execute
    /// @param <T>    The type of the action
    /// @return The closure
    /// @apiNote For static methods only.
    public static <T> Closure<Void> action(Action<? super T> action) {
        return action(ReflectionUtils.getCallingClass(), action);
    }

    /// Creates a closure backed by the given action.
    ///
    /// @param owner  The owner of the closure
    /// @param action The action to execute
    /// @param <T>    The type of the action
    /// @return The closure
    /// @apiNote For instance methods only.
    public static <T> Closure<Void> action(Object owner, Action<? super T> action) {
        return consumer(owner, action::execute);
    }

    /// Creates an action backed by the given closure.
    ///
    /// @param closure The closure to call
    /// @param <T>     The type of the action
    /// @return The action
    public static <T> Action<T> toAction(Closure<?> closure) {
        return it -> Closures.invoke(closure, it);
    }

    /// Creates a closure backed by the given consumer.
    ///
    /// @param consumer The consumer to execute
    /// @param <T>      The type of the action
    /// @return The closure
    /// @apiNote For static methods only.
    public static <T> Closure<Void> consumer(Consumer<? super T> consumer) {
        return consumer(ReflectionUtils.getCallingClass(), consumer);
    }

    /// Creates a closure backed by the given consumer.
    ///
    /// @param owner    The owner of the closure
    /// @param consumer The consumer to execute
    /// @param <T>      The type of the action
    /// @return The closure
    /// @apiNote For instance methods only.
    public static <T> Closure<Void> consumer(Object owner, Consumer<? super T> consumer) {
        return new Consuming<>(owner, consumer);
    }

    /// Creates a closure backed by the given runnable.
    ///
    /// @param runnable The runnable to execute
    /// @return The closure
    /// @apiNote For static methods only.
    public static Closure<Void> runnable(Runnable runnable) {
        return runnable(ReflectionUtils.getCallingClass(), runnable);
    }

    /// Creates a closure backed by the given runnable.
    ///
    /// @param owner    The owner of the closure
    /// @param runnable The runnable to execute
    /// @return The closure
    /// @apiNote For instance methods only.
    public static Closure<Void> runnable(Object owner, Runnable runnable) {
        return new Running(owner, runnable);
    }

    /// Creates an empty closure.
    ///
    /// @return The empty closure
    /// @apiNote For static methods only.
    public static Closure<Void> empty() {
        return empty(ReflectionUtils.getCallingClass());
    }

    /// Creates an empty closure.
    ///
    /// @param owner The owner of the closure
    /// @return The empty closure
    /// @apiNote For instance methods only.
    public static Closure<Void> empty(Object owner) {
        return new Empty(owner);
    }

    private static final class Functional<T, R> extends Closure<R> {
        private final Function<? super T, ? extends R> function;

        private Functional(Object owner, Function<? super T, ? extends R> function) {
            super(owner, owner);
            this.function = function;
        }

        @SuppressWarnings("unused") // invoked by Groovy
        public R doCall(T object) {
            return this.function.apply(object);
        }
    }

    private static final class Supplying<R> extends Closure<R> {
        private final Callable<? extends R> supplier;

        private Supplying(Object owner, Callable<? extends R> supplier) {
            super(owner, owner);
            this.supplier = supplier;
        }

        @SuppressWarnings("unused") // invoked by Groovy
        public R doCall() throws Exception {
            return this.supplier.call();
        }
    }

    private static final class Consuming<T> extends Closure<Void> {
        private final Consumer<? super T> consumer;

        private Consuming(Object owner, Consumer<? super T> consumer) {
            super(owner, owner);
            this.consumer = consumer;
        }

        @SuppressWarnings("unused") // invoked by Groovy
        public Void doCall(T object) {
            this.consumer.accept(object);
            return null;
        }
    }

    private static final class Running extends Empty {
        private final Runnable runnable;

        private Running(Object owner, Runnable runnable) {
            super(owner);
            this.runnable = runnable;
        }

        @Override
        public Void doCall() {
            this.runnable.run();
            return super.doCall();
        }
    }

    private static class Empty extends Closure<Void> {
        public Empty(Object owner) {
            super(owner, owner);
        }

        @SuppressWarnings("unused") // invoked by Groovy
        public Void doCall() {
            return null;
        }
    }

    private Closures() { }
}
