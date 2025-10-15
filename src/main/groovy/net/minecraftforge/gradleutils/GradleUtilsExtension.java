/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import groovy.lang.Closure;
import kotlin.jvm.functions.Function0;
import net.minecraftforge.gradleutils.shared.Closures;
import org.gradle.api.Action;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/// Contains various utilities for working with Gradle scripts.
///
/// @see GradleUtilsExtensionForProject
public sealed interface GradleUtilsExtension permits GradleUtilsExtensionForProject, GradleUtilsExtensionInternal {
    /// The name for this extension.
    String NAME = "gradleutils";


    /* MAVEN REPOSITORIES */

    /**
     * A closure for the Forge maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)}.
     * <pre><code>
     * repositories {
     *     maven fg.forgeMaven
     * }
     * </code></pre>
     */
    Action<MavenArtifactRepository> forgeMaven = GradleUtilsExtensionInternal.forgeMaven;

    /**
     * A closure for the Forge releases maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)}.
     * <pre><code>
     * repositories {
     *     maven fg.forgeReleaseMaven
     * }
     * </code></pre>
     *
     * @see #forgeMaven
     */
    Action<MavenArtifactRepository> forgeReleaseMaven = GradleUtilsExtensionInternal.forgeReleaseMaven;

    /**
     * A closure for the Minecraft libraries maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)}.
     * <pre><code>
     * repositories {
     *     maven fg.minecraftLibsMaven
     * }
     * </code></pre>
     */
    Action<MavenArtifactRepository> minecraftLibsMaven = GradleUtilsExtensionInternal.minecraftLibsMaven;


