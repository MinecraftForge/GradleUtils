/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FirstParam;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.function.Consumer;

/// Shared utilities for Gradle plugins.
///
/// @implNote Consumers should make their own `Util` class and extend this one with it to inherit all static methods.
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
            JavaToolchainSpec currentToolchain = j.getToolchain();
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
        ProviderFactory providers = project.getProviders();
        ExtensionContainer extensions = project.getExtensions();
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
        ProviderFactory providers = project.getProviders();
        ExtensionContainer extensions = project.getExtensions();
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
        ProviderFactory providers = project.getProviders();
        ExtensionContainer extensions = project.getExtensions();
        return launcherForStrictly(providers.provider(() -> extensions.getByType(JavaToolchainService.class)), version);
    }

    /// Gets the Java launcher strictly for the given version, even if the currently running Java toolchain is higher
    /// than it.
    ///
    /// @param project The extension-aware object to get the Java extensions from
    /// @param version The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherForStrictly(Project project, JavaLanguageVersion version) {
        ProviderFactory providers = project.getProviders();
        ExtensionContainer extensions = project.getExtensions();
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
    public static PipedOutputStream toLog(Consumer<? super String> logger) {
        final PipedOutputStream output;
        final PipedInputStream input;
        try {
            output = new PipedOutputStream();
            input = new PipedInputStream(output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                reader.lines().forEach(logger);
            } catch (IOException ignored) { }
        });

        thread.setDaemon(true);
        thread.start();

        return output;
    }
    //endregion

    //region toString()

    /// Converts a given module to string. Use this instead of [Object#toString()].
    ///
    /// @param module The module
    /// @return The string representation
    public static String toString(ModuleVersionSelector module) {
        String version = module.getVersion();
        return String.format("%s:%s%s",
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
        String group = dependency.getGroup();
        String version = dependency.getVersion();
        String reason = dependency.getReason();
        return String.format("(%s) %s%s%s%s%s",
            dependency.getClass().getName(),
            group != null ? group + ':' : "",
            dependency.getName(),
            version != null ? ':' + version : "",
            reason != null ? " (" + reason + ')' : "",
            dependency instanceof FileCollectionDependency ? String.format(" [%s]", String.join(", ", ((FileCollectionDependency) dependency).getFiles().getFiles().stream().map(File::getAbsolutePath).map(CharSequence.class::cast)::iterator)) : ""
        );
    }
    //endregion

    //region Properties

    /// Makes a returning-self closure that finalizes a given property using [#finalizeProperty(Property)].
    ///
    /// This is best used as the method argument for [org.codehaus.groovy.runtime.DefaultGroovyMethods#tap(Object,
    /// Closure)], which allows for in-lining property creation in Groovy code.
    ///
    /// @param <P> The type of property to finalize
    /// @return The returning-self closure for finalizing a property
    public static <P extends Property<?>> Closure<P> finalizeProperty() {
        Closure<P> ret = Closures.unaryOperator(SharedUtil::finalizeProperty);
        ret.setResolveStrategy(Closure.DELEGATE_FIRST);
        return ret;
    }

    /// Finalizes the given property to prevent any additional changes from being made to it.
    ///
    /// This is done by simply calling [Property#disallowChanges()] and [Property#finalizeValueOnRead()]. These methods
    /// do not return the object itself, so this helper method exists to in-line property creation without needing to
    /// reference it again just to call these methods.
    ///
    /// @param <P>      The type of property to finalize
    /// @param property The property to finalize
    /// @return The property
    @Contract(value = "_ -> param1", mutates = "param1")
    public static <P extends Property<?>> P finalizeProperty(P property) {
        property.disallowChanges();
        property.finalizeValueOnRead();
        return property;
    }
    //endregion

    /// Empty constructor. This class should only be extended to make referencing these static methods easier.
    protected SharedUtil() { }
}
