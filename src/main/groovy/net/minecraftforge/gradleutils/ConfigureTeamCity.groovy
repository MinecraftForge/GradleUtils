/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

/**
 * This task prints the marker lines into the log which configure the pipeline.
 *
 * @deprecated Will be removed once Forge moves off of TeamCity.
 */
@CompileStatic
@Deprecated(forRemoval = true)
@SuppressWarnings('GrDeprecatedAPIUsage')
abstract class ConfigureTeamCity extends DefaultTask {
    public static final String NAME = 'configureTeamCity'

    @Inject
    ConfigureTeamCity(ProviderFactory providers) {
        this.description = 'Prints the marker lines into the log which configure the pipeline. [deprecated]'
        this.onlyIf/*('Only runs on TeamCity, so the TEAMCITY_VERSION environment variable must be set.')*/ {
            providers.environmentVariable('TEAMCITY_VERSION').present
        }

        this.buildNumber.convention providers.provider { this.project.version?.toString() }
    }

    /** The build number to print, usually the project version. */
    abstract @Input Property<String> getBuildNumber()

    @TaskAction
    void exec() {
        this.logger.warn 'WARNING: Usage of TeamCity is deprecated within Minecraft Forge Minecraft Forge has been gradually moving off of TeamCity and into GitHub Actions. When the migration is fully complete, this task along with its automatic setup will be removed.'

        final buildNumber = this.buildNumber.get()

        this.logger.lifecycle 'Setting project variables and parameters.'
        println "##teamcity[buildNumber '$buildNumber']"
        println "##teamcity[setParameter name='env.PUBLISHED_JAVA_ARTIFACT_VERSION' value='$buildNumber']"
    }
}
