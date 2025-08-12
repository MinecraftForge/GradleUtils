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
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.JavaExec;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import javax.inject.Inject;
import java.io.File;
import java.util.Locale;
import java.util.Objects;

/// This tool execution task is a template on top of [JavaExec] to make executing [tools][Tool] much easier and more
/// consistent between plugins.
///
/// @param <P> The type of enhanced problems, used for common problems reporting with illegal task arguments
/// @see JavaExec
/// @see Tool
public abstract class ToolExecBase<P extends EnhancedProblems> extends JavaExec {
    private transient final P problems;
    /// The default tool directory (usage is not required).
    protected final DirectoryProperty defaultToolDir;

    /**
     * @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#sec:projectlayout">ProjectLayout
     * Service Injection</a>
     */
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

        if (this instanceof EnhancedTask<?> enhancedTask) {
            this.defaultToolDir = this.getObjectFactory().directoryProperty().value(
                enhancedTask.getPlugin().globalCaches().dir(tool.getName().toLowerCase(Locale.ENGLISH)).map(this.ensureFileLocationInternal())
            );
            this.setClasspath(this.getObjectFactory().fileCollection().from(enhancedTask.getPlugin().getTool(tool)));
        } else {
            this.getProject().afterEvaluate(project -> this.problems.reportToolExecNotEnhanced(this));

            this.defaultToolDir = this.getObjectFactory().directoryProperty().value(
                this.getProjectLayout().getBuildDirectory().dir("minecraftforge/tools/%s/workDir".formatted(tool.getName().toLowerCase(Locale.ENGLISH))).map(this.ensureFileLocationInternal())
            );
            this.setClasspath(this.getObjectFactory().fileCollection().from(tool.get(
                this.getProjectLayout().getBuildDirectory().dir("minecraftforge/tools/" + tool.getName().toLowerCase(Locale.ENGLISH)).map(this.ensureFileLocationInternal()),
                this.getProviderFactory()
            )));
        }

        this.defaultToolDir.disallowChanges();
        this.defaultToolDir.finalizeValueOnRead();

        this.getMainClass().convention(Objects.requireNonNull(tool.getMainClass(), "Tool must have a main class"));
        this.getJavaLauncher().convention(SharedUtil.launcherForStrictly(this.getJavaToolchainService(), tool.getJavaVersion()));
    }

    /// The enhanced problems instance to use for this task.
    ///
    /// @return The enhanced problems
    @Internal
    protected final P getProblems() {
        return this.problems;
    }

    private <T extends FileSystemLocation> Transformer<T, T> ensureFileLocationInternal() {
        return t -> this.problems.<T>ensureFileLocation().transform(t);
    }

    /// This method should be overridden by subclasses to add arguments to this task via [JavaExec#args]. To preserve
    /// arguments added by superclasses, this method [must be invoked by overriders][MustBeInvokedByOverriders].
    @MustBeInvokedByOverriders
    protected void addArguments() { }

    @Override
    public void exec() {
        if (this.getArgs().isEmpty())
            this.addArguments();
        else
            this.problems.reportToolExecEagerArgs(this);

        this.getLogger().info("{} {}", this.getClasspath().getAsPath(), String.join(" ", this.getArgs()));

        super.exec();
    }

    /// Adds each file to the arguments preceded by the given argument. Designed to work well with
    /// <a href="https://jopt-simple.github.io/jopt-simple/">JOpt Simple</a>.
    ///
    /// @param arg   The flag to use for each file
    /// @param files The files to add
    protected final void args(String arg, Iterable<? extends File> files) {
        for (var file : files)
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
    protected final void args(String arg, Provider<?> provider) {
        var value = provider.map(it -> it instanceof FileSystemLocation f ? f.getAsFile() : it).get();

        this.args(arg, String.valueOf(value));
    }

    /// Adds the given argument if and only if the given boolean property is [present][Provider#isPresent()] and true.
    ///
    /// @param arg    The argument to add
    /// @param onlyIf The provider to test
    protected final void argOnlyIf(String arg, Provider<Boolean> onlyIf) {
        this.argOnlyIf(arg, task -> onlyIf.isPresent() && onlyIf.getOrElse(false));
    }

    /// Adds the given argument if and only if the given spec, using this task, is satisfied.
    ///
    /// @param arg    The argument to add
    /// @param onlyIf The spec to test
    protected final void argOnlyIf(String arg, Spec<? super ToolExecBase<?>> onlyIf) {
        if (onlyIf.isSatisfiedBy(this))
            this.args(arg);
    }
}
