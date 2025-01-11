/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gradleutils.GradleUtils
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.Problems
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.Nullable

import javax.inject.Inject

/** The heart of the Changelog plugin. This extension is used to enable and partially configure the changelog generation task. */
@CompileStatic
class ChangelogExtension {
    public static final String NAME = 'changelog'
    @PackageScope static final ProblemGroup PROBLEM_GROUP = ProblemGroup.create("gradleutils-changelog", "Changelog Generation")

    @PackageScope final Project project
    @PackageScope final Problems problems
    @PackageScope @Nullable TaskProvider<GenerateChangelog> task

    boolean publishAll = true
    /** @deprecated The Git root is automatically discovered by Git Version on Changelog generation. */
    @Deprecated(forRemoval = true, since = '2.4') @Nullable Directory gitRoot

    @Inject
    ChangelogExtension(Project project, Problems problems) {
        this.project = project
        this.problems = problems
    }

    private void setupTask() {
        if (this.task) return

        this.task = ChangelogUtils.setupChangelogTask(this.project)
        this.project.afterEvaluate {
            if (this.gitRoot) {
                this.task.configure {
                    it.gitDirectory.set gitRoot
                }
            }

            if (this.publishAll)
                ChangelogUtils.setupChangelogGenerationOnAllPublishTasks(it)
        }
    }

    void fromBase() {
        from(null)
    }

    void from(String marker) {
        this.setupTask()
        this.task.configure { it.start.set marker }
    }

    void publish(MavenPublication publication) {
        ChangelogUtils.setupChangelogGenerationForPublishing(this.project, publication)
    }
}
