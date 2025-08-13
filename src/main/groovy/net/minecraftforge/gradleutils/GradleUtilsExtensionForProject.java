/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

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
public sealed interface GradleUtilsExtensionForProject extends GradleUtilsExtension permits GradleUtilsExtensionInternal.ForProject {
    /// Utilities for working with a [org.gradle.api.publish.maven.MavenPom] for publishing artifacts.
    ///
    /// @return The POM utilities
    /// @see PomUtils
    PomUtils getPom();
}
