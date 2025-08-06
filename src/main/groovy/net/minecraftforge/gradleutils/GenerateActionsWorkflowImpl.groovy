/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
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
import java.nio.file.Path

@CompileStatic
@PackageScope abstract class GenerateActionsWorkflowImpl extends DefaultTask implements GenerateActionsWorkflowInternal {
    private static final String DEFAULT_BRANCH = 'master'
    private static final int DEFAULT_GRADLE_JAVA = 17
    private static final String DEFAULT_SHARED_ACTIONS_BRANCH = 'v0'

    private final GradleUtilsProblems problems

    protected abstract @Inject ObjectFactory getObjects()
    protected abstract @Inject ProviderFactory getProviders()
    protected abstract @Inject ProjectLayout getProjectLayout()

    GenerateActionsWorkflowImpl() {
        this.problems = this.objects.newInstance(GradleUtilsProblems)

        this.group = 'Build Setup'
        this.description = 'Generates the GitHub Actions workflow file for the project, respecting declared subprojects in Git Version.'

        final rootDirectory = this.project.rootProject.layout.projectDirectory

        this.projectName.convention(this.providers.provider { this.project.name })
        this.branch.convention(DEFAULT_BRANCH)
        this.localPath.convention(this.providers.provider { getRelativePath(rootDirectory, this.projectLayout.projectDirectory) })
        this.gradleJavaVersion.convention(this.project.extensions.getByType(JavaPluginExtension).toolchain.languageVersion.map { it.canCompileOrRun(DEFAULT_GRADLE_JAVA) ? it.asInt() : DEFAULT_GRADLE_JAVA })
        this.sharedActionsBranch.convention(DEFAULT_SHARED_ACTIONS_BRANCH)

        this.outputFile.convention(rootDirectory.dir('.github/workflows').file(this.projectName.map { "publish_${it}.yaml" }))
        this.gitVersionPresent.convention(providers.provider { this.project.pluginManager.hasPlugin('net.minecraftforge.gitversion') })
    }

    private static String getRelativePath(FileSystemLocation root, FileSystemLocation file) {
        getRelativePath(root.asFile, file.asFile)
    }

    private static String getRelativePath(File root, File file) {
        return root == file ? '' : getRelativePath(root.toPath(), file.toPath())
    }

    private static String getRelativePath(Path root, Path path) {
        return root.relativize(path).toString().replace(root.fileSystem.separator, '/')
    }

    @Override abstract @OutputFile RegularFileProperty getOutputFile()
    protected abstract @Input Property<Boolean> getGitVersionPresent()

    @Override abstract @Input Property<String> getProjectName()
    @Override abstract @Optional @Input Property<String> getBranch()
    @Override abstract @Optional @Input Property<String> getLocalPath()
    @Override abstract @Optional @Input ListProperty<String> getPaths()
    @Override abstract @Input Property<Integer> getGradleJavaVersion()
    @Override abstract @Input Property<String> getSharedActionsBranch()

    @TaskAction
    void exec() throws IOException {
        if (!this.gitVersionPresent.getOrElse(false)) {
            this.logger.warn('WARNING: {} output file will be missing key data. See Problems report for details.', this.name)
            this.problems.ghWorkflowGitVersionMissing(this.name)
        }

        var localPath = this.localPath.orNull
        var paths = this.paths.getOrElse(List.of())

        var push = [
            'branches': this.branch.get(),
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
            throw new IllegalStateException()

        Files.writeString(
            file.toPath(),
            workflow,
            StandardCharsets.UTF_8
        )
    }
}
