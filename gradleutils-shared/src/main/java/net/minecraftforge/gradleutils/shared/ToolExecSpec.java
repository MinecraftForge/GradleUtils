/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.Project;
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
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
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
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.process.JavaExecSpec;
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
import java.util.Map;
import java.util.concurrent.Callable;

public abstract non-sealed class ToolExecSpec implements EnhancedTaskAdditions {
    private static final Logger LOGGER = Logging.getLogger(ToolExecSpec.class);

    //region JavaExec
    public abstract @InputFiles @Classpath ConfigurableFileCollection getClasspath();

    public abstract @Input @Optional Property<String> getMainClass();

    public abstract @Nested Property<JavaLauncher> getJavaLauncher();

    protected abstract @Nested Property<JavaLauncher> getToolchainLauncher();

    public abstract @Input @Optional Property<Boolean> getPreferToolchainJvm();

    public abstract @Internal DirectoryProperty getWorkingDir();

    protected abstract @Internal MapProperty<String, String> getForkProperties();
    //endregion

    //region JavaExec Arguments
    protected abstract @Input @Optional ListProperty<String> getArgs();

    protected abstract @Input @Optional ListProperty<String> getJvmArgs();

    protected abstract @Input @Optional MapProperty<String, String> getEnvironment();

    protected abstract @Input @Optional MapProperty<String, String> getSystemProperties();
    //endregion

    //region Logging
    protected abstract @Console Property<LogLevel> getStandardOutputLogLevel();

    protected abstract @Console Property<LogLevel> getStandardErrorLogLevel();

    protected abstract @Internal RegularFileProperty getLogFile();
    //endregion

    @Override
    public EnhancedPlugin<?> plugin() {
        return this.plugin;
    }

    protected abstract @Inject Project getProject();

    protected abstract @Inject ObjectFactory getObjects();

    protected abstract @Inject ProviderFactory getProviders();

    protected abstract @Inject ExecOperations getExecOperations();

    protected abstract @Inject DependencyFactory getDependencyFactory();

    protected abstract @Inject JavaToolchainService getJavaToolchains();

    private final String name;
    private final transient EnhancedPlugin<?> plugin;
    private final String toolName;

    private Closure<Void> javaexecAction = Closures.<JavaExecSpec>consumer(it -> { });

    private transient @Nullable List<Provider<String>> overflowArgs;
    private transient @Nullable List<Provider<String>> overflowJvmArgs;
    private transient @Nullable Map<String, String> overflowEnvironment;
    private transient @Nullable Map<String, String> overflowSystemProperties;

    private void addArgs(Provider<String> provider) {
        if (this.overflowArgs != null) {
            this.overflowArgs.add(provider);
            return;
        }

        try {
            this.getArgs().add(provider);
        } catch (IllegalStateException e) {
            this.overflowArgs = new ArrayList<>();
            this.overflowArgs.add(provider);
        }
    }

    private void addJvmArgs(Provider<String> provider) {
        if (this.overflowJvmArgs != null) {
            this.overflowJvmArgs.add(provider);
            return;
        }

        try {
            this.getJvmArgs().add(provider);
        } catch (IllegalStateException e) {
            this.overflowJvmArgs = new ArrayList<>();
            this.overflowJvmArgs.add(provider);
        }
    }

    private void putEnvironment(String key, String value) {
        if (this.overflowEnvironment != null) {
            this.overflowEnvironment.put(key, value);
            return;
        }

        try {
            this.getEnvironment().put(key, value);
        } catch (IllegalStateException e) {
            this.overflowEnvironment = new HashMap<>();
            this.overflowEnvironment.put(key, value);
        }
    }

    private void putSystemProperties(String key, String value) {
        if (this.overflowSystemProperties != null) {
            this.overflowSystemProperties.put(key, value);
            return;
        }

        try {
            this.getSystemProperties().put(key, value);
        } catch (IllegalStateException e) {
            this.overflowSystemProperties = new HashMap<>();
            this.overflowSystemProperties.put(key, value);
        }
    }

    @Inject
    public ToolExecSpec(String name, EnhancedPlugin<?> plugin, Tool.Resolved resolved) {
        this.name = name;
        this.plugin = plugin;
        this.toolName = resolved.getName();
        this.getClasspath().convention(resolved.getClasspath());

        if (resolved.hasMainClass())
            this.getMainClass().set(resolved.getMainClass());
        this.getJavaLauncher().set(resolved.getJavaLauncher());

        try {
            this.getToolchainLauncher().convention(getJavaToolchains().launcherFor(spec -> spec.getLanguageVersion().set(JavaLanguageVersion.current())));
            getProject().getPluginManager().withPlugin("java", javaAppliedPlugin ->
                this.getToolchainLauncher().set(getJavaToolchains().launcherFor(getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain()))
            );
        } catch (Exception ignored) {
            // If these fail, we're likely in a Settings environment
            // This is fine, we can just try using the Daemon JVM
        }

        this.getForkProperties().set(SharedUtil.getForkProperties(getProviders()));

        this.getStandardOutputLogLevel().convention(LogLevel.LIFECYCLE);
        this.getStandardErrorLogLevel().convention(LogLevel.ERROR);

        this.getWorkingDir().convention(this.getDefaultOutputDirectory());
        this.getLogFile().convention(this.getDefaultLogFile());
    }

    @Override
    public String baseName() {
        return this.name;
    }

    public final void using(CharSequence dependency) {
        this.using(getDependencyFactory().create(dependency));
    }

    public final void using(Provider<? extends Dependency> dependency) {
        this.getClasspath().setFrom(
            getProject().getConfigurations().detachedConfiguration().withDependencies(d -> d.addLater(dependency))
        );
    }

    public final void using(Provider<MinimalExternalModuleDependency> dependency, Action<? super ExternalModuleDependencyVariantSpec> variantSpec) {
        this.using(getProject().getDependencies().variantOf(dependency, variantSpec));
    }

    public final void using(ProviderConvertible<? extends Dependency> dependency) {
        this.using(dependency.asProvider());
    }

    public final void using(ProviderConvertible<MinimalExternalModuleDependency> dependency, Action<? super ExternalModuleDependencyVariantSpec> variantSpec) {
        this.using(getProject().getDependencies().variantOf(dependency, variantSpec));
    }

    public final void using(Dependency dependency) {
        this.getClasspath().setFrom(
            getProject().getConfigurations().detachedConfiguration(dependency)
        );
    }

    @Deprecated
    public final void usingDirectly(CharSequence downloadUrl) {
        var url = getProviders().provider(downloadUrl::toString);
        this.getClasspath().setFrom(getProviders().of(ToolImpl.Source.class, spec -> spec.parameters(parameters -> {
            parameters.getInputFile().set(getProviders().zip(localCaches(), url, (d, s) -> d.file("tools/" + toolName + '/' + s.substring(s.lastIndexOf('/')))));
            parameters.getDownloadUrl().set(url);
        })));
    }

    private <T extends FileSystemLocation> Transformer<T, T> ensureFileLocationInternal() {
        return t -> this.plugin.getProblemsInternal().<T>ensureFileLocation().transform(t);
    }

    public void configure(Action<? super JavaExecSpec> action) {
        this.javaexecAction = this.javaexecAction.compose(Closures.<JavaExecSpec>unaryOperator(spec -> {
            action.execute(spec);
            return spec;
        }));
    }

    public ExecResult exec() {
        return this.exec(getExecOperations());
    }

    public ExecResult exec(ExecOperations execOperations) {
        var args = this.getArgs().getOrElse(List.of());
        if (this.overflowArgs != null)
            args.addAll(DefaultGroovyMethods.collect(this.overflowArgs, Closures.<Provider<String>, String>function(Provider::get)));

        var jvmArgs = this.getJvmArgs().getOrElse(List.of());
        if (this.overflowJvmArgs != null)
            jvmArgs.addAll(DefaultGroovyMethods.collect(this.overflowJvmArgs, Closures.<Provider<String>, String>function(Provider::get)));

        var environment = new HashMap<>(this.getEnvironment().getOrElse(Map.of()));
        if (this.overflowEnvironment != null)
            environment.putAll(this.overflowEnvironment);

        var systemProperties = new HashMap<>(this.getSystemProperties().getOrElse(Map.of()));
        if (this.overflowSystemProperties != null)
            systemProperties.putAll(this.overflowSystemProperties);
        for (var property : this.getForkProperties().get().entrySet())
            systemProperties.putIfAbsent(property.getKey(), property.getValue());

        var stdOutLevel = this.getStandardOutputLogLevel().get();
        var stdErrLevel = this.getStandardErrorLogLevel().get();

        JavaLauncher javaLauncher;
        if (getPreferToolchainJvm().getOrElse(false)) {
            var candidateLauncher = getJavaLauncher().get();
            var toolchainLauncher = getToolchainLauncher().get();
            javaLauncher = toolchainLauncher.getMetadata().getLanguageVersion().canCompileOrRun(candidateLauncher.getMetadata().getLanguageVersion())
                ? toolchainLauncher
                : candidateLauncher;
        } else {
            javaLauncher = getJavaLauncher().get();
        }

        var workingDirectory = this.getWorkingDir().map(ensureFileLocationInternal()).get().getAsFile();

        try (var log = new PrintWriter(new FileWriter(this.getLogFile().getAsFile().get()), true)) {
            return execOperations.javaexec(spec -> {
                spec.setIgnoreExitValue(true);

                spec.setWorkingDir(workingDirectory);
                spec.setClasspath(this.getClasspath());
                if (this.getMainClass().isPresent())
                    spec.getMainClass().set(this.getMainClass());
                spec.setExecutable(javaLauncher.getExecutablePath().getAsFile().getAbsolutePath());
                spec.setArgs(args);
                spec.setJvmArgs(jvmArgs);
                spec.setEnvironment(environment);
                spec.setSystemProperties(systemProperties);

                spec.setStandardOutput(SharedUtil.toLog(
                    line -> {
                        LOGGER.log(stdOutLevel, line);
                        log.println(line);
                    }
                ));
                spec.setErrorOutput(SharedUtil.toLog(
                    line -> {
                        LOGGER.log(stdErrLevel, line);
                        log.println(line);
                    }
                ));

                javaexecAction.call(spec);

                log.print("Java Launcher: ");
                log.println(spec.getExecutable());
                log.print("Working directory: ");
                log.println(spec.getWorkingDir().getAbsolutePath());
                log.print("Main class: ");
                log.println(spec.getMainClass().getOrElse("AUTOMATIC"));
                log.println("Arguments:");
                for (var s : spec.getArgs()) {
                    log.print("  ");
                    log.println(s);
                }
                log.println("JVM Arguments:");
                for (var s : spec.getAllJvmArgs()) {
                    log.print("  ");
                    log.println(s);
                }
                log.println("Classpath:");
                for (var f : getClasspath()) {
                    log.print("  ");
                    log.println(f.getAbsolutePath());
                }
                log.println("====================================");
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to open log file", e);
        }
    }

    public void args(Object... args) {
        this.args(Arrays.asList(args));
    }

    public void args(Iterable<?> args) {
        for (var arg : args) {
            if (arg instanceof ProviderConvertible<?> providerConvertible)
                this.addArgs(providerConvertible.asProvider().map(Object::toString));
            if (arg instanceof Provider<?> provider)
                this.addArgs(provider.map(Object::toString));
            else
                this.addArgs(this.getProviders().provider(arg::toString));
        }
    }

    /// Adds each file to the arguments preceded by the given argument. Designed to work well with
    /// <a href="https://jopt-simple.github.io/jopt-simple/">JOpt Simple</a>.
    ///
    /// @param arg   The flag to use for each file
    /// @param files The files to add
    public void args(String arg, Iterable<? extends File> files) {
        for (File file : files)
            this.args(arg, file);
    }

    /// Adds the given argument followed by the given file location to the arguments.
    ///
    /// @param arg          The flag to use
    /// @param fileProvider The file to add
    public void args(String arg, FileSystemLocationProperty<? extends FileSystemLocation> fileProvider) {
        this.args(arg, fileProvider.getLocationOnly());
    }

    /// Adds the given argument followed by the given object (may be a file location) to the arguments.
    ///
    /// @param arg      The flag to use
    /// @param provider The object (or file) to add
    public void args(String arg, @UnknownNullability Provider<?> provider) {
        if (provider == null || !provider.isPresent()) return;

        // NOTE: We don't use File#getAbsoluteFile because path sensitivity should be handled by tasks.
        var value = provider.map(it -> it instanceof FileSystemLocation ? ((FileSystemLocation) it).getAsFile() : it).getOrNull();
        if (value == null) return;

        if (value instanceof Boolean booleanValue) {
            if (booleanValue)
                this.args(arg);
        } else {
            this.args(arg, String.valueOf(value));
        }
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
    public void args(Map<?, ?> args) {
        for (Map.Entry<?, ?> entry : args.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            this.args(
                key instanceof Provider<?> provider ? provider.map(Object::toString).get() : this.getProviders().provider(key::toString).get(),
                value instanceof Provider<?> provider ? (provider instanceof FileSystemLocationProperty<?> file ? file.getLocationOnly() : provider) : this.getProviders().provider(() -> value)
            );
        }
    }

    public void jvmArgs(Object... args) {
        for (var arg : args) {
            this.addJvmArgs(this.getProviders().provider(arg::toString));
        }
    }

    public void jvmArgs(Iterable<?> args) {
        for (var arg : args) {
            this.addJvmArgs(this.getProviders().provider(arg::toString));
        }
    }

    public void environment(String key, String value) {
        this.putEnvironment(key, value);
    }

    public void systemProperty(String key, String value) {
        this.putSystemProperties(key, value);
    }
}
