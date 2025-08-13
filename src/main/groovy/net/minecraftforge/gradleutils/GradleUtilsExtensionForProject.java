/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import java.io.File;

/// The GradleUtils extension for [projects][org.gradle.api.Project], which include additional utilities that are only
/// available for them.
///
/// When applied, GradleUtils will
/// - Create a referenceable [PomUtils] instance in [#getPom()].
/// - Register the `generateActionsWorkflow` task to the project for generating a default template GitHub Actions
/// workflow.
/// - Register the `configureTeamCity` task to the project for working with TeamCity CI pipelines.
///
/// @see GradleUtilsExtension
sealed public interface GradleUtilsExtensionForProject extends GradleUtilsExtension permits GradleUtilsExtensionInternal.ForProject {
    /// Utilities for working with a [org.gradle.api.publish.maven.MavenPom] for publishing artifacts.
    ///
    /// @return The POM utilities
    /// @see PomUtils
    PomUtils getPom();

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block.
    ///
    /// This closure respects the current project's version in regard to publishing to a release or snapshot
    /// repository.
    ///
    /// **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL_RELEASE`: Containing the URL to use for the release repository
    /// - `MAVEN_URL_SNAPSHOT`: Containing the URL to use for the snapshot repository
    ///
    /// If the required environment variables are not present, the output Maven will be a local folder named `repo` on
    /// the root of the [project directory][org.gradle.api.file.ProjectLayout#getProjectDirectory()].
    ///
    /// If the `MAVEN_URL_RELEASE` variable is not set, the Forge releases repository will be used
    /// (`https://maven.minecraftforge.net/releases`).
    ///
    /// @return The closure
    default Action<MavenArtifactRepository> getPublishingForgeMaven() {
        return getPublishingForgeMaven("https://maven.minecraftforge.net/releases");
    }

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block.
    ///
    /// This closure respects the current project's version in regard to publishing to a release or snapshot
    /// repository.
    ///
    /// **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL_RELEASE`: Containing the URL to use for the release repository
    /// - `MAVEN_URL_SNAPSHOT`: Containing the URL to use for the snapshot repository
    ///
    /// If the required environment variables are not present, the output Maven will be a local folder named `repo` on
    /// the root of the [project directory][org.gradle.api.file.ProjectLayout#getProjectDirectory()].
    ///
    /// If the `MAVEN_URL_RELEASE` variable is not set, the passed in fallback URL will be used for the release
    /// repository.
    ///
    /// @param fallbackPublishingEndpoint The fallback URL for the release repository
    /// @return The closure
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint);

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block.
    ///
    /// This closure respects the current project's version in regard to publishing to a release or snapshot
    /// repository.
    ///
    /// **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL_RELEASE`: Containing the URL to use for the release repository
    /// - `MAVEN_URL_SNAPSHOT`: Containing the URL to use for the snapshot repository
    ///
    /// If the required environment variables are not present, the output Maven will be set to the given default
    /// folder.
    ///
    /// If the `MAVEN_URL_RELEASE` variable is not set, the passed in fallback URL will be used for the release
    /// repository.
    ///
    /// @param fallbackPublishingEndpoint The fallback URL for the release repository
    /// @param defaultFolder              The default folder if the required maven information is not set
    /// @return The closure
    default Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint, File defaultFolder) {
        return getPublishingForgeMaven(fallbackPublishingEndpoint, defaultFolder, new File(defaultFolder.getAbsoluteFile().getParentFile(), "snapshots"));
    }

    /// Get a configuring closure to be passed into [org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)] in a
    /// publishing block.
    ///
    /// This closure respects the current project's version in regard to publishing to a release or snapshot
    /// repository.
    ///
    /// **Important:** The following environment variables must be set for this to work:
    /// - `MAVEN_USER`: Containing the username to use for authentication
    /// - `MAVEN_PASSWORD`: Containing the password to use for authentication
    ///
    /// The following environment variables are optional:
    /// - `MAVEN_URL_RELEASE`: Containing the URL to use for the release repository
    /// - `MAVEN_URL_SNAPSHOT`: Containing the URL to use for the snapshot repository
    ///
    /// If the required environment variables are not present, the output Maven will be set to the given default
    /// folder.
    ///
    /// If the `MAVEN_URL_RELEASE` variable is not set, the passed in fallback URL will be used for the release
    /// repository.
    ///
    /// @param fallbackPublishingEndpoint The fallback URL for the release repository
    /// @param defaultFolder              The default folder if the required maven information is not set
    /// @param defaultSnapshotFolder      The default folder for the snapshot repository if the required maven
    ///                                   information is not set
    /// @return The closure
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint, File defaultFolder, File defaultSnapshotFolder);
}
