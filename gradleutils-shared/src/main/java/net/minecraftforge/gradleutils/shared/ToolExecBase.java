/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Transformer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileSystemLocationProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.Optional;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.UnknownNullability;

import javax.inject.Inject;
import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

/// This tool execution task is a template on top of [JavaExec] to make executing [tools][Tool] much easier and more
/// consistent between plugins.
///
/// @param <P> The type of enhanced problems, used for common problems reporting with illegal task arguments
/// @implSpec Implementing plugins should make a shared subclass named `ToolExec` which all other tool executing tasks
/// should extend from.
/// @see JavaExec
/// @see Tool
public abstract class ToolExecBase<P extends EnhancedProblems> extends JavaExec {
    private final P problems;
    /// The default tool directory (usage is not required).
    protected final DirectoryProperty defaultToolDir = this.getObjectFactory().directoryProperty();
    private final ListProperty<String> additionalArgs = this.getObjectFactory().listProperty(String.class);

    /// Additional arguments to use when invoking the tool. Use in configuration instead of [#args].
    ///
    /// @return The list property for the additional arguments
    public @Input @Optional ListProperty<String> getAdditionalArgs() {
        return this.additionalArgs;
    }

    /// The project layout provided by Gradle services.
    ///
    /// @return The project layout
    /// @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#projectlayout">ProjectLayout
    /// Service Injection</a>
    protected abstract @Inject ProjectLayout getProjectLayout();

    /// Creates a new task instance using the given types and tool information.
    ///
    /// @param problemsType The type of problems to use for this task (accessible via [#getProblems()])
    /// @param tool         The tool to use for this task
    /// @implSpec The implementing subclass <strong>must</strong> make their constructor public, annotated with
    /// [Inject], and have only a single parameter for [Tool], passing in static plugin and problems types to this base
    /// constructor. The types must also be manually specified in the class declaration when overriding this class. The
    /// best practice is to make a single `ToolExec` class for the implementing plugin to use, which other tasks can
    /// extend off of.
    protected ToolExecBase(Class<P> problemsType, Tool tool) {
        this.problems = this.getObjectFactory().newInstance(problemsType);

        Tool.Resolved resolved;
        if (this instanceof EnhancedTask) {
            resolved = ((EnhancedTask) this).getTool(tool);
            this.defaultToolDir.value(
                ((EnhancedTask) this).globalCaches().dir(tool.getName().toLowerCase(Locale.ENGLISH)).map(this.ensureFileLocationInternal())
            );
        } else {
            this.getProject().afterEvaluate(project -> this.getProblems().reportToolExecNotEnhanced(this));
            resolved = ((ToolInternal) tool).get(
                this.getProjectLayout().getBuildDirectory().dir("minecraftforge/tools/" + tool.getName().toLowerCase(Locale.ENGLISH)).map(this.ensureFileLocationInternal()),
                this.getProviderFactory(),
                this.getObjectFactory().newInstance(ToolsExtensionImpl.class, (Callable<? extends JavaToolchainService>) this::getJavaToolchainService)
            );

            this.defaultToolDir.value(
                this.getProjectLayout().getBuildDirectory().dir(String.format("minecraftforge/tools/%s/workDir", tool.getName().toLowerCase(Locale.ENGLISH))).map(this.ensureFileLocationInternal())
            );
        }

        this.setClasspath(resolved.getClasspath());

        SharedUtil.finalizeProperty(this.defaultToolDir);

        if (resolved.hasMainClass())
            this.getMainClass().set(resolved.getMainClass());
        this.getJavaLauncher().set(resolved.getJavaLauncher());

        this.setStandardOutput(SharedUtil.toLog(this.getLogger()::lifecycle));
        this.setErrorOutput(SharedUtil.toLog(this.getLogger()::error));
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
    protected void addArguments() {
        this.args(this.getAdditionalArgs().get());
    }

    /// @implNote Not invoking this method from an overriding method *will* result in the tool never being executed and
    /// [#addArguments()] never being run.
    @Override
    public void exec() {
        if (!this.getArgs().isEmpty())
            this.getProblems().reportToolExecEagerArgs(this);

        this.addArguments();

        this.getLogger().info("{} {}", this.getClasspath().getAsPath(), String.join(" ", this.getArgs()));

        super.exec();
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
        this.args(arg, fileProvider, false);
    }

    /// Adds the given argument followed by the given file location to the arguments.
    ///
    /// @param arg          The flag to use
    /// @param fileProvider The file to add
    protected final void args(String arg, FileSystemLocationProperty<? extends FileSystemLocation> fileProvider, boolean locationOnly) {
        this.args(arg, locationOnly ? fileProvider.getLocationOnly() : fileProvider);
    }

    /// Adds the given argument followed by the given object (may be a file location) to the arguments.
    ///
    /// @param arg      The flag to use
    /// @param provider The object (or file) to add
    protected final void args(String arg, @UnknownNullability Provider<?> provider) {
        if (provider == null || !provider.isPresent()) return;

        // NOTE: We don't use File#getAbsoluteFile because path sensitivity should be handled by tasks.
        Object value = provider.map(it -> it instanceof FileSystemLocation ? ((FileSystemLocation) it).getAsFile() : it).getOrNull();
        if (value == null) return;

        if (value instanceof Boolean && ((boolean) value))
            this.args(arg);
        else
            this.args(arg, String.valueOf(value));
    }

    protected final void args(Map<?, ?> args) {
        for (Map.Entry<?, ?> entry : args.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            this.args(
                key instanceof Provider ? ((Provider<?>) key).map(Object::toString).get() : this.getProviderFactory().provider(() -> key).map(Object::toString).get(),
                value instanceof Provider ? (Provider<?>) value : this.getProviderFactory().provider(() -> value)
            );
        }
    }

    /// Adds the given argument if and only if the given boolean property is [present][Provider#isPresent()] and true.
    ///
    /// @param arg    The argument to add
    /// @param onlyIf The provider to test
    /// @deprecated Use [#args(String, Provider)].
    @Deprecated
    @ApiStatus.ScheduledForRemoval
    protected final void argOnlyIf(String arg, Provider<Boolean> onlyIf) {
        this.argOnlyIf(arg, task -> onlyIf.isPresent() && onlyIf.getOrElse(false));
    }

    /// Adds the given argument if and only if the given spec, using this task, is satisfied.
    ///
    /// @param arg    The argument to add
    /// @param onlyIf The spec to test
    /// @deprecated Use [#args(String, Provider)].
    @Deprecated
    @ApiStatus.ScheduledForRemoval
    protected final void argOnlyIf(String arg, Spec<? super ToolExecBase<?>> onlyIf) {
        if (onlyIf.isSatisfiedBy(this))
            this.args(arg);
    }
}
