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
import org.jetbrains.annotations.ApiStatus

import javax.inject.Inject

/**
 * The heart of the GradleUtils library. This class can be directly accessed from buildscripts that have the
 * {@linkplain GradleUtilsPlugin plugin} applied using {@code gradleutils}.
 */
@CompileStatic
class GradleUtilsExtension {
    public static final String NAME = 'gradleutils'

    private final Project project
    private final ObjectFactory objects
    private final ProviderFactory providers

    private final GitVersionExtension gitversion

    /** Holds a project-aware Pom utilities class, useful for configuring repositories and publishing. */
    public final PomUtils pom

    /** @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getRoot() GitVersion.getRoot()} via {@link GitVersionExtension#getVersion()} instead. */
    @Deprecated(forRemoval = true, since = '2.4') @Lazy DirectoryProperty gitRoot = {
        this.project.logger.warn "WARNING: This project is still using 'gradleutils.gitRoot'. It has been deprecated and will be removed in GradleUtils 3.0. Consider using 'gitversion.rootDir' instead."

        this.gitversion.rootDir
    }()
    /** @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getInfo() GitVersion.getInfo()} via {@link GitVersionExtension#getVersion()} instead. */
    @Deprecated(forRemoval = true, since = '2.4') @Lazy Map<String, String> gitInfo = {
        this.project.logger.warn "WARNING: This project is still using 'gradleutils.gitInfo'. It has been deprecated and will be removed in GradleUtils 3.0. Consider using 'gitversion.info' instead."

        var gitversion = this.project.extensions.getByType(GitVersionExtension)
        var info = gitversion.info
        [
            dir          : gitversion.gitDir.get().asFile.absolutePath,
            tag          : info.tag,
            offset       : info.offset,
            hash         : info.hash,
            branch       : info.branch,
            commit       : info.commit,
            abbreviatedId: info.abbreviatedId,
            url          : gitversion.url
        ].tap { it.removeAll { it.value == null } }
    }()

    /** @deprecated This constructor will be made package-private in GradleUtils 3.0 */
    @Inject
    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    GradleUtilsExtension(Project project, ObjectFactory objects, ProviderFactory providers) {
        this.project = project
        this.objects = objects
        this.providers = providers

        // Git Version
        this.gitversion = project.extensions.getByType(GitVersionExtension)

        // Pom Utils
        this.pom = new PomUtils(project, providers, this.gitversion)

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
     *     version = gitversion.tagOffset
     * </code></pre>
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getTagOffset() GitVersion.tagOffset} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    String getTagOffsetVersion() {
        this.logDeprecation('tagOffsetVersion', 'tagOffsetVersion')
        this.project.extensions.getByType(GitVersionExtension).tagOffset
    }

    /**
     * This method has been deprecated in favor of usage of GitVersion.
     * <pre><code>
     *     // Before:
     *     version = gradleutils.tagOffsetVersion
     *
     *     // After:
     *     version = gitversion.tagOffset
     * </code></pre>
     * <strong>You must declare your filters in the Git Version config file!</strong>.
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getTagOffset() GitVersion.tagOffset} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
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
     *     version = gitversion.tagOffsetBranch
     * </code></pre>
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getTagOffsetBranch(String ...) GitVersion.getTagOffsetBranch(String...)} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    String getTagOffsetBranchVersion(String... allowedBranches) {
        this.logDeprecation('tagOffsetBranchVersion', 'getTagOffsetBranchVersion(String...)')
        var gitversion = this.project.extensions.getByType(GitVersionExtension)
        allowedBranches ? gitversion.getTagOffsetBranch(allowedBranches) : gitversion.tagOffsetBranch
    }

    /**
     * This method has been deprecated in favor of usage of GitVersion.
     * <pre><code>
     *     // Before:
     *     version = gradleutils.tagOffsetBranchVersion
     *
     *     // After:
     *     version = gitversion.tagOffsetBranch
     * </code></pre>
     * <p>
     * <strong>You must declare your filters in the Git Version config file!</strong>.
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getTagOffset() GitVersion.tagOffset} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
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
     *     version = gitversion.getMCTagOffsetBranch('1.21.4')
     * </code></pre>
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getMCTagOffsetBranch(String, String ...) GitVersion.getMCTagOffsetBranch(String, String...)} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    String getMCTagOffsetBranchVersion(String mcVersion, String... allowedBranches) {
        this.logDeprecation('MCTagOffsetBranchVersion', 'getMCTagOffsetBranchVersion(String, String...)')
        var gitVersion = this.project.extensions.getByType(GitVersionExtension)
        allowedBranches ? gitVersion.getMCTagOffsetBranch(mcVersion, allowedBranches) : gitVersion.getMCTagOffsetBranch(mcVersion)
    }

    /**
     * This method has been deprecated in favor of usage of GitVersion.
     * <pre><code>
     *     // Before:
     *     version = gradleutils.getMCTagOffsetBranchVersion('1.21.4')
     *
     *     // After:
     *     version = gitversion.getMCTagOffsetBranch('1.21.4')
     * </code></pre>
     * <p>
     * <strong>You must declare your filters in the Git Version config file!</strong>.
     *
     * @deprecated Use {@link net.minecraftforge.gitver.api.GitVersion#getMCTagOffsetBranch(String, String ...) GitVersion.getMCTagOffsetBranch(String, String...)} instead.
     */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    String getFilteredMCTagOffsetBranchVersion(boolean prefix = false, String filter, String mcVersion, String... allowedBranches) {
        this.updateInfo(prefix, filter)
        this.getMCTagOffsetBranchVersion(mcVersion, allowedBranches)
    }

    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    private void updateInfo(boolean prefix, String filter) {
        this.gitversion.versionInternal.tap {
            if (prefix) it.tagPrefix = filter
            else it.filters = new String[] {filter}
        }
    }

    private void logDeprecation(String name, String fullName) {
        this.project.logger.warn "WARNING: This project is still using 'gradleutils.$name'. It has been deprecated and will be removed in GradleUtils 3.0. Consider using 'gitversion.$fullName' instead."
    }

    /** @see GradleUtils#getPublishingForgeMaven(Project, File) */
    Action<? super MavenArtifactRepository> getPublishingForgeMaven(File defaultFolder = this.project.rootProject.file('repo')) {
        GradleUtils.getPublishingForgeMaven(this.project, defaultFolder)
    }

    /** @see GradleUtils#getForgeMaven() */
    static Closure getForgeMaven() {
        GradleUtils.forgeMaven
    }

    /** @see GradleUtils#getForgeReleaseMaven() */
    static Closure getForgeReleaseMaven() {
        GradleUtils.forgeReleaseMaven
    }

    /** @see GradleUtils#getForgeSnapshotMaven() */
    static Closure getForgeSnapshotMaven() {
        GradleUtils.forgeSnapshotMaven
    }

    /** @see GradleUtils#getMinecraftLibsMaven() */
    static Closure getMinecraftLibsMaven() {
        GradleUtils.minecraftLibsMaven
    }
}
