/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class CopyChangelog extends DefaultTask {
    CopyChangelog() {
        getOutputFile().convention(getProject().getLayout().getBuildDirectory().file("changelog.txt"));
    }

    @InputFiles
    abstract Property<Configuration> getConfiguration();

    @OutputFile
    abstract RegularFileProperty getOutputFile();

    @TaskAction
    void exec() {
        var input = getConfiguration().get().getSingleFile()
        var output = getOutputFile().getAsFile().get()
        if (!output.parentFile.exists())
            output.parentFile.mkdirs()
        output.bytes = input.bytes
    }
}