    /* PUBLISHING */

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block.
    ///
    /// **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL(_RELEASE)`: Containing the URL to use for the release repository
    /// - Please note that since Forge does not have a snapshot repository, snapshot maven publishing via GradleUtils is
    /// no longer supported as of 3.0.0.
    ///
    /// If the required environment variables are not present, the output Maven will be a local folder named `repo` on
    /// the root of the [build directory][org.gradle.initialization.layout.BuildLayout#getRootDirectory()].
    ///
    /// If the `MAVEN_URL_RELEASE` variable is not set, the Forge releases repository will be used
    /// (`https://maven.minecraftforge.net/releases`).
    ///
    /// @return The closure
    default Action<MavenArtifactRepository> getPublishingForgeMaven() {
        return getPublishingForgeMaven(Constants.FORGE_MAVEN_RELEASE);
    }

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block.
    ///
    /// **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL(_RELEASE)`: Containing the URL to use for the release repository
    /// - Please note that since Forge does not have a snapshot repository, snapshot maven publishing via GradleUtils is
    /// no longer supported as of 3.0.0.
    ///
    /// If the required environment variables are not present, the output Maven will be a local folder named `repo` on
    /// the root of the [build directory][org.gradle.initialization.layout.BuildLayout#getRootDirectory()].
    ///
    /// If the `MAVEN_URL_RELEASE` variable is not set, the passed in fallback URL will be used for the release
    /// repository.
    ///
    /// @param fallbackPublishingEndpoint The fallback URL for the release repository
    /// @return The closure
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint);

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block. **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL(_RELEASE)`: Containing the URL to use for the release repository
    /// - Please note that since Forge does not have a snapshot repository, snapshot maven publishing via GradleUtils is
    /// no longer supported as of 3.0.0.
    ///
    /// If the required environment variables are not present, the output Maven will be set to the given default
    /// folder.
    ///
    /// If the `MAVEN_URL(_RELEASE)` variable is not set, the passed in fallback URL will be used for the release
    /// repository.
    ///
    /// @param fallbackPublishingEndpoint The fallback URL for the release repository
    /// @param defaultFolder              The default folder if the required maven information is not set
    /// @return The closure
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint, Object defaultFolder);

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block.
    ///
    /// **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL(_RELEASE)`: Containing the URL to use for the release repository
    /// - Please note that since Forge does not have a snapshot repository, snapshot maven publishing via GradleUtils is
    /// no longer supported as of 3.0.0.
    ///
    /// If the required environment variables are not present, the output Maven will be set to the given default
    /// folder.
    ///
    /// If the `MAVEN_URL_RELEASE` variable is not set, the Forge releases repository will be used
    /// (`https://maven.minecraftforge.net/releases`).
    ///
    /// @param defaultFolder The default folder if the required maven information is not set
    /// @return The closure
    default Action<MavenArtifactRepository> getPublishingForgeMaven(File defaultFolder) {
        return this.getPublishingForgeMaven(Constants.FORGE_MAVEN_RELEASE, defaultFolder);
    }

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block. **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL(_RELEASE)`: Containing the URL to use for the release repository
    /// - Please note that since Forge does not have a snapshot repository, snapshot maven publishing via GradleUtils is
    /// no longer supported as of 3.0.0.
    ///
    /// If the required environment variables are not present, the output Maven will be set to the given default
    /// folder.
    ///
    /// If the `MAVEN_URL(_RELEASE)` variable is not set, the passed in fallback URL will be used for the release
    /// repository.
    ///
    /// @param fallbackPublishingEndpoint The fallback URL for the release repository
    /// @param defaultFolder              The default folder if the required maven information is not set
    /// @return The closure
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint, File defaultFolder);

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block.
    ///
    /// **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL(_RELEASE)`: Containing the URL to use for the release repository
    /// - Please note that since Forge does not have a snapshot repository, snapshot maven publishing via GradleUtils is
    /// no longer supported as of 3.0.0.
    ///
    /// If the required environment variables are not present, the output Maven will be set to the given default
    /// folder.
    ///
    /// If the `MAVEN_URL_RELEASE` variable is not set, the Forge releases repository will be used
    /// (`https://maven.minecraftforge.net/releases`).
    ///
    /// @param defaultFolder The default folder if the required maven information is not set
    /// @return The closure
    default Action<MavenArtifactRepository> getPublishingForgeMaven(Directory defaultFolder) {
        return this.getPublishingForgeMaven(Constants.FORGE_MAVEN_RELEASE, defaultFolder);
    }

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block. **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL(_RELEASE)`: Containing the URL to use for the release repository
    /// - Please note that since Forge does not have a snapshot repository, snapshot maven publishing via GradleUtils is
    /// no longer supported as of 3.0.0.
    ///
    /// If the required environment variables are not present, the output Maven will be set to the given default
    /// folder.
    ///
    /// If the `MAVEN_URL(_RELEASE)` variable is not set, the passed in fallback URL will be used for the release
    /// repository.
    ///
    /// @param fallbackPublishingEndpoint The fallback URL for the release repository
    /// @param defaultFolder              The default folder if the required maven information is not set
    /// @return The closure
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint, Directory defaultFolder);

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block.
    ///
    /// **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL(_RELEASE)`: Containing the URL to use for the release repository
    /// - Please note that since Forge does not have a snapshot repository, snapshot maven publishing via GradleUtils is
    /// no longer supported as of 3.0.0.
    ///
    /// If the required environment variables are not present, the output Maven will be set to the given default
    /// folder.
    ///
    /// If the `MAVEN_URL_RELEASE` variable is not set, the Forge releases repository will be used
    /// (`https://maven.minecraftforge.net/releases`).
    ///
    /// @param defaultFolder The default folder if the required maven information is not set
    /// @return The closure
    default Action<MavenArtifactRepository> getPublishingForgeMaven(Provider<?> defaultFolder) {
        return this.getPublishingForgeMaven(Constants.FORGE_MAVEN_RELEASE, defaultFolder);
    }

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block. **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL(_RELEASE)`: Containing the URL to use for the release repository
    /// - Please note that since Forge does not have a snapshot repository, snapshot maven publishing via GradleUtils is
    /// no longer supported as of 3.0.0.
    ///
    /// If the required environment variables are not present, the output Maven will be set to the given default
    /// folder.
    ///
    /// If the `MAVEN_URL(_RELEASE)` variable is not set, the passed in fallback URL will be used for the release
    /// repository.
    ///
    /// @param fallbackPublishingEndpoint The fallback URL for the release repository
    /// @param defaultFolder              The default folder if the required maven information is not set
    /// @return The closure
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint, Provider<?> defaultFolder);

    /// Utilities for working with a [org.gradle.api.publish.maven.MavenPom] for publishing artifacts.
    ///
    /// @return The POM utilities
    /// @see PomUtils
    PomUtils getPom();


    /* MISCELLANEOUS */

    /// Unpacks a deferred value.
    ///
    /// @param value The value to unpack
    /// @param <T>   The type of value held by the provider
    /// @return The unpacked value
    /// @see #unpack(Object)
    default <T> T unpack(Provider<T> value) {
        return Util.unpack(value);
    }

    /// Unpacks a deferred value.
    ///
    /// @param value The value to unpack
    /// @param <T>   The type of value held by the provider
    /// @return The unpacked value
    /// @see #unpack(Object)
    default <T> T unpack(ProviderConvertible<T> value) {
        return Util.unpack(value);
    }

    /// Unpacks a deferred value.
    ///
    /// @param value The value to unpack
    /// @param <T>   The type of value held by the provider
    /// @return The unpacked value
    /// @see #unpack(Object)
    default <T> T unpack(Closure<T> value) {
        return Util.unpack(value);
    }

    /// Unpacks a deferred value.
    ///
    /// @param value The value to unpack
    /// @param <T>   The type of value held by the provider
    /// @return The unpacked value
    /// @see #unpack(Object)
    default <T> T unpack(Callable<T> value) {
        return Util.unpack(value);
    }

    /// Unpacks a deferred value.
    ///
    /// @param value The value to unpack
    /// @param <T>   The type of value held by the provider
    /// @return The unpacked value
    /// @see #unpack(Object)
    default <T> T unpack(Function0<T> value) {
        return Util.unpack(value);
    }

    /// Unpacks a deferred value.
    ///
    /// @param value The value to unpack
    /// @param <T>   The type of value held by the provider
    /// @return The unpacked value
    /// @see #unpack(Object)
    default <T> T unpack(Supplier<T> value) {
        return Util.unpack(value);
    }

    /// Unpacks a deferred value.
    ///
    /// Since buildscripts are dynamically compiled, this allows buildscript authors to use this method with version
    /// catalog entries, other provider-like objects. This prevents the need to arbitrarily call
    /// [Provider#get()] (or similar) on values which may or may not be deferred based on circumstance.
    ///
    /// @param value The value to unpack
    /// @param <T>   The type of value held by the provider
    /// @return The unpacked value
    default <T> T unpack(Object value) {
        return Util.unpack(value);
    }
}
