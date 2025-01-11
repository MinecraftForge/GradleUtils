/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

import javax.inject.Inject

/**
 * This task prints the marker lines into the log which configure the pipeline.
 *
 * @deprecated Will be removed once Forge moves off of TeamCity
 */
@CompileStatic
@Deprecated(forRemoval = true)
abstract class ConfigureTeamCity extends DefaultTask {
    public static final String NAME = 'configureTeamCity'

    static TaskProvider<ConfigureTeamCity> register(Project project) {
        register(project, NAME)
    }

    static TaskProvider<ConfigureTeamCity> register(Project project, String name) {
        project.tasks.register(name, ConfigureTeamCity)
    }

    @Inject
    abstract Problems getProblems()

    @Inject
    abstract ProviderFactory getProviders()

    ConfigureTeamCity() {
        this.description = 'Prints the marker lines into the log which configure the pipeline. [deprecated]'
        this.onlyIf('Only runs on TeamCity, so the TEAMCITY_VERSION environment variable must be set.') { System.getenv('TEAMCITY_VERSION') }

        this.version.convention this.providers.provider { this.project.version?.toString() }
    }

    /** The version string to print, usually the {@linkplain Project#getVersion() project version}. */
    @Input
    abstract Property<String> getVersion()

    @TaskAction
    void exec() {
        this.problems.reporter.report(ProblemId.create('teamcity-deprecated', 'Usage of TeamCity is deprecated within Minecraft Forge', GradleUtils.PROBLEM_GROUP)) { spec ->
            spec.details("""
                       Minecraft Forge has been gradually moving off of TeamCity and into GitHub Actions.
                       When the migration is fully complete, this task along with its automatic setup will be removed.""")
                .severity(Severity.WARNING)
                .solution('Stop using TeamCity.')
                .solution('If you still need to use TeamCity, consider using an alternative plugin or making a custom task.')
        }

        this.logger.lifecycle 'Setting project variables and parameters.'
        println "##teamcity[buildNumber '${this.version.get()}']"
        println "##teamcity[setParameter name='env.PUBLISHED_JAVA_ARTIFACT_VERSION' value='${this.version.get()}']"
    }
}
