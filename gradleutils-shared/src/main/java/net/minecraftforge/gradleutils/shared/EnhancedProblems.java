/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.problems.Problem;
import org.gradle.api.problems.ProblemGroup;
import org.gradle.api.problems.ProblemId;
import org.gradle.api.problems.ProblemReporter;
import org.gradle.api.problems.ProblemSpec;
import org.gradle.api.problems.Problems;
import org.gradle.api.problems.Severity;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// The enhanced problems contain several base helper members to help reduce duplicate code between Gradle plugins.
public abstract class EnhancedProblems implements Serializable, Predicate<String> {
    private static final @Serial long serialVersionUID = 2037193772993696096L;

    /// The common message to send in [ProblemSpec#solution(String)] when reporting problems.
    protected static final String HELP_MESSAGE = "Consult the documentation or ask for help on the Forge Forums, GitHub, or Discord server.";

    /// The display name used in reported problems to describe the plugin using this.
    private final String displayName;
    /// The problem group used when reporting problems using this.
    private final ProblemGroup problemGroup;

    /// The problems instance provided by Gradle services.
    ///
    /// @return The problems instance
    /// @see <a href="https://docs.gradle.org/current/userguide/reporting_problems.html">Reporting Problems</a>
    /// @deprecated Does not handle if Problems API cannot be accessed. Use [#getDelegate()].
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed") // Used by #getDelegate
    protected abstract @Inject Problems getProblems();

    /// The provider factory provided by Gradle services.
    ///
    /// @return The provider factory
    /// @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#providerfactory">ProviderFactory
    /// Service Injection</a>
    protected abstract @Inject ProviderFactory getProviders();

    /// Gets the problems instance used by this enhanced problems.
    ///
    /// @return The delegate problems instance
    public final Problems getDelegate() {
        try {
            return this.getProblems();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get Problems instance! This is an unrecoverable error, likely due to the removal of (the service injection of) the incubating Problems API in a Gradle update. Please report this to MinecraftForge.", e);
        }
    }

    /// Gets the problem reporter used by the [delegate][#getDelegate()] problems instance.
    ///
    /// @return The problem reporter
    /// @deprecated Use [#report(String, String, Action)] or [#throwing(Throwable, String, String, Action)].
    @Deprecated(forRemoval = true, since = "3.2.26")
    protected final ProblemReporter getReporter() {
        return this.getDelegate().getReporter();
    }

    /// Gets the problem group used by this enhanced problems. It is unique for the plugin.
    ///
    /// @return The problem group
    public final ProblemGroup getProblemGroup() {
        return this.problemGroup;
    }

    /// Creates a new enhanced problems instance using the given name and display name. These names are passed into a
    /// problem group that will be used by this instance.
    ///
    /// @param name        The name for this enhanced problems instance
    /// @param displayName The display name for this enhanced problems instance
    /// @implSpec The implementing subclass <strong>must</strong> make their constructor public, annotated with
    /// [Inject], and have no parameters, passing in static strings to this base constructor.
    protected EnhancedProblems(String name, String displayName) {
        this.problemGroup = ProblemGroup.create(name, this.displayName = displayName);
    }

    /// Gets the logger to be used by this enhanced problems.
    ///
    /// @return The logger
    protected final Logger getLogger() {
        return Logging.getLogger(this.getClass());
    }

    /// Reports an issue using the give name and display name as the [ProblemId] and the given spec to create the
    /// [Problem].
    ///
    /// @param name        The name of the problem
    /// @param displayName The display name of the problem
    /// @param spec        The details to use when creating the problem
    /// @see #throwing(Throwable, String, String, Action)
    protected final void report(String name, String displayName, Action<? super ProblemSpec> spec) {
        this.getDelegate().getReporter().report(ProblemId.create(name, displayName, this.getProblemGroup()), spec);
    }

