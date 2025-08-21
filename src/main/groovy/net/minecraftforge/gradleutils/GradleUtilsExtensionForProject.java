/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.TaskProvider;

/// A subset of [GradleUtilsExtension] that is given to projects. Includes additional convenience methods that only
/// apply to projects.
public sealed interface GradleUtilsExtensionForProject extends GradleUtilsExtension permits GradleUtilsExtensionInternal.ForProject {
    /// Promotes a publication to the <a href="https://files.minecraftforge.net">Forge Files Site</a>.
    ///
    /// Publications that are promoted will automatically have the relevant task added as a finalizer to the
    /// `publishPublicationToForgeRepository` task, where the publication matches the task's publication and the
    /// repository name is "forge". The publishing Forge repo added via [GradleUtilsExtension#getPublishingForgeMaven]
    /// always sets it with the name "forge".
    ///
    /// @param publication The publication to promote
    /// @return The provider for the promotion task
    TaskProvider<? extends PromotePublication> promote(MavenPublication publication);
}
