/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.gitversion

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout

import javax.inject.Inject

/** The entry point for the Git Version Gradle plugin. Exists to create the {@linkplain GitVersionExtension extension}. */
@CompileStatic
abstract class GitVersionPlugin implements Plugin<Project> {
    @Inject
    abstract ProjectLayout getLayout()

    @Override
    void apply(Project project) {
        project.extensions.create(GitVersionExtension.NAME, GitVersionExtension, project, this.layout)
    }
}
