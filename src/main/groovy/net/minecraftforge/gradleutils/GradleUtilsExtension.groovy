/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import net.minecraftforge.gradleutils.gitversion.GitVersionExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject

/**
 * The heart of the GradleUtils library. This class can be directly accessed from buildscripts that have the
 * {@linkplain GradleUtilsPlugin plugin} applied using {@code gradleutils}.
 */
@CompileStatic
class GradleUtilsExtension {
    public static final String NAME = 'gradleutils'

    private final Project project
    private final ProviderFactory providers
    private final ObjectFactory objects

    private final GitVersionExtension gitversion

    /** Holds a project-aware Pom utilities class, useful for configuring repositories and publishing. */
    public final PomUtils pom

    /** @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getRoot() GitVersion.getRoot()} via {@link GitVersionExtension#getVersion()} instead. */
    @Deprecated(forRemoval = true, since = '2.4') @Lazy DirectoryProperty gitRoot = {
        this.project.logger.warn """
            WARNING: This project is still using 'gradleutils.gitRoot'.
            It has been deprecated and will be removed in GradleUtils 3.0.
            Consider using 'gitversion.version.root' instead."""

        objects.directoryProperty().fileProvider providers.provider { this.gitversion.version.root }
    }()
    /** @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getInfo() GitVersion.getInfo()} via {@link GitVersionExtension#getVersion()} instead. */
    @Deprecated(forRemoval = true, since = '2.4') @Lazy Map<String, String> gitInfo = {
        this.project.logger.warn """
            WARNING: This project is still using 'gradleutils.gitInfo'.
            It has been deprecated and will be removed in GradleUtils 3.0.
            Consider using 'gitversion.version.info' instead."""

        var version = this.project.extensions.getByType(GitVersionExtension).version
        [
            dir          : version.gitDir.absolutePath,
            tag          : version.info.tag,
            offset       : version.info.offset,
            hash         : version.info.hash,
            branch       : version.info.branch,
            commit       : version.info.commit,
            abbreviatedId: version.info.abbreviatedId,
            url          : version.info.url
        ].tap { it.removeAll { it.value == null } }
    }()

    @Inject
    GradleUtilsExtension(Project project, ProviderFactory providers, ObjectFactory objects) {
        this.project = project
        this.providers = providers
        this.objects = objects

        // Git Version
        this.gitversion = project.extensions.getByType(GitVersionExtension)

        // Pom Utils
        this.pom = new PomUtils(project.logger, providers, this.gitversion)

        // Tasks
        GenerateActionsWorkflow.register(this.project)
        GradleUtils.setupCITasks(this.project)
    }

