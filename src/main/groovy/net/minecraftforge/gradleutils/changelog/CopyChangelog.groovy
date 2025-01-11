/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

/** This task copies a changelog file to this project's build directory.*/
@CompileStatic
abstract class CopyChangelog extends DefaultTask {
    public static final String NAME = 'copyChangelog'

    @Inject
    abstract ProjectLayout getLayout()

    CopyChangelog() {
        this.description = 'Copies a changelog file to this project\'s build directory.'

        this.outputFile.convention this.layout.buildDirectory.file('changelog.txt')
    }

    /** The output file for the copied changelog. */
    @OutputFile
    abstract RegularFileProperty getOutputFile()

    /**
     * The configuration (or file collection) containing the changelog to copy. It must be a
     * {@link FileCollection#getSingleFile() single file}.
     *
     * @see ChangelogUtils#findChangelogTask(org.gradle.api.Project)
     */
    @InputFiles
    abstract Property<FileCollection> getConfiguration()

    @TaskAction
    void exec() {
        var input = this.configuration.get().singleFile
        var output = this.outputFile.get().asFile
        if (!output.parentFile.exists())
            output.parentFile.mkdirs()
        output.bytes = input.bytes
    }
}
