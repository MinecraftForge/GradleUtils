/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Named;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/// Tools are definitions of Java libraries (may or may not be executable) that are managed by Gradle using a
/// [org.gradle.api.provider.ValueSource]. This means that while the downloading and local caching of this file are done
/// in house, the Gradle-specific caching and file tracking are done by Gradle. This enables the usage of downloading
/// external files quickly without breaking caches.
public interface Tool extends Named {
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

    /// Gets this tool and returns a provider for the downloaded/cached file.
    ///
    /// @param cachesDir The caches directory to store the downloaded tool in
    /// @param providers The provider factory for creating the provider
    /// @return The provider to the tool file
    /// @deprecated Use [EnhancedPlugin#getTool(Tool)]
    Provider<File> get(Provider<? extends Directory> cachesDir, ProviderFactory providers);

    /// Gets this tool and returns a provider for the downloaded/cached file.
    ///
    /// @param cachesDir The caches directory to store the downloaded tool in
    /// @param providers The provider factory for creating the provider
    /// @return The provider to the tool file
    /// @deprecated Use [EnhancedPlugin#getTool(Tool)]
    default Provider<File> get(Directory cachesDir, ProviderFactory providers) {
        return this.get(providers.provider(() -> cachesDir), providers);
    }
}
