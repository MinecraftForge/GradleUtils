/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.Transformer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileSystemLocationProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.LoggingManager;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/// This tool execution task is a template on top of [JavaExec] to make executing [tools][Tool] much easier and more
/// consistent between plugins.
///
/// @param <P> The type of enhanced problems, used for common problems reporting with illegal task arguments
/// @implSpec Implementing plugins should make a shared subclass named `ToolExec` which all other tool executing tasks
/// should extend from.
/// @see JavaExec
/// @see Tool
public abstract class ToolExecBase<P extends EnhancedProblems> extends DefaultTask implements EnhancedTask<P> {
    private final P problems = this.getObjects().newInstance(this.problemsType());

    /// The default tool directory (usage is not required).
    protected final DirectoryProperty defaultToolDir = this.getObjects().directoryProperty();
    private final ListProperty<String> additionalArgs = this.getObjects().listProperty(String.class);

    /// Additional arguments to use when invoking the tool. Use in configuration instead of [#args].
    ///
    /// @return The list property for the additional arguments
    public @Input @Optional ListProperty<String> getAdditionalArgs() {
        return this.additionalArgs;
    }

    //region JavaExec
    protected abstract @InputFiles @Classpath ConfigurableFileCollection getClasspath();

    protected abstract @Input @Optional Property<String> getMainClass();

    protected abstract @Nested Property<JavaLauncher> getJavaLauncher();
    //endregion

    //region Logging
    @Deprecated
    @Override public LoggingManager getLogging() {
        return super.getLogging();
    }

    protected abstract @Console Property<LogLevel> getStandardOutputLogLevel();

    protected abstract @Console Property<LogLevel> getStandardErrorLogLevel();
    //endregion

    protected abstract @Inject ObjectFactory getObjects();

    protected abstract @Inject ProviderFactory getProviders();

    protected abstract @Inject ExecOperations getExecOperations();

    /// Creates a new task instance using the given types and tool information.
    ///
    /// @param tool The tool to use for this task
    /// @implSpec The implementing subclass <strong>must</strong> make their constructor public, annotated with
    /// [Inject], and have only a single parameter for [Tool], passing in static plugin and problems types to this base
    /// constructor. The types must also be manually specified in the class declaration when overriding this class. The
    /// best practice is to make a single `ToolExec` class for the implementing plugin to use, which other tasks can
    /// extend off of.
    protected ToolExecBase(Tool tool) {
        var resolved = this.getTool(tool);
        SharedUtil.finalizeProperty(this.defaultToolDir.value(
            this.globalCaches().dir(tool.getName().toLowerCase(Locale.ENGLISH)).map(this.ensureFileLocationInternal())
        ));

        this.getClasspath().setFrom(resolved.getClasspath());

        if (resolved.hasMainClass())
            this.getMainClass().set(resolved.getMainClass());
        this.getJavaLauncher().set(resolved.getJavaLauncher());

        this.getStandardOutputLogLevel().convention(LogLevel.LIFECYCLE);
        this.getStandardErrorLogLevel().convention(LogLevel.ERROR);
    }

    /// The enhanced problems instance to use for this task.
    ///
    /// @return The enhanced problems
    protected final @Internal P getProblems() {
        return this.problems;
    }

    private <T extends FileSystemLocation> Transformer<T, T> ensureFileLocationInternal() {
        return t -> this.getProblems().<T>ensureFileLocation().transform(t);
    }

    /// This method should be overridden by subclasses to add arguments to this task via [JavaExec#args]. To preserve
    /// arguments added by superclasses, this method [must be invoked by overriders][MustBeInvokedByOverriders].
    @MustBeInvokedByOverriders
    protected void addArguments() { }

    private transient boolean executing = false;
    private transient @Nullable List<Provider<String>> args;
    private transient @Nullable List<Provider<String>> jvmArgs;
    private transient @Nullable Map<String, String> environment;
    private transient @Nullable Map<String, String> systemProperties;

    /// @implNote Not invoking this method from an overriding method *will* result in the tool never being executed and
    /// [#addArguments()] never being run.
    @TaskAction
    protected ExecResult exec() {
        this.executing = true;
        this.args = new ArrayList<>();
        this.jvmArgs = new ArrayList<>();
        this.environment = new HashMap<>();
        this.systemProperties = new HashMap<>();

        this.addArguments();
        this.args(this.getAdditionalArgs().get());

        var args = DefaultGroovyMethods.collect(this.args, Closures.<Provider<String>, String>function(Provider::get));
        var jvmArgs = DefaultGroovyMethods.collect(this.jvmArgs, Closures.<Provider<String>, String>function(Provider::get));
        this.getLogger().info("{} {}", this.getClasspath().getAsPath(), String.join(" ", args));

        var stdOutLevel = this.getStandardOutputLogLevel().get();
        var stdErrLevel = this.getStandardErrorLogLevel().get();
        return this.getExecOperations().javaexec(spec -> {
            spec.setIgnoreExitValue(true);

            spec.setClasspath(this.getClasspath());
            spec.getMainClass().set(this.getMainClass());
            spec.setExecutable(this.getJavaLauncher().get().getExecutablePath().getAsFile().getAbsolutePath());
            spec.setArgs(args);
            spec.setJvmArgs(jvmArgs);
            spec.setEnvironment(this.environment);
            spec.setSystemProperties(this.systemProperties);

            spec.setStandardOutput(SharedUtil.toLog(this.getLogger(), stdOutLevel));
            spec.setErrorOutput(SharedUtil.toLog(this.getLogger(), stdErrLevel));
        });
    }

