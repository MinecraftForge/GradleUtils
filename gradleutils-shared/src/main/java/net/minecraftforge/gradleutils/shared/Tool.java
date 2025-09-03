/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;

/// Tools are definitions of Java libraries (may or may not be executable) that are managed by Gradle using a
/// [org.gradle.api.provider.ValueSource]. This means that while the downloading and local caching of this file are done
/// in house, the Gradle-specific caching and file tracking are done by Gradle. This enables the usage of downloading
/// external files quickly without breaking caches.
public interface Tool extends Named, Serializable {
    /// Creates a new tool with the given information.
    ///
    /// @param name        The name for this tool (will be used in the file name)
    /// @param version     The version for this tool (will be used in the file name)
    /// @param downloadUrl The download URL for this tool
    /// @param javaVersion The Java version this tool was built with, or should run on
    /// @param mainClass   The main class to use when executing this tool
    /// @return The tool
    static Tool of(String name, String version, String downloadUrl, int javaVersion, String mainClass) {
        return new ToolImpl(name, version, downloadUrl, javaVersion, mainClass);
    }

    /// Creates a new tool with the given information.
    ///
    /// @param name        The name for this tool (will be used in the file name)
    /// @param version     The version for this tool (will be used in the file name)
    /// @param downloadUrl The download URL for this tool
    /// @param javaVersion The Java version this tool was built with, or should run on
    /// @return The tool
    static Tool of(String name, String version, String downloadUrl, int javaVersion) {
        return new ToolImpl(name, version, downloadUrl, javaVersion, null);
    }

    /// The name for this tool. Primarily used by [ToolExecBase] to create a default tool directory.
    ///
    /// @return The name of this tool
    @Override
    String getName();

    /// The version of this tool.
    ///
    /// @return The version of this tool
    String getVersion();

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

    @ApiStatus.Experimental
    interface Definition extends Named {
        /// Gets the classpath to use for the tool. If empty, the static default set by the plugin will be used.
        ///
        /// @return The classpath
        /// @apiNote This is *not* the dependency's classpath. This is the classpath used in
        /// [org.gradle.process.JavaExecSpec#setClasspath(FileCollection)] to invoke AccessTransformers.
        ConfigurableFileCollection getClasspath();

        /// Gets the main class to invoke when running AccessTransformers.
        ///
        /// @return The property for the main class.
        /// @apiNote This is *not required* if the [classpath][#getClasspath()] is a single executable jar.
        Property<String> getMainClass();

        /// Gets the Java launcher used to run AccessTransformers.
        ///
        /// This can be easily acquired using [Java toolchains][org.gradle.jvm.toolchain.JavaToolchainService].
        ///
        /// @return The property for the Java launcher
        /// @see org.gradle.jvm.toolchain.JavaToolchainService#launcherFor(Action)
        Property<JavaLauncher> getJavaLauncher();
    }

    interface Resolved extends Tool {
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
