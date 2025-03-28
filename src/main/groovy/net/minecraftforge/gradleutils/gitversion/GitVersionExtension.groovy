/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.gitversion

import groovy.transform.CompileStatic
import net.minecraftforge.gitver.api.GitVersion
import net.minecraftforge.gitver.api.GitVersionException
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout

import javax.inject.Inject

/**
 * The heart of the Git Version Gradle plugin. This extension is responsible for creating the GitVersion object and
 * allowing access to it from Gradle buildscripts.
 * <p>
 * To avoid issues with Gradle's Configuration Cache, the system Git config is disabled using
 * {@link GitVersion#disableSystemConfig()}.
 */
@CompileStatic
class GitVersionExtension {
    public static final String NAME = 'gitversion'

    private final Project project
    private final ProjectLayout layout

    /**
     * The Git Version, created lazily with the {@linkplain GitVersion#disableSystemConfig() system config disabled} and
     * the ability to recover from errors by creating a default version of 0.0.0.
     */
    @Lazy GitVersion version = {
        GitVersion.disableSystemConfig()

        var builder = GitVersion.builder().project(this.layout.projectDirectory.asFile)
        try {
            return builder.build().tap { it.info }
        } catch (GitVersionException ignored) {
            this.project.logger.warn '''WARNING: Git Version failed to get version numbers! Attempting to use default version 0.0.0.
Check your GitVersion config file and make sure the correct tag prefix and filters are in use.
Ensure that the tags you are attempting to use exist in the repository.'''
            return builder.strict(false).build()
        } catch (IllegalArgumentException e) {
            this.project.logger.error '''ERROR: Git Version is misconfigured and cannot be used, likely due to incorrect paths being set.
This is an unrecoverable problem and needs to be addressed in the config file.
Ensure that the correct subprojects and paths are declared in the config file'''
            throw e
        }
    }()

    @Inject
    GitVersionExtension(Project project, ProjectLayout layout) {
        this.project = project
        this.layout = layout
    }
}
