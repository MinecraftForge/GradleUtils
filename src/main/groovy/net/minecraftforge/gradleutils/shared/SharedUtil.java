/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FirstParam;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.OutputStream;
import java.util.Objects;

/// Shared utilities for Gradle plugins.
public abstract class SharedUtil {
    //region Java Launcher

    /// Gets the Java launcher that [can compile or run][JavaLanguageVersion#canCompileOrRun(JavaLanguageVersion)] the
    /// given version.
    ///
    /// If the currently running Java toolchain is able to compile and run the given version, it will be used instead.
    ///
    /// @param java           The Java plugin extension of the currently-used toolchain
    /// @param javaToolchains The Java toolchain service to get the Java launcher from
    /// @param version        The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherFor(JavaPluginExtension java, JavaToolchainService javaToolchains, int version) {
        return launcherFor(java, javaToolchains, JavaLanguageVersion.of(version));
    }

    /// Gets the Java launcher that [can compile or run][JavaLanguageVersion#canCompileOrRun(JavaLanguageVersion)] the
    /// given version.
    ///
    /// If the currently running Java toolchain is able to compile and run the given version, it will be used instead.
    ///
    /// @param java           The Java plugin extension of the currently-used toolchain
    /// @param javaToolchains The Java toolchain service to get the Java launcher from
    /// @param version        The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherFor(JavaPluginExtension java, JavaToolchainService javaToolchains, JavaLanguageVersion version) {
        JavaToolchainSpec currentToolchain = java.getToolchain();
        return currentToolchain.getLanguageVersion().orElse(JavaLanguageVersion.current()).flatMap(languageVersion -> languageVersion.canCompileOrRun(version)
            ? javaToolchains.launcherFor(spec -> spec.getLanguageVersion().set(languageVersion))
            : launcherForStrictly(javaToolchains, version));
    }

    /// Gets the Java launcher that [can compile or run][JavaLanguageVersion#canCompileOrRun(JavaLanguageVersion)] the
    /// given version.
    ///
    /// If the currently running Java toolchain is able to compile and run the given version, it will be used instead.
    ///
    /// @param java           The Java plugin extension of the currently-used toolchain
    /// @param javaToolchains The Java toolchain service to get the Java launcher from
    /// @param version        The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherFor(Provider<? extends JavaPluginExtension> java, Provider<? extends JavaToolchainService> javaToolchains, int version) {
        return launcherFor(java, javaToolchains, JavaLanguageVersion.of(version));
    }

    /// Gets the Java launcher that [can compile or run][JavaLanguageVersion#canCompileOrRun(JavaLanguageVersion)] the
    /// given version.
    ///
    /// If the currently running Java toolchain is able to compile and run the given version, it will be used instead.
    ///
    /// @param java           The Java plugin extension of the currently-used toolchain
    /// @param javaToolchains The Java toolchain service to get the Java launcher from
    /// @param version        The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherFor(Provider<? extends JavaPluginExtension> java, Provider<? extends JavaToolchainService> javaToolchains, JavaLanguageVersion version) {
        return java.flatMap(j -> {
            var currentToolchain = j.getToolchain();
            return currentToolchain.getLanguageVersion().orElse(JavaLanguageVersion.current()).flatMap(languageVersion -> languageVersion.canCompileOrRun(version)
                ? javaToolchains.flatMap(t -> t.launcherFor(spec -> spec.getLanguageVersion().set(languageVersion)))
                : launcherForStrictly(javaToolchains, version));
        });
    }

    /// Gets the Java launcher that [can compile or run][JavaLanguageVersion#canCompileOrRun(JavaLanguageVersion)] the
    /// given version.
    ///
    /// If the currently running Java toolchain is able to compile and run the given version, it will be used instead.
    ///
    /// @param project The project to get the Java extensions from
    /// @param version The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherFor(Project project, int version) {
        var providers = project.getProviders();
        var extensions = project.getExtensions();
        return launcherFor(providers.provider(() -> extensions.getByType(JavaPluginExtension.class)), providers.provider(() -> extensions.getByType(JavaToolchainService.class)), version);
    }

    /// Gets the Java launcher that [can compile or run][JavaLanguageVersion#canCompileOrRun(JavaLanguageVersion)] the
    /// given version.
    ///
    /// If the currently running Java toolchain is able to compile and run the given version, it will be used instead.
    ///
    /// @param project The project to get the Java extensions from
    /// @param version The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherFor(Project project, JavaLanguageVersion version) {
        var providers = project.getProviders();
        var extensions = project.getExtensions();
        return launcherFor(providers.provider(() -> extensions.getByType(JavaPluginExtension.class)), providers.provider(() -> extensions.getByType(JavaToolchainService.class)), version);
    }

    /// Gets the Java launcher strictly for the given version, even if the currently running Java toolchain is higher
    /// than it.
    ///
    /// @param javaToolchains The Java toolchain service to get the Java launcher from
    /// @param version        The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherForStrictly(JavaToolchainService javaToolchains, int version) {
        return launcherForStrictly(javaToolchains, JavaLanguageVersion.of(version));
    }

    /// Gets the Java launcher strictly for the given version, even if the currently running Java toolchain is higher
    /// than it.
    ///
    /// @param javaToolchains The Java toolchain service to get the Java launcher from
    /// @param version        The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherForStrictly(JavaToolchainService javaToolchains, JavaLanguageVersion version) {
        return javaToolchains.launcherFor(spec -> spec.getLanguageVersion().set(version));
    }

    /// Gets the Java launcher strictly for the given version, even if the currently running Java toolchain is higher
    /// than it.
    ///
    /// @param javaToolchains The Java toolchain service to get the Java launcher from
    /// @param version        The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherForStrictly(Provider<? extends JavaToolchainService> javaToolchains, int version) {
        return launcherForStrictly(javaToolchains, JavaLanguageVersion.of(version));
    }

    /// Gets the Java launcher strictly for the given version, even if the currently running Java toolchain is higher
    /// than it.
    ///
    /// @param javaToolchains The Java toolchain service to get the Java launcher from
    /// @param version        The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherForStrictly(Provider<? extends JavaToolchainService> javaToolchains, JavaLanguageVersion version) {
        return javaToolchains.flatMap(t -> t.launcherFor(spec -> spec.getLanguageVersion().set(version)));
    }

    /// Gets the Java launcher strictly for the given version, even if the currently running Java toolchain is higher
    /// than it.
    ///
    /// @param project The extension-aware object to get the Java extensions from
    /// @param version The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherForStrictly(Project project, int version) {
        var providers = project.getProviders();
        var extensions = project.getExtensions();
        return launcherForStrictly(providers.provider(() -> extensions.getByType(JavaToolchainService.class)), version);
    }

    /// Gets the Java launcher strictly for the given version, even if the currently running Java toolchain is higher
    /// than it.
    ///
    /// @param project The extension-aware object to get the Java extensions from
    /// @param version The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherForStrictly(Project project, JavaLanguageVersion version) {
        var providers = project.getProviders();
        var extensions = project.getExtensions();
        return launcherForStrictly(providers.provider(() -> extensions.getByType(JavaToolchainService.class)), version);
    }
    //endregion

    //region Project Eval

    /// Runs the given closure using [Project#afterEvaluate(Action)]. If the project is already executed, the closure
    /// will be called instantly.
    ///
    /// @param project The project to run the closure on
    /// @param closure The closure to execute
    public static void ensureAfterEvaluate(
        Project project,
        @DelegatesTo(value = Project.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FirstParam.class)
        Closure<?> closure
    ) {
        ensureAfterEvaluate(project, Closures.toAction(closure));
    }

    /// Runs the given action using [Project#afterEvaluate(Action)]. If the project is already executed, the action will
    /// be executed instantly.
    ///
    /// @param project The project to run the action on
    /// @param action  The action to execute
    public static void ensureAfterEvaluate(Project project, Action<? super Project> action) {
        if (project.getState().getExecuted())
            action.execute(project);
        else
            project.afterEvaluate(action);
    }
    //endregion

    //region Action Logging

    /// Creates an output stream that logs to the given action.
    ///
    /// @param logger The logger to log to
    /// @return The output stream
    public static OutputStream toLog(Action<? super String> logger) {
        return new OutputStream() {
            private StringBuffer buffer = new StringBuffer(512);

            @Override
            public void write(int b) {
                if (b == '\r' || b == '\n') {
                    if (!this.buffer.isEmpty()) {
                        logger.execute(this.buffer.toString());
                        this.buffer = new StringBuffer(512);
                    }
                } else {
                    this.buffer.append(b);
                }
            }
        };
    }
    //endregion

    //region toString()

    /// Converts a given module to string. Use this instead of [Object#toString()].
    ///
    /// @param module The module
    /// @return The string representation
    public static String toString(ModuleVersionSelector module) {
        var version = module.getVersion();
        return "%s:%s%s".formatted(
            module.getGroup(),
            module.getName(),
            version != null ? ':' + version : ""
        );
    }

    /// Converts a given dependency to string. Use this instead of [Object#toString()].
    ///
    /// @param dependency The dependency
    /// @return The string representation
    public static String toString(Dependency dependency) {
        var group = dependency.getGroup();
        var version = dependency.getVersion();
        var reason = dependency.getReason();
        return "(%s) %s%s%s%s%s".formatted(
            dependency.getClass().getName(),
            group != null ? group + ':' : "",
            dependency.getName(),
            version != null ? ':' + version : "",
            reason != null ? " (" + reason + ')' : "",
            dependency instanceof FileCollectionDependency files ? " [%s]".formatted(String.join(", ", files.getFiles().getFiles().stream().map(File::getAbsolutePath).map(CharSequence.class::cast)::iterator)) : ""
        );
    }
    //endregion

    //region Properties

    public static <P extends Property<?>> Closure<P> finalizeProperty() {
        var ret = Closures.<P>unaryOperator(SharedUtil::finalizeProperty);
        ret.setResolveStrategy(Closure.DELEGATE_FIRST);
        return ret;
    }

    public static <P extends Property<?>> P finalizeProperty(P property) {
        property.disallowChanges();
        property.finalizeValueOnRead();
        return property;
    }
    //endregion

    /// Empty constructor. This class should only be extended to make referencing these static methods easier.
    protected SharedUtil() { }
}
