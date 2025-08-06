/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.authentication.http.BasicAuthentication

import javax.inject.Inject

import static net.minecraftforge.gradleutils.GradleUtilsPlugin.LOGGER

@CompileStatic
@PackageScope abstract class GradleUtilsExtensionImpl implements GradleUtilsExtensionInternal {
    protected abstract @Inject ObjectFactory getObjects()
    protected abstract @Inject ProviderFactory getProviders()

    @Inject
    GradleUtilsExtensionImpl() { }

    @CompileStatic
    @PackageScope static abstract class ForProjectImpl extends GradleUtilsExtensionImpl implements GradleUtilsExtensionInternal.ForProject {
        private final Project project

        final PomUtils pom

        private ForProjectImpl(Project project) {
            this.project = project

            this.pom = this.objects.newInstance(PomUtilsImpl, project)

            project.tasks.register(GenerateActionsWorkflow.NAME, GenerateActionsWorkflowImpl)
            project.tasks.register(ConfigureTeamCity.NAME, ConfigureTeamCity)
        }

        @Override
        Closure getPublishingForgeMaven(String fallbackPublishingEndpoint) {
            this.getPublishingForgeMaven(fallbackPublishingEndpoint, this.project.rootProject.file('repo'))
        }

        @Override
        Closure getPublishingForgeMaven(String fallbackPublishingEndpoint, File defaultFolder, File defaultSnapshotFolder) {
            // make properties of what we use so gradle's cache is aware
            final snapshot = this.objects.property(Boolean).value(this.providers.provider {
                this.project.version?.toString()?.endsWith('-SNAPSHOT')
            })

            // collecting all of our environment variables here so gradle's cache is aware
            final mavenUser = this.providers.environmentVariable('MAVEN_USER')
            final mavenPassword = this.providers.environmentVariable('MAVEN_PASSWORD')
            final mavenUrlRelease = this.providers.environmentVariable('MAVEN_URL').orElse(this.providers.environmentVariable('MAVEN_URL_RELEASE'))
            final mavenUrlSnapshots = this.providers.environmentVariable('MAVEN_URL_SNAPSHOTS')

            return { MavenArtifactRepository repo ->
                repo.name = 'forge'

                if (mavenUser.present && mavenPassword.present) {
                    var publishingEndpoint = mavenUrlRelease.present ? mavenUrlRelease.get() : fallbackPublishingEndpoint

                    repo.url = snapshot.getOrElse(false) && mavenUrlSnapshots.present
                        ? mavenUrlSnapshots.get()
                        : publishingEndpoint

                    repo.authentication { authentication ->
                        authentication.create('basic', BasicAuthentication)
                    }

                    repo.credentials { credentials ->
                        credentials.username = mavenUser.get()
                        credentials.password = mavenPassword.get()
                    }
                } else {
                    LOGGER.info('Forge publishing credentials not found, using local folder')
                    repo.url = snapshot.getOrElse(false)
                        ? defaultSnapshotFolder.absoluteFile.toURI()
                        : defaultFolder.absoluteFile.toURI()
                }
            }
        }
    }
}
