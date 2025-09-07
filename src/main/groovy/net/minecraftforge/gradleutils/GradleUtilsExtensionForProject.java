/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import org.gradle.api.Action;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.TaskProvider;

/// A subset of [GradleUtilsExtension] that is given to projects. Includes additional convenience methods that only
/// apply to projects.
public sealed interface GradleUtilsExtensionForProject extends GradleUtilsExtension permits GradleUtilsExtensionInternal.ForProject {
    /// The display name for the project.
    ///
    /// If the relevant properties are enabled, it is used in areas such as the Javadoc window title, among other
    /// things.
    ///
    /// @return The property for the display name
    Property<String> getDisplayName();

    void pluginDevDefaults(ConfigurationContainer configurations, CharSequence gradleVersion);

    void pluginDevDefaults(ConfigurationContainer configurations, Provider<? extends CharSequence> gradleVersion);

    default void pluginDevDefaults(ConfigurationContainer configurations, ProviderConvertible<? extends CharSequence> gradleVersion) {
        this.pluginDevDefaults(configurations, gradleVersion.asProvider());
    }

    /// Promotes a publication to the <a href="https://files.minecraftforge.net">Forge Files Site</a>.
    ///
    /// Publications that are promoted will automatically have the relevant task added as a finalizer to the
    /// `publishPublicationToForgeRepository` task, where the publication matches the task's publication and the
    /// repository name is "forge". The publishing Forge repo added via [GradleUtilsExtension#getPublishingForgeMaven]
    /// always sets it with the name "forge".
    ///
    /// @param publication The publication to promote
    /// @return The provider for the promotion task
    default TaskProvider<? extends PromotePublication> promote(MavenPublication publication) {
        return this.promote(publication, null);
    }

    /// Promotes a publication to the <a href="https://files.minecraftforge.net">Forge Files Site</a>.
    ///
    /// Publications that are promoted will automatically have the relevant task added as a finalizer to the
    /// `publishPublicationToForgeRepository` task, where the publication matches the task's publication and the
    /// repository name is "forge". The publishing Forge repo added via [GradleUtilsExtension#getPublishingForgeMaven]
    /// always sets it with the name "forge".
    ///
    /// @param publication The publication to promote
    /// @param cfg         A configuring action for the task
    /// @return The provider for the promotion task
    TaskProvider<? extends PromotePublication> promote(MavenPublication publication, Action<? super PromotePublication> cfg);
}
