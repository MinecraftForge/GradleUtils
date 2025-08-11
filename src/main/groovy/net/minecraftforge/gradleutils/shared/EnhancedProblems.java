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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// The enhanced problems contain several base helper members to help reduce duplicate code between Gradle plugins.
@ApiStatus.OverrideOnly
public abstract class EnhancedProblems implements Problems {
    /// The common message to send in [ProblemSpec#solution(String)] when reporting problems.
    protected static final String HELP_MESSAGE = "Consult the documentation or ask for help on the Forge Forums, GitHub, or Discord server.";

    private final String displayName;
    private final ProblemGroup problemGroup;

    private final Problems delegate;
    private final Predicate<String> properties;

    @Override
    public ProblemReporter getReporter() {
        return this.delegate.getReporter();
    }

    /// Gets the problem group used by this problems instance. It is unique for the plugin.
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

        this.delegate = this.unwrapProblems();
        this.properties = this.unwrapProperties();
    }

    /// Creates a problem ID to be used when reporting problems. The name must be unique so as to not potentially
    /// override other reported problems in the report.
    ///
    /// @param name        The name for this problem
    /// @param displayName The display name for this problem
    /// @return The problem ID
    protected final ProblemId id(String name, String displayName) {
        return ProblemId.create(name, displayName, this.getProblemGroup());
    }

    /// Checks if the given property exists and equals `true`. This checks both [Gradle][ProviderFactory#gradleProperty]
    /// and [System][ProviderFactory#systemProperty] properties, giving the former higher priority. If for some reason a
    /// provider factory is not available in the current environment, [Boolean#getBoolean(String)] will be used
    /// instead.
    ///
    /// @param property The property to test
    /// @return If the property exists and is `true`
    protected final boolean hasProperty(String property) {
        return this.properties.test(property);
    }


    /* DEFAULT PROBLEMS */

    //region Enhanced Plugin

    /// Reports an illegal plugin target.
    ///
    /// @param e                  The exception that was caught, will be re-thrown (or wrapped with a
    ///                           [RuntimeException])
    /// @param firstAllowedTarget The first allowed target for the plugin
    /// @param allowedTargets     The remaining allowed targets for the plugin
    /// @return The exception to throw
    public final RuntimeException illegalPluginTarget(Exception e, Class<?> firstAllowedTarget, Class<?>... allowedTargets) {
        return this.getReporter().throwing(e, id("invalid-plugin-target", "Invalid plugin target"), spec -> spec
            .details("""
                Attempted to apply the %s plugin to an invalid target.
                This plugin can only be applied on the following types:
                %s""".formatted(this.displayName, Stream.concat(Stream.of(firstAllowedTarget), Stream.of(allowedTargets)).map(Class::getName).collect(Collectors.joining(", ", "[", "]"))))
            .severity(Severity.ERROR)
            .stackLocation()
            .solution("Use a valid plugin target.")
            .solution(HELP_MESSAGE));
    }

    /// Reports an illegal plugin target.
    ///
    /// @param e              The exception that was caught, will be re-thrown (or wrapped with a [RuntimeException])
    /// @param allowedTargets A string stating the allowed targets for the plugin
    /// @return The exception to throw
    public final RuntimeException illegalPluginTarget(Exception e, String allowedTargets) {
        return this.getReporter().throwing(e, id("invalid-plugin-target", "Invalid plugin target"), spec -> spec
            .details("""
                Attempted to apply the %s plugin to an invalid target.
                This plugin can only be applied on %s""".formatted(this.displayName, allowedTargets))
            .severity(Severity.ERROR)
            .stackLocation()
            .solution("Use a valid plugin target.")
            .solution(HELP_MESSAGE));
    }

    /// Reports an illegal access of a plugin before it has been applied.
    ///
    /// @param e The exception that was caught, will be re-thrown (or wrapped with a [RuntimeException])
    /// @return The exception to throw
    public final RuntimeException pluginNotYetApplied(Exception e) {
        return this.getReporter().throwing(e, id("plugin-not-yet-applied", "%s is not applied".formatted(this.displayName)), spec -> spec
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

    //region ToolExecBase

    /// Reports an implementation of [ToolExecBase] that is not enhanced with [EnhancedTask].
    ///
    /// @param task The affected task
    public final void reportToolExecNotEnhanced(Task task) {
        this.getReporter().report(id("tool-exec-not-enhanced", "ToolExec subclass doesn't implement EnhancedTask"), spec -> spec
            .details("""
                Implementing subclass of ToolExecBase should also implement (a subclass of) EnhancedTask.
                Not doing so will result in global caches being ignored. Please check your implementations.
                Affected task: %s (%s)""".formatted(task, task.getClass()))
            .severity(Severity.WARNING)
            .stackLocation()
            .solution("Double check your task implementation."));
    }

    /// Reports an implementation of [ToolExecBase] that adds arguments without using [ToolExecBase#addArguments()]
    ///
    /// @param task The affected task
    public final void reportToolExecEagerArgs(Task task) {
        this.getReporter().report(id("tool-exec-eager-args", "ToolExecBase implementation adds arguments without using addArguments()"), spec -> spec
            .details("""
                A ToolExecBase task is eagerly adding arguments using JavaExec#args without using ToolExecBase#addArguments.
                This may cause implementations or superclasses to have their arguments ignored or missing.
                Affected task: %s (%s)""".formatted(task, task.getClass()))
            .severity(Severity.WARNING)
            .stackLocation()
            .solution("Use ToolExecBase#addArguments"));
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
            var dir = file instanceof Directory ? file.getAsFile() : file.getAsFile().getParentFile();
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException e) {
                throw this.getReporter().throwing(e, id("cannot-ensure-directory", "Failed to create directory"), spec -> spec
                    .details("""
                        Failed to create a directory required for %s to function.
                        Directory: %s"""
                        .formatted(this.displayName, dir.getAbsolutePath()))
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

        private RuntimeException toRTE(Throwable exception) {
            return exception instanceof RuntimeException rte ? rte : new RuntimeException(exception);
        }
    }


    /* MINIMAL */

    static abstract class Minimal extends EnhancedProblems implements HasPublicType {
        @Inject
        public Minimal(String name, String displayName) {
            super(name, displayName);
        }

        @Override
        public TypeOf<?> getPublicType() {
            return TypeOf.typeOf(EnhancedProblems.class);
        }
    }


    /* IMPL INSTANTIATION */

    /** @see <a href="https://docs.gradle.org/current/userguide/reporting_problems.html">Reporting Problems</a> */
    protected @Inject Problems getProblems() {
        throw new IllegalStateException();
    }

    /**
     * @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#providerfactory">ProviderFactory
     * Service Injection</a>
     */
    protected @Inject ProviderFactory getProviders() {
        throw new IllegalStateException();
    }

    private Problems unwrapProblems() {
        try {
            return this.getProblems();
        } catch (Exception e) {
            return EmptyReporter.AS_PROBLEMS;
        }
    }

    private Predicate<String> unwrapProperties() {
        try {
            var providers = Objects.requireNonNull(this.getProviders());
            return property -> isTrue(providers, property);
        } catch (Exception e) {
            return Boolean::getBoolean;
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
