/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.tasks;

import net.minecraftforge.gradleutils.ChangelogUtils;
import net.minecraftforge.gradleutils.GradleUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Transformer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.provider.ProviderInternal;
import org.gradle.api.internal.provider.TransformBackedProvider;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.internal.artifact.SingleOutputTaskMavenArtifact;
import org.gradle.api.tasks.*;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.BiFunction;

public abstract class GenerateChangelogTask extends DefaultTask
{

    public GenerateChangelogTask()
    {
        super();

        //Setup defaults: Using merge-base based text changelog generation of the local project into build/changelog.txt
        getGitDirectory().convention(getProject().getLayout().getProjectDirectory().dir(".git"));
        getBuildMarkdown().convention(false);
        getOutputFile().convention(getProject().getLayout().getBuildDirectory().file("changelog.txt"));
        getStartingCommit().convention("");
        getStartingTag().convention("");
        getProjectUrl().convention(GradleUtils.buildProjectUrl(getProject().getProjectDir()));
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    public abstract DirectoryProperty getGitDirectory();

    @Input
    public abstract Property<Boolean> getBuildMarkdown();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Input
    public abstract Property<String> getStartingCommit();

    @Input
    public abstract Property<String> getStartingTag();

    @Input
    public abstract Property<String> getProjectUrl();

    @TaskAction
    public void generate() {
        final String startingCommit = getStartingCommit().getOrElse("");
        final String startingTag = getStartingTag().getOrElse("");

        if (!startingCommit.isEmpty() && !startingTag.isEmpty()) {
            throw new IllegalStateException("Both starting commit and tag are supplied to the task: " + getName() + ". Only supply one!");
        }

        String changelog = "";
        if (startingCommit.isEmpty() && startingTag.isEmpty()) {
            changelog = ChangelogUtils.generateChangelog(getGitDirectory().getAsFile().get(), getProjectUrl().get(), !getBuildMarkdown().get());
        }
        else if (startingCommit.isEmpty())  {
            changelog = ChangelogUtils.generateChangelog(getGitDirectory().getAsFile().get(), getProjectUrl().get(), !getBuildMarkdown().get(), startingTag);
        }
        else {
            changelog = ChangelogUtils.generateChangelogFromCommit(getGitDirectory().getAsFile().get(), getProjectUrl().get(), !getBuildMarkdown().get(), startingCommit);
        }

        final File outputFile = getOutputFile().getAsFile().get();
        outputFile.getParentFile().mkdirs();
        if (outputFile.exists()) {
            outputFile.delete();
        }

        try
        {
            Files.write(outputFile.toPath(), changelog.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Failed to write changelog to file: " + outputFile.getAbsolutePath());
        }
    }
}
