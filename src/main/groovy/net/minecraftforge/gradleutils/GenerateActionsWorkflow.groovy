/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.minecraftforge.gradleutils.gitversion.GitVersionExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import javax.inject.Inject
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * This task generates the GitHub Actions workflow file for the project, respecting declared subprojects in Git Version.
 * <p>This can be very useful when creating new projects or subprojects.</p>
 */
@CompileStatic
abstract class GenerateActionsWorkflow extends DefaultTask {
    public static final String NAME = 'generateActionsWorkflow'

    @Inject
    GenerateActionsWorkflow(ProviderFactory providers) {
        this.description = 'Generates the GitHub Actions workflow file for the project, respecting declared subprojects in Git Version.'

        this.projectName.convention providers.provider { this.project.name }
        this.branch.convention providers.provider { this.project.extensions.getByType(GitVersionExtension).info.branch }
        this.localPath.convention this.project.extensions.getByType(GitVersionExtension).projectPath
        this.paths.convention providers.provider { this.project.extensions.getByType(GitVersionExtension).subprojectPaths.get().collect { "!${it}/**".toString() } }
        this.gradleJavaVersion.convention 21
        this.sharedActionsBranch.convention 'v0'

        this.outputFile.convention this.project.rootProject.layout.projectDirectory.file(providers.provider { "build_${this.project.name}.yaml" })
    }

    abstract @OutputFile RegularFileProperty getOutputFile()

    abstract @Input Property<String> getProjectName()
    abstract @Input @Optional Property<String> getBranch()
    abstract @Input @Optional Property<String> getLocalPath()
    abstract @Input @Optional ListProperty<String> getPaths()
    abstract @Input Property<Integer> getGradleJavaVersion()
    abstract @Input Property<String> getSharedActionsBranch()

    @CompileDynamic
    private List<String> getPathsResolved() {
        this.paths.getOrElse(List.of())
    }

    @TaskAction
    void exec() throws IOException {
        var localPath = this.localPath.orNull
        var paths = this.pathsResolved

        var push = [
            'branches': this.branch.getOrElse('master'),
            'paths'   : new ArrayList<String>().tap {
                if (localPath) add(localPath + '/**')

                add('!.github/workflows/**')
                add('!settings.gradle')
                addAll(paths)
            }
        ] as Map<String, ?>

        var taskPrefix = localPath ? ":${localPath}:" : ''
        var with = [
            'java'        : this.gradleJavaVersion.get(),
            'gradle_tasks': "${taskPrefix}check ${taskPrefix}publish".toString()
        ] as Map<String, ?>
        if (localPath) with.put('subproject', localPath)

        Map<String, ?> yaml = [
            'name'       : "Build ${this.projectName.get()}",
            'on'         : ['push': push],
            'permissions': ['contents': 'read'],
            'jobs'       : [
                'build': [
                    'uses'   : "MinecraftForge/SharedActions/.github/workflows/gradle.yml@${this.sharedActionsBranch.get()}".toString(),
                    'with'   : with,
                    'secrets': [
                        'DISCORD_WEBHOOK': '${{ secrets.DISCORD_WEBHOOK }}'
                    ]
                ]
            ]
        ]
        var workflow = new Yaml(
            new DumperOptions().tap {
                explicitStart = false
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                prettyFlow = true
            }
        ).dump(yaml).replace("'on':", 'on:')

        var file = outputFile.asFile.get()
        if (!file.parentFile.exists() && !file.parentFile.mkdirs())
            throw new IllegalStateException('Failed to create directories for output!')

        Files.writeString(
            file.toPath(),
            workflow,
            StandardCharsets.UTF_8
        )
    }
}
