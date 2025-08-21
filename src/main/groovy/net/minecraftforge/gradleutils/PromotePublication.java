/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import org.gradle.api.Task;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.jetbrains.annotations.ApiStatus;

/// This task promotes a publication to the <a href="https://files.minecraftforge.net">Forge Files Site</a>.
///
/// @apiNote This task is still [experimental][ApiStatus.Experimental]. It may be buggy and is subject to change.
@ApiStatus.Experimental
public sealed interface PromotePublication extends Task permits PromotePublicationInternal {
    /// The publication group to promote.
    ///
    /// By convention, this is [org.gradle.api.Project#getGroup()], but the set value is
    /// [org.gradle.api.publish.maven.MavenPublication#getGroupId()].
    ///
    /// @return The property for the artifact group
    @Input Property<String> getArtifactGroup();

    /// The publication name to promote.
    ///
    /// By convention, this is [org.gradle.api.plugins.BasePluginExtension#getArchivesName()], but the set value is
    /// [org.gradle.api.publish.maven.MavenPublication#getArtifactId()].
    ///
    /// @return The property for the artifact name
    @Input Property<String> getArtifactName();

    /// The publication version to promote.
    ///
    /// By convention, this is [org.gradle.api.Project#getVersion()], but the set value is
    /// [org.gradle.api.publish.maven.MavenPublication#getVersion()].
    ///
    /// @return The property for the artifact version
    @Input Property<String> getArtifactVersion();

    /// The promotion type to use.
    ///
    /// By default, this is `latest`.
    ///
    /// @return The property for the promotion type
    @Input Property<String> getPromotionType();

    /// The webhook URL to use. If this is not set, this task will not run.
    ///
    /// This is set by the `PROMOTE_ARTIFACT_WEBHOOK` environment variable using
    /// [org.gradle.api.provider.ProviderFactory#environmentVariable(String)].
    ///
    /// @return The property for the webhook URL.
    @Input Property<String> getWebhookURL();

    /// The username to use. If this is not set, this task will not run.
    ///
    /// This is set by the `PROMOTE_ARTIFACT_USERNAME` environment variable using
    /// [org.gradle.api.provider.ProviderFactory#environmentVariable(String)].
    ///
    /// @return The property for the username.
    @Input Property<String> getUsername();

    /// The password to use. If this is not set, this task will not run.
    ///
    /// This is set by the `PROMOTE_ARTIFACT_PASSWORD` environment variable using
    /// [org.gradle.api.provider.ProviderFactory#environmentVariable(String)].
    ///
    /// @return The property for the password URL.
    @Input Property<String> getPassword();
}
