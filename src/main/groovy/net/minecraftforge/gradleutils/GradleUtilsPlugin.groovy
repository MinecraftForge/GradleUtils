/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class GradleUtilsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        GradleUtilsExtension extension = project.extensions.create("gradleutils", GradleUtilsExtension, project)
        ChangelogGenerationExtension changelogGenerationExtension = project.extensions.create("changelog", ChangelogGenerationExtension, project)

        //Setup the teamcity project task.
        GradleUtils.setupCITasks(project)
    }
}
