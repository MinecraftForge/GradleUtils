/*
 * GradleUtils
 * Copyright (C) 2021 Forge Development LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
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
import java.nio.file.Files;
import java.util.function.BiFunction;

public abstract class GenerateChangelogTask extends DefaultTask implements ProviderInternal<MavenArtifact>
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

        if (!startingCommit.isBlank() && !startingTag.isBlank()) {
            throw new IllegalStateException("Both starting commit and tag are supplied to the task: " + getName() + ". Only supply one!");
        }

        String changelog = "";
        if (startingCommit.isBlank() && startingTag.isBlank()) {
            changelog = ChangelogUtils.generateChangelog(getGitDirectory().getAsFile().get(), getProjectUrl().get(), !getBuildMarkdown().get());
        }
        else if (startingCommit.isBlank())  {
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
            Files.writeString(outputFile.toPath(), changelog);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Failed to write changelog to file: " + outputFile.getAbsolutePath());
        }
    }

    @Nullable
    @Override
    public MavenArtifact getOrNull() {
        return new SingleOutputTaskMavenArtifact(
                getProject().getTasks().named(getName()),
                "txt",
                "changelog"
        );
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public MavenArtifact get() {
        return getOrNull();
    }

    @Override
    public MavenArtifact getOrElse(MavenArtifact mavenArtifact) {
        return getOrNull();
    }

    @Override
    public <S> Provider<S> map(Transformer<? extends S, ? super MavenArtifact> transformer) {
        return new TransformBackedProvider<>();
    }

    @Override
    public <S> Provider<S> flatMap(Transformer<? extends Provider<? extends S>, ? super MavenArtifact> transformer) {
        return null;
    }

    @Override
    public Provider<MavenArtifact> orElse(MavenArtifact mavenArtifact) {
        return null;
    }

    @Override
    public Provider<MavenArtifact> orElse(Provider<? extends MavenArtifact> provider) {
        return null;
    }

    @Override
    public Provider<MavenArtifact> forUseAtConfigurationTime() {
        return null;
    }

    @Override
    public <B, R> Provider<R> zip(Provider<B> provider, BiFunction<MavenArtifact, B, R> biFunction) {
        return null;
    }
}
