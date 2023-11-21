/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog

import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider

import javax.inject.Inject

class ChangelogExtension {
    public static final String NAME = "changelog"

    @PackageScope final Project project
    @PackageScope TaskProvider<GenerateChangelog> task

    boolean publishAll = true
    Directory gitRoot

    @Inject
    ChangelogExtension(Project project) {
        this.project = project
    }

    void fromBase() {
        from(null)
    }

    void from(String marker) {
        task = ChangelogUtils.setupChangelogTask(this.project)
        task.configure {
            start = marker
        }

        project.afterEvaluate {
            if (gitRoot != null) {
                task.configure {
                    gitDirectory = gitRoot
                }
            }

            if (publishAll)
                ChangelogUtils.setupChangelogGenerationOnAllPublishTasks(project)
        }
    }

    void publish(MavenPublication publication) {
        ChangelogUtils.setupChangelogGenerationForPublishing(project, publication)
    }
}
