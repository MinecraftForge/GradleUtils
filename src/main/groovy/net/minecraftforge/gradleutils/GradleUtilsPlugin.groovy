/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import net.minecraftforge.gradleutils.changelog.ChangelogPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class GradleUtilsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply(ChangelogPlugin)
        project.extensions.create("gradleutils", GradleUtilsExtension, project)
        GenerateActionsWorkflow.register(project)
        //Setup the teamcity project task.
        GradleUtils.setupCITasks(project)
    }
}
