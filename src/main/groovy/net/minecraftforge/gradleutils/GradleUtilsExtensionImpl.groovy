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
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ProviderFactory
import org.gradle.authentication.http.BasicAuthentication

@CompileStatic
final class GradleUtilsExtensionImpl implements GradleUtilsExtension {
    private final ObjectFactory objects
    private final ProviderFactory providers

    @PackageScope static GradleUtilsExtension create(ExtensionAware target, ObjectFactory objects, ProviderFactory providers) {
        final base = new GradleUtilsExtensionImpl(objects, providers)
        target instanceof Project ? new ForProject(base, target) : base
    }

    private GradleUtilsExtensionImpl(ObjectFactory objects, ProviderFactory providers) {
        this.objects = objects
        this.providers = providers
    }

    @CompileStatic
    static final class ForProject implements GradleUtilsExtension.ForProject {
        // Groovy 3 doesn't have non-sealed, so we are using the base as a delegate for now
        @Delegate GradleUtilsExtensionImpl base
        private final Project project

        final PomUtils pom

        private ForProject(GradleUtilsExtensionImpl base, Project project) {
            this.base = base
            this.project = project

            this.pom = new PomUtilsImpl(this.project, base.providers)

            this.project.tasks.register GenerateActionsWorkflow.NAME, GenerateActionsWorkflow
            this.project.tasks.register ConfigureTeamCity.NAME, ConfigureTeamCity
        }

        @Override
        Closure getPublishingForgeMaven(String fallbackPublishingEndpoint) {
            this.getPublishingForgeMaven(fallbackPublishingEndpoint, this.project.rootProject.file('repo'))
        }

        @Override
        Closure getPublishingForgeMaven(String fallbackPublishingEndpoint, File defaultFolder, File defaultSnapshotFolder) {
            // make properties of what we use so gradle's cache is aware
            final snapshot = this.base.objects.property(Boolean).value this.base.providers.provider {
                this.project.version?.toString()?.endsWith('-SNAPSHOT')
            }

            // collecting all of our environment variables here so gradle's cache is aware
            final mavenUser = this.base.providers.environmentVariable 'MAVEN_USER'
            final mavenPassword = this.base.providers.environmentVariable 'MAVEN_PASSWORD'
            final mavenUrlRelease = this.base.providers.environmentVariable 'MAVEN_URL_RELEASE'
            final mavenUrlSnapshots = this.base.providers.environmentVariable 'MAVEN_URL_SNAPSHOTS'

            { MavenArtifactRepository repo ->
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
                    repo.url = snapshot.getOrElse(false)
                        ? defaultSnapshotFolder.absoluteFile.toURI()
                        : defaultFolder.absoluteFile.toURI()
                }
            }
        }
    }
}
