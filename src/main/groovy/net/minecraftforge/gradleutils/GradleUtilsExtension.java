/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

/// Contains various utilities for working with Gradle scripts.
///
/// [Projects][org.gradle.api.Project] that apply GradleUtils are given [GradleUtilsExtensionForProject].
public sealed interface GradleUtilsExtension permits GradleUtilsExtensionInternal, GradleUtilsExtensionForProject {
    /// The name for this extension.
    String NAME = "gradleutils";

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
}
