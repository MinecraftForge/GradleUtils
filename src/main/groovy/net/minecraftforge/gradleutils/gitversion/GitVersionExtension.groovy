/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.gitversion

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gitver.api.GitVersion
import net.minecraftforge.gitver.api.GitVersionException
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity

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
    @PackageScope static final ProblemGroup PROBLEM_GROUP = ProblemGroup.create(NAME, 'Git Version')

    private final Project project
    private final Problems problems
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
        } catch (GitVersionException e) {
            this.project.logger.warn 'WARNING: Git Version failed to get version numbers! Attempting to use default version 0.0.0'
            this.problems.reporter.report(ProblemId.create('git-version-failed', 'Git Version failed to get version numbers', PROBLEM_GROUP)) { spec ->
                spec.details("""
                Git Version failed to get version numbers from the Git repository.
                It will attempt to substitute any potential version numbers with 0.0.0.""")
                    .severity(Severity.ERROR)
                    .withException(e)
                    .solution('Ensure that you are in a valid Git repository')
                    .solution('Check your GitVersion config file and make sure the correct tag prefix and filters are in use')
                    .solution('Ensure that the tags you are attempting to use exist in the repository')
                    .solution("Ensure that you have read access to the '.git' directory")
            }
            return builder.strict(false).build()
        } catch (IllegalArgumentException e) {
            throw this.problems.reporter.throwing(e, ProblemId.create('misconfigured-git-version', 'Git Version is misconfigured', PROBLEM_GROUP)) { spec ->
                spec.details("""
                Git Version is misconfigured and cannot be used, likely due to incorrect paths being set.
                This is an unrecoverable problem and needs to be addressed in the config file.""")
                    .withException(e)
                    .severity(Severity.ERROR)
                    .solution('Ensure that the correct subprojects and paths are declared in the config file')
            }
        }
    }()

    @Inject
    GitVersionExtension(Project project, Problems problems, ProjectLayout layout) {
        this.project = project
        this.problems = problems
        this.layout = layout
    }
}
