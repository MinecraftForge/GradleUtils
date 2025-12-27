/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.concurrent.Callable;

/// Tools are definitions of Java libraries (may or may not be executable) that are managed by Gradle using a
/// [org.gradle.api.provider.ValueSource]. This means that while the downloading and local caching of this file are done
/// in house, the Gradle-specific caching and file tracking are done by Gradle. This enables the usage of downloading
/// external files quickly without breaking caches.
public sealed interface Tool extends Named, Serializable permits ToolInternal, Tool.Resolved {
    /// Creates a new tool with the given information.
    ///
    /// @param name        The name for this tool, used to reference it in configuration and for the file name
    /// @param artifact    The artifact for this tool, used to get the download URL
    /// @param mavenUrl    The maven URL this tool is hosted on (if protocol is omitted, prepends `https://`, and
    ///                    appends adds trailing slash if missing)
    /// @param javaVersion The Java version this tool was built with, or should run on
    /// @param mainClass   The main class to use when executing this tool (optional)
    /// @return The tool
    static Tool of(String name, String artifact, String mavenUrl, int javaVersion, @Nullable String mainClass) {
        return new ToolImpl(name, artifact, mavenUrl, javaVersion, mainClass);
    }

    /// Creates a new tool with the given information.
    ///
    /// @param name        The name for this tool, used to reference it in configuration and for the file name
    /// @param artifact    The artifact for this tool, used to get the download URL
    /// @param mavenUrl    The maven URL this tool is hosted on (if protocol is omitted, prepends `https://`, and
    ///                    appends adds trailing slash if missing)
    /// @param javaVersion The Java version this tool was built with, or should run on
    /// @return The tool
    static Tool of(String name, String artifact, String mavenUrl, int javaVersion) {
        return new ToolImpl(name, artifact, mavenUrl, javaVersion, null);
    }

    /// Creates a new tool with the given information.
    ///
    /// @param name        The name for this tool, used to reference it in configuration and for the file name
    /// @param artifact    The artifact for this tool, used to get the download URL
    /// @param javaVersion The Java version this tool was built with, or should run on
    /// @param mainClass   The main class to use when executing this tool (optional)
    /// @return The tool
    static Tool ofForge(String name, String artifact, int javaVersion, String mainClass) {
        return new ToolImpl(name, artifact, "https://maven.minecraftforge.net/", javaVersion, mainClass);
    }

    /// Creates a new tool with the given information.
    ///
    /// @param name        The name for this tool, used to reference it in configuration and for the file name
    /// @param artifact    The artifact for this tool, used to get the download URL
    /// @param javaVersion The Java version this tool was built with, or should run on
    /// @return The tool
    static Tool ofForge(String name, String artifact, int javaVersion) {
        return new ToolImpl(name, artifact, "https://maven.minecraftforge.net/", javaVersion, null);
    }

    /// The module for this tool.
    ///
    /// @return The module for this tool
    ModuleVersionIdentifier getModule();

    /// The name for this tool. Primarily used by [ToolExecBase] to create a default tool directory.
    ///
    /// @return The name of this tool
    @Override
    String getName();

    /// The Java version this tool was built with. Primarily used by [ToolExecBase] to determine the
    /// [org.gradle.jvm.toolchain.JavaLauncher].
    ///
    /// @return The Java version
    int getJavaVersion();

    /// The main class to use when executing this tool. Can be `null`, but does not necessarily mean that the tool is
    /// not executable.
    ///
    /// @return The main class, or `null` if unspecified
    @Nullable String getMainClass();

    /// If this tool has a strictly defined main class. Can be `false`, but does not necessarily mean that this tools is
    /// not executable.
    ///
    /// @return If this tool has a main class
    default boolean hasMainClass() {
        return this.getMainClass() != null;
    }

    /// A definition of how a tool should be resolved and used by the plugin.
    ///
    /// @see #getClasspath()
    @ApiStatus.Experimental
    sealed interface Definition extends Named permits ToolInternal.Definition {
        /// Gets the version to use for the tool. If empty, the static default set by the plugin will be used.
        ///
        /// @return The version to use instead of the one configured by the plugin.
        /// @apiNote This will *not* be used if [#getClasspath()] or [#getArtifact()] has a value set.
        Property<String> getVersion();

        /// Gets the artifact, in "group:name:version[:classifier][@extension]" format to use for this tool.
        /// If empty, the static default set by the plugin will be used.
        ///
        /// @return The full artifact to use for this tool.
        /// @apiNote This will *not* be used if [#getClasspath()] has a value set.
        Property<String> getArtifact();

        /// Gets the classpath to use for the tool. If empty, the static default set by the plugin will be used.
        ///
        /// @return The classpath
        /// @apiNote This is *not* the dependency's classpath. This is the classpath used in
        /// [org.gradle.process.JavaExecSpec#setClasspath(FileCollection)] to invoke this tool.
        ConfigurableFileCollection getClasspath();

        /// Gets the main class to invoke when running this tool.
        ///
        /// @return The property for the main class.
        /// @apiNote This is *not required* if the [classpath][#getClasspath()] is a single executable jar.
        Property<String> getMainClass();

        /// Gets the Java launcher used to run this tool.
        ///
        /// This can be easily acquired using [Java toolchains][org.gradle.jvm.toolchain.JavaToolchainService].
        ///
        /// @return The property for the Java launcher
        /// @see org.gradle.jvm.toolchain.JavaToolchainService#launcherFor(Action)
        Property<JavaLauncher> getJavaLauncher();
    }

    /// A resolved tool that has a [classpath][#getClasspath()] that can be readily used.
    ///
    /// This interface extends {@link Callable}`<`{@link FileCollection}`>` so that it can be used directly in methods
    /// such as [org.gradle.api.Project#files(Object...)] and [ConfigurableFileCollection#from(Object...)].
    sealed interface Resolved extends Tool, Callable<FileCollection> permits ToolInternal.Resolved {
        @Override
        default FileCollection call() {
            return this.getClasspath();
        }

        /// Gets the classpath containing the tool to be used.
        ///
        /// @return The classpath of the resolved tool
        FileCollection getClasspath();

        /// Gets the Java launcher used to run AccessTransformers.
        ///
        /// This can be easily acquired using [Java toolchains][org.gradle.jvm.toolchain.JavaToolchainService].
        ///
        /// @return The property for the Java launcher
        /// @see org.gradle.jvm.toolchain.JavaToolchainService#launcherFor(Action)
        Property<JavaLauncher> getJavaLauncher();
    }
}
