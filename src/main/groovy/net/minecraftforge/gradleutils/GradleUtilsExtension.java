/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.util.concurrent.Callable;

/// Contains various utilities for working with Gradle scripts.
///
/// @see GradleUtilsExtensionForProject
public interface GradleUtilsExtension {
    /// The name for this extension.
    String NAME = "gradleutils";


    /* MAVEN REPOSITORIES */

    /// @deprecated Use [#getForgeMaven()]
    @Deprecated(forRemoval = true, since = "3.3.28")
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    Action<MavenArtifactRepository> forgeMaven = repo -> {
        repo.setName("MinecraftForge");
        repo.setUrl("https://maven.minecraftforge.net/");
    };

    /**
     * A closure for the Forge maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)}.
     * <pre><code>
     * repositories {
     *     maven fg.forgeMaven
     * }
     * </code></pre>
     */
    Action<MavenArtifactRepository> getForgeMaven();

    /// @deprecated Use [#getForgeReleaseMaven()]
    @Deprecated(forRemoval = true, since = "3.3.28")
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    Action<MavenArtifactRepository> forgeReleaseMaven = repo -> {
        repo.setName("MinecraftForge releases");
        repo.setUrl("https://maven.minecraftforge.net/releases");
    };

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
    Action<MavenArtifactRepository> getForgeReleaseMaven();

    /// @deprecated Use [#getMinecraftLibsMaven()]
    @Deprecated(forRemoval = true, since = "3.3.28")
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    Action<MavenArtifactRepository> minecraftLibsMaven = repo -> {
        repo.setName("Minecraft libraries");
        repo.setUrl("https://libraries.minecraft.net/");
    };

    /**
     * A closure for the Minecraft libraries maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)}.
     * <pre><code>
     * repositories {
     *     maven fg.minecraftLibsMaven
     * }
     * </code></pre>
     */
    Action<MavenArtifactRepository> getMinecraftLibsMaven();


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
    Action<MavenArtifactRepository> getPublishingForgeMaven();

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
    Action<MavenArtifactRepository> getPublishingForgeMaven(File defaultFolder);

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
    Action<MavenArtifactRepository> getPublishingForgeMaven(Directory defaultFolder);

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
    Action<MavenArtifactRepository> getPublishingForgeMaven(Provider<?> defaultFolder);

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
    /// Since buildscripts are dynamically compiled, this allows buildscript authors to use this method with version
    /// catalog entries, other provider-like objects. This prevents the need to arbitrarily call [Provider#get()] (or
    /// similar) on values which may or may not be deferred based on circumstance.
    ///
    /// @param value The value to unpack
    /// @param <T>   The type of value held by the provider
    /// @return The unpacked value
    <T> T unpack(Object value);

    /// Packs a (deferred) value as a provider.
    ///
    /// Since buildscripts are dynamically compiled, this allows buildscript authors to use this method with version
    /// catalog entries, other provider-like objects. This prevents the need to call [ProviderConvertible#asProvider()]
    /// or otherwise create arbitrary providers using [org.gradle.api.provider.ProviderFactory#provider(Callable)] on
    /// values which may or may not be deferred based on circumstance.
    ///
    /// @param value The value to pack
    /// @param <T>   The type of value held by the provider
    /// @return The packed value
    <T> Provider<T> asProvider(Object value);
}
