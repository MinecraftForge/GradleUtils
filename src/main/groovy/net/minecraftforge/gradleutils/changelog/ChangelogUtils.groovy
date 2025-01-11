/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import net.minecraftforge.gradleutils.GradleUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin

/** Utility methods for configuring and working with the changelog tasks. */
@CompileStatic
@PackageScope([PackageScopeTarget.CLASS, PackageScopeTarget.METHODS])
class ChangelogUtils {
    /**
     * Adds the createChangelog task to the target project. Also exposes it as a artifact of the 'createChangelog'
     * configuration.
     * <p>
     * This is the
     * <a href="https://docs.gradle.org/current/samples/sample_cross_project_output_sharing.html"> recommended way</a>
     * to share task outputs between multiple projects.
     *
     * @param project Project to add the task to
     * @return The task responsible for generating the changelog
     */
    static TaskProvider<GenerateChangelog> setupChangelogTask(Project project) {
        project.tasks.register(GenerateChangelog.NAME, GenerateChangelog).tap { task ->
            project.configurations.register(GenerateChangelog.NAME) { it.canBeResolved = false }
            project.artifacts.add(GenerateChangelog.NAME, task)
            project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).configure { it.dependsOn(task) }
        }
    }

    /**
     * Sets up the changelog generation on all maven publications in the project.
     * <p>
     * It also sets up publishing for all subprojects as long as that subproject does not have another changelog plugin
     * overriding the propagation.
     *
     * @param project The project to add changelog generation publishing to
     */
    static void setupChangelogGenerationOnAllPublishTasks(Project project) {
        setupChangelogGenerationForAllPublications(project)
        project.subprojects.forEach { sub ->
            sub.afterEvaluate {
                // attempt to get the current subproject's changelog extension
                var ext = sub.extensions.findByType(ChangelogExtension)

                // find the changelog extension for the highest project that has it, if the subproject doesn't
                for (var parent = project; ext == null && parent != null; parent = parent.parent == parent ? null : parent.parent) {
                    ext = parent.extensions.findByType(ChangelogExtension)
                }

                // if the project with changelog is publishing all changelogs, set up changelogs for the subproject
                if (ext != null && ext.publishAll)
                    setupChangelogGenerationForAllPublications(sub)
            }
        }
    }

    private static void setupChangelogGenerationForAllPublications(Project project) {
        var ext = project.extensions.findByName(PublishingExtension.NAME) as PublishingExtension
        if (ext == null) return

        // Get each extension and add the publishing task as a publishing artifact
        ext.publications.withType(MavenPublication).configureEach {
            setupChangelogGenerationForPublishing(project, it)
        }
    }

    private static ChangelogExtension findParent(Project project) {
        var ext = project.extensions.findByType(ChangelogExtension)
        if (ext?.task != null) return ext

        var parent = project.parent == project ? null : project.parent
        return parent == null ? null : findParent(parent)
    }

    /**
     * The recommended way to share task outputs across projects is to export them as dependencies
     * <p>
     * So for any project that doesn't generate the changelog directly, we must create a
     * {@linkplain CopyChangelog copy task} and new configuration
     */
    private static TaskProvider<? extends Task> findChangelogTask(Project project) {
        // See if we've already made the task
        if (project.tasks.names.contains(GenerateChangelog.NAME))
            return project.tasks.named(GenerateChangelog.NAME)

        if (project.tasks.names.contains(CopyChangelog.NAME))
            return project.tasks.named(CopyChangelog.NAME)

        // See if there is any parent with a changelog configured
        var parent = findParent(project)
        if (parent == null) return null

        project.tasks.register(CopyChangelog.NAME, CopyChangelog) {
            var dependency = project.dependencies.project('path': parent.project.path, 'configuration': GenerateChangelog.NAME)
            it.configuration.set project.configurations.detachedConfiguration(dependency).tap { it.canBeConsumed = false }
        }
    }

    /**
     * Sets up the changelog generation on the given maven publication.
     *
     * @param project The project in question
     * @param publication The publication in question
     */
    static void setupChangelogGenerationForPublishing(Project project, MavenPublication publication) {
        GradleUtils.ensureAfterEvaluate(project) {
            setupChangelogGenerationForPublishingAfterEvaluation(it, publication)
        }
    }

    private static void setupChangelogGenerationForPublishingAfterEvaluation(Project project, MavenPublication publication) {
        boolean existing = !publication.artifacts.findAll { MavenArtifact it -> it.classifier == 'changelog' && it.extension == 'txt' }.isEmpty()
        if (existing) return

        // Grab the task
        var task = findChangelogTask(project)

        // Add a new changelog artifact and publish it
        publication.artifact(task.get().outputs.files.singleFile) {
            it.builtBy(task)
            it.classifier = 'changelog'
            it.extension = 'txt'
        }
    }

    private ChangelogUtils() {}
}