    protected final void args(Object... args) {
        try {
            for (var arg : args) {
                Objects.requireNonNull(this.args).add(this.getProviders().provider(arg::toString));
            }
        } catch (NullPointerException e) {
            throw new IllegalStateException("ToolExecBase#jvmArgs can only be called inside of #addArguments()", e);
        }
    }

    protected final void args(Iterable<?> args) {
        try {
            for (var arg : args) {
                Objects.requireNonNull(this.args).add(this.getProviders().provider(arg::toString));
            }
        } catch (NullPointerException e) {
            throw new IllegalStateException("ToolExecBase#jvmArgs can only be called inside of #addArguments()", e);
        }
    }

    /// Adds each file to the arguments preceded by the given argument. Designed to work well with
    /// <a href="https://jopt-simple.github.io/jopt-simple/">JOpt Simple</a>.
    ///
    /// @param arg   The flag to use for each file
    /// @param files The files to add
    protected final void args(String arg, Iterable<? extends File> files) {
        for (File file : files)
            this.args(arg, file);
    }

    /// Adds the given argument followed by the given file location to the arguments.
    ///
    /// @param arg          The flag to use
    /// @param fileProvider The file to add
    protected final void args(String arg, FileSystemLocationProperty<? extends FileSystemLocation> fileProvider) {
        this.args(arg, fileProvider.getLocationOnly());
    }

    /// Adds the given argument followed by the given object (may be a file location) to the arguments.
    ///
    /// @param arg      The flag to use
    /// @param provider The object (or file) to add
    protected final void args(String arg, @UnknownNullability Provider<?> provider) {
        if (provider == null || !provider.isPresent()) return;

        // NOTE: We don't use File#getAbsoluteFile because path sensitivity should be handled by tasks.
        var value = provider.map(it -> it instanceof FileSystemLocation ? ((FileSystemLocation) it).getAsFile() : it).getOrNull();
        if (value == null) return;

        if (value instanceof Boolean && ((boolean) value))
            this.args(arg);
        else
            this.args(arg, String.valueOf(value));
    }

    /// Adds the given map of arguments.
    ///
    /// [#args(String, Provider)] will be invoked for each entry in the map. If the key and/or value are not of the
    /// required types, they will be automatically converted using [Object#toString()] and
    /// [org.gradle.api.provider.ProviderFactory#provider(Callable)].
    ///
    /// @param args The args to add
    protected final void args(Map<?, ?> args) {
        for (Map.Entry<?, ?> entry : args.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            this.args(
                key instanceof Provider<?> provider ? provider.map(Object::toString).get() : this.getProviders().provider(key::toString).get(),
                value instanceof Provider<?> provider ? (provider instanceof FileSystemLocationProperty<?> file ? file.getLocationOnly() : provider) : this.getProviders().provider(() -> value)
            );
        }
    }

    protected final void jvmArgs(Object... args) {
        try {
            for (var arg : args) {
                Objects.requireNonNull(this.jvmArgs).add(this.getProviders().provider(arg::toString));
            }
        } catch (NullPointerException e) {
            throw new IllegalStateException("ToolExecBase#jvmArgs can only be called inside of #addArguments()", e);
        }
    }

    protected final void jvmArgs(Iterable<?> args) {
        try {
            for (var arg : args) {
                Objects.requireNonNull(this.jvmArgs).add(this.getProviders().provider(arg::toString));
            }
        } catch (NullPointerException e) {
            throw new IllegalStateException("ToolExecBase#jvmArgs can only be called inside of #addArguments()", e);
        }
    }

    protected final void environment(String key, String value) {
        try {
            Objects.requireNonNull(this.environment).put(key, value);
        } catch (NullPointerException e) {
            throw new IllegalStateException("ToolExecBase#environment can only be called inside of #addArguments()", e);
        }
    }

    protected final void systemProperty(String key, String value) {
        try {
            Objects.requireNonNull(this.systemProperties).put(key, value);
        } catch (NullPointerException e) {
            throw new IllegalStateException("ToolExecBase#systemProperty can only be called inside of #addArguments()", e);
        }
    }
}
