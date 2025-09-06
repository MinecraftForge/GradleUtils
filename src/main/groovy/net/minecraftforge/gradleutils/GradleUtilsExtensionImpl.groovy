/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.flow.FlowProviders
import org.gradle.api.flow.FlowScope
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.TaskProvider
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.initialization.layout.BuildLayout
import org.jetbrains.annotations.Nullable

import javax.inject.Inject

import static net.minecraftforge.gradleutils.GradleUtilsPlugin.LOGGER

@CompileStatic
@PackageScope abstract class GradleUtilsExtensionImpl implements GradleUtilsExtensionInternal {
    private final DirectoryProperty rootDirectory

    private final Property<String> mavenUser
    private final Property<String> mavenPassword
    private final Property<String> mavenUrl

    final PomUtils pom

    protected abstract @Inject ObjectFactory getObjects()
    protected abstract @Inject BuildLayout getBuildLayout()
    protected abstract @Inject ProviderFactory getProviders()

    protected abstract @Inject FlowScope getFlowScope()
    protected abstract @Inject FlowProviders getFlowProviders()

    @Inject
    GradleUtilsExtensionImpl(ExtensionAware target) {
        this.rootDirectory = this.objects.directoryProperty().fileValue(this.buildLayout.rootDirectory)

        this.mavenUser = this.objects.property(String).value(this.providers.environmentVariable('MAVEN_USER')).tap(Util.finalizeProperty())
        this.mavenPassword = this.objects.property(String).value(this.providers.environmentVariable('MAVEN_PASSWORD')).tap(Util.finalizeProperty())
        this.mavenUrl = this.objects.property(String).value(this.providers.environmentVariable('MAVEN_URL').orElse(this.providers.environmentVariable('MAVEN_URL_RELEASE'))).tap(Util.finalizeProperty())

        this.pom = this.objects.newInstance(PomUtilsImpl, target)

        this.flowScope.always(GradleUtilsFlowAction.JavadocLinksClassCheck) {
            it.parameters { parameters ->
                parameters.failure.set(this.flowProviders.buildWorkResult.map { it.failure.orElse(null) })
            }
        }
    }

    @Override
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint) {
        this.getPublishingForgeMaven(fallbackPublishingEndpoint, this.rootDirectory.dir('repo'))
    }

    @Override
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint, Object defaultFolder) {
        this.getPublishingForgeMaven(fallbackPublishingEndpoint, this.providers.provider { defaultFolder })
    }

    @Override
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint, File defaultFolder) {
        this.getPublishingForgeMavenInternal(fallbackPublishingEndpoint, this.providers.provider { defaultFolder })
    }

    @Override
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint, Directory defaultFolder) {
        this.getPublishingForgeMavenInternal(fallbackPublishingEndpoint, this.providers.provider { defaultFolder.asFile.absoluteFile })
    }

    @Override
    Action<MavenArtifactRepository> getPublishingForgeMaven(String fallbackPublishingEndpoint, Provider<?> defaultFolder) {
        this.getPublishingForgeMavenInternal(fallbackPublishingEndpoint, defaultFolder.map {
            if (it instanceof Directory)
                it.asFile
            else if (it instanceof File)
                it
            else
                this.rootDirectory.files(it).singleFile
        }.map(File.&getAbsoluteFile))
    }

    private Action<MavenArtifactRepository> getPublishingForgeMavenInternal(String fallbackPublishingEndpoint, Provider<? extends File> defaultFolder) {
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
                repo.url = defaultFolder.get().absoluteFile.toURI()
            }
        }
    }

    @CompileStatic
    @PackageScope static abstract class ForProjectImpl extends GradleUtilsExtensionImpl implements GradleUtilsExtensionInternal.ForProject {
        private final GradleUtilsProblems problems

        private final Project project

        final Property<String> displayName

        @Inject
        ForProjectImpl(Project project) {
            super(project)
            this.project = project

            this.problems = this.objects.newInstance(GradleUtilsProblems)

            this.displayName = this.objects.property(String)

            project.tasks.register(GenerateActionsWorkflow.NAME, GenerateActionsWorkflowImpl)

            project.afterEvaluate { this.finish(it) }
        }

        private void finish(Project project) {
            if (this.problems.test('net.minecraftforge.gradleutils.publishing.use-base-archives-name')) {
                project.extensions.getByType(PublishingExtension).publications.withType(MavenPublication).configureEach {
                    it.artifactId = project.extensions.getByType(BasePluginExtension).archivesName
                }
            }
        }

        @Override
        TaskProvider<? extends PromotePublication> promote(MavenPublication publication, @Nullable Action<? super PromotePublication> cfg) {
            this.project.tasks.register("promote${publication.name.capitalize()}Publication", PromotePublicationImpl, publication).tap { promote ->
                if (cfg !== null)
                    promote.configure { cfg.execute(it) }

                this.project.tasks.withType(PublishToMavenRepository).configureEach { publish ->
                    // if the publish task's publication isn't this one and the repo name isn't 'forge', skip
                    // the name being 'forge' is enforced by gradle utils
                    if (publish.publication !== publication || publish.repository.name != 'forge')
                        return

                    publish.finalizedBy(promote)
                    promote.get().mustRunAfter(publish)
                }
            }
        }
    }
}
