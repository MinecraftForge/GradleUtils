/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.attributes.plugin.GradlePluginApiVersion
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.flow.FlowProviders
import org.gradle.api.flow.FlowScope
import org.gradle.api.java.archives.Manifest
import org.gradle.api.logging.LogLevel
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.initialization.layout.BuildLayout
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath
import org.gradle.plugins.ide.idea.model.IdeaModule
import org.jetbrains.annotations.Nullable

import javax.inject.Inject
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

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

        try {
            this.flowScope.always(GradleUtilsFlowAction.JavadocLinksClassCheck) {
                it.parameters { parameters ->
                    parameters.failure.set(this.flowProviders.buildWorkResult.map { it.failure.orElse(null) })
                }
            }
        } catch (IllegalStateException ignored) {
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
        private final GradleUtilsProblems problems = this.objects.newInstance(GradleUtilsProblems)

        private final Project project

        final Property<String> displayName = this.objects.property(String)
        final Property<String> vendor = this.objects.property(String)
        private final Property<String> version = this.objects.property(String)

        protected abstract @Inject ProjectLayout getProjectLayout()

        @Inject
        ForProjectImpl(Project project) {
            super(project)
            this.project = project

            this.setup(project)
            project.afterEvaluate { this.finish(it) }
        }

        private void setup(Project project) {
            this.vendor.convention(providers.provider { project.group }.map { it.toString().startsWithIgnoreCase('net.minecraftforge') ? 'Forge Development LLC' : null })
            this.version.set(providers.provider { project.version }.map(Object.&toString))

            project.tasks.register(GenerateActionsWorkflow.NAME, GenerateActionsWorkflowImpl)

            project.pluginManager.withPlugin('publishing') {
                if (this.problems.test('net.minecraftforge.gradleutils.publishing.use-base-archives-name')) {
                    project.extensions.getByType(PublishingExtension).publications.withType(MavenPublication).configureEach {
                        if (it.artifactId == project.name)
                            it.artifactId = project.extensions.getByType(BasePluginExtension).archivesName.get()
                    }
                }
            }
        }

        private void finish(Project project) {
            this.version.finalizeValue()

            project.pluginManager.withPlugin('com.github.ben-manes.versions') {
                project.tasks.withType(DependencyUpdatesTask).configureEach { task ->
                    if (!task.compatibleWithConfigurationCache)
                        task.notCompatibleWithConfigurationCache("The gradle-versions-plugin isn't compatible with the configuration cache")

                    if (!project.gradle.startParameter.offline) {
                        task.logging.captureStandardOutput(LogLevel.LIFECYCLE)
                        task.logging.captureStandardError(LogLevel.WARN)

                        var formatters = task.outputFormatterName?.split(',')?.toUnique()
                        if (formatters !== null) {
                            task.inputs.property('compileClasspathCount', project.configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME).map(DefaultGroovyMethods.&size))
                            task.inputs.property('runtimeClasspathCount', project.configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).map(DefaultGroovyMethods.&size))
                            task.inputs.property('formatters', formatters)
                            for (final formatterType in formatters) {
                                var file = projectLayout.projectDirectory.dir(task.outputDir).file("${task.reportfileName}.txt").asFile
                                task.outputs.file(file).withPropertyName('textOutput')
                            }

                            task.outputFormatter = { Result result ->
                                var textReporter = new EnhancedVersionReporter.PlainTextDependencyReporter(task.project.path, task.revision, task.gradleReleaseChannel)
                                var enhancedResult = new EnhancedVersionReporter.EnhancedResult(result, task.revision)
                                var textOutput = new ByteArrayOutputStream().with { outputStream ->
                                    textReporter.write(outputStream, enhancedResult)
                                    outputStream.toByteArray()
                                }

                                println(new String(textOutput, StandardCharsets.UTF_8))
                                println()

                                for (final formatterType in task.inputs.properties.formatters as String[]) {
                                    switch (formatterType) {
                                        case 'text':
                                            var file = projectLayout.projectDirectory.dir(task.outputDir).file("${task.reportfileName}.txt").asFile
                                            file.bytes = textOutput

                                            task.logger.lifecycle("Generated report file ${projectLayout.projectDirectory.asFile.toPath().relativize(file.toPath())}")
                                            break
                                    }
                                }
                            }
                        }
                    }
                }
            }

            project.pluginManager.withPlugin('java') {
                // Removes local Gradle API dependencies if we are using external alternatives
                // Gradle's core plugins often force these when using 'java-gradle-plugin' or others
                project.extensions.getByType(JavaPluginExtension).sourceSets { SourceSetContainer sourceSets ->
                    final gradleApi = project.dependencies.gradleApi()
                    final gradleTestKit = project.dependencies.gradleTestKit()
                    final localGroovy = project.dependencies.localGroovy()

                    def hasExternalGradleApi = { Set<Configuration> configurations ->
                        for (var configuration in configurations) {
                            if (configuration?.allDependencies?.find {
                                (it.group == 'dev.gradleplugins' || it.group == 'name.remal.gradle-api')
                                    && it.name == 'gradle-api'
                            } !== null) { return true }
                        }

                        return false
                    }

                    def hasExternalGradleTestKit = { Set<Configuration> configurations ->
                        for (var configuration in configurations) {
                            if (configuration?.allDependencies?.find {
                                (it.group == 'dev.gradleplugins' || it.group == 'name.remal.gradle-api')
                                    && it.name == 'gradle-test-kit'
                            } !== null) { return true }
                        }

                        return false
                    }

                    def hasExternalLocalGroovy = { Set<Configuration> configurations ->
                        for (var configuration in configurations) {
                            if (configuration?.allDependencies?.find {
                                it.group == 'name.remal.gradle-api'
                                    && it.name == 'local-groovy'
                            } !== null) { return true }
                        }

                        return false
                    }

                    def processConfigurations = { Set<Configuration> configurations ->
                        if (configurations === null) return

                        if (hasExternalGradleApi(configurations)) {
                            for (var configuration in configurations) {
                                configuration?.withDependencies { dependencies ->
                                    dependencies.remove(gradleApi)
                                    dependencies.remove(localGroovy)
                                }
                            }
                        }

                        if (hasExternalGradleTestKit(configurations)) {
                            for (var configuration in configurations) {
                                configuration?.withDependencies { dependencies ->
                                    dependencies.remove(gradleTestKit)
                                    dependencies.remove(gradleApi)
                                    dependencies.remove(localGroovy)
                                }
                            }
                        }

                        if (hasExternalLocalGroovy(configurations)) {
                            for (var configuration in configurations) {
                                configuration?.withDependencies { dependencies ->
                                    dependencies.remove(localGroovy)
                                }
                            }
                        }

                    }

                    for (var sourceSet in sourceSets) {
                        processConfigurations(project.configurations.findByName(sourceSet.compileClasspathConfigurationName)?.hierarchy)
                        processConfigurations(project.configurations.findByName(sourceSet.runtimeClasspathConfigurationName)?.hierarchy)
                    }
                }
            }

            if (this.problems.test('net.minecraftforge.gradleutils.ide.automatic.sources')) {
                project.extensions.findByType(IdeaModule)?.tap { downloadSources = downloadJavadoc = true }
                project.extensions.findByType(EclipseClasspath)?.tap { downloadSources = downloadJavadoc = true }
            }

            if (this.problems.test('net.minecraftforge.gradleutils.compilation.defaults')) {
                project.tasks.withType(JavaCompile).configureEach { task ->
                    task.options.encoding = 'UTF-8'
                }

                project.tasks.withType(GroovyCompile).configureEach { task ->
                    task.options.encoding = 'UTF-8'
                    task.groovyOptions.optimizationOptions.indy = true
                }

                final windowTitle = "${this.displayName.orElse(this.project.name).get()} ${this.project.version ?: ''}"

                project.tasks.withType(Javadoc).configureEach { task ->
                    task.options { StandardJavadocDocletOptions options ->
                        options.encoding = 'UTF-8'
                        options.windowTitle = windowTitle
                        options.tags 'apiNote:a:API Note:', 'implNote:a:Implementation Note:', 'implSpec:a:Implementation Requirements:'
                    }
                }

                project.tasks.withType(Groovydoc).configureEach { task ->
                    task.windowTitle = windowTitle

                    if (task.enabled && Charset.defaultCharset() !== StandardCharsets.UTF_8) {
                        project.logger.warn('WARNING: Current charset is not UTF-8 but {}! This will affect the output of Groovydoc task {}', Charset.defaultCharset(), task.name)
                        this.problems.reportGroovydocIncorrectCharset(task)
                    }
                }
            }
        }

        @Override
        void manifestDefaults(Manifest manifest, String packageName, Map<? extends CharSequence, ?> additionalEntries) {
            var specificationVersion = providers.provider {
                project.extensions.getByName('gitversion').properties.info?.properties?.tag?.toString()
            }.orElse(this.version)

            var attributes = ([
                'Specification-Title'   : this.displayName,
                'Specification-Vendor'  : this.vendor,
                'Specification-Version' : specificationVersion,
                'Implementation-Title'  : this.displayName,
                'Implementation-Vendor' : this.vendor,
                'Implementation-Version': this.version
            ] as Map<? extends CharSequence, ?>).tap { putAll(additionalEntries) }.with {
                final attributes = new HashMap<String, String>(size())

                forEach { key, value ->
                    var unpacked = this.unpackOrNull(value)
                    if (unpacked !== null)
                        attributes.put(key.toString(), unpacked.toString())
                }

                return attributes
            }

            manifest.attributes(attributes, packageName)
        }

        @Override
        void pluginDevDefaults(ConfigurationContainer configurations, CharSequence gradleVersion) {
            this.pluginDevDefaults(configurations, this.providers.provider { gradleVersion })
        }

        @Override
        void pluginDevDefaults(ConfigurationContainer configurations, Provider<? extends CharSequence> gradleVersion) {
            // Applies the "Gradle Plugin API Version" attribute to configuration
            // This was added in Gradle 7, gives consumers useful errors if they are on an old version
            this.project.configurations.named(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME.&containsIgnoreCase as Spec<String>).configureEach { configuration ->
                configuration.attributes { attributes ->
                    attributes.attributeProvider(
                        GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                        gradleVersion.map { this.objects.named(GradlePluginApiVersion, it.toString()) }
                    )
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
