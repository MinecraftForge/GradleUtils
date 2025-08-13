/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gradleutils.shared.SharedUtil
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.initialization.layout.BuildLayout

import javax.inject.Inject

import static net.minecraftforge.gradleutils.GradleUtilsPlugin.LOGGER

@CompileStatic
@PackageScope abstract class GradleUtilsExtensionImpl implements GradleUtilsExtensionInternal {
    protected abstract @Inject ObjectFactory getObjects()
    protected abstract @Inject BuildLayout getBuildLayout()
    protected abstract @Inject ProviderFactory getProviders()

    private final DirectoryProperty rootDirectory

    private final Property<String> mavenUser
    private final Property<String> mavenPassword
    private final Property<String> mavenUrl

    @Inject
    GradleUtilsExtensionImpl() {
        this.rootDirectory = this.objects.directoryProperty().fileValue(this.buildLayout.rootDirectory)

        this.mavenUser = this.objects.property(String).value(this.providers.environmentVariable('MAVEN_USER')).tap(SharedUtil.finalizeProperty())
        this.mavenPassword = this.objects.property(String).value(this.providers.environmentVariable('MAVEN_PASSWORD')).tap(SharedUtil.finalizeProperty())
        this.mavenUrl = this.objects.property(String).value(this.providers.environmentVariable('MAVEN_URL').orElse(this.providers.environmentVariable('MAVEN_URL_RELEASE'))).tap(SharedUtil.finalizeProperty())
    }

    @Override
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint) {
        this.getPublishingForgeMaven(fallbackPublishingEndpoint, this.rootDirectory.dir('repo'))
    }

    @Override
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint, Provider<? extends Directory> defaultFolder) {
        { MavenArtifactRepository repo ->
            repo.name = 'forge'

            if (this.mavenUser.present && this.mavenPassword.present) {
                repo.url = this.mavenUrl.present ? this.mavenUrl.get() : fallbackPublishingEndpoint

                repo.authentication { authentication ->
                    authentication.create('basic', BasicAuthentication)
                }

                repo.credentials { credentials ->
                    credentials.username = this.mavenUser.get()
                    credentials.password = this.mavenPassword.get()
                }
            } else {
                LOGGER.info('Forge publishing credentials not found, using local folder')
                repo.url = defaultFolder.get().asFile.absoluteFile.toURI()
            }
        }
    }

    @CompileStatic
    @PackageScope static abstract class ForProject extends GradleUtilsExtensionImpl implements GradleUtilsExtensionInternal.ForProject {
        final PomUtils pom

        @Inject
        ForProject(Project project) {
            this.pom = this.objects.newInstance(PomUtilsImpl, project)

            project.tasks.register(GenerateActionsWorkflow.NAME, GenerateActionsWorkflowImpl)
        }
    }
}