    /// Reports an issue much like [#report(String, String, Action)], but also returns the given exception to be thrown
    /// by the calling class.
    ///
    /// @param exception   The exception to throw
    /// @param name        The name of the problem
    /// @param displayName The display name of the problem
    /// @param spec        The details to use when creating the problem
    /// @return The exception to be thrown by the calling class
    /// @apiNote Due to a bug in Gradle, any exceptions that do not extend [RuntimeException] will be wrapped inside
    /// one. While this pollutes the stacktrace, it keeps the details of the problem in view for terminal and IDE
    /// users.
    /// @see #report(String, String, Action)
    protected final RuntimeException throwing(Throwable exception, String name, String displayName, Action<? super ProblemSpec> spec) {
        return this.getDelegate().getReporter().throwing(exception instanceof RuntimeException ? exception : new RuntimeException(exception), ProblemId.create(name, displayName, this.getProblemGroup()), spec);
    }

    /// Creates a problem ID to be used when reporting problems. The name must be unique so as to not potentially
    /// override other reported problems in the report.
    ///
    /// @param name        The name for this problem
    /// @param displayName The display name for this problem
    /// @return The problem ID
    /// @deprecated Use [#report(String, String, Action)] or [#throwing(Throwable, String, String, Action)].
    @Deprecated(forRemoval = true, since = "3.2.26")
    protected final ProblemId id(String name, String displayName) {
        return ProblemId.create(name, displayName, this.getProblemGroup());
    }

    /// Checks if the given property exists and equals `true`.
    ///
    /// This checks both [Gradle][ProviderFactory#gradleProperty] and [System][ProviderFactory#systemProperty]
    /// properties, giving the former higher priority. If for some reason a provider factory is not available in the
    /// current environment, [Boolean#getBoolean(String)] will be used instead.
    ///
    /// @param property The property to test
    /// @return If the property exists and is `true`
    @Override
    public final boolean test(String property) {
        try {
            return isTrue(this.getProviders(), property);
        } catch (Exception e) {
            return Boolean.getBoolean(property);
        }
    }

    /// Checks if the given property exists and equals `false`.
    ///
    /// This checks both [Gradle][ProviderFactory#gradleProperty] and [System][ProviderFactory#systemProperty]
    /// properties, giving the former higher priority. If for some reason a provider factory is not available in the
    /// current environment, [Boolean#getBoolean(String)] will be used instead.
    ///
    /// @param property The property to test
    /// @return If the property exists and is `false`
    public final boolean testFalse(String property) {
        try {
            return isFalse(this.getProviders(), property);
        } catch (Exception e) {
            return Boolean.getBoolean(property);
        }
    }


    /* DEFAULT PROBLEMS */

    //region Enhanced Plugin
    @SuppressWarnings("SameParameterValue")
    final RuntimeException illegalPluginTarget(Exception e, Class<?> firstAllowedTarget, Class<?>... allowedTargets) {
        return this.throwing(e, "invalid-plugin-target", "Invalid plugin target", spec -> spec
            .details("""
                Attempted to apply the %s plugin to an invalid target.
                This plugin can only be applied on the following types:
                %s""".formatted(this.displayName, Stream.concat(Stream.of(firstAllowedTarget), Stream.of(allowedTargets)).map(Class::getName).collect(Collectors.joining(", ", "[", "]"))))
            .severity(Severity.ERROR)
            .stackLocation()
            .solution("Use a valid plugin target.")
            .solution(HELP_MESSAGE));
    }

    @SuppressWarnings("SameParameterValue")
    final RuntimeException illegalPluginTarget(Exception e, String allowedTargets) {
        return this.throwing(e, "invalid-plugin-target", "Invalid plugin target", spec -> spec
            .details("""
                Attempted to apply the %s plugin to an invalid target.
                This plugin can only be applied on %s.""".formatted(this.displayName, allowedTargets))
            .severity(Severity.ERROR)
            .stackLocation()
            .solution("Use a valid plugin target.")
            .solution(HELP_MESSAGE));
    }

