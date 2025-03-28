/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gitver.api.GitVersion
import net.minecraftforge.gitver.api.GitVersionException
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

/** This task generates a changelog for the project based on the Git history using Git Version. */
@CompileStatic
abstract class GenerateChangelog extends DefaultTask {
    @PackageScope static final String NAME = 'createChangelog'

    @Inject
    abstract ProjectLayout getLayout()

    @Inject
    abstract ProviderFactory getProviders()

    @Inject
    abstract ObjectFactory getObjects()

    GenerateChangelog() {
        this.description = 'Generates a changelog for the project based on the Git history using Git Version.'

        //Setup defaults: Using merge-base based text changelog generation of the local project into build/changelog.txt
        this.outputFile.convention this.layout.buildDirectory.file('changelog/changelog.txt')

        this.gitDirectory.convention this.objects.directoryProperty().fileProvider(this.providers.provider { GitVersion.findGitRoot(this.layout.projectDirectory.asFile) }).dir('.git')
        this.projectPath.convention this.providers.provider { GitVersion.findRelativePath(this.layout.projectDirectory.asFile) }
        this.buildMarkdown.convention false
    }

    /** The output file for the changelog. */
    @OutputFile
    abstract RegularFileProperty getOutputFile()

    /** The {@code .git} directory to base the Git Version off of. */
    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    abstract DirectoryProperty getGitDirectory()

    /** The path string of the project from the root. Used to configure Git Version without needing to specify the directory itself. */
    @Input
    abstract Property<String> getProjectPath()

    /** The tag (or object ID) to start the changelog from. */
    @Input
    @Optional
    abstract Property<String> getStart()

    /** The project URL to use in the changelog. Will attempt to use {@link GitVersion.Info#getUrl()} if unspecified. */
    @Input
    @Optional
    abstract Property<String> getProjectUrl()

    /** Whether to build the changelog in markdown format. */
    @Input
    abstract Property<Boolean> getBuildMarkdown()

    @TaskAction
    void exec() throws IOException {
        GitVersion.disableSystemConfig()

        var gitDir = this.gitDirectory.asFile.orNull
        try (var version = GitVersion.builder().gitDir(gitDir).project(new File(gitDir.absoluteFile.parentFile, this.projectPath.get())).build()) {
            var changelog = version.generateChangelog(this.start.orNull, this.projectUrl.orNull, !this.buildMarkdown.get())

            var file = outputFile.asFile.get()
            if (!file.parentFile.exists())
                file.parentFile.mkdirs()

            file.setText(changelog, 'UTF8')
        } catch (GitVersionException e) {
            this.logger.error '''ERROR: Failed to generate the changelog for this project, likely due to a misconfiguration.
GitVersion has caught the exception, the details of which are attached to this error.
Check that the correct tags are being used, or updating the tag prefix accordingly.'''
            throw e
        } catch (IOException e) {
            this.logger.error '''ERROR: Changelog was generated successfully, but could not be written to the disk.
Ensure that you have write permissions to the output directory.'''
            throw new RuntimeException(e)
        }
    }
}
