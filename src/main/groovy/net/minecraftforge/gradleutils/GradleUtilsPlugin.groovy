/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.minecraftforge.gradleutils.changelog.ChangelogPlugin
import net.minecraftforge.gradleutils.gitversion.GitVersionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject
import java.nio.charset.StandardCharsets

/** The entry point for the Gradle Utils plugin. Exists to create the {@linkplain GradleUtilsExtension extension}. */
@CompileStatic
abstract class GradleUtilsPlugin implements Plugin<Project> {
    protected abstract @Inject ProviderFactory getProviders()
    protected abstract @Inject ProjectLayout getProjectLayout()

    @Override
    void apply(Project project) {
        project.plugins.apply(GitVersionPlugin)
        project.plugins.apply(ChangelogPlugin)
        project.extensions.create(GradleUtilsExtension.NAME, GradleUtilsExtension, project)

        project.afterEvaluate { Project it -> this.enhanceVersionsPlugin(it) }
    }

    @CompileDynamic // Groovy 3.0.9 compiler issues
    private void enhanceVersionsPlugin(Project project) {
        project.pluginManager.withPlugin('com.github.ben-manes.versions') {
            project.tasks.withType(DependencyUpdatesTask).configureEach { task ->
                if (!project.gradle.startParameter.offline) {
                    task.logging.captureStandardOutput(LogLevel.LIFECYCLE)
                    task.logging.captureStandardError(LogLevel.WARN)

                    var formatters = task.outputFormatterName?.split(',')?.toUnique()
                    if (formatters !== null) {
                        task.inputs.property('compileClasspathCount', providers.provider {
                            project.configurations.findByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)?.size()
                        }).optional(true)
                        task.inputs.property('runtimeClasspathCount', providers.provider {
                            project.configurations.findByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)?.size()
                        }).optional(true)
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
    }
}