    /**
     * This method has been deprecated in favor of usage of GitVersion.
     * <pre><code>
     *     // Before:
     *     version = gradleutils.tagOffsetVersion
     *
     *     // After:
     *     version = gitversion.version.tagOffset
     * </code></pre>
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getTagOffset() GitVersion.tagOffset} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    String getTagOffsetVersion() {
        this.logDeprecation('tagOffsetVersion', 'tagOffsetVersion')
        this.project.extensions.getByType(GitVersionExtension).version.tagOffset
    }

    /**
     * This method has been deprecated in favor of usage of GitVersion.
     * <pre><code>
     *     // Before:
     *     version = gradleutils.tagOffsetVersion
     *
     *     // After:
     *     version = gitversion.version.tagOffset
     * </code></pre>
     * <strong>You must declare your filters in the Git Version config file!</strong>.
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getTagOffset() GitVersion.tagOffset} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    String getFilteredTagOffsetVersion(boolean prefix = false, String filter) {
        this.updateInfo(prefix, filter)
        this.tagOffsetVersion
    }

    /**
     * This method has been deprecated in favor of usage of GitVersion.
     * <pre><code>
     *     // Before:
     *     version = gradleutils.getTagOffsetBranchVersion()
     *
     *     // After:
     *     version = gitversion.version.tagOffsetBranch
     * </code></pre>
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getTagOffsetBranch(String ...) GitVersion.getTagOffsetBranch(String...)} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    String getTagOffsetBranchVersion(String... allowedBranches) {
        this.logDeprecation('tagOffsetBranchVersion', 'getTagOffsetBranchVersion(String...)')
        var version = this.project.extensions.getByType(GitVersionExtension).version
        allowedBranches ? version.getTagOffsetBranch(allowedBranches) : version.tagOffsetBranch
    }

    /**
     * This method has been deprecated in favor of usage of GitVersion.
     * <pre><code>
     *     // Before:
     *     version = gradleutils.tagOffsetBranchVersion
     *
     *     // After:
     *     version = gitversion.version.tagOffsetBranch
     * </code></pre>
     * <p>
     * <strong>You must declare your filters in the Git Version config file!</strong>.
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getTagOffset() GitVersion.tagOffset} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    String getFilteredTagOffsetBranchVersion(boolean prefix = false, String filter, String... allowedBranches) {
        this.updateInfo(prefix, filter)
        this.getTagOffsetBranchVersion(allowedBranches)
    }

    /**
     * This method has been deprecated in favor of usage of GitVersion.
     * <pre><code>
     *     // Before:
     *     version = gradleutils.getMCTagOffsetBranchVersion('1.21.4')
     *
     *     // After:
     *     version = gitversion.version.getMCTagOffsetBranch('1.21.4')
     * </code></pre>
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getMCTagOffsetBranch(String, String ...) GitVersion.getMCTagOffsetBranch(String, String...)} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    String getMCTagOffsetBranchVersion(String mcVersion, String... allowedBranches) {
        this.logDeprecation('MCTagOffsetBranchVersion', 'getMCTagOffsetBranchVersion(String, String...)')
        var version = this.project.extensions.getByType(GitVersionExtension).version
        allowedBranches ? version.getMCTagOffsetBranch(mcVersion, allowedBranches) : version.getMCTagOffsetBranch(mcVersion)
    }

    /**
     * This method has been deprecated in favor of usage of GitVersion.
     * <pre><code>
     *     // Before:
     *     version = gradleutils.getMCTagOffsetBranchVersion('1.21.4')
     *
     *     // After:
     *     version = gitversion.version.getMCTagOffsetBranch('1.21.4')
     * </code></pre>
     * <p>
     * <strong>You must declare your filters in the Git Version config file!</strong>.
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getMCTagOffsetBranch(String, String ...) GitVersion.getMCTagOffsetBranch(String, String...)} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    String getFilteredMCTagOffsetBranchVersion(boolean prefix = false, String filter, String mcVersion, String... allowedBranches) {
        this.updateInfo(prefix, filter)
        this.getMCTagOffsetBranchVersion(mcVersion, allowedBranches)
    }

    @Deprecated(since = '2.4')
    private void updateInfo(boolean prefix, String filter) {
        this.project.extensions.getByType(GitVersionExtension).version.tap {
            if (prefix) it.tagPrefix = filter
            else it.filters = new String[] {filter}
        }
    }

    private void logDeprecation(String name, String fullName) {
        this.project.logger.warn """
            WARNING: This project is still using 'gradleutils.$name'.
            It has been deprecated and will be removed in GradleUtils 3.0.
            Consider using 'gitversion.version.$fullName' instead."""
    }

    /** @see GradleUtils#getPublishingForgeMaven(Project, File) */
    Action<? super MavenArtifactRepository> getPublishingForgeMaven(File defaultFolder = this.project.rootProject.file('repo')) {
        GradleUtils.getPublishingForgeMaven(this.project, defaultFolder)
    }

    /** @see GradleUtils#getForgeMaven() */
    static Action<? super MavenArtifactRepository> getForgeMaven() {
        GradleUtils.forgeMaven
    }

    /** @see GradleUtils#getForgeReleaseMaven() */
    static Action<? super MavenArtifactRepository> getForgeReleaseMaven() {
        GradleUtils.forgeReleaseMaven
    }

    /** @see GradleUtils#getForgeSnapshotMaven() */
    static Action<? super MavenArtifactRepository> getForgeSnapshotMaven() {
        GradleUtils.forgeSnapshotMaven
    }

    /** @see GradleUtils#getMinecraftLibsMaven() */
    static Action<? super MavenArtifactRepository> getMinecraftLibsMaven() {
        GradleUtils.minecraftLibsMaven
    }
}