    final RuntimeException pluginNotYetApplied(Exception e) {
        return this.throwing(e, "plugin-not-yet-applied", String.format("%s is not applied", this.displayName), spec -> spec
            .details("""
                Attempted to get details from the %s plugin, but it has not yet been applied to the target.""".formatted(this.displayName))
            .severity(Severity.ERROR)
            .stackLocation()
            .solution("Apply the plugin before attempting to use it from the target's plugin manager.")
            .solution("Apply the plugin before attempting to register any of its tasks, especially those that require in-house caching or tools.")
            .solution(HELP_MESSAGE)
        );
    }
    //endregion

    //region Utilities

    /// A utility method to ensure that a [FileSystemLocation] [Provider] has (its parent) directory created. If the
    /// directory cannot be created, an exception will be thrown when the provider that consumes this is resolved.
    ///
    /// @param <T> The type of file system location (i.e. [org.gradle.api.file.RegularFile] or [Directory])
    /// @return The transformer to apply onto a provider
    public final <T extends FileSystemLocation> Transformer<T, T> ensureFileLocation() {
        return file -> {
            File dir = file instanceof Directory ? file.getAsFile() : file.getAsFile().getParentFile();
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException e) {
                throw this.throwing(e, "cannot-ensure-directory", "Failed to create directory", spec -> spec
                    .details("""
                        Failed to create a directory required for %s to function.
                        Directory: %s""".formatted(this.displayName, dir.getAbsolutePath()))
                    .severity(Severity.ERROR)
                    .stackLocation()
                    .solution("Ensure that the you have write access to the directory that needs to be created.")
                    .solution(HELP_MESSAGE));
            }

            return file;
        };
    }
    //endregion


    /* EMPTY */

    private interface EmptyReporter extends ProblemReporter, HasPublicType {
        EmptyReporter INSTANCE = new EmptyReporter() { };
        Problems AS_PROBLEMS = () -> INSTANCE;

        @Override
        default TypeOf<?> getPublicType() {
            return TypeOf.typeOf(ProblemReporter.class);
        }

        @Override
        default Problem create(ProblemId problemId, Action<? super ProblemSpec> action) {
            return new Problem() { };
        }

        @Override
        default void report(ProblemId problemId, Action<? super ProblemSpec> spec) { }

        @Override
        default void report(Problem problem) { }

        @Override
        default void report(Collection<? extends Problem> problems) { }

        @Override
        default RuntimeException throwing(Throwable exception, ProblemId problemId, Action<? super ProblemSpec> spec) {
            return this.toRTE(exception);
        }

        @Override
        default RuntimeException throwing(Throwable exception, Problem problem) {
            return this.toRTE(exception);
        }

        @Override
        default RuntimeException throwing(Throwable exception, Collection<? extends Problem> problems) {
            return this.toRTE(exception);
        }

        default RuntimeException toRTE(Throwable exception) {
            return exception instanceof RuntimeException ? (RuntimeException) exception : new RuntimeException(exception);
        }
    }


    /* MINIMAL */

    static abstract class Minimal extends EnhancedProblems implements HasPublicType {
        private static final @Serial long serialVersionUID = -6804792858587052477L;

        @Inject
        public Minimal(String name, String displayName) {
            super(name, displayName);
        }

        @Override
        public TypeOf<?> getPublicType() {
            return TypeOf.typeOf(EnhancedProblems.class);
        }
    }


    /* IMPL UTILS */

    private static @Nullable Boolean getBoolean(Provider<? extends String> provider) {
        if (Boolean.TRUE.equals(provider.map("true"::equalsIgnoreCase).getOrNull())) return true;
        if (Boolean.FALSE.equals(provider.map("false"::equalsIgnoreCase).getOrNull())) return false;
        return null;
    }

    private static boolean isTrue(ProviderFactory providers, String property) {
        return isTrue(providers.gradleProperty(property)) || isTrue(providers.systemProperty(property));
    }

    private static boolean isTrue(Provider<? extends String> provider) {
        return Boolean.TRUE.equals(getBoolean(provider));
    }

    private static boolean isFalse(ProviderFactory providers, String property) {
        return isFalse(providers.gradleProperty(property)) || isFalse(providers.systemProperty(property));
    }

    private static boolean isFalse(Provider<? extends String> provider) {
        return Boolean.FALSE.equals(getBoolean(provider));
    }
}
