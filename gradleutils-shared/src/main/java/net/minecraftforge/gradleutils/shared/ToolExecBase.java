/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.artifacts.dsl.ExternalModuleDependencyVariantSpec;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileSystemLocationProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.LoggingManager;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
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
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

    protected final @Nested ToolExecSpec getSpec() {
        return this.spec;
    }

    //region JavaExec
    public @Internal ConfigurableFileCollection getClasspath() {
        return this.spec.getClasspath();
    }

    public @Internal Property<String> getMainClass() {
        return this.spec.getMainClass();
    }

    public @Internal Property<JavaLauncher> getJavaLauncher() {
        return this.spec.getJavaLauncher();
    }

    public @Internal Property<Boolean> getPreferToolchainJvm() {
        return this.spec.getPreferToolchainJvm();
    }

    public @Internal DirectoryProperty getWorkingDir() {
        return this.spec.getWorkingDir();
    }
    //endregion

    //region Logging
    @Deprecated
    @Override public LoggingManager getLogging() {
        return super.getLogging();
    }

    protected @Console Property<LogLevel> getStandardOutputLogLevel() {
        return this.spec.getStandardOutputLogLevel();
    }

    protected @Console Property<LogLevel> getStandardErrorLogLevel() {
        return this.spec.getStandardErrorLogLevel();
    }

    protected @Internal RegularFileProperty getLogFile() {
        return this.spec.getLogFile();
    }
    //endregion

    protected abstract @Inject ObjectFactory getObjects();

    private final ToolExecSpec spec;

    /// Creates a new task instance using the given types and tool information.
    ///
    /// @param tool The tool to use for this task
    /// @implSpec The implementing subclass <strong>must</strong> make their constructor public, annotated with
    /// [Inject], and have only a single parameter for [Tool], passing in static plugin and problems types to this base
    /// constructor. The types must also be manually specified in the class declaration when overriding this class. The
    /// best practice is to make a single `ToolExec` class for the implementing plugin to use, which other tasks can
    /// extend off of.
    protected ToolExecBase(Tool tool) {
        this.spec = getObjects().newInstance(ToolExecSpec.class, this.plugin(), this.getTool(tool));
    }

    public final void using(CharSequence dependency) {
        this.spec.using(dependency);
    }

    public final void using(Provider<? extends Dependency> dependency) {
        this.spec.using(dependency);
    }

    public final void using(Provider<MinimalExternalModuleDependency> dependency, Action<? super ExternalModuleDependencyVariantSpec> variantSpec) {
        this.spec.using(dependency, variantSpec);
    }

    public final void using(ProviderConvertible<? extends Dependency> dependency) {
        this.spec.using(dependency);
    }

    public final void using(ProviderConvertible<MinimalExternalModuleDependency> dependency, Action<? super ExternalModuleDependencyVariantSpec> variantSpec) {
        this.spec.using(dependency, variantSpec);
    }

    public final void using(Dependency dependency) {
        this.spec.using(dependency);
    }

    @Deprecated
    public final void usingDirectly(CharSequence downloadUrl) {
        this.spec.usingDirectly(downloadUrl);
    }

    /// This method should be overridden by subclasses to add arguments to this task via [JavaExec#args]. To preserve
    /// arguments added by superclasses, this method [must be invoked by overriders][MustBeInvokedByOverriders].
    @MustBeInvokedByOverriders
    protected void addArguments() { }

    /// @implNote Not invoking this method from an overriding method *will* result in the tool never being executed and
    /// [#addArguments()] never being run.
    @TaskAction
    protected ExecResult exec() throws IOException {
        return this.spec.exec();
    }

    protected final void args(Object... args) {
        this.spec.args(args);
    }

    protected final void args(Iterable<?> args) {
        this.spec.args(args);
    }

    /// Adds each file to the arguments preceded by the given argument. Designed to work well with
    /// <a href="https://jopt-simple.github.io/jopt-simple/">JOpt Simple</a>.
    ///
    /// @param arg   The flag to use for each file
    /// @param files The files to add
    protected final void args(String arg, Iterable<? extends File> files) {
        this.spec.args(arg, files);
    }

    /// Adds the given argument followed by the given file location to the arguments.
    ///
    /// @param arg          The flag to use
    /// @param fileProvider The file to add
    protected final void args(String arg, FileSystemLocationProperty<? extends FileSystemLocation> fileProvider) {
        this.spec.args(arg, fileProvider);
    }

    /// Adds the given argument followed by the given object (may be a file location) to the arguments.
    ///
    /// @param arg      The flag to use
    /// @param provider The object (or file) to add
    protected final void args(String arg, @UnknownNullability Provider<?> provider) {
        this.spec.args(arg, provider);
    }

    /// Adds the given map of arguments.
    ///
    /// [#args(String, Provider)] will be invoked for each entry in the map. If the key and/or value are not of the
    /// required types, they will be automatically converted using [Object#toString()] and
    /// [org.gradle.api.provider.ProviderFactory#provider(Callable)].
    ///
    /// @param args The args to add
    /// @deprecated Too ambiguous with [#args(String, Provider)]. Prefer that method instead.
    @Deprecated(forRemoval = true)
    protected final void args(Map<?, ?> args) {
        this.spec.args(args);
    }

    protected final void jvmArgs(Object... args) {
        this.spec.jvmArgs(args);
    }

    protected final void jvmArgs(Iterable<?> args) {
        this.spec.jvmArgs(args);
    }

    protected final void environment(String key, String value) {
        this.environment(key, value);
    }

    protected final void systemProperty(String key, String value) {
        this.spec.systemProperty(key, value);
    }
}
